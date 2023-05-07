(ns jepsen.edge.cli
  (:require [jepsen.cli :as jc]
            [clojure.pprint :refer [pprint]]
            [jepsen.tests :as tests]
            [jepsen.edge.core :as core]))

(def opts
  "Extra command line opts."
[[nil "--workload WORKLOAD" "Test workload to run; e.g. edge"
    :default :edge
    :parse-fn keyword]
   (jc/package-opt "edge-url" "https://github.com/0xPolygon/polygon-edge/releases/download/v0.6.3/polygon-edge_0.6.3_linux_amd64.tar.gz")])
                                   
(defn -main
  "Handles command line arguments. And run a test."
  [& args]
  (jc/run! (jc/single-test-cmd {:test-fn core/edge-test
                                  :opt-spec opts})
            args))



