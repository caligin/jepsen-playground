(defproject demo.consumer "0.1.0-SNAPSHOT"
  :description "a demo rabbit consumer we want to test for distributed properties"
  :main demo.consumer
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.novemberain/langohr "3.6.1"]
                 [com.novemberain/monger "3.1.0"]
                 [ring/ring-core "1.6.2"]
                 [ring/ring-jetty-adapter "1.6.2"]
                 [clj-http "3.7.0"]
                 [cheshire "5.8.0"]])
