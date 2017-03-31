(ns euphony.structs.cograph-test
  (:require [clojure.test :as t]
            [euphony.structs
             [cograph :refer :all]
             [graph :as g]]
            [euphony.test-helpers :as th]))

(defonce SEQUENCES-2 '(([1 :A] [2 :B] [3 :C])
                       ([1 :A] [2 :B] [3 :D])
                       ([1 :A] [2 :E] [3 :C])))

(def COGRAPH (cograph SEQUENCES-2))

                                        ; CONSTRUCTORS

(t/deftest can-construct-cograph
  (t/is (th/set= (g/nodes COGRAPH) #{[1 :A] [2 :B] [3 :C] [3 :D] [2 :E]}))
  (t/is (th/set= (g/edges COGRAPH)
                 #{[[1 :A] [3 :C]] [[2 :B] [1 :A]]
                   [[2 :B] [3 :C]] [[2 :E] [1 :A]]
                   [[2 :E] [3 :C]] [[3 :D] [1 :A]]
                   [[3 :D] [2 :B]]})))

                                        ; GETTERS

(t/deftest can-get-occurrence
  (t/are [node, output] (= (occur COGRAPH node) output)
    [1 :A], 3
    [2 :B], 2
    [3 :C], 2
    [3 :D], 1
    [2 :E], 1
    ;; do no exists
    [5 :Z], nil))

(t/deftest can-get-co-occurrence
  (t/are [edge, output] (= (co-occur COGRAPH edge) output)
    [[2 :B] [3 :C]], 1
    [[3 :C] [2 :B]], 1
    [[1 :A] [2 :B]], 2
    [[2 :B] [1 :A]], 2
    ;; same head and tail
    [[1 :A] [1 :A]], nil
    ;; do not exist
    [[2 :E] [3 :D]], nil
    [[3 :D] [2 :E]], nil))
