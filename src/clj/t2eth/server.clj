(ns t2eth.server
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [t2eth.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [resources]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [net.cgrand.reload :refer [auto-reload]]
            [clojure.core.async :refer [<!! >!! <! >! chan go thread]]
            [ring.middleware.reload :as reload]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [oauth.client :as oauth]
            [twitter :as tw]
            [t2eth.utils :refer [dbg]]
            [clojure.data.json :as js]
            [clj-http.client :as cl]))

(def contract-address "ad96e1c4abd7b668413d617cf5e1b4e867264944")

(def primary-address "9cbc42e64ece50c0d6136402ea6e04d84e1e0d7b")

(def data-format "f7a6604f 626f62736d697468000000000000000000000000000000000000000000000000 00000000000000000000000000000000000000000000000000000000012d591") 

(def configs (edn/read-string (slurp "config.edn")))

(def consumer (oauth/make-consumer (:consumer-key configs)
                                   (:consumer-secret configs)
                                   "https://api.twitter.com/oauth/request_token"
                                   "https://api.twitter.com/oauth/access_token"
                                   "https://api.twitter.com/oauth/authorize"
                                   :hmac-sha1))
(deftemplate page (io/resource "index.html") []
  [:body] (if is-dev? inject-devmode-html identity))

(def status (atom {})) ;will hold twitter handle and transaction id

(def task-channel (chan 10))

(defn geth-call [method & params]
      (:result (js/read-str (:body (cl/post "http://localhost:8545"
                                           {:socket-timeout 1000
                                            :conn-timeout   1000
                                            :body           (js/write-str {:jsonrpc "2.0"
                                                                           :method  method
                                                                           :params  params
                                                                           :id      1})}))
                           :key-fn keyword)))

(defn balance [address]
      (geth-call "eth_getBalance" address "latest"))

(defn send-transaction [& {:keys [from-address to-address value data] :or {from-address primary-address value "0" data null}}]
      (get-call "eth_sendTransaction"
                address
                {:from     from-address
                 :to       to-address
                 :gas      "30400"
                 :gasPrice "0x9184e72a000"
                 :value    value
                 :data     data}))

(defn format-assign-data [screen-name address]
      (str "f7a6604f"
           (apply str
                  (take 32
                        (concat (map (fn [c]
                                         (format "%x" (int c)))
                                     screen-name)
                                (repeat "00"))))
           "000000000000000000000000"
           address))

(defroutes routes
           (resources "/")
           (resources "/react" {:root "react"})
           (GET "/derp" req "deerrppyderp")
           (GET "/approvaluri"
                req
                (oauth/user-approval-uri consumer
                                         (let [request-token (oauth/request-token consumer "http://groundhog.kr2n.com")]
                                              (go (>! task-channel [:add request-token]))
                                              (:oauth_token request-token))))
           (POST "/verify" [oauth-token oauth-verifier]
                 (go (>! task-channel [:verify oauth-token oauth-verifier]))
                 "OK")
           (GET "/status" [oauth-token]
                (pr-str (@status oauth-token)))
           (GET "/balance" []
                (pr-str (balance primary-address)))
           (GET "/assign" [oauth-token address]
                (format-assign-data "bobsmith" "123454364ece50c0d6136402ea6e04d84e1e0d7b")
                #_(go (>! task-channel [:assign oauth-token address])))
           (GET "/*" [] (page)))

(def http-handler
     (if is-dev?
         (reload/wrap-reload (wrap-defaults #'routes api-defaults))
         (wrap-defaults routes api-defaults)))

(defn run-web-server [& [port]]
  (let [port (Integer. (or port (env :port) 80))]
    (print "Starting web server on port" port ".\n")
    (run-jetty http-handler {:port port :join? false})))

(defn run-auto-reload [& [port]]
  (auto-reload *ns*)
  (start-figwheel))

(defn run [& [port]]
  (when is-dev?
    (run-auto-reload))
  (run-web-server port))

(defn -main [& [port]]
  (run port))

(thread (loop [db {}]
              (let [[cmd & args] (<!! task-channel)]
                   (recur (case cmd
                                :add (let [[request-token] args] (let [{:keys [oauth_token]} request-token]
                                                                      (assoc db oauth_token {:request-token request-token})))
                                :verify (let [[oauth-token oauth-verify]          args
                                              {:keys [screen-name request-token]} (db oauth-token)]
                                             (if screen-name
                                                 db
                                                 (let [access-token-response (oauth/access-token consumer request-token oauth-verify)]
                                                      (assoc-in db [oauth-token :screen-name] (:screen-name access-token-response)))))
                                :assign (let [[oauth-token address] args
                                              {:keys [screen-name] (db oauth-token)}]
                                             (if screen-name
                                                 (do (send-transaction (format-assign-data screen-name address) contract-address)
                                                     (assoc-in db [oauth-token :address] address))
                                                 db)))))))
