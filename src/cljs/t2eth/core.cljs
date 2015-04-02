(ns t2eth.core
    (:require [om.core :as om :include-macros true]
              [om-tools.dom :as dom :include-macros true]
              [ajax.core :refer [GET]]))

(defonce app-state (atom {:text "Hello Chestnut!"}))

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
