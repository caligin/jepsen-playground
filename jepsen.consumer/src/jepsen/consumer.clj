(ns jepsen.consumer
  (:gen-class)
  (:require [jepsen.cli :as cli]
            [jepsen.tests :as tests]
            [jepsen.db :as db]
            [jepsen.control :as c]
            [jepsen.generator :as gen]
            [jepsen.client :as client]
            [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [monger.core :as mg]
            [monger.collection :as mc]))


;;;;;;;;;;;;;;;;;;; test target setup
(def logfile "/home/vagrant/demo-consumer.log")

(defn drop-collection [nodes]
  (let [mconn (mg/connect (map mg/server-address nodes) (mg/mongo-options {}))
        mongo   (mg/get-db mconn "democonsumer")]
        (mc/drop mongo "entities")
        (mg/disconnect mconn)))

(defn demo-consumer []
  (reify db/DB
    (setup! [_ test node]
            (c/exec
              :java
              :-jar
              "/home/vagrant/demo-consumer.jar"
              :>>
              logfile
              (c/lit "2>&1")
              (c/lit "&"))
              (Thread/sleep 300))
    (teardown! [_ test node]
      (c/exec
        :pkill
        :java
        (c/lit "||")
        :true)
      (drop-collection (:nodes test)))
    db/LogFiles
    (log-files [_ test node]
      [logfile])))

;;;;;;;;;;;;;;;;;;; client & generators
(defn op-new   [id _ _] {:type :invoke, :f :new, :value id})
(defn op-update   [id _ _] {:type :invoke, :f :update, :value id})
(defn op-terminate [id _ _] {:type :invoke, :f :terminate, :value id})

(defn make-sequence-generator [id]
  (gen/seq
    (map
      (fn [op] (partial op id))
      (list op-new op-update op-update op-update op-update op-update op-terminate))))

(defn make-loads-of-generators []
  (map
    (fn [id] (make-sequence-generator id))
    (range 1 100)))

(def generator (->> (gen/mix (make-loads-of-generators))
                    (gen/stagger 0.1)
                    (gen/clients)
                    (gen/time-limit 10)))

(defn client
  [conn ch]
  (reify client/Client
    (setup! [_ test node]
      (let [conn  (rmq/connect {:host node
                                :username "guest"
                                :password "guest"})]
            (client conn (lch/open conn))))

    (invoke! [this test op]
      (lb/publish ch "democonsumer" "events.for.democonsumer" (str (:value op) ":" (:f op)) {:type "op" :content-type "text/plain"})
      (assoc op :type :ok))

    (teardown! [_ test]
      (rmq/close ch)
      (rmq/close conn))))

(defn client-seed [] (client nil nil))



;;;;;;;;;;;;;;;;;;; main setup
(def keypath (str (java.lang.System/getProperty "user.home") "/.vagrant.d/insecure_private_key"))
(defn consumer-test [opts]
  (merge
    tests/noop-test
    { :nodes ["172.28.128.11" "172.28.128.12" "172.28.128.13"]
      :ssh { :username "vagrant"
             :private-key-path keypath
             :strict-host-key-checking false}
      :db (demo-consumer)
      :client (client-seed)
      :generator generator}))

(defn -main [& args]
  (cli/run!
    (merge
      (cli/single-test-cmd {:test-fn consumer-test})
      (cli/serve-cmd))
    args))
