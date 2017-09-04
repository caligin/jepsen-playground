(ns jepsen.consumer
  (:gen-class)
  (:require [jepsen.cli :as cli]
            [jepsen.tests :as tests]
            [jepsen.db :as db]
            [jepsen.control :as c]
            [jepsen.generator :as gen]
            [jepsen.client :as client]
            [jepsen.checker :as checker]
            [knossos.model :as model]
            [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [monger.core :as mg]
            [monger.collection :as mc]
            [clj-http.client :as http])
  (:import knossos.model.Model))


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
              (Thread/sleep 3000))
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
(def fsm {:s1 {:s1to2 :s2
              :stay :s1}
         :s2 {:s2to3 :s3
              :stay :s2}
         :s3 {:s3to1 :s1
              :stay :s3}})
(defn apply-fsm [fsm state transition]
  ((keyword transition)
    ((keyword state)
      fsm)))

(defn generate-next-transition [[state last-transition]]
  (let [next-transition (nth
                  (keys (state fsm))
                  (rand-int (count (keys (state fsm)))))]
    [(apply-fsm fsm state next-transition) next-transition]))

(defn state-generator []
  (let [state (atom [:s1 nil])]
    (reify gen/Generator
      (op [gen test process]
        (let [[_ next-transition] (swap! state generate-next-transition)]
          next-transition)))))

(defn op-read [test process] {:type :invoke, :f :read, :value nil})
(defn op-write [transition-gen test process] {:type :invoke, :f :write, :value (gen/op transition-gen test process)})

(def generator (->> (gen/mix [op-read (partial op-write (state-generator))])
                    ; (gen/stagger 0.1)
                    (gen/clients)
                    (gen/time-limit 10)))

(defn client
  [conn ch reader]
  (reify client/Client
    (setup! [_ test node]
      (let [conn  (rmq/connect {:host node
                                :username "guest"
                                :password "guest"})
            reader (partial http/get (str "http://" node ":8888/1"))]
            (client conn (lch/open conn) reader)))

    (invoke! [this test op]
      (case (:f op)
        :read (assoc op :type :ok :value (keyword (:body (reader))))
        :write (do
          (lb/publish ch "democonsumer" "events.for.democonsumer" (str "1:" (:value op)) {:type "op" :content-type "text/plain"})
          (assoc op :type :ok))
        (throw (IllegalArgumentException. (str "invalid op " op)))))

    (teardown! [_ test]
      (rmq/close ch)
      (rmq/close conn))))

(defn client-seed [] (client nil nil nil))

;;;;;;;;;;;;;;;;;;; model
(def inconsistent model/inconsistent)

(defrecord FSMRegister [value]
  Model
  (step [r op]
    (condp = (:f op)
      :write (if-let [next-state (apply-fsm fsm value (:value op))]
               (FSMRegister. next-state)
               (inconsistent (str "invalid write detected, trying to apply transition " (:value op) "on top of state " value)))
      :read  (if (= :b0rk (:value op))
                (inconsistent "b0rk detected")
                r)))
  Object
  (toString [r] (pr-str value)))

(defn fsm-register
  ([] (FSMRegister. :s1)))

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
      :model  (fsm-register)
      :checker checker/linearizable
      :generator generator}))

(defn -main [& args]
  (cli/run!
    (merge
      (cli/single-test-cmd {:test-fn consumer-test})
      (cli/serve-cmd))
    args))
