(ns euphony.commands.clusterer-test
  (:require [clojure.test :as t]
            [euphony.commands.clusterer :refer :all]
            [euphony.structs.graph :as g]
            [euphony.test-helpers :as th]))

(def RESULTS '((["1" "java"] ["2" "java"]    ["3" "java7"]   ["4" "androjava"])
               (["1" "java"] ["2" "jruby"]   ["3" "ruby"]    ["4" "generic"])
               (["1" "java"] ["2" "java"]    ["3" "java8"]   ["4" "androjava"])
               (["1" "c"]    ["2" "csharp"]  ["3" "csharpe"] ["4" "generic"])
               (["1" "c"]    ["2" "csharp"]  ["3" "csharpe"])))

(def GRAPH (results-graph RESULTS))
(def OPTIONS {:threshold 0.055})

                                        ; CONSTRUCTORS

(t/deftest can-construct-results-graph
  (t/is (= (count (g/nodes GRAPH)) 11))
  (t/is (= (count (g/edges GRAPH)) 21))
  (t/is (th/set= (g/nodes GRAPH)
                 #{["1" "c"] ["1" "java"]
                   ["2" "csharp"] ["2" "java"] ["2" "jruby"]
                   ["3" "csharpe"] ["3" "java7"] ["3" "java8"] ["3" "ruby"]
                   ["4" "androjava"] ["4" "generic"]}))
  (t/is (th/set= (g/edges GRAPH)
                 #{[["1" "c"] ["4" "generic"]] [["1" "java"] ["2" "jruby"]]
                   [["1" "java"] ["4" "androjava"]] [["1" "java"] ["4" "generic"]]
                   [["2" "csharp"] ["1" "c"]] [["2" "csharp"] ["3" "csharpe"]] [["2" "csharp"] ["4" "generic"]]
                   [["2" "java"] ["1" "java"]] [["2" "java"] ["3" "java7"]] [["2" "java"] ["4" "androjava"]]
                   [["2" "jruby"] ["4" "generic"]]
                   [["3" "csharpe"] ["1" "c"]] [["3" "csharpe"] ["4" "generic"]] [["3" "java7"] ["1" "java"]]
                   [["3" "java7"] ["4" "androjava"]] [["3" "java8"] ["1" "java"]] [["3" "java8"] ["2" "java"]]
                   [["3" "java8"] ["4" "androjava"]] [["3" "ruby"] ["1" "java"]] [["3" "ruby"] ["2" "jruby"]]
                   [["3" "ruby"] ["4" "generic"]]}))
  (t/is (every? (partial g/weight GRAPH) (g/edges GRAPH))))

                                        ; MAIN FUNCTIONS

(t/deftest can-cluster-results
  (let [mapping (results-clusters GRAPH OPTIONS)]
    (= mapping
       {["1" "c"] "c",
        ["1" "java"] "java",
        ["2" "csharp"] "c",
        ["2" "java"] "java",
        ["2" "jruby"] "jruby",
        ["3" "csharpe"] "c",
        ["3" "java7"] "java",
        ["3" "java8"] "java",
        ["3" "ruby"] "jruby",
        ["4" "androjava"] "java",
        ["4" "generic"] "generic"})))
