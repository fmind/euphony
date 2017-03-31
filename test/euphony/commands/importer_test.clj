(ns euphony.commands.importer-test
  (:require [clojure.test :as t]
            [euphony.commands.importer :refer :all]
            [euphony.test-system :as ts]))

(t/use-fixtures :once ts/with-conn-initial)

(def TRUTHS-FILE  "test/data/truths.gt")
(def REPORTS-FILE  "test/data/reports.vt")

                                      ; MAIN FUNCTIONS

(t/deftest can-import-truths-file
  (t/is (some? (import-to-connection! truths-in-json TRUTHS-FILE ts/*conn-initial*)))
  (t/is (= (import-to-memory! truths-in-json TRUTHS-FILE)
           '[[{:truth/resource "f63256cf4eef0a60fe56989b1474dd9b0b2bb580ce9fd262b18592bf0506f911",
               :truth/name "adwo",:truth/type "adware",:truth/platform "android"}]
             [{:truth/resource "a9cbe3e3d446cea683c1e72f2994f40024afed1bb1186b27690ff21741046312",
               :truth/name "dowgin",:truth/type "trojan",:truth/platform "linux"}]
             [{:truth/resource "9da56b0cb31d412a1ed20fb089f5364acf6b7c0a77c1774c202cd8ff6e13a1ad",
               :truth/type "ads"}]
             [{:truth/resource "a0196e43aaf90bef85b9661ec23037c79d246d94e1100192295079529d529d97",
               :truth/name "singleton:a0196e43aaf90bef85b9661ec23037c79d246d94e1100192295079529d529d97",
               :truth/platform "android"}]
             []])))

(t/deftest can-import-reports-file
  (t/is (some? (import-to-connection! reports-in-json REPORTS-FILE ts/*conn-initial*)))
  (t/is (= (import-to-memory! reports-in-json REPORTS-FILE)
           '[[{:db/id "avware", :antivirus/antivirus "avware"}
              {:db/id "trojan.androidos.generic.a",:label/label "trojan.androidos.generic.a"}
              {:db/id "avware++trojan.androidos.generic.a",:result/result "avware++trojan.androidos.generic.a",
               :result/antivirus "avware",:result/label "trojan.androidos.generic.a"}
              {:db/id "eset-nod32", :antivirus/antivirus "eset-nod32"}
              {:db/id "android/adrd.a", :label/label "android/adrd.a"}
              {:db/id "eset-nod32++android/adrd.a",:result/result "eset-nod32++android/adrd.a",
               :result/antivirus "eset-nod32",:result/label "android/adrd.a"}
              {:db/id "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4-1468430330",
               :scan/scan "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4-1468430330",
               :scan/date #inst "2016-07-13T15:18:50.000-00:00",
               :scan/results ("avware++trojan.androidos.generic.a" "eset-nod32++android/adrd.a")}
              {:report/resource "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4",
               :report/scan "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4-1468430330",
               :db/txInstant #inst "2016-07-13T15:18:50.000-00:00"}]
             [{:db/id "tencent", :antivirus/antivirus "tencent"}
              {:db/id "a.remote.adrd", :label/label "a.remote.adrd"}
              {:db/id "tencent++a.remote.adrd",:result/result "tencent++a.remote.adrd",
               :result/antivirus "tencent",:result/label "a.remote.adrd"}
              {:db/id "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d-1467894540",
               :scan/scan "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d-1467894540",
               :scan/date #inst "2016-07-07T10:29:00.000-00:00",
               :scan/results ("tencent++a.remote.adrd")}
              {:report/resource "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d",
               :report/scan "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d-1467894540",
               :db/txInstant #inst "2016-07-07T10:29:00.000-00:00"}]
             []])))
