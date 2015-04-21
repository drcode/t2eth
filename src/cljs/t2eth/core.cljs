(ns t2eth.core
    (:require-macros [cljs.core.async.macros :refer [go]])                                                           
    (:require [om.core :as om :include-macros true]
              [om-tools.dom :as dom :include-macros true]
              [cljs.core.async :refer [<! >! chan close!]] 
              [ajax.core :refer [GET POST]]
              [t2eth.utils :refer [dbg]]))

(enable-console-print!)

(defn query-params []
      (when-let [[_ oauth-token oauth-verifier] (re-find #"\\?oauth_token=(.+)&oauth_verifier=(.+)" (.-search js/location))]
                {:oauth-token oauth-token
                 :oauth-verifier oauth-verifier}))

(defonce app-state (atom {:oauth (query-params)}))

(defn async-get
      ([uri params]
          (let [c (chan)]
               (GET uri
                    {:handler (fn [response]
                                  (go (>! c response)))
                     :format :url
                     :params  params})
               c))
      ([uri]
         (async-get uri {})))


(defn async-post [uri params]
      (let [c (chan)]
           (POST uri
                {:handler (fn [response]
                              (go (>! c response)))
                 :format :url
                 :keywords? true
                 :params  params})
           c))

(defn timeout [ms]
      (let [c (chan)]
           (js/setTimeout (fn []
                              (close! c))
                          ms)
           c))

(defn main []
  (om/root
    (fn [app owner]
      (reify
        om/IRender
        (render [_]
          (dom/div (dom/p "Register your twitter handle on the ethereum blockchain!")
                   (dom/p "First:")
                   (dom/p {on-click (fn []
                                        (GET "/approvaluri"
                                             {:handler (fn [response]
                                                           (print response)
                                                           (set! js/window.location.href response))}))}
                          (dom/button "Log in to twitter"))
                   (dom/p "Then:")
                   (dom/p "Enter your ethereum address:")
                   (dom/p (dom/button "Link them together"))
                   (dom/p "Our service will register your twitter handle on the ethereum blockchain for free. At that point, any ethereum contract will be able to verify your twitter handle by calling the contract at 0x239743983724. If your preferred ethereum address")
                   ))))
    app-state
    {:target (. js/document (getElementById "app"))}))

(go (if-let [{:keys [oauth-token oauth-verifier]} (dbg "ttt" (query-params))]
            (do (<! (async-post "verify" {:oauth-token oauth-token :oauth-verifier oauth-verifier}))
                (swap! app-state assoc :status "waiting to aquire twitter name...")
                #_(while (not (:twitter-userid (<! (async-get "status" {:oauth-token oauth-token}))))
                       (<! (timeout 5000)))
                #_(print "got twitter handle!"))
            ))
