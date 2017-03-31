(ns euphony.structs.pqueue-test
  (:require [clojure.test :as t]
            [euphony.structs.pqueue :refer :all]))

                                        ; CONSTRUCTORS

(t/deftest can-construct-priority-queue
  (let [q1 (pqueue {:a 5 :b 1 :c 2 :d 4 :e 3})
        q2 (pqueue [:a :b :c :d :e] [5 1 2 4 3])
        q3 (pqueue [[:a 5] [:b 1] [:c 2] [:d 4] [:e 3]])]
    (t/is (= q1 q2 q3 {:b 1 :c 2 :e 3 :d 4 :a 5}))
    (t/is (= (vals q1) (vals q2) (vals q3) [1 2 3 4 5]))
    (t/is (= (keys q1) (keys q2) (keys q3) [:b :c :e :d :a]))))
