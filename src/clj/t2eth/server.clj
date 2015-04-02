(ns t2eth.server
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [t2eth.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [net.cgrand.reload :refer [auto-reload]]
            [clojure.core.async :refer [<! >! chan go]]
            [ring.middleware.reload :as reload]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [oauth.client :as oauth]
            [twitter :as tw]))

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

(defroutes routes
           (resources "/")
           (resources "/react" {:root "react"})
           (GET "/derp" req "deerrppyderp")
           (GET "/temp-data" req (pr-str temp-data))
           (GET "/approvaluri"
                req
                (oauth/user-approval-uri consumer
                                         (let [request-token (oauth/request-token consumer "http://t2eth.com/callback")]
                                              (go (>! task-channel [:add (:oauth_token request-token)]))
                                              (:oauth_token request-token))))
           (POST "/verify" [oauth-token oauth-verifier]
                 (let [{cur-oauth-verifier :oauth-verifier :as data} (temp-data oauth-token)]
                      (if (= data {})
                          (do (swap! temp-data assoc-in [oauth-token :oauth-verifier] oauth-verifier)
                              ()))))
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

(go (loop [db {}]
          (let [[cmd & args] (<! task-channel)]
               (case cmd
                     :add (let [[oauth-token] args] (recur (assoc db oauth-token {})))
                     :verify (let [[oauth-token oauth-verify] args] (if (= (db oauth-token) {})
                                                                        (let [access-token-response (oauth/access-token consumer oauth-token oauth-verify)]
                                                                             (tw/with-oauth consumer
                                                                                            (:oauth_token access-token-response)
                                                                                            (:oauth_token_secret access-token-response)
                                                                                            (let [twitter-userid (tw/verify-credentials)]
                                                                                                 (swap! status assoc oauth-token :twitter-userid twitter-userid)
                                                                                                 (recur (assoc db oauth-token twitter-userid)))))
                                                                        (recur db)))))))
