(ns euphony.commands.sub.features
  (:require [euphony.functions.metrics :as m]
            [euphony.structs.cograph :as cog]))

                                        ; EDGE FEATURES

(defn ldistance
  "Compute the label distance between two av-labels."
  [cograph [[av-head label-head] [av-tail label-tail] :as edge]] {:post [(<= 0 % 1)]}
  (- 1 (m/str-similarity label-head label-tail)))

(defn imbalance
  "Compute the imbalance between two av-labels (i.e. the ratio between their occurrences)."
  [cograph [head tail :as edge]] {:post [(<= 0 % 1)]}
  (- 1 (m/set-granularity (cog/occur cograph head) (cog/occur cograph tail))))

(defn incomplete
  "Compute the incompleteness between two av-labels (i.e. excluded elements in the smallest set)"
  [cograph [head tail :as edge]] {:post [(<= 0 % 1)]}
  (- 1 (m/set-completeness (cog/occur cograph head)
                           (cog/occur cograph tail)
                           (cog/co-occur cograph edge))))
