(ns euphony.functions.counters
  (:require [clojure.core.reducers :as r]
            [clojure.math.combinatorics :as c]))

                                        ; REDUCERS

(defn merge-counts
  ([] {})
  ([& counts] (apply merge-with + counts)))

(defn count-items
  ([] {})
  ([counts item] (assoc counts item (inc (get counts item 0)))))

                                        ; COMPOSABLES

(def unroll (r/mapcat identity)) ;; 1-dimension flatten

(defn associations [n]
  (comp (r/map set) (r/mapcat #(c/combinations % n))))

                                        ; MAIN FUNCTIONS

(defn count-flat-items
  "Count items from a flat collection."
  [coll] (r/fold merge-counts count-items coll))

(defn count-nested-items
  "Count items from a list of collections."
  [colls] (count-flat-items (unroll colls)))

(defn count-assocs-items
  "Count n associations from a list of collections."
  [n colls] (count-flat-items ((associations n) colls)))
