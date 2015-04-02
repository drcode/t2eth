(ns t2eth.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [t2eth.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        't2eth.core-test))
    0
    1))
