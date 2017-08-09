(ns euphony.core
  (:gen-class)
  (:require 
            [clojure.string :as Str]
            [euphony.tasks :as tasks]
            [euphony.utils
             [cli :as cli]
             [log :as log]]))

                                        ; SUMMARY

(def USAGE "euphony [options]")

(def OPTIONS
  [;; HELPERS
   ["-h" "--help" "Display a help summary with acceptable arguments and options."]
   ;; SETTINGS
   ["-l" "--log-level LEVEL" "Set the log level of the program." :default :warn  :parse-fn #(keyword %)
    :validate [#(log/LEVELS %) (str "Must be in: " (->> log/LEVELS (map name) (interpose ",")))]]
   ["-m" "--max-turn VALUE" "Set the maximum number of turns allowed for inference at the parsing stage."
    :parse-fn #(Integer/parseInt %) :default tasks/MAX-TURN :validate [#(pos? %) "Must be a natural number."]]
   ["-t" "--threshold VALUE" "Set the threshold value for the trimming operation at the clustering stage."
    :parse-fn #(Float/parseFloat %) :default tasks/THRESHOLD :validate [#(<= 0 % 1) "Must be float: 0 <= x <= 1."]]
   ["-e" "--export-dir DIR" "Set the output directory of the program." :default (System/getProperty "user.dir")]
   ["-f" "--field FIELD" "Set the label field to cluster and export." :default tasks/DEFAULT-FIELD :parse-fn #(keyword %)
    :validate [#(tasks/FIELDS %) (str "Must be in: " (->> tasks/FIELDS (map name) (Str/join ",")))]]
   ;; RESOURCES
   ["-r" "--reports-file FILE" "Provide a sequence of reports from VirusTotal formatted as JSON records."]
   ["-g" "--ground-file FILE" "Provide a ground-truth to evaluate the output formatted as CSV tuples."]
   ["-s" "--seeds-file FILE" "Provide a seeds file with some initial domain knowledge about malware."]
   ["-d" "--database-uri URI" "Provide a database URI to run the program and persist the learning."]
   ;; EXPORT FLAGS
   ["-A" "--export-all" "Export every information"]
   ["-E" "--export-election" "Export field frequency per malware signature"]
   ["-O" "--export-proposed" "Export best candidate per malware signature"]
   ["-P" "--export-parse-rules" "Export association between malware labels and fields"]
   ["-T" "--export-parse-mapping" "Export tokenization of malware labels into fields"]
   ["-V" "--export-vendor-reports" "Export the transformation dataset after parsing"]
   ["-G" "--export-cluster-graph" "Export the association graph after clustering"]
   ["-C" "--export-cluster-rules" "Export associations between raw and clustered fields"]
   ["-D" "--export-cluster-mapping" "Export the clustering of malware results"]
   ["-R" "--export-cluster-reports" "Export the transformation dataset after parsing"]
   ["-M" "--export-malstats" "Export statistics about malware files based on ground-truth"]
   ["-F" "--export-famstats" "Export statistics about malware families based on ground-truth"]])

                                        ; MAIN FUNCTIONS

(defn system-conf [options]
  (cond-> options
    (contains? options :database-uri) (update-in [:system :conn] assoc :uri (options :database-uri))
    (contains? options :seeds-file) (update-in [:system :conn] assoc :seeds-file (options :seeds-file))))

(defn -main [& args]
  (let [{:keys [arguments options summary errors]} (cli/parse args OPTIONS)]
    (when-let [level (:log-level options)] (log/set-level! level))
    (cond
      (contains? options :help) (do (cli/feedback! USAGE summary) (System/exit 0))
      (not-empty errors) (do (cli/feedback! USAGE summary errors) (System/exit 1))
      :else (do (tasks/make! tasks/all (system-conf options)) (System/exit 0)))))
