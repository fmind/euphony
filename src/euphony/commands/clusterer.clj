(ns euphony.commands.clusterer
  (:require [euphony.commands.sub.features :as fx]
            [euphony.structs
             [cograph :as cog]
             [graph :as g]]))

                                        ; DEFAULTS

(def THRESHOLD 0.04)

(def FEATURES {:ldistance  fx/ldistance
               :imbalance  fx/imbalance
               :incomplete fx/incomplete})

(def WEIGHTER (fn [cograph edge]
                (let [{:keys [incomplete imbalance ldistance]} (g/attrs cograph edge)]
                  (float (+ incomplete (/ imbalance 10) (/ ldistance 100))))))

                                        ; CONSTRUCTORS

(defn results-graph
  "Construct a graph of antivirus results with the given features and weighter."
  [results-seq & [{:keys [features weighter] :or {features FEATURES weighter WEIGHTER}}]]
  (-> (cog/cograph results-seq) (g/with-edges-features FEATURES) (g/with-weight WEIGHTER)))

                                        ; MAIN FUNCTIONS

(defn results-clusters
  "Cluster a graph of antivirus results into named groups."
  [graph & [{:keys [threshold] :or {threshold THRESHOLD}}]]
  (reduce (fn [mapping nodes] ;; a component/cluster is a set of nodes
            (if (<= (count nodes) 1) ;; assoc singleton with themselves
              (let [[[av label :as node]] nodes] (assoc mapping node label))
              (let [votes (map (fn [[av l :as n]] {l (cog/occur graph n)}) nodes)
                    label (->> votes (apply merge-with +) (apply max-key val) key)]
                (apply assoc mapping (interleave nodes (repeat label))))))
          (hash-map) (g/cluster graph threshold)))
