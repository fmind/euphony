(ns euphony.queries
  (:require [euphony.utils.db :as d]
            [medley.core :as m]))

                                        ; HELPERS

(defn- by-key
  "Retrieve a database entity by its ident."
  [key db value] (d/entity db [key value]))

(defn reverse-index
  "Construct a reverse index from an index."
  [index] (reduce (fn [idx [k v]] (update idx k conj v))
                  (empty index) (map reverse index)))

                                        ; MEMORY QUERIES

(defn mem>truth->attribute [attribute records]
  (->> records
       (filter (fn [r] (and (contains? r :ground-truth/resource) (contains? r attribute))))
       (map (fn [r] [(:ground-truth/resource r) (attribute r)]))
       (into (hash-map))))

                                        ; DATABASE QUERIES

(def db>word (partial by-key :antivirus.word/word))
(def db>label (partial by-key :antivirus.label/label))
(def db>resource (partial by-key :resource/resource))
(def db>antivirus (partial by-key :antivirus.system/name))
(def db>result (partial by-key :antivirus.result/id))
(def db>scan (partial by-key :antivirus.scan/id))
(def db>truth (partial by-key :ground-truth/resource))
(def db>report (partial by-key :antivirus.report/resource))

(defn db>word->field [db]
  (->>  (d/q '[:find ?word ?field
               :where
               [?w :antivirus.word/word ?word]
               [?w :antivirus.word/field ?field]]
             db)
        (into (hash-map))))

(defn db>label->antivirus [l db]
  (d/q '[:find [?av ...]
         :in $ ?label
         :where
         [?l :antivirus.label/label ?label]
         [?r :antivirus.result/label ?l]
         [?r :antivirus.result/system ?a]
         [?a :antivirus.system/name ?av]]
       db l))

(defn db>label->attribute [attribute db]
  (->> (d/q '[:find ?label ?value
              :in $ ?attribute
              :where
              [?l :antivirus.label/label ?label]
              [?l ?attribute ?value]]
            db attribute)
       (into (sorted-map))))

(defn db>result->attribute [attribute db]
  (->> (d/q '[:find ?result ?value
              :in $ ?attribute
              :where
              [?r :antivirus.result/id ?result]
              [?r ?attribute ?value]]
            db attribute)
       (into (sorted-map))))

(defn db>truth->attribute [attribute db]
  (->> (d/q '[:find ?resource ?value
              :in $ ?attribute
              :where
              [?t :ground-truth/resource ?resource]
              [?t ?attribute ?value]]
            db attribute)
       (into (sorted-map))))

(defn db>result->av_label
  [label-field db]
  (->> (d/q '[:find ?result ?av ?label
              :in $ ?field
              :where
              [?r :antivirus.result/label ?l]
              [?r :antivirus.result/system ?a]
              [?r :antivirus.result/id ?result]
              [?a :antivirus.system/name ?av]
              [?l ?field ?label]]
            db label-field)
       (group-by first)
       (m/map-vals (fn [[[r a l]]] [a l]))))

(defn db>report->vendor-results [vendor-field db]
  (->> (d/q '[:find ?resource ?antivirus ?value
              :in $ ?attribute
              :where
              [?r :antivirus.report/resource ?resource]
              [?r :antivirus.report/scan ?s]
              [?s :antivirus.scan/results ?rs]
              [?rs :antivirus.result/system ?av]
              [?av :antivirus.system/name ?antivirus]
              [?rs :antivirus.result/label ?l]
              [?l ?attribute ?value]]
            db vendor-field)
       (group-by first)
       (m/map-vals (partial map (fn [[r a v]] [a v])))))

(defn db>report->cluster-results [cluster-field db]
  (->> (d/q '[:find ?resource ?antivirus ?value
              :in $ ?attribute
              :where
              [?r :antivirus.report/resource ?resource]
              [?r :antivirus.report/scan ?s]
              [?s :antivirus.scan/results ?rs]
              [?rs :antivirus.result/system ?av]
              [?av :antivirus.system/name ?antivirus]
              [?rs ?attribute ?value]]
            db cluster-field)
       (group-by first)
       (m/map-vals (partial map (fn [[r a v]] [a v])))))

(defn db>words-with-this-field [field db]
  (d/q '[:find [?word ...]
         :in $ ?field
         :where
         [?w :antivirus.word/field ?field]
         [?w :antivirus.word/word ?word]]
       db field))

(defn db>labels-with-unknown-fields-pattern [db]
  (d/q '[:find [?label ...]
         :where
         [?l :antivirus.label/label ?label]
         [(missing? $ ?l :antivirus.label/fields-pattern)]]
       db))

(defn db>fields-patterns-related-to-label [db l]
  (if-let [pattern (-> (db>label db l) :antivirus.label/words-pattern)]
    (d/q '[:find [?fields-pattern ...]
           :in $ ?words-pattern [?av ...]
           :where
           [?l :antivirus.label/words-pattern ?words-pattern]
           [?l :antivirus.label/fields-pattern ?fields-pattern]
           [?r :antivirus.result/label ?l]
           [?r :antivirus.result/system ?a]
           [?a :antivirus.system/name ?av]]
         db pattern (db>label->antivirus l db))
    []))
