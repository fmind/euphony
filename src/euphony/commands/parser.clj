(ns euphony.commands.parser
  (:require [clojure.set :as Set]
            [euphony.commands.sub.heuristics :as hx]
            [euphony.commits :as c]
            [euphony.queries :as q]
            [euphony.structs.label :as l]
            [euphony.structs.pqueue :as p]
            [euphony.utils.db :as d]))

                                        ; DEFAULTS

(def MAX-TURN 4)

(def HEURISTICS [;; exact inference
                 [hx/deduce-known-words {}]
                 [hx/deduce-signature-tokens {:once true}]
                 [hx/deduce-words-suffixed-by-ware {:once true}]
                 [hx/deduce-words-between-parenthesis {:once true}]
                 [hx/deduce-words-between-square-brackets {:once true}]
                 ;; direct inferences
                 [hx/infer-fields-by-elimination {}]
                 [hx/infer-synonyms-from-known-platforms-and-types {:once true}]
                 ;; delayed inferences
                 [hx/infer-fields-from-compatible-patterns {:delay 3}]
                 [hx/infer-name-from-last-one-unknown-token {:delay 1}]
                 [hx/infer-name-from-last-two-unknown-tokens {:delay 1}]
                 [hx/infer-words-in-english-sentence-at-beginning {:delay 1}]])

                                        ; CONSTRUCTORS

(defn priority
  "Compute a priority from a turn and an entry."
  [turn [label tokens :as entry]]
  (let [words (filter l/token-word? tokens)
        fields (mapcat l/token-fields words)]
    [turn (- (count fields) (count words))]))

(defn pqueue
  "Construct a queue from a list of labels and a priority function."
  [f labels]
  (let [tokens (map l/tokenize-and-domainize labels)
        entries (map vector labels tokens)]
    (p/pqueue entries (map f entries))))

(defn mem-setter
  "Construct a memory setter to change token fields using a commit function."
  [heuristic] (fn [token fields] (c/mem>token-fields heuristic token fields {:on-conflict->new true})))

                                        ; INTERNAL FUNCTIONS

(defn- search
  "Execute a sequence of heuristics to produce new findings, take into account heuristic options."
  [heuristics database label tokens turn] {:post [(= (count %) (count tokens))]}
  (reduce (fn [findings [h {:keys [delay once]}]]
            (if (or (and (some? once) (not= (- turn (or delay 0)) 0))
                    (and (some? delay) (< turn delay))) findings
                (h database label findings {:setter (mem-setter h)})))
          tokens heuristics))

(defn- populate
  "Populate a connection with label words and words fields from a list of entry."
  [conn entries]
  (let [words-parts (future (->> entries (mapcat second) distinct
                                 (filter l/token-word?) (map l/token-parts)))]
    (as-> conn $
          (reduce (fn [co [label tokens]]
                    (let [pattern (l/w-patternize tokens)
                          words (->> tokens (filter l/token-word?) (map l/token-text))]
                      (c/db>label-words-pattern populate co label pattern words)))
                  $ entries)
          (reduce (fn [co [_ word fields]]
                    (c/db>word-fields populate co word fields))
                  $ @words-parts))))

(defn- combine
  "Combine new findings in database and in memory."
  [conn findings] {:post [(= (count (second %)) (count findings))]}
  (loop [findings findings, conn conn, tokens (vector)]
    (if (empty? findings) [conn tokens] ;;return case
        (let [[token & findings] findings]
          (if (l/token-sep? token) ;; separator case
            (recur findings conn (conj tokens token))
            (let [db (d/db conn)
                  [_ word mem-fields] (l/token-parts token)
                  db-fields (-> (q/db>word db word) :antivirus.word/candidate-fields)
                  intersection (Set/intersection mem-fields db-fields)]
              (recur findings
                     (c/db>word-fields combine conn word mem-fields)
                     (conj tokens (c/mem>token-fields combine token db-fields {:on-conflict->new true})))))))))

(defn- enrich
  "Enrich the database with new patterns/words."
  [conn label tokens]
  (as-> conn $
        (c/db>label-fields-pattern enrich $ label (l/f-patternize tokens))
        (reduce (fn [co [_ word fields]] (c/db>word-field enrich co word (first fields)))
                $ (->> tokens (filter l/token-word?) (map l/token-parts)))))

                                        ; MAIN FUNCTIONS

(defn parse
  "Parse labels using a knowledge database and return a mapping of token assignments per label."
  [conn labels & [{:keys [heuristics max-turn] :or {heuristics HEURISTICS max-turn MAX-TURN}}]]
  (let [labels (filter l/label-valid? labels), queue (pqueue (partial priority 0) labels)
        conn (populate conn (keys queue)), complete (hash-map)]
    (loop [conn conn, queue queue, complete complete]
      (let [[[label tokens] [turn _]] (peek queue)]
        (if (or (>= turn max-turn) (empty? queue))
          [conn (into complete (keys queue))] ;; return case: conn and tokens
          (let [findings (search heuristics (d/db conn) label tokens turn)
                [conn tokens] (combine conn findings), entry [label tokens]]
            (if-not (l/tokens-assignment-complete? tokens)
              (recur conn (conj (pop queue) [entry (priority (inc turn) entry)]) complete)
              (recur (enrich conn label tokens) (pop queue) (assoc complete label tokens)))))))))
