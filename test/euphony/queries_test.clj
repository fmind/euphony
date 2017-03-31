(ns euphony.queries-test
  (:require [clojure.test :as t]
            [euphony
             [queries :refer :all]
             [test-helpers :as th]
             [test-system :as ts]]
            [euphony.protocols.conn :as pc]))

(t/use-fixtures :once ts/with-conn-after-import ts/with-conn-after-parse ts/with-conn-after-cluster)

(defonce TRUTHS
  [{:truth/resource "f63256cf4eef0a60fe56989b1474dd9b0b2bb580ce9fd262b18592bf0506f911"
    :truth/name "adwo", :truth/type "adware", :truth/platform "android"}
   {:truth/resource "a9cbe3e3d446cea683c1e72f2994f40024afed1bb1186b27690ff21741046312"
    :truth/name "dowgin", :truth/type "trojan", :truth/platform "linux"}
   {:truth/resource "9da56b0cb31d412a1ed20fb089f5364acf6b7c0a77c1774c202cd8ff6e13a1ad"
    :truth/type "ads"}
   {:truth/resource "a0196e43aaf90bef85b9661ec23037c79d246d94e1100192295079529d529d97",
    :truth/name "singleton:a0196e43", :truth/platform "android"}
   {}])

                                        ; HELPERS

(t/deftest can-construct-reverse-index
  (t/are [index, output] (= (reverse-index index) output)
    {:a 1 :b 2 :c 3}, {1 [:a] 2 [:b] 3 [:c]}
    {:a 1 :b 1 :c 3}, {1 [:b :a] 3 [:c]}))

                                        ; MEMORY QUERIES

(t/deftest can-query-mem>truth->attribute
  (t/is (= (mem>truth->attribute :truth/name TRUTHS)
           {"f63256cf4eef0a60fe56989b1474dd9b0b2bb580ce9fd262b18592bf0506f911" "adwo",
            "a9cbe3e3d446cea683c1e72f2994f40024afed1bb1186b27690ff21741046312" "dowgin",
            "a0196e43aaf90bef85b9661ec23037c79d246d94e1100192295079529d529d97" "singleton:a0196e43"}))
  (t/is (= (mem>truth->attribute :truth/type TRUTHS)
           {"f63256cf4eef0a60fe56989b1474dd9b0b2bb580ce9fd262b18592bf0506f911" "adware",
            "a9cbe3e3d446cea683c1e72f2994f40024afed1bb1186b27690ff21741046312" "trojan",
            "9da56b0cb31d412a1ed20fb089f5364acf6b7c0a77c1774c202cd8ff6e13a1ad" "ads"}))
  (t/is (= (mem>truth->attribute :truth/platform TRUTHS)
           {"f63256cf4eef0a60fe56989b1474dd9b0b2bb580ce9fd262b18592bf0506f911" "android",
            "a9cbe3e3d446cea683c1e72f2994f40024afed1bb1186b27690ff21741046312" "linux",
            "a0196e43aaf90bef85b9661ec23037c79d246d94e1100192295079529d529d97" "android"})))

                                        ; DATABASE QUERIES

