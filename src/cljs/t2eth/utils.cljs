(ns t2eth.utils
    #_ (:require [clojure.repl :refer [doc]]))

(defn average [& lst]
      (/ (apply + lst) (count lst)))

#_ (defmacro dlet [bindings & body]
             `(let [~@(mapcat (fn [[n v]]
                                  (if (or (vector? n) (map? n))
                                      [n v]
                                      [n v '_ `(println (name '~n) " : " ~v)]))
                              (partition 2 bindings))]
                   ~@body))

(defn dbg [str val]
      (println str "->" val)
      val)
