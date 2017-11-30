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
           '[[{:ground-truth/resource "f63256cf4eef0a60fe56989b1474dd9b0b2bb580ce9fd262b18592bf0506f911",
               :ground-truth/name "adwo",:ground-truth/type "adware",:ground-truth/plat "android"}]
             [{:ground-truth/resource "a9cbe3e3d446cea683c1e72f2994f40024afed1bb1186b27690ff21741046312",
               :ground-truth/name "dowgin",:ground-truth/type "trojan",:ground-truth/plat "linux"}]
             [{:ground-truth/resource "9da56b0cb31d412a1ed20fb089f5364acf6b7c0a77c1774c202cd8ff6e13a1ad",
               :ground-truth/type "ads"}]
             [{:ground-truth/resource "a0196e43aaf90bef85b9661ec23037c79d246d94e1100192295079529d529d97",
               :ground-truth/name "singleton:a0196e43aaf90bef85b9661ec23037c79d246d94e1100192295079529d529d97",
               :ground-truth/plat "android"}]
             []])))

(t/deftest can-import-reports-file
  (t/is (some? (import-to-connection! reports-in-json REPORTS-FILE ts/*conn-initial*)))
  (t/is (= (import-to-memory! reports-in-json REPORTS-FILE)
           '[[{:db/id "avware", :antivirus.system/name "avware"}
              {:db/id "trojan.androidos.generic.a",:antivirus.label/label "trojan.androidos.generic.a"}
              {:db/id "avware++trojan.androidos.generic.a",:antivirus.result/id "avware++trojan.androidos.generic.a",
               :antivirus.result/system "avware",:antivirus.result/label "trojan.androidos.generic.a"}
              {:db/id "eset-nod32", :antivirus.system/name "eset-nod32"}
              {:db/id "android/adrd.a", :antivirus.label/label "android/adrd.a"}
              {:db/id "eset-nod32++android/adrd.a",:antivirus.result/id "eset-nod32++android/adrd.a",
               :antivirus.result/system "eset-nod32",:antivirus.result/label "android/adrd.a"}
              {:db/id "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4-1468430330",
               :antivirus.scan/id "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4-1468430330",
               :antivirus.scan/date #inst "2016-07-13T15:18:50.000-00:00",
               :antivirus.scan/results ("avware++trojan.androidos.generic.a" "eset-nod32++android/adrd.a")}
              {:antivirus.report/resource "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4",
               :antivirus.report/scan "5e82d73a3b2d4df192d674729f9578c4081d5096d5e3641bf8b233e1bee248d4-1468430330",
               :db/txInstant #inst "2016-07-13T15:18:50.000-00:00"}]
             [{:db/id "tencent", :antivirus.system/name "tencent"}
              {:db/id "a.remote.adrd", :antivirus.label/label "a.remote.adrd"}
              {:db/id "tencent++a.remote.adrd",:antivirus.result/id "tencent++a.remote.adrd",
               :antivirus.result/system "tencent",:antivirus.result/label "a.remote.adrd"}
              {:db/id "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d-1467894540",
               :antivirus.scan/id "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d-1467894540",
               :antivirus.scan/date #inst "2016-07-07T10:29:00.000-00:00",
               :antivirus.scan/results ("tencent++a.remote.adrd")}
              {:antivirus.report/resource "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d",
               :antivirus.report/scan "2357651f3d15838330368dacf37252f1ff2362ce7fd84d42c175c4f3b65a8d8d-1467894540",
               :db/txInstant #inst "2016-07-07T10:29:00.000-00:00"}]
             []])))
