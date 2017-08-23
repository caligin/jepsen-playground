(ns jepsen.consumer
  (:gen-class)
  (:require [jepsen.cli :as cli]
            [jepsen.tests :as tests]
            [jepsen.db :as db]
            [jepsen.control :as c]))


(def logfile "/home/vagrant/demo-consumer.log")

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
        :true))
    db/LogFiles
    (log-files [_ test node]
      [logfile])))



(def keypath (str (java.lang.System/getProperty "user.home") "/.vagrant.d/insecure_private_key"))
(defn consumer-test [opts]
  (merge
    tests/noop-test
    { :nodes ["172.28.128.11" "172.28.128.12" "172.28.128.13"]
      :ssh { :username "vagrant"
             :private-key-path keypath
             :strict-host-key-checking false}
      :db (demo-consumer)}))

(defn -main [& args]
  (cli/run!
    (merge
      (cli/single-test-cmd {:test-fn consumer-test})
      (cli/serve-cmd))
    args))
