(ns euphony.commands.analyzers-test
  (:require [clojure.test :as t]
            [euphony.commands.analyzers :refer :all]))

(def TRUTHS {1 "A"
             2 "A"
             3 "A"
             4 "B"
             5 "B"
             6 "C"})

(def PROPOS {1 "A"
             2 "B"
             3 "C"
             4 "A"
             5 "B"
             6 "C"})

(def PARSE {"android.ddlight"
            [[:w "android" #{:P}] [:s "."] [:w "ddlight" #{:N}]],
            "elf/backdoor.tyip-"
            [[:w "elf" #{:I :P :N}] [:s "/"] [:w "backdoor" #{:T}] [:s "."] [:w "tyip" #{:I :P :N}] [:s "-"]],
            "trojan.pjapps.bdokty"
            [[:w "trojan" #{:T}] [:s "."] [:w "pjapps" #{:N}] [:s "."] [:w "bdokty" #{:I :P}]],
            "android/basebridge"
            [[:w "android" #{:P}] [:s "/"] [:w "basebridge" #{:N}]],
            "elf/trojan.fccn-10"
            [[:w "elf" #{:I :P :N}] [:s "/"] [:w "trojan" #{:T}] [:s "."]
             [:w "fccn" #{:I :P :N}] [:s "-"] [:w "10" #{:I}]]})

(def CLUSTER {["av1" "l1"] "c1"
              ["av1" "l2"] "c2"
              ["av2" "l1"] "c1"
              ["av2" "la"] "c2"
              ["av3" "la"] "c1"})

                                        ; STATISTICS

(t/deftest can-compute-malware-stats
  (t/is (= (malstats TRUTHS PROPOS)
           [{:resource 1, :truth "A", :label "A", :match? true}
            {:resource 2, :truth "A", :label "B", :match? false}
            {:resource 3, :truth "A", :label "C", :match? false}
            {:resource 4, :truth "B", :label "A", :match? false}
            {:resource 5, :truth "B", :label "B", :match? true}
            {:resource 6, :truth "C", :label "C", :match? true}])))

(t/deftest can-compute-family-stats
  (t/is (= (famstats TRUTHS PROPOS)
           [{:external? true,  :family "A", :family-card 3, :inter 1, :match "C", :match-card 2}
            {:external? true,  :family "B", :family-card 2, :inter 1, :match "B", :match-card 2}
            {:external? true,  :family "C", :family-card 1, :inter 1, :match "C", :match-card 2}
            {:external? false, :family "A", :family-card 2, :inter 1, :match "B", :match-card 2}
            {:external? false, :family "B", :family-card 2, :inter 1, :match "B", :match-card 2}
            {:external? false, :family "C", :family-card 2, :inter 1, :match "C", :match-card 1}])))

                                        ; DATA ANALYSIS

(t/deftest can-analyze-parse
  (t/is (= (analyze-parse PARSE)
           {:labels 5,
            :with-family 3,
            :distinct-seps 3,
            :distinct-words 11,
            :ambiguous-words 4,
            :incomplete-assignments 3})))

(t/deftest can-analyze-cluster
  (t/is (= (analyze-cluster CLUSTER)
           {:antivirus 3
            :vendor-families 3
            :cluster-families 2})))

(t/deftest can-analyze-malstats-output
  (t/is (= (analyze-malstats (malstats TRUTHS PROPOS))
           {:accuracy 1/2})))

(t/deftest can-analyze-famstats-output
  (t/is (= (analyze-famstats (famstats TRUTHS PROPOS))
           {:proposed 3, :expected 3, :precision 1/2, :recall 1/2, :f1 1/2})))
