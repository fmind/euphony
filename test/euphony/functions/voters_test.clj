(ns euphony.functions.voters-test
  (:require [clojure.test :as t]
            [euphony.functions.voters :refer :all]))

(defonce INDEX {"f1" ["dogwin", "dogwin",  "dogwin", "generic", "koguo"]
                "f2" ["dogwin", "dogwin",  "dogwin", "generic", ]
                "f3" ["dogwin", "generic", ]
                "f4" ["dogwin", "pjapps",  "adrd",   "adrd",    "generic"]
                "f5" ["adrd",   "adrd",    "adrd",   "generic", ]})

                                        ; MAIN FUNCTIONS

(t/deftest can-vote-from-index
  (t/is (= (vote INDEX)
           {"f1" {"dogwin" 3, "generic" 1, "koguo" 1},
            "f2" {"dogwin" 3, "generic" 1},
            "f3" {"dogwin" 1,  "generic" 1},
            "f4" {"dogwin" 1, "pjapps" 1, "adrd" 2, "generic" 1},
            "f5" {"adrd" 3, "generic" 1}})))

(t/deftest can-elect-from-index
  (t/is (= (vote-and-elect INDEX)
           {"f1" "dogwin",
            "f2" "dogwin",
            "f3" "dogwin",
            "f4" "adrd",
            "f5" "adrd"})))
