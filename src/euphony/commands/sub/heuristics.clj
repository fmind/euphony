(ns euphony.commands.sub.heuristics
  (:require [clojure
             [set :as Set]
             [string :as Str]]
            [clojure.java.io :as jio]
            [euphony.queries :as q]
            [euphony.structs.label :as l]))

                                        ; RESOURCES

(defonce ENGLISH (-> (jio/resource "english.dict") jio/reader line-seq set))

                                        ; HEURISTICS

(defn deduce-known-words
  "Associate the fields of known words to the same field in the database."
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (l/map-token-word
   (fn [token]
     (when-let [db-field (-> (q/db>word db (l/token-text token)) :word/field)]
       (setter token #{db-field})))
   tokens))

(defn deduce-signature-tokens
  "Associate the fields of signatures to INFORMATION."
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (l/map-token-word
   (fn [token]
     (let [[_ text fields] (l/token-parts token)]
       (when (and (re-find #"[0-9]" text)
                  (or (re-matches #"[a-fA-F0-9]{8}" text)
                      (re-matches #"[a-fA-F0-9]{12}" text))
                  (l/token-contains-field? :I token))
         (setter token #{:I}))))
   tokens))

(defn deduce-words-suffixed-by-ware
  "Associate the fields of words suffixed by -ware to TYPE."
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (l/map-token-word
   (fn [token]
     (when (re-matches #"\p{Alpha}+ware" (l/token-text token))
       (setter token #{:T})))
   tokens))

(defn deduce-words-between-parenthesis
  "Associate the fields of words between parenthesis to INFO."
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (let [words-between-parenthesis (l/label-words-between-parenthesis label)]
    (l/map-token-word
     (fn [token]
       (when (words-between-parenthesis (l/token-text token))
         (setter token #{:I})))
     tokens)))

(defn deduce-words-between-square-brackets
  "Associate the fields of words between square brackets to TYPE or INFO."
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (let [words-between-square-brackets (l/label-words-between-square-brackets label)]
    (l/map-token-word
     (fn [token]
       (when (words-between-square-brackets (l/token-text token))
         (setter token #{:I :T})))
     tokens)))

(defn infer-words-in-english-sentence-at-beginning
  "Associate the fields of words in an English sentence at the beginning of the label to INFO."
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (letfn [(in-sentence? [token]
            (let [[sym text fields] (l/token-parts token)]
              (or (and (l/token-sep? token)
                       (re-matches #"[- ]+" text))
                  (and (l/token-word? token)
                       (l/token-contains-field? :I token)
                       (re-matches #"\p{Alpha}+" text)
                       (ENGLISH text)))))]
    (let [sentence (->> tokens (take-while in-sentence?) (filter l/token-word?) set)]
      (if (<= (count sentence) 1) tokens
          (l/map-token-word
           (fn [token]
             (when (sentence token)
               (setter token #{:I})))
           tokens)))))

(defn infer-fields-by-elimination
  "Associate the fields of words in order to satisfy these constraints:
    - a label can only have one platform
    - a label can only have one family
    - a label can only have one type"
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (let [knowns (->> tokens (filter l/token-has-one-field?) set)
        constraints (->> knowns (map l/token-fields) (apply Set/union) (Set/intersection #{:P :N :T}))]
    (l/map-token-word
     (fn [token]
       (when (and (not (knowns token)) (not= (l/token-fields token) constraints))
         (setter token (Set/difference (l/token-fields token) constraints))))
     tokens)))

(defn infer-fields-from-compatible-patterns
  "Associate the fields of a token list based on compatible patterns from the database."
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (let [words (filter l/token-word? tokens)
        unknowns (filter l/token-has-many-fields? words)]
    (if (or (<= (count words) 2) (not= (count unknowns) 1)) tokens
        (let [related-patterns (q/db>fields-patterns-related-to-label db label)
              compatibles (filter #(l/pattern-compatible? % tokens) related-patterns)]
          (if (not= (count compatibles) 1) tokens
              (map (fn [token target]
                     (if (or (l/token-sep? token) (= (l/token-fields token) (l/token-fields target))) token
                         (setter token (l/token-fields target))))
                   tokens (l/pattern-set-tokens-fields (first compatibles) tokens)))))))

(defn infer-name-from-last-one-unknown-token
  "Associate the fields of the last unknown token to name on strict conditions."
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (let [words (filter l/token-word? tokens)
        unknowns (filter l/token-has-many-fields? tokens)
        [_ text fields :as token] (-> unknowns first l/token-parts)]
    (if (or
         (< (count words) 2)
         (not= (count unknowns) 1)
         (< (count text) 4)
         (not (l/token-contains-field? :N token))
         (some (partial l/token-is-this-field? :N) words)
         (not (re-find #"[aeiouy]" text))
         (ENGLISH text)) tokens
        ;; else
        (l/map-token-word
         (fn [token]
           (when (= text (l/token-text token))
             (setter token #{:N})))
         tokens))))

(defn infer-name-from-last-two-unknown-tokens
  "Associate the fields of the last two unknown tokens to name/information on strict conditions."
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (let [words (filter l/token-word? tokens)
        unknowns (filter l/token-has-many-fields? tokens)
        inference-domain (partial l/token-has-same-fields? #{:I :N})
        english-words (->> unknowns (map l/token-text) (filter ENGLISH) set)
        non-english-words (->> unknowns (map l/token-text) (remove ENGLISH) set)]
    (if (or
         (not= (count unknowns) 2)
         (not= (count english-words) 1)
         (not= (count non-english-words) 1)
         (not (every? inference-domain unknowns))
         (not (every? #(>= (count %) 4) (map l/token-text unknowns)))) tokens
        (l/map-token-word
         (fn [token]
           (cond
             (english-words (l/token-text token))
             (setter token #{:I})
             (non-english-words (l/token-text token))
             (setter token #{:N})))
         tokens))))

(defn- synonym?
  [word synonym]
  (and (>= (count synonym) 4)
       (or (Str/starts-with? word synonym)
           (Str/starts-with? synonym word))))

(def mem-synonym? (memoize synonym?))

(defn infer-synonyms-from-known-platforms-and-types
  "Associate the fields of types and platforms synonyms based on the current database content."
  [db label tokens & [{:keys [setter] :or {setter l/token-set-fields}}]] {:post [(= (count %) (count tokens))]}
  (letfn [(is-candidate? [token]
            (and (l/token-word? token)
                 (l/token-has-many-fields? token)
                 (>= (count (l/token-text token)) 3)))
          (is-synonym? [synonyms text]
                       (seq (drop-while #(not (mem-synonym? text %)) synonyms)))]
    (let [types (q/db>words-with-this-field :T db)
          plats (q/db>words-with-this-field :P db)
          index (reduce (fn [hm [_ text fields :as token]]
                          (let [is-type? (and (fields :T) (is-synonym? types text))
                                is-plat? (and (fields :P) (is-synonym? plats text))]
                            (cond
                              (and is-type? (not is-plat?)) (assoc hm text :T)
                              (and is-plat? (not is-type?)) (assoc hm text :P)
                              :else hm)))
                        (hash-map) (map l/token-parts (filter is-candidate? tokens)))]
      (l/map-token-word
       (fn [token]
         (when-let [field (get index (l/token-text token))]
           (setter token #{field})))
       tokens))))
