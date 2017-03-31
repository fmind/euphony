(ns euphony.structs.graph-test
  (:require [clojure.test :as t]
            [euphony.structs.graph :refer :all]
            [euphony.test-helpers :as th]))

(defonce EDGES [[\A \B] [\A \C]
                [\A \D] [\A \E]
                [\B \C] [\C \E]
                [\D \B]])

(def GRAPH (-> (apply graph EDGES)
               (with-nodes-features {:n (fn [g n] (int n))})
               (with-weight (fn [g [h t]] (+ (int h) (int t))))
               (with-edges-features {:e (fn [g [h t]] (* (int h) (int t)))})))

                                        ; CONSTRUCTORS

(t/deftest can-construct-unweighted-graph
  (t/is (th/set= (nodes GRAPH) #{\A \B \C \D \E}))
  (t/is (th/set= (edges GRAPH) #{[\A \B] [\A \C] [\A \D] [\A \E] [\B \C] [\B \D] [\C \E]})))

(t/deftest can-construct-weighted-graph
  (t/are [edge, output] (= (weight GRAPH edge) output)
    [\B \C], 133
    [\C \B], 133
    [\A \B], 131
    [\B \A], 131
    ;; same head and tail
    [\A \A], nil
    ;; edges no not exist
    [\E \D], nil
    [\D \E], nil
    [\X \Y], nil))

                                        ; SORTERS

(t/deftest can-sort-nodes-by-degree
  (t/is (= (sort-nodes-by-degree GRAPH)
           [[\D 2] [\E 2] [\B 3] [\C 3] [\A 4]])))

(t/deftest can-sort-edges-by-weight
  (t/is (= (sort-edges-by-weight GRAPH)
           [[[\A \B] 131]
            [[\A \C] 132]
            [[\A \D] 133]
            [[\B \C] 133]
            [[\A \E] 134]
            [[\B \D] 134]
            [[\C \E] 136]])))

(t/deftest can-sort-nodes-by-attribute
  (t/is (= (sort-nodes-by-attr :n GRAPH)
           [[\A {:n 65}] [\B {:n 66}] [\C {:n 67}] [\D {:n 68}] [\E {:n 69}]])))

(t/deftest can-sort-edges-by-attribute
  (t/is (= (sort-edges-by-attr :e GRAPH)
           [[[\A \B] {:e 4290}]
            [[\A \C] {:e 4355}]
            [[\A \D] {:e 4420}]
            [[\B \C] {:e 4422}]
            [[\A \E] {:e 4485}]
            [[\B \D] {:e 4488}]
            [[\C \E] {:e 4623}]])))

                                        ; SELECTERS

(t/deftest can-select-by-node-degree
  (let [graph (select-node (where-node-degree >= 3) GRAPH)]
    (t/is (th/set= (nodes graph) #{\A \B \C}))
    (t/is (th/set= (edges graph) #{[\A \B] [\A \C] [\B \C]}))))

(t/deftest can-select-by-edge-weight
  (let [graph (select-edge (where-edge-weight = 134) GRAPH)]
    (t/is (th/set= (nodes graph) (nodes GRAPH) #{\A \B \C \D \E}))
    (t/is (th/set= (edges graph) #{[\A \E] [\B \D]}))))

(t/deftest can-select-nodes-by-attribute
  (let [graph (select-node (where-node-attr :n <= 67) GRAPH)]
    (t/is (th/set= (nodes graph) #{\A \B \C}))
    (t/is (th/set= (edges graph) #{[\A \B] [\A \C] [\B \C]}))))

(t/deftest can-select-edges-by-attribute
  (let [graph (select-edge (where-edge-attr :e <= 4400) GRAPH)]
    (t/is (th/set= (nodes graph) (nodes GRAPH) #{\A \B \C \D \E}))
    (t/is (th/set= (edges graph) #{[\A \C] [\A \B]}))))

                                        ; ALGORITHMS

(t/deftest can-prune-graph
  (let [graph (prune GRAPH)]
    (t/is (th/set= (nodes graph) (nodes GRAPH) #{\A \B \C \D \E}))
    (t/is (th/set= (edges graph) #{[\A \B] [\A \C] [\A \D] [\A \E]}))))

(t/deftest can-trim-graph
  (let [graph (trim GRAPH 133)]
    (t/is (th/set= (nodes graph) (nodes GRAPH) #{\A \B \C \D \E}))
    (t/is (th/set= (edges graph) #{[\A \B] [\A \C] [\A \D] [\B \C]}))))

(t/deftest can-cluster-graph
  (t/is (= (cluster GRAPH 132) [[\A \B \C] [\D] [\E]])))
