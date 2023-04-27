(ns jepsen.edge.db
  (:import (java.lang Process Runtime))
  (:require [clojure.tools.logging :refer :all]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [clojure.pprint :refer [pprint]]
            [slingshot.slingshot :refer [try+]]
            [jepsen.core :as jepsen]
            [jepsen.control :as c]
            [jepsen.db :as db]
            [jepsen.util :as util :refer [timeout with-retry map-vals]]
            [jepsen.control.util :as cu] 
            [jepsen.os.debian :as debian] ))



(def empty-state {:n1 {:address "", :bls "", :node_id ""},
                  :n2 {:address "", :bls "", :node_id ""},
                  :n3 {:address "", :bls "", :node_id ""},
                  :n4 {:address "", :bls "", :node_id ""},
                  :n5 {:address "", :bls "", :node_id ""}})

;;create a atom from empty-state
(def empty-atom (atom empty-state))


(def pat "/root/edge/edge")
(def base-dir "/root/edge")
(def data-dir "data-dir")

;;Process ID while starting the edge nodes
(def edge-init-logfile (str pat "/edge-init.log"))
(def edge-init-pidfile (str pat "/edge-init.pid"))   

(def Edge-genesis-log (str pat "/Edge-genesis.log"))
(def Edge-genesis-pid (str pat "/Edge-genesis.pid"))


(defn install-component!
  "Download and install a Polygon edge component"
  [app opts]
  (let [opt-name (keyword (str app "-url"))
        path (get opts opt-name)]
    (cu/install-archive! path (str base-dir "/" app))))

(defn stop-edge! [test node]
  (c/su (cu/stop-daemon! edge-init-pidfile))
  :stopped)

(defn read-logfile!
  "Reads the contents of the given logfile and prints it to the console."
  [logfile-path]
  (let [data (slurp logfile-path)]
    (println data)))

;;function to execute commands on remote nodes from control nodes
(defn execute-on-node
  [test node]
  (let [ssh-config (c/ssh {:host node
                    :user (:username (:ssh test))
                    :password (:password (:ssh test))
                    :private-key-path (:private-key-path (:ssh test))})]
    
     (c/with-ssh ssh-config
       (do
         (println "Executing code on remote server...")
         (c/ssh* "ls -la")))))


;;create genesis and config files for all nodes
(defn init-edge!
    "init Polygon edge "
    [test node]
     (c/su
       (c/cd pat
         (cu/start-daemon!
            {:logfile edge-init-logfile
             :pidfile edge-init-pidfile
             :chdir   pat}
            "./polygon-edge"
            :secrets
            :init
            :--data-dir data-dir)))
    :initialized)


(defn db
  "Etcd DB for a particular version."
  [opts]
  (reify db/DB
    (setup! [_ test node]

    ;; intall polygon edge binary on all nodes
    (c/su
        (install-component! "edge"  opts))

    (jepsen/synchronize test 240) 


    ;;init all nodes 
    (c/su
        (init-edge! test node))
    (jepsen/synchronize test 240) 
    (Thread/sleep 20000)

;;get info about node id, bls key etc of all the nodes 
;;executed from control node
(let [output (c/su (c/ssh* {:cmd "/root/edge/edge/polygon-edge secrets output --data-dir /root/edge/edge/data-dir --json"}))
      
      json-str (:out output)
      json-data (json/read-str json-str)]
      (swap! empty-atom assoc-in [node] json-data)
)
(jepsen/synchronize test 240)
(Thread/sleep 20000)

;; ;;create genesis commnad string
;; (def  genesis-validator-string (str " --ibft-validator " (get-in @empty-atom ["n1" "address"]) ":" (get-in @empty-atom ["n1" "bls"]) 
;;                                     " --ibft-validator " (get-in @empty-atom ["n2" "address"]) ":" (get-in @empty-atom ["n2" "bls"])
;;                                     " --ibft-validator " (get-in @empty-atom ["n3" "address"]) ":" (get-in @empty-atom ["n3" "bls"])
;;                                     " --ibft-validator " (get-in @empty-atom ["n4" "address"]) ":" (get-in @empty-atom ["n4" "bls"])
;;                                     " --ibft-validator " (get-in @empty-atom ["n5" "address"]) ":" (get-in @empty-atom ["n5" "bls"])))

;; (def genesis-bootnode-string (str " --bootnode " "/ip4/n1/tcp/1478/p2p/" (get-in @empty-atom ["n1" "node_id"])
;;                                   " --bootnode " "/ip4/n2/tcp/1478/p2p/" (get-in @empty-atom ["n2" "node_id"])))

;; (def genesis-command (str pat "/polygon-edge genesis --consensus ibft" genesis-validator-string genesis-bootnode-string))
;; (let [output (c/su (c/ssh* {:cmd "genesis-command"}))] 
;;   (println output))

;;create a ibft validator string of each node
(def val1 (str (get-in @empty-atom ["n1" "address"]) ":" (get-in @empty-atom ["n1" "bls"])))
(def val2 (str (get-in @empty-atom ["n2" "address"]) ":" (get-in @empty-atom ["n2" "bls"])))
(def val3 (str (get-in @empty-atom ["n3" "address"]) ":" (get-in @empty-atom ["n3" "bls"])))
(def val4 (str (get-in @empty-atom ["n4" "address"]) ":" (get-in @empty-atom ["n4" "bls"])))
(def val5 (str (get-in @empty-atom ["n5" "address"]) ":" (get-in @empty-atom ["n5" "bls"])))


;;create boot node string of first 2 nodes
(def bnode1 (str "/ip4/172.23.0.2/tcp/1478/p2p/" (get-in @empty-atom ["n1" "node_id"])))
(def bnode2 (str "/ip4/172.23.0.3/tcp/1478/p2p/" (get-in @empty-atom ["n2" "node_id"])))


;;create genesis file of all the nodes(5 in this case) with specific peers and boot nodes
  (c/su
   (c/cd pat
         (cu/start-daemon!
          {:logfile Edge-genesis-log
           :pidfile Edge-genesis-pid
           :chdir   pat}
          "./polygon-edge"
          :genesis
          :--consensus "ibft"
          :--ibft-validator val1
          :--ibft-validator val2
          :--ibft-validator val3
          :--ibft-validator val4
          :--ibft-validator val5
          :--premine "0xCB7038f9Bd7762a46bBb2A5208B6644e1945cb52:1000000000000"
          :--premine "0xFE4B24A8daC952671564632ff10c51ECe80e7Aa5:1000000000000"
          :--bootnode bnode1
          :--bootnode bnode2
          )))

(Thread/sleep 20000)

;;start all the nodes(5 in this case) of polygon edge present in the cluster
(c/su
 (c/cd pat
       (cu/start-daemon!
        {:logfile edge-init-logfile
         :pidfile edge-init-pidfile
         :chdir   pat}
        "./polygon-edge"
        :server
        :--data-dir "./data-dir"
        :--chain "./genesis.json"
        :--libp2p "0.0.0.0:1478"
        :--grpc-address ":10000"
        :--jsonrpc ":10002"
        :--seal
)))

(Thread/sleep 20000))
               
;;sleep for some time before teardown

    (teardown! [_ test node]
        (info node "tearing down etcd")
        (stop-edge! test node)
        (c/su 
          (c/exec :rm :-rf base-dir)))

;;code for log collection

))