(t/deftest can-query-db>word->field
  (let [index (db>word->field (pc/db ts/*conn-after-import*))]
    (t/are [word, output] (= (get index word) output)
      "ads", nil
      "adware", :T
      "androidos", :P
      "dll", :I)))

(t/deftest can-query-db>label->antivirus
  (t/are [label, output] (= (db>label->antivirus label (pc/db ts/*conn-after-import*)) output)
    "ads", []
    "ads.adrd", ["av1"]
    "android.koguo.1", ["av3" "av4"]))

(t/deftest can-query-db>label->attribute
  (t/is (= (db>label->attribute :label/vendor-name (pc/db ts/*conn-after-parse*))
           {"ads.adrd" "adrd"
            "adware.pjapps.a" "pjapps"
            "adware.pjapps.b" "pjapps"
            "andr.adrd" "adrd"
            "android.adrd.1" "adrd"
            "android.koguo.1" "koguo"
            "android.pjapps.1" "pjapps"
            "generic" "generic"
            "trj.dogwin" "dogwin"
            "trojan.dogwin.a" "dogwin"
            "trojan.dogwin.b" "dogwin"
            "trojan.koguo.a" "koguo"}))
  (t/is (= (db>label->attribute :label/vendor-type (pc/db ts/*conn-after-parse*))
           {"ads.adrd" "ads"
            "adware.pjapps.a" "adware"
            "adware.pjapps.b" "adware"
            "trj.dogwin" "trj"
            "trojan.dogwin.a" "trojan"
            "trojan.dogwin.b" "trojan"
            "trojan.koguo.a" "trojan"}))
  (t/is (= (db>label->attribute :label/vendor-platform (pc/db ts/*conn-after-parse*))
           {"andr.adrd" "andr"
            "android.adrd.1" "android"
            "android.koguo.1" "android"
            "android.pjapps.1" "android"})))

(t/deftest can-query-db>result->attribute
  (t/is (= (db>result->attribute :result/cluster-name (pc/db ts/*conn-after-cluster*))
           {"av1++ads.adrd" "adrd"
            "av1++trj.dogwin" "dogwin"
            "av2++adware.pjapps.a" "adrd"
            "av2++adware.pjapps.b" "adrd"
            "av2++trojan.dogwin.a" "dogwin"
            "av2++trojan.dogwin.b" "dogwin"
            "av2++trojan.koguo.a" "dogwin"
            "av3++android.adrd.1" "adrd"
            "av3++android.koguo.1" "dogwin"
            "av3++android.pjapps.1" "pjapps"
            "av4++android.koguo.1" "koguo"
            "av4++trojan.dogwin.a" "dogwin"
            "av5++generic" "generic"}))
  (t/is (= (db>result->attribute :result/cluster-type (pc/db ts/*conn-after-cluster*))
           {"av1++ads.adrd" "adware"
            "av1++trj.dogwin" "trojan"
            "av2++adware.pjapps.a" "adware"
            "av2++adware.pjapps.b" "adware"
            "av2++trojan.dogwin.a" "trojan"
            "av2++trojan.dogwin.b" "trojan"
            "av2++trojan.koguo.a" "trojan"}))
  (t/is (= (db>result->attribute :result/cluster-platform (pc/db ts/*conn-after-cluster*))
           {"av3++android.adrd.1" "android"
            "av3++android.koguo.1" "android"
            "av3++android.pjapps.1" "android"})))

(t/deftest can-query-db>truth->attribute
  (t/is (= (db>truth->attribute :truth/name (pc/db ts/*conn-after-import*))
           {"f1" "dogwin"
            "f2" "dogwin"
            "f3" "dogwin"
            "f4" "adrd"
            "f5" "adrd"}))
  (t/is (= (db>truth->attribute :truth/type (pc/db ts/*conn-after-import*))
           {"f1" "trojan"
            "f2" "trojan"
            "f3" "trojan"
            "f4" "adware"
            "f5" "adware"}))
  (t/is (= (db>truth->attribute :truth/platform (pc/db ts/*conn-after-import*))
           {"f1" "android"
            "f2" "android"
            "f3" "android"
            "f4" "android"
            "f5" "android"})))

(t/deftest can-query-db>result->av_label
  (t/is (= (db>result->av_label :label/vendor-name (pc/db ts/*conn-after-parse*))
           {"av1++ads.adrd" ["av1" "adrd"]
            "av1++trj.dogwin" ["av1" "dogwin"]
            "av2++adware.pjapps.a" ["av2" "pjapps"]
            "av2++adware.pjapps.b" ["av2" "pjapps"]
            "av2++trojan.dogwin.a" ["av2" "dogwin"]
            "av2++trojan.dogwin.b" ["av2" "dogwin"]
            "av2++trojan.koguo.a" ["av2" "koguo"]
            "av3++android.adrd.1" ["av3" "adrd"]
            "av3++android.koguo.1" ["av3" "koguo"]
            "av3++android.pjapps.1" ["av3" "pjapps"]
            "av4++andr.adrd" ["av4" "adrd"]
            "av4++android.koguo.1" ["av4" "koguo"]
            "av4++trojan.dogwin.a" ["av4" "dogwin"]
            "av5++generic" ["av5" "generic"]}))
  (t/is (= (db>result->av_label :label/vendor-type (pc/db ts/*conn-after-parse*))
           {"av1++ads.adrd" ["av1" "ads"]
            "av1++trj.dogwin" ["av1" "trj"]
            "av2++adware.pjapps.a" ["av2" "adware"]
            "av2++adware.pjapps.b" ["av2" "adware"]
            "av2++trojan.dogwin.a" ["av2" "trojan"]
            "av2++trojan.dogwin.b" ["av2" "trojan"]
            "av2++trojan.koguo.a" ["av2" "trojan"]
            "av4++trojan.dogwin.a" ["av4" "trojan"]}))
  (t/is (= (db>result->av_label :label/vendor-platform (pc/db ts/*conn-after-parse*))
           {"av3++android.adrd.1" ["av3" "android"]
            "av3++android.koguo.1" ["av3" "android"]
            "av3++android.pjapps.1" ["av3" "android"]
            "av4++andr.adrd" ["av4" "andr"]
            "av4++android.koguo.1" ["av4" "android"]})))

(t/deftest can-query-db>report->label-results
  (t/is (= (db>report->vendor-results :label/vendor-name (pc/db ts/*conn-after-parse*))
           '{"f1" [["av1" "dogwin"] ["av2" "dogwin"] ["av5" "generic"] ["av3" "koguo"] ["av4" "koguo"]]
             "f2" [["av1" "dogwin"] ["av5" "generic"] ["av2" "koguo"] ("av3" "koguo")]
             "f3" [["av2" "dogwin"] ["av1" "dogwin"]]
             "f4" [["av4" "dogwin"] ["av2" "pjapps"] ["av3" "pjapps"] ["av1" "adrd"] ["av5" "generic"]]
             "f5" [["av2" "pjapps"] ["av1" "adrd"] ["av3" "adrd"] ["av4" "adrd"] ["av5" "generic"]]})))

(t/deftest can-query-db>report->cluster-results
  (t/is (= (db>report->cluster-results :result/cluster-name (pc/db ts/*conn-after-cluster*))
           {"f1" [["av1" "dogwin"] ["av3" "dogwin"] ["av2" "dogwin"] ["av5" "generic"] ["av4" "koguo"]]
            "f2" [["av2" "dogwin"] ["av1" "dogwin"] ["av3" "dogwin"] ["av5" "generic"]]
            "f3" [["av2" "dogwin"] ["av1" "dogwin"]]
            "f4" [["av4" "dogwin"] ["av3" "pjapps"] ["av1" "adrd"] ["av2" "adrd"] ["av5" "generic"]]
            "f5" [["av1" "adrd"] ["av2" "adrd"] ["av3" "adrd"] ["av5" "generic"]]})))

(t/deftest can-query-db>words-with-this-field
  (t/are [field, subset] (th/subset? subset (db>words-with-this-field field (pc/db ts/*conn-initial*)))
    :Z, #{}
    :I, #{"dll" "pak" "gen"}
    :T, #{"trojan" "adware" "worm"}
    :P, #{"androidos" "linux" "win64"}))

(t/deftest can-query-db>labels-with-unknown-fields-pattern
  (t/is (th/set= (db>labels-with-unknown-fields-pattern (pc/db ts/*conn-after-parse*)) #{"dogwin.b" "dogwin"})))

(t/deftest can-query-db>fields-patterns-related-to-label
  (t/are [label, output] (= (db>fields-patterns-related-to-label (pc/db ts/*conn-after-parse*) label) output)
    "ads.adrd", ["T.N"]         ;; match: trojan.dogwin (av1)
    "adware.pjapps.a", ["T.N.I"];; match: android.pjapps.a (av2)
    "andr.adrd", ["P.N"]        ;; match: android.adrd (av4) -> self
    "dogwin.b", ["P.N"]         ;; match: android.adrd (av4) -> wrong!
    "dogwin", []                ;; no match: generic (av5) -> not same av
    "nothin", []                ;; no match: label does not exists
))
