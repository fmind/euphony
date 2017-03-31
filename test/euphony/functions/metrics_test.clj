(ns euphony.functions.metrics-test
  (:require [clojure.test :as t]
            [euphony.functions.metrics :refer :all]))

                                        ; SET METRICS

(t/deftest can-compute-set-completeness
  (t/is (thrown? AssertionError (set-completeness 0 0 0)))
  (t/are [a b i, output] (= (set-completeness a b i) output)
    5 5 5, 1
    5 3 3, 1
    3 5 3, 1
    5 3 2, 2/3
    3 5 2, 2/3
    5 3 1, 1/3
    3 5 1, 1/3
    5 3 0, 0
    3 5 0, 0
    5 5 0, 0))

(t/deftest can-compute-set-granularity
  (t/is (thrown? AssertionError (set-granularity 0 0)))
  (t/are [a b, output] (= (set-granularity a b) output)
    5 5, 1
    5 3, 3/5
    3 5, 3/5
    5 1, 1/5
    1 5, 1/5
    0 5, 0
    5 0, 0))

                                        ; STRING METRICS

(t/deftest can-compute-string-similarity
  (t/is (= (str-similarity "clojure" "clojure") 1.0))
  (t/is (< 0.0 (str-similarity "clojure" "clojar") (str-similarity "clojure" "closure") 1.0))
  (t/is (= (str-similarity "clojure" "php") 0.0)))
