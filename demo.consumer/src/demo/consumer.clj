(ns demo.consumer
  (:gen-class)
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:import [com.mongodb MongoOptions ServerAddress]))   


(defn next-state [current-state command]
    ((keyword command)
        ((keyword current-state)
        {:init {:new :in-progress}
         :in-progress {:update :in-progress :terminate :terminated}
         :terminated {}
        })))

(defn load-state [mongo collection id] (if-let [{:keys [state]} (mc/find-map-by-id mongo collection id)]
    state
    :init))

(defn update-state [mongo collection id state] (mc/update mongo collection {:_id id} {:state state} {:upsert true}))

(defn make-message-handler [mongo collection]
  (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
      (let [[id, command] (clojure.string/split (String. payload "UTF-8") #":")]
        (update-state mongo collection id (or (next-state (load-state mongo collection id) command) :b0rk)))))


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
        mongo   (mg/get-db mconn dbname)]
    (println (format "Consumer Connected. Channel id: %d" (.getChannelNumber ch)))
    (le/declare ch xchgname "topic" {:durable true})
    (lq/declare ch qname {:exclusive false :auto-delete true})
    (lq/bind    ch qname xchgname {:routing-key "events.for.*"})
    (lc/subscribe ch qname (make-message-handler mongo collectionname) {:auto-ack true})
    (Thread/sleep 60000)
    (println "Closing")
    (rmq/close ch)
    (rmq/close conn)
    (mg/disconnect mconn)))
