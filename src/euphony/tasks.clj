(ns euphony.tasks
  (:require [euphony.commands.analyzers :as analyzers]
            [euphony.commands.clusterer :as clusterer]
            [euphony.commands.importer :as importer]
            [euphony.commands.parser :as parser]
            [euphony.commits :as c]
            [euphony.functions.voters :as v]
            [euphony.queries :as q]
            [euphony.system :as sys]
            [euphony.utils.db :as d]
            [euphony.utils.io :as io]
            [euphony.utils.log :as log]
            [medley.core :as m]))

                                        ; DEFAULTS

(def DEFAULT-FIELD :name)
(def MAX-TURN parser/MAX-TURN)
(def THRESHOLD clusterer/THRESHOLD)
(def FIELDS #{:name :type :platform})

(def DATABASE-URI (-> sys/CONF :datomic :uri))
(def SEEDS-FILE (-> sys/CONF :datomic :seeds-file))

                                        ; COMPOSABLES

(defn with-datomic-conn [{:keys [datomic] :as context}]
  {:pre [datomic] :post [(contains? % :conn)]}
  (assoc context :conn (:conn datomic)))

(defn import-reports-to-connection! [{:keys [conn reports-file] :as context}]
  {:pre [conn] :post [(contains? % :conn)]}
  (if (nil? reports-file) context
      (do (log/log :info "Importing reports to connection from:" reports-file)
          (assoc context :conn (importer/import-to-connection! importer/reports-in-json reports-file conn)))))

(defn import-truths-to-connection! [{:keys [conn ground-file] :as context}]
  {:pre [conn] :post [(contains? % :conn)]}
  (if (nil? ground-file) context
      (do (log/log :info "Importing truths to connection from:" ground-file)
          (assoc context :conn (importer/import-to-connection! importer/truths-in-json ground-file conn)))))

(defn import-truths-to-memory [{:keys [ground-file truth-field] :as context}]
  (if (nil? ground-file) context
      (do (log/log :info "Importing truths to memory based on:" truth-field)
          (assoc context :truths (->> (importer/import-to-memory! importer/truths-in-json ground-file)
                                      (apply concat) (q/mem>truth->attribute truth-field))))))

(defn with-unknown-labels [{:keys [conn] :as context}]
  {:pre [conn] :post [(contains? % :labels)]}
  (log/log :info "Associating labels with unknown fields pattern")
  (assoc context :labels (q/db>labels-with-unknown-fields-pattern (d/db conn))))

(defn with-parse-mapping! [{:keys [conn labels] :as context}]
  {:pre [conn (not-empty labels)] :post [(contains? % :parse-mapping)]}
  (log/log :info "Associating parse mapping created from labels")
  (let [[conn mapping] (parser/parse conn labels context)]
    (assoc context :conn conn :parse-mapping mapping)))

(defn commit-vendor-attributes! [{:keys [conn parse-mapping] :as context}]
  {:pre [conn parse-mapping]}
  (log/log :info "Committing vendor attributes from parse mapping")
  (assoc context :conn
         (reduce (fn [co [label tokens]] (c/db>vendors-from-tokens commit-vendor-attributes! co label tokens))
                 conn parse-mapping)))

(defn with-vendor-reports [{:keys [conn vendor-field] :as context}]
  {:pre [conn vendor-field] :post [(contains? % :vendor-reports)]}
  (log/log :info "Associating vendor reports based on:" vendor-field)
  (assoc context :vendor-reports (q/db>report->vendor-results vendor-field (d/db conn))))

(defn with-cluster-graph [{:keys [vendor-reports] :as context}]
  {:pre [vendor-reports] :post [(contains? % :cluster-graph)]}
  (log/log :info "Associating cluster graph from vendor reports")
  (assoc context :cluster-graph (clusterer/results-graph (vals vendor-reports))))

(defn with-cluster-mapping [{:keys [cluster-graph] :as context}]
  {:pre [cluster-graph] :post [(contains? % :cluster-mapping)]}
  (log/log :info "Associating cluster mapping from cluster graph")
  (assoc context :cluster-mapping (clusterer/results-clusters cluster-graph context)))

(defn commit-cluster-attribute! [{:keys [conn cluster-mapping vendor-field cluster-field] :as context}]
  {:pre [conn cluster-mapping vendor-field cluster-field]}
  (log/log :info "Committing cluster families to:" cluster-field)
  (assoc context :conn (c/db>cluster-attribute-from-cluster-mapping commit-cluster-attribute! conn
                                                                    cluster-mapping vendor-field
                                                                    cluster-field context)))

(defn with-cluster-reports [{:keys [conn cluster-field] :as context}]
  {:pre [conn cluster-field] :post [(contains? % :cluster-reports)]}
  (log/log :info "Associating cluster reports based on:" cluster-field)
  (assoc context :cluster-reports (q/db>report->cluster-results cluster-field (d/db conn))))

(defn with-truth-field [{:keys [field] :as context}]
  {:pre [(FIELDS field)] :post [(contains? % :truth-field)]}
  (log/log :info "Associating truth field based on:" field)
  (assoc context :truth-field (keyword "ground-truth" (name field))))

(defn with-vendor-field [{:keys [field] :as context}]
  {:pre [(FIELDS field)] :post [(contains? % :vendor-field)]}
  (log/log :info "Associating vendor field based on:" field)
  (assoc context :vendor-field (keyword "antivirus.label" (str (name field) "-part"))))

(defn with-cluster-field [{:keys [field] :as context}]
  {:pre [(FIELDS field)] :post [(contains? % :cluster-field)]}
  (log/log :info "Associating cluster field based on:" field)
  (assoc context :cluster-field (keyword "antivirus.result" (str (name field) "-cluster"))))

(defn based-on-cluster-reports [{:keys [cluster-reports] :as context}]
  {:pre [cluster-reports]}
  (log/log :info "Setting context for election on cluster reports")
  (assoc context :election-reports (m/map-vals (partial map second) cluster-reports)))

(defn with-election [{:keys [election-reports] :as context}]
  {:pre [election-reports] :post [(contains? % :election)]}
  (log/log :info "Associating election based on election reports")
  (assoc context :election (v/vote election-reports)))

(defn with-proposed [{:keys [election] :as context}]
  {:pre [election] :post [(contains? % :proposed)]}
  (log/log :info "Associating proposed labels based on election")
  (assoc context :proposed (v/elect election)))

(defn with-parse-rules [{:keys [conn vendor-field] :as context}]
  {:pre [conn vendor-field] :post [(contains? % :parse-rules)]}
  (log/log :info "Associating parse rules based on:" vendor-field)
  (assoc context :parse-rules (q/db>label->attribute vendor-field (d/db conn))))

(defn with-cluster-rules [{:keys [conn cluster-field] :as context}]
  {:pre [conn cluster-field] :post [(contains? % :cluster-rules)]}
  (log/log :info "Associating cluster rules based on:" cluster-field)
  (assoc context :cluster-rules (q/db>result->attribute cluster-field (d/db conn))))

(defn with-malstats [{:keys [truths proposed] :as context}]
  (if (empty? truths) context
      (do (log/log :info "Associating malstats based on truths and proposed")
          (assoc context :malstats (analyzers/malstats truths proposed)))))

(defn with-famstats [{:keys [truths proposed] :as context}]
  (if (empty? truths) context
      (do (log/log :info "Associating famstats based on truths and proposed")
          (assoc context :famstats (analyzers/famstats truths proposed)))))

(defn with-threstats [{:keys [conn truths thresholds] :as context}]
  {:pre [(not-empty truths) thresholds (:no-side-effect conn)] :post [(contains? % :threstats)]}
  (let [analyze (comp analyzers/humanize
                      analyzers/analyze-famstats (partial analyzers/famstats truths)
                      :proposed with-proposed with-election based-on-cluster-reports
                      with-cluster-reports commit-cluster-attribute! with-cluster-mapping)
        context (assoc context :on-conflict->new true)]
    (log/log :info "Associating threstats based on cluster-graph and threshold")
    (log/with-level :warn
      (assoc context :threstats
             (->> (pmap (fn [t] (analyze (assoc context :threshold t))) thresholds)
                  (zipmap thresholds))))))

(defn summarize! [{:keys [parse-mapping cluster-mapping malstats famstats] :as context}]
  (when (some? parse-mapping)
    (log/log :info "parse:" (analyzers/humanize (analyzers/analyze-parse parse-mapping))))
  (when (some? cluster-mapping)
    (log/log :info "cluster:" (analyzers/humanize (analyzers/analyze-cluster cluster-mapping))))
  (when (some? malstats)
    (log/log :info "malstats:" (analyzers/humanize (analyzers/analyze-malstats malstats))))
  (when (some? famstats)
    (log/log :info "famstats:" (analyzers/humanize (analyzers/analyze-famstats famstats))))
  context)

(defn export! [{:keys [election proposed parse-rules parse-mapping vendor-reports
                       cluster-graph cluster-rules cluster-mapping cluster-reports
                       malstats famstats threstats
                       export-dir export-all export-election export-proposed
                       export-parse-rules export-parse-mapping export-vendor-reports
                       export-cluster-graph export-cluster-rules export-cluster-mapping
                       export-cluster-reports export-malstats export-famstats export-threstats]
                :as context}]
  (when (some? export-dir)
    (let [_ (io/mkdir! export-dir)
          in-dir (partial io/filepath export-dir)]
      (log/log :info "Exporting context to:" export-dir)
      (when (and election (or export-election export-all))
        (io/write-json! (in-dir "election.json") election))
      (when (and proposed (or export-proposed export-all))
        (io/write-json! (in-dir "proposed.json") proposed))
      (when (and parse-rules (or export-parse-rules export-all))
        (io/write-json! (in-dir "parse-rules.json") parse-rules))
      (when (and parse-mapping (or export-parse-mapping export-all))
        (io/write-json! (in-dir "parse-mapping.json") parse-mapping))
      (when (and vendor-reports (or export-vendor-reports export-all))
        (io/write-json! (in-dir "vendor-reports.json") vendor-reports))
      (when (and cluster-graph (or export-cluster-graph export-all))
        (io/write-dot! (in-dir "cluster-graph.dot") cluster-graph))
      (when (and cluster-rules (or export-cluster-rules export-all))
        (io/write-json! (in-dir "cluster-rules.json") cluster-rules))
      (when (and cluster-mapping (or export-cluster-mapping export-all))
        (io/write-json! (in-dir "cluster-mapping.json") cluster-mapping))
      (when (and cluster-reports (or export-cluster-reports export-all))
        (io/write-json! (in-dir "cluster-reports.json") cluster-reports))
      (when (and malstats (or export-malstats export-all))
        (io/write-json! (in-dir "malstats.json") malstats))
      (when (and famstats (or export-famstats export-all))
        (io/write-json! (in-dir "famstats.json") famstats))
      (when (and threstats export-threstats)
        (io/write-json! (in-dir "threstats.json") threstats))))
  context)

(def import! (comp import-truths-to-connection! import-reports-to-connection! import-truths-to-memory))

(def parse! (comp with-vendor-reports commit-vendor-attributes! with-parse-mapping! with-unknown-labels))

(def cluster! (comp with-cluster-reports commit-cluster-attribute! with-cluster-mapping))

(def vote-on-clusters! (comp with-proposed with-election based-on-cluster-reports))

(def with-all-fields (comp with-cluster-field with-vendor-field with-truth-field))

(def with-all-rules (comp with-cluster-rules with-parse-rules))

(def with-all-stats (comp with-famstats with-malstats))

(def all (comp export!
            summarize!
            with-all-stats
            with-all-rules
            vote-on-clusters!
            cluster!
            with-cluster-graph
            parse!
            import!
            with-all-fields
            with-datomic-conn))

                                        ; MAIN FUNCTIONS

(defn make! [task conf]
  (sys/with-system [system (:system conf)]
    (task (merge system conf))))
