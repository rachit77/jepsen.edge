(ns jepsen.edge.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :refer :all]
            [knossos.model :as model]
            [slingshot.slingshot :refer [try+]]
            [jepsen.checker :as checker]
            [jepsen.client :as client]
            [jepsen.cli :as cli]
            [jepsen.control :as c]
            [jepsen.db :as db]
            [jepsen.generator :as gen] 
            [jepsen.independent :as independent]
            [jepsen.nemesis :as nemesis]
            [jepsen.tests :as tests] 
            [jepsen.checker.timeline :as timeline]
            [jepsen.nemesis.time :as nt]
            [jepsen.control.util :as cu]
            [jepsen.util :as util :refer [timeout with-retry map-vals]]
            [jepsen.os.debian :as debian] 
            [jepsen.edge.db :as td]
            ))


(defn edge-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency ...), constructs a test map."
  [opts]
  (let [test (merge opts
                    tests/noop-test
                    {:name "edge-test"
                     :os debian/os
                     :pure-generators true})
        dbt (td/db test)
        test-with-db (merge test {:db dbt})]
    test-with-db))
