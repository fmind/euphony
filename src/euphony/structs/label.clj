(ns euphony.structs.label
  (:require [clojure.string :as Str]
            [instaparse.core :as insta]))

                                        ; CONSTANTS

(defonce GRAMMAR
  (insta/parser
   "LABEL=(w|s)+
    w=#'\\p{Alnum}+'
    s=#'[\\p{Punct}\\p{Blank}]+'"))

(defonce FIELDS #{:P :T :N :I})

                                        ; HELPERS

(def ^:private my-get (fn [key obj] (get obj key)))

                                        ; WORDS

(defn word-name?
  "Check if a a word is a possible malware name."
  [word] {:pre [(string? word)]} (boolean (re-matches #"\p{Alpha}{3,}" word)))

(defn word-type?
  "Check if a word is a possible malware type."
  [word] {:pre [(string? word)]} (boolean (re-matches #"\p{Alpha}{2,}" word)))

(defn word-platform?
  "Check if a word is a possible malware platform."
  [word] {:pre [(string? word)]} (boolean (and (re-matches #"\p{Alnum}{2,}" word)
                                               (re-find #"\p{Alpha}" word))))

(defn word-information?
  "Check if a word is a possible malware information."
  [word] {:pre [(string? word)]} (boolean (re-matches #"\p{Alnum}+" word)))

(def words-concat (partial Str/join "++"))

                                        ; TOKENS

(defonce TOKEN-SYM-KEY 0)
(defonce TOKEN-TEXT-KEY 1)
(defonce TOKEN-FIELDS-KEY 2)
(def token-sym (partial my-get TOKEN-SYM-KEY))
(def token-text (partial my-get TOKEN-TEXT-KEY))
(def token-fields (partial my-get TOKEN-FIELDS-KEY))
(def token-parts (juxt token-sym token-text token-fields))

(defn- token-test-on
  "Select a part of a token and test it with a predicate function."
  [selector pred token] (boolean (when-let [part (selector token)] (pred part))))

(def token-test-on-sym (partial token-test-on token-sym))
(def token-test-on-text (partial token-test-on token-text))
(def token-test-on-fields (partial token-test-on token-fields))

(def token-sep?  (partial token-test-on-sym #(= % :s)))
(def token-word? (partial token-test-on-sym #(= % :w)))

(defn token-is-this-field?
  "Test if the token fields is the given field."
  [field token] (token-test-on-fields #(= % #{field}) token))

(defn token-has-same-fields?
  "Test if the token fields are the same as the given fields."
  [fields token] (token-test-on-fields #(= % fields) token))

(defn token-contains-field?
  "Test if the token fields contains the given field."
  [field token] (token-test-on-fields #(contains? % field) token))

(defn token-has-one-field?
  "Test if there is a single token field."
  [token] (token-test-on-fields #(= (count %) 1) token))

(defn token-has-many-fields?
  "Test if there is many token fields."
  [token] (token-test-on-fields #(> (count %) 1) token))

(defn tokens-assignment-complete?
  [tokens] (every? token-has-one-field? (filter token-word? tokens)))

(defn token-set-fields
  "Set the fields of a token only if it's a word."
  [token fields]
  (cond
    (not (token-word? token)) token
    (not (every? FIELDS fields))
    (throw (AssertionError. (str "unacceptable fields in: " fields)))
    :else (assoc token TOKEN-FIELDS-KEY fields)))

(defn map-token
  "Apply f on a list of tokens. If f returns nil, leave the token unchanged."
  [pred f tokens]
  (map (fn [token]
         (if-not (pred token) token
                 (if-let [new-token (f token)] new-token token)))
       tokens))

(def map-token-sep (partial map-token token-sep?))
(def map-token-word (partial map-token token-word?))

(defn tokenize-with
  "Split a label string into word and separator tokens."
  [grammar label] {:pre [(string? label)]}
  (rest (insta/parse grammar label)))

(defn domainize-with
  "Set the initial fields for every word in a token list."
  [domain tokens]
  (letfn [(text->fields [text]
            (reduce (fn [fields [field pred]]
                      (if-not (pred text) fields
                              (conj fields field)))
                    (hash-set) domain))
          (with-fields [token]
            (if (token-sep? token) token
                (token-set-fields token (text->fields (token-text token)))))]
    (map with-fields tokens)))

(defonce DOMAIN {:N word-name?
                 :T word-type?
                 :P word-platform?
                 :I word-information?})

(def tokenize (partial tokenize-with GRAMMAR))
(def domainize (partial domainize-with DOMAIN))
(def tokenize-and-domainize (comp domainize tokenize))

                                        ; LABELS

(defn label-valid?
  "Check if a label contains invalid characters."
  [label] (boolean (and (string? label)
                        (not (Str/blank? label))
                        (re-find #"[\p{Alnum}]" label)
                        (not (re-find #"[^\p{Alnum}\p{Blank}\p{Punct}]" label)))))

(defn label-find-words-by-regexp [re label] (->> (re-seq re label) (map second) set))
(def label-words-between-parenthesis (partial label-find-words-by-regexp #"\((\p{Alnum}+)\)"))
(def label-words-between-square-brackets (partial label-find-words-by-regexp #"\[(\p{Alnum}+)\]"))

                                        ; RESULTS

(def av first)
(def label second)

(defn result->id [[av label]]
  {:pre [(not (Str/blank? av)) (not (Str/blank? label))]}
  (str (Str/lower-case av) "++" (Str/lower-case label)))
                                        ; PATTERNS

(def pattern-word? (comp boolean (partial re-matches #"\p{Alpha}")))
(def pattern-sep? (comp boolean (partial re-matches #"[\p{Blank}\p{Punct}]")))

(def pattern-parts (comp (partial map token-text) tokenize))
(def pattern-words (comp (partial filter pattern-word?) pattern-parts))
(def pattern-keywords (comp (partial map keyword) pattern-words))

(defn pattern-compatible?
  "Test if a list of tokens is compatible with the given field pattern."
  [f-pattern tokens]
  (let [pattern-fs (->> f-pattern pattern-keywords)
        token-fs (->> tokens (filter token-word?) (map token-fields))]
    (if (not= (count token-fs) (count pattern-fs)) false
        (every? (fn [[tf pf]] (contains? tf pf))
                (map vector token-fs pattern-fs)))))

(defn w-patternize
  "Transform a label to a general pattern based on its words and separators."
  [tokens]
  (letfn [(token->w-part [token]
            (let [sym (token-sym token)]
              (case sym
                :s (token-text token)
                :w (name (token-sym token))
                (throw (AssertionError. (str sym " is not a valid token symbol."))))))]
    (apply str (map token->w-part tokens))))

(defn f-patternize
  "Transform a label to a general pattern based on its fields and separators."
  [tokens]
  (letfn [(field->f-part [field]
            (assert (FIELDS field))
            (name field))
          (token->f-part [token]
                         (case (token-sym token)
                           :s (token-text token)
                           :w (let [fields (token-fields token)]
                                (assert (= (count fields) 1))
                                (field->f-part (first fields)))))]
    (apply str (map token->f-part tokens))))

(defn pattern-set-tokens-fields
  "Set the fields of a token list from a field pattern."
  [f-pattern tokens]
  (if-not (pattern-compatible? f-pattern tokens) tokens
          (map (fn [token part]
                 (if (token-sep? token) token
                     (token-set-fields token #{(keyword part)})))
               tokens (pattern-parts f-pattern))))
