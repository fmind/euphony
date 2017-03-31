(ns euphony.structs.pqueue
  (:require [clojure.data.priority-map :refer [priority-map]]))

                                        ; CONSTRUCTORS

(defn pqueue
  "Construct a priority queue from a list of elements and priorities."
  ([elements priorities] (pqueue (zipmap elements priorities)))
  ([associations] (into (priority-map) associations)))
