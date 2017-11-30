(ns euphony.commands.importer
  (:require [clojure.java.io :as jio]
            [clojure.string :as Str]
            [euphony.structs.label :as l]
            [euphony.utils.db :as d]
            [euphony.utils.io :as io]))

                                        ; HELPERS

(defn- parse-date [formatter date]
  (.parse formatter date))

(def ^:private to-date (partial parse-date (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")))

                                        ; COMPOSABLES

(defn truth>struct->datoms [{:strs [resource name type platform] :as struct}]
  (if (Str/blank? resource) []
      [(cond-> {:ground-truth/resource resource}
         name (assoc :ground-truth/name (Str/lower-case name))
         type (assoc :ground-truth/type (Str/lower-case type))
         platform (assoc :ground-truth/plat (Str/lower-case platform)))]))

(defn result>struct->datoms [[antivirus {:strs [result detected]}]]
  (if (or (Str/blank? antivirus) (Str/blank? result) (not detected)) []
      (let [av (Str/lower-case antivirus), label (Str/lower-case result), rid (l/result->id [av label])]
        [{:db/id av :antivirus.system/name av} {:db/id label :antivirus.label/label label}
         {:db/id rid :antivirus.result/id rid :antivirus.result/system av :antivirus.result/label label}])))

(defn report>struct->datoms [{:strs [scan_id scan_date resource positives scans] :as struct}]
  (if (or (Str/blank? resource) (Str/blank? scan_id) (Str/blank? scan_date) (zero? positives)) []
      (let [id (Str/lower-case scan_id), date (to-date scan_date), resource (Str/lower-case resource)
            results-datoms (mapcat result>struct->datoms scans)]
        (concat results-datoms
                [{:db/id id :antivirus.scan/id id :antivirus.scan/date date
                  :antivirus.scan/results (->> results-datoms (filter :antivirus.result/id) (map :db/id))}
                 {:antivirus.report/resource resource :antivirus.report/scan id :db/txInstant date}]))))

                                        ; COMPOSITIONS

(def truths-in-json (comp truth>struct->datoms io/json-line->struct))
(def reports-in-json (comp report>struct->datoms io/json-line->struct))

                                        ; MAIN FUNCTIONS

(defn import-to-memory!
  "Import a file to memory using a pipeline."
  [pipeline file]
  (with-open [reader (jio/reader file)]
    (mapv pipeline (line-seq reader))))

(defn import-to-connection!
  "Import a file to connection using a pipeline."
  [pipeline file conn]
  (with-open [reader (jio/reader file)]
    (reduce (fn [co line] (d/transact co (pipeline line)))
            conn (line-seq reader))))
