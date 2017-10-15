(ns demo.consumer
  (:gen-class)
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [monger.core :as mg]
            [monger.collection :as mc]
            [ring.adapter.jetty :as ring-j]
            [ring.middleware.file :as ring-file]
            [clj-http.client :as http]
            [cheshire.core :as json])
  (:import [com.mongodb MongoOptions ServerAddress WriteConcern]))


(defn next-state [current-state command]
    ((keyword command)
        ((keyword current-state)
        {:s1 {:s1to2 :s2
              :stay :s1}
         :s2 {:s2to3 :s3
              :stay :s2}
         :s3 {:s3to1 :s1
              :stay :s3}})))

(defn load-state [mongo collection id] (if-let [{:keys [state]} (mc/find-map-by-id mongo collection id)]
    state
    :s1))

(defn update-state [mongo collection id state] (mc/update mongo collection {:_id id} {:state state} {:upsert true}))

(defn make-message-handler [mongo collection]
  (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
      (let [[id, command] (clojure.string/split (String. payload "UTF-8") #":")]
        (update-state mongo collection id (or (next-state (load-state mongo collection id) command) :b0rk)))))

(defn create-consul-session []
  (:ID
    (json/parse-string
      (:body
        (http/put "http://localhost:8500/v1/session/create" {:body "{\"Name\":\"consumer\",\"TTL\":\"10s\"}"}))
      true)))

(defn start-session-refresher [session]
  (future
    (loop []
      (do
        (Thread/sleep 3000)
        (println (str "refreshing session " session))
        (http/put (str "http://localhost:8500/v1/session/renew/" session))
        (recur)))))

(defn acquire-lock [session]
  (=
    "true"
    (:body
      (http/put (str "http://localhost:8500/v1/kv/service/democonsumer/lock?acquire=" session) {:body "ohai dis lock iz mine"}))))

(defn after-locking-on [session suspended-fn]
  (future
    (println "starting lock acquisition")
    (loop [has-lock false]
      (if has-lock
        (do
          (println "lock acquired, proceeding")
          (suspended-fn))
        (do
          (println "lock acquisition failed, retrying in 1000ms")
          (Thread/sleep 1000)
          (recur (acquire-lock session)))))))

(defn id-from-uri [uri]
  (nth
    (re-matches #"^/([^/]+)/?$" uri)
    1))

(defn make-get-resource [mongo collection]
  (fn [{:keys [uri]}]
    { :status 200
      :headers {"Content-Type" "text/plain"}
      :body (str (load-state mongo collection (id-from-uri uri)))}))

(defn -main
  "Consumer"
  [& args]
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        qname "democonsumer"
        xchgname "democonsumer"
        dbname "democonsumer"
        collectionname "entities"
        mconn (mg/connect)
        mongo   (mg/get-db mconn dbname)
        consul-session (create-consul-session)]
    (mg/set-default-write-concern! WriteConcern/JOURNALED)
    (println (format "Consumer Connected. Channel id: %d" (.getChannelNumber ch)))
    (lb/qos ch 1)
    (le/declare ch xchgname "topic" {:durable true})
    (lq/declare ch qname {:exclusive false :auto-delete false})
    (lq/bind    ch qname xchgname {:routing-key "events.for.*"})
    (start-session-refresher consul-session)
    (after-locking-on
      consul-session
      (partial lc/subscribe ch qname (make-message-handler mongo collectionname) {:auto-ack true}))
    (ring-j/run-jetty (make-get-resource mongo collectionname) {:port 8888})
    (println "Closing")
    (rmq/close ch)
    (rmq/close conn)
    (mg/disconnect mconn)))
