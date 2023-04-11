(defproject jepsen.edge "0.1.0-SNAPSHOT"
  :description "Jepsen tests for the Polygon Edge Blockchain"
  :url "http://github.com/rachit77/jepsen.edge"
  :license {:name "Apache License, version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "2.4.0"] 
                 [org.clojure/core.typed "0.4.0"]
                 [cheshire "5.7.1"]
                 [slingshot "0.12.2"]
                 [clj-http "3.6.1"]
                 [jepsen "0.2.1"]]
  :jvm-opts ["-Xmx6g"
             "-XX:+CMSParallelRemarkEnabled"
             "-XX:MaxInlineLevel=32"
             "-XX:MaxRecursiveInlineLevel=2"
             "-XX:-OmitStackTraceInFastThrow"
             "-server"]
  :plugins [[lein-cljfmt "0.7.0"]]
  :main jepsen.edge.cli
  :injections [(require 'clojure.core.typed)
               (clojure.core.typed/install)])




