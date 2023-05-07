(ns jepsen.edge.client
  "Client for edge."
  (:refer-clojure :exclude [read])
  (:require
   [clojure.tools.logging :refer :all]
   [jepsen.control :as c]
   [clojure.java.shell :as shell]))

;; (def signer {:private-key "6acc43ba21cfff332106a9318e9ed08c11e7222273419c2c728dbe1d1a9aa032",
;;              :address "0xCB7038f9Bd7762a46bBb2A5208B6644e1945cb52"})

(def web3-path "/jepsen/jepsen/web3")
(def port 10002)
(def sender-private-key "6acc43ba21cfff332106a9318e9ed08c11e7222273419c2c728dbe1d1a9aa032")
(def sender-address "0xCB7038f9Bd7762a46bBb2A5208B6644e1945cb52")
(def recipient-address "0x9927ff21b9bb0eee9b0ee4867ebf9102d12d6ecb")
(defn node-url [node] (str "http://" node ":" port))

;; (defn set-signer [] (reset! cloth/global-signer signer))

;; (defn set-rpc-endpoint
;;   [node]
;;   (reset! cloth.chain/ethereum-rpc (str "http://" node ":" port)))

(defn default-send-tx!
  "Parameterless send tx"
  [node]
  (->
   (let [result (:out (shell/sh
                       "npm" "run" "start" "--" "sendtx"
                       (node-url node) sender-private-key recipient-address
                       "--value" "10"
                       "--gas" "21000"
                       "--nonce" "0"
                       :dir web3-path))]
     (info (str "<default-send-tx> Result: " result)))))

(defn default-get-balance
  "Parameterless get balance"
  [node]
  (->
   (let [result (:out (shell/sh
                       "npm" "run" "start" "--" "balance"
                       (node-url node) recipient-address
                       :dir web3-path))]
     (info (str "<default-get-balance> Result: " result)))))