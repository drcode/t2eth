(ns t2eth.main
  (:require [t2eth.core :as core]
            [figwheel.client :as figwheel :include-macros true]
            [cljs.core.async :refer [put!]]
            [weasel.repl :as weasel]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://kr2n.com:7012/figwheel-ws"
  :jsload-callback (fn []
                     (core/main)))

;(weasel/connect "ws://localhost:9001" :verbose true :print #{:repl :console})

(core/main)
