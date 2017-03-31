(ns euphony.functions.metrics
  (:require [clj-fuzzy.metrics :refer [dice]]))

                                        ; SET METRICS

(defn set-granularity
  "Compute the granularity of two sets given their cardinality."
  [a b] {:pre [(pos? (max a b))] :post [(<= 0 % 1)]} (/ (min a b) (max a b)))

(defn set-completeness
  "Compute the completeness of two sets given their cardinality and intersection."
  [a b inter] {:pre [(pos? (min a b))] :post [(<= 0 % 1)]} (/ inter (min a b)))

                                        ; STRING METRICS

(def str-similarity (memoize (fn [a b] {:post [(<= 0 % 1)]} (dice a b))))
