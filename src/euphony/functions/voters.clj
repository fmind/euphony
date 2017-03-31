(ns euphony.functions.voters
  (:require [clojure.core.reducers :as r]
            [medley.core :as m]))

                                        ; REDUCERS

(defn merge-votes
  ([] {})
  ([& votes] (apply merge-with + votes)))

(defn merge-indexes
  ([] {})
  ([& indexes] (apply merge indexes)))

(defn index-votes
  ([] {})
  ([index entry items] (assoc index entry (frequencies items))))

(defn index-elected [global-votes]
  (fn ([] {})
      ([index entry local-votes]
      (let [[_ valmax] (apply max-key val local-votes)
            short-list (->> (m/filter-vals #(= % valmax) local-votes) keys)]
        (assoc index entry
                (if (= (count short-list) 1) (first short-list)
                    (->> short-list (sort-by #(get global-votes %)) last)))))))

                                        ; MAIN FUNCTIONS

(defn vote
  "Compute the frequency of items per index entry."
  [index] (r/fold merge-indexes index-votes index))

(defn elect
  "Select the most occurring item per entry through majority voting.
   When there is no majority, select the most occurring item globally."
  [votes-index]
  (let [global-votes (r/fold merge-votes (vals votes-index))]
    (r/fold merge-indexes (index-elected global-votes) votes-index)))

(def vote-and-elect (comp elect vote))
