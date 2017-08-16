(ns jepsen.consumer
  (:gen-class)
  (:require [jepsen.cli :as cli]
            [jepsen.tests :as tests]))

(def keypath (str (java.lang.System/getProperty "user.home") "/.vagrant.d/insecure_private_key"))
(defn consumer-test [opts]
  (merge
    tests/noop-test
    { :nodes ["172.28.128.11" "172.28.128.12" "172.28.128.13"]
      :ssh { :username "vagrant"
             :private-key-path keypath
             :strict-host-key-checking false}}))

(defn -main [& args]
  (println keypath)
  (cli/run! 
    (cli/single-test-cmd {:test-fn consumer-test})
    args))
