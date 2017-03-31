(ns euphony.functions.counters-test
  (:require [clojure.test :as t]
            [euphony.functions.counters :refer :all]))

(def SCALARS-1D '("A" "B" "C" "A" "B" "A"))

(def VECTORS-1D '([1 "A"] [2 "B"] [3 "C"] [1 "A"] [2 "B"] [1 "A"]))

(def SCALARS-2D '(["A" "B" "C"]
                  ["A" "B"]
                  ["A"]))

(def VECTORS-2D '([[1 "A"] [2 "B"] [3 "C"]]
                  [[1 "A"] [2 "B"]]
                  [[1 "A"]]))

                                        ; MAIN FUNCTIONS

(t/deftest can-count-flat-items
  (t/is (= (count-flat-items SCALARS-1D) {"A" 3, "B" 2, "C" 1}))
  (t/is (= (count-flat-items VECTORS-1D) {[1 "A"] 3, [2 "B"] 2, [3 "C"] 1})))

(t/deftest can-count-nested-items
  (t/is (= (count-nested-items SCALARS-2D)
           {"A" 3, "B" 2, "C" 1}))
  (t/is (= (count-nested-items VECTORS-2D)
           {[1 "A"] 3, [2 "B"] 2, [3 "C"] 1})))

(t/deftest can-count-assocs-items
  (t/is (= (count-assocs-items 2 SCALARS-2D)
           {#{"B" "A"} 2
            #{"C" "A"} 1
            #{"C" "B"} 1}))
  (t/is (= (count-assocs-items 2 VECTORS-2D)
           {#{[2 "B"] [1 "A"]} 2
            #{[1 "A"] [3 "C"]} 1
            #{[2 "B"] [3 "C"]} 1})))
