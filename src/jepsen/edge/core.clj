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
            [jepsen.edge.client :as ec]))

(defn balance   [] {:type :invoke, :f :get_balance,  :value nil})
(defn tx   [] {:type :invoke, :f :send_transaction, :value nil})

(defrecord EdgeClient [node]
  client/Client

  (setup! [this test])

  (open! [_ test node]
    (EdgeClient. node))

  (invoke! [_ test op]
    (let [[k v] (:value op)
          crash (if (= (:f op) :get_balance)
                  :fail
                  :info)]
      (case (:f op)
        :get_balance  (assoc op
                             :type :ok
                             :value (independent/tuple k (ec/default-get-balance node)))
        :send_transaction (do (ec/default-send-tx! node)
                              (assoc op :type :ok)))
      ;; (try+


      ;;  (catch [] e
      ;;    (assoc op :type :fail)))
      ))

  (teardown! [_ test])

  (close! [_ test]))

(defn workload
  "Given a test map, computes

      {:generator a generator of client ops
       :client    a client to execute those ops}."
  [test]
  (let [n (count (:nodes test))]
    (case (:workload test)
      :edge {:client    (EdgeClient. nil)
             :concurrency 1
             :generator (independent/concurrent-generator
                         1
                         (range 0 1 1)
                         (fn [k]
                           (->> (gen/once (tx))
                                (gen/stagger 1)
                                (gen/limit 1))))
             :final-generator (delay
                                (independent/concurrent-generator
                                 1
                                 (range 0 1 1)
                                 (fn [k]
                                   (->> (gen/once (balance))
                                        (gen/stagger 1)
                                        (gen/limit 1)))))})))

(defn mychecker [node]
  (reify checker/Checker
    (check [this test history opts]
      ;; Your validation logic goes here 
      (let [balance (ec/default-get-balance node)
            is-valid (> (Integer/parseInt balance) 0)]
        {:valid? is-valid}))))

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
        workload (workload test)
        node (first (:nodes opts))
        test-with-db (merge test {:db dbt
                                  :client     (:client workload)
                                  :concurrency     (:concurrency workload)
                                  :checker (mychecker node)
                                  :generator  (gen/phases
                                               (->> (:generator workload)
                                                    (gen/time-limit (:time-limit opts)))
                                               (gen/sleep 10)
                                               (gen/clients
                                                (:final-generator workload)))})]
    test-with-db))