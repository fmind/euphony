(ns euphony.structs.cograph
  (:require [euphony.functions.counters :as c]
            [euphony.structs.graph :as g]))

                                        ; CONSTRUCTORS

(defn cograph
  "Construct a co-occurrence graph from a list of sequences."
  [sequences]
  (let [occurrences (future (c/count-nested-items sequences))
        co-occurrences (c/count-assocs-items 2 sequences)]
    ;; co-occurrences keys are sets, graph edges are vecs
    (-> (apply g/graph (map (comp vec key) co-occurrences))
        (g/with-edges-features {:co-occur (fn [graph edge] (get co-occurrences (set edge)))})
        (g/with-nodes-features {:occur (fn [graph node] (get @occurrences node))}))))

                                        ; ATTRIBUTES

(def occur (partial g/node-attr :occur))
(def co-occur (partial g/edge-attr :co-occur))
