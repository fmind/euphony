(ns euphony.structs.graph
  (:require [loom
             [alg :as Alg]
             [attr :as Attr]
             [graph :as Graph]
             [io :as Io]]))

                                        ; ALIASES

;; on graph
(def nodes Graph/nodes)
(def edges Alg/distinct-edges)
(def has-node? Graph/has-node?)
(def has-edge? Graph/has-edge?)
(def remove-nodes Graph/remove-nodes)
(def remove-edges Graph/remove-edges)
(def components Alg/connected-components)

;; on node
(def node-edges Graph/out-edges)
(def node-degree Graph/out-degree)

;; on edge
(def src Graph/src)
(def dest Graph/dest)
(def weight Graph/weight)

;; on attribute
(def attrs Attr/attrs)
(def add-attr Attr/add-attr)

(defn node-attr
  "Get the attribute value of a node."
  [attribute graph node]
  (when (has-node? graph node)
    (Attr/attr graph node attribute)))

(defn edge-attr
  "Get the attribute value of an edge."
  [attribute graph [head tail :as edge]]
  (when (has-edge? graph head tail)
    (Attr/attr graph head tail attribute)))

;; statistics
(def loners Alg/loners)
(def density Alg/density)

;; visualization
(def view Io/view)

                                        ; CONSTRUCTORS

(def graph Graph/graph)

(defn- with-features
  "Template function to add element features as element attributes."
  [elements graph features]
  (reduce (fn [outer-graph element]
            (reduce (fn [inner-graph [feature-name feature-fn]]
                      (add-attr inner-graph element feature-name
                                (feature-fn inner-graph element)))
                    outer-graph features))
          graph (elements graph)))

(def with-nodes-features (partial with-features nodes))
(def with-edges-features (partial with-features edges))

(defn with-same-attrs
  "Copy the attributes from a source graph to another graph."
  [source graph] (assoc graph :attrs (:attrs source)))

(defn with-weight
  "Add a weight to each graph edge using a weight function."
  [graph weight-fn]
  ;; the weight of Loom graph cannot be changed. Thus, we need to copy attrs values.
  (letfn [(weighted-edge [graph f [head tail :as edge]] [head tail (f graph edge)])]
    (->> (edges graph)
         (map (partial weighted-edge graph weight-fn))
         (apply Graph/weighted-graph) (with-same-attrs graph))))

                                        ; SORTERS

(defn sort-nodes-by-degree
  "Sort graph nodes by degree."
  [graph]
  (let [self-and-node-degree (juxt identity (partial node-degree graph))]
    (->> (nodes graph) (map self-and-node-degree) (sort-by second))))

(defn sort-edges-by-weight
  "Sort graph edges by weight."
  [graph]
  (let [self-and-edge-weight (juxt identity (partial weight graph))]
    (->> (edges graph) (map self-and-edge-weight) (sort-by second))))

(defn- sort-elements-by-attribute
  "Template function that returns elements sorted by the given attribute."
  [elements attribute graph]
  (let [self-and-attributes (juxt identity (partial attrs graph))]
    (->> (elements graph) (map self-and-attributes) (sort-by (comp attribute second)))))

(def sort-nodes-by-attr (partial sort-elements-by-attribute nodes))
(def sort-edges-by-attr (partial sort-elements-by-attribute edges))

                                        ; SELECTORS

(defn where-node-degree
  "Build a selector based on node degree."
  [pred value]
  (fn [graph node]
    (pred (node-degree graph node) value)))

(defn where-edge-weight
  "Build a selector based on edge weight."
  [pred value]
  (fn [graph edge]
    (pred (weight graph edge) value)))

(defn where-node-attr
  "Build a selector based on node attribute."
  [attribute pred value]
  (fn [graph node]
    (pred (node-attr attribute graph node) value)))

(defn where-edge-attr
  "Build a selector based on edge attribute."
  [attribute pred value]
  (fn [graph edge]
    (pred (edge-attr attribute graph edge) value)))

                                        ; SELECTERS

(defn select-node
  "Keep nodes where: (pred graph node) is true."
  [pred graph]
  (->> (nodes graph)
       (remove (partial pred graph))
       (apply remove-nodes graph)))

(defn select-edge
  "Keep edges where: (pred graph edge) is true."
  [pred graph]
  (->> (edges graph)
       (remove (partial pred graph))
       (apply remove-edges graph)))

                                        ; ALGORITHMS

(defn prune
  "Transform a graph into a Minimum Spanning Tree (MST)."
  [graph] (with-same-attrs graph (Alg/prim-mst graph)))

(defn trim
  "Divide a graph into sub-graphs based on its edge weights."
  [graph threshold] (select-edge (where-edge-weight <= threshold) graph))

(defn cluster
  "Cluster a graph into components based on a weight threshold."
  [graph threshold] (-> graph prune (trim threshold) components))
