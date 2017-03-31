(ns euphony.commands.analyzers
  (:require [clojure.set :as Set]
            [euphony.structs.label :as l]
            [medley.core :as m]))

                                        ; HELPERS

(defn index->clusters
  "Convert an index of: resource -> family to a cluster of: family -> [resource ...]."
  [index] (->> (group-by val index) (m/map-vals (partial map first)) (m/map-vals set)))

(defn humanize
  [index] (m/map-vals (fn [v] (if (ratio? v) (float v) v)) index))

                                        ; STATISTICS

(defn malstats [truths proposed]
  (for [[resource truth] truths
        :let [label (get proposed resource)
              match? (= label truth)]]
    {:resource resource, :truth truth,
     :label label, :match? match?}))

(defn famstats [truths proposed]
  (letfn [(max-cluster [external? reference [family files]]
            (let [intersections (m/map-vals (comp count (partial Set/intersection files)) reference)
                  [match inter] (apply max-key val intersections), matchfiles (get reference match)]
              {:external? external?, :family family, :family-card (count files)
               :inter inter :match match :match-card (count matchfiles)}))]
    (let [truths-clusters (index->clusters truths), proposed-clusters (index->clusters proposed)]
      (concat (for [c truths-clusters] (max-cluster true proposed-clusters c))
              (for [c proposed-clusters] (max-cluster false truths-clusters c))))))

                                        ; DATA ANALYSIS

(defn analyze-parse [output]
  (let [assignments (vals output)
        tokens (apply concat assignments)
        seps (->> tokens (filter l/token-sep?))
        words (->> tokens (filter l/token-word?))
        ambiguous (->> words (filter l/token-has-many-fields?))
        with-family (->> assignments (filter (fn [ts] (some #(l/token-is-this-field? :N %) ts))))]
    {:labels (count output)
     :with-family (count with-family)
     :distinct-seps (->> seps (map l/token-text) distinct count)
     :distinct-words (->> words (map l/token-text) distinct count)
     :ambiguous-words (->> ambiguous (map l/token-text) distinct count)
     :incomplete-assignments (->> assignments (remove l/tokens-assignment-complete?) count)}))

(defn analyze-cluster [output]
  (let [results (keys output)
        antivirus (->> results (map l/av) distinct)
        cluster-families (->> (vals output) distinct)
        vendor-families (->> results (map l/label) distinct)]
    {:antivirus (count antivirus)
     :vendor-families (count vendor-families)
     :cluster-families (count cluster-families)}))

(defn analyze-malstats [output]
  {:accuracy (/ (count (filter :match? output))
                (count output))})

(defn analyze-famstats [output]
  (let [{externals true, internals false} (group-by :external? output)
        prec (/ (reduce + (map :inter internals))
                (reduce + (map :family-card internals)))
        rec (/ (reduce + (map :inter externals))
               (reduce + (map :family-card externals)))
        f1 (/ (* 2 prec rec) (+ prec rec))]
    {:proposed (count internals) :expected (count externals)
     :precision prec :recall rec :f1 f1}))
