(ns euphony.structs.label-test
  (:require [clojure.test :as t]
            [euphony.structs.label :refer :all]))

                                        ; WORDS

(t/deftest can-test-word-domains?
  (t/are [word, output] (= ((juxt word-name? word-type? word-platform? word-information?) word) output)
    "android0", [false false true true]
    "android",  [true true true true]
    "0000000",  [false false false true]
    "aks0",     [false false true true]
    "aks",      [true true true true]
    "000",      [false false false true]
    "js0",      [false false true true]
    "js",       [false true true true]
    "00",       [false false false true]
    "a",        [false false false true]
    "0",        [false false false true]
    "",         [false false false false]
    "and-roid", [false false false false]))

                                        ; TOKENS

(t/deftest can-select-token-parts
  (t/are [token, output] (= (token-parts token) output)
    [:w "android" #{:P}] , [:w "android" #{:P}]
    [:w "dogwin" #{:N} 4], [:w "dogwin" #{:N}]
    [:s "/"]             , [:s "/" nil]))

(t/deftest can-test-token-word?
  (t/are [token, output] (= (token-word? token) output)
    [:w "android" #{:P}], true
    [:w "android"]      , true
    [:s "/"]            , false
    [:t "/"]            , false))

(t/deftest can-test-token-sep?
  (t/are [token, output] (= (token-sep? token) output)
    [:w "android" #{:P}], false
    [:w "android"]      , false
    [:s "/"]            , true
    [:t "/"]            , false))

(t/deftest can-test-token-is-this-field?
  (t/are [token, output] (= (token-is-this-field? :N token) output)
    [:s "/"],                false
    [:w "word"],             false
    [:w "word" #{}],         false
    [:w "word" #{:N}],       true
    [:w "word" #{:T}],       false
    [:w "word" #{:N :T}],    false
    [:w "word" #{:N :T :P}], false))

(t/deftest can-test-token-has-same-fields?
  (t/are [token, output] (= (token-has-same-fields? #{:N :T} token) output)
    [:s "/"],                false
    [:w "word"],             false
    [:w "word" #{}],         false
    [:w "word" #{:N}],       false
    [:w "word" #{:T}],       false
    [:w "word" #{:N :T}],    true
    [:w "word" #{:N :T :P}], false))

(t/deftest can-test-token-contains-field?
  (t/are [token, output] (= (token-contains-field? :N token) output)
    [:s "/"],             false
    [:w "word"],          false
    [:w "word" #{}],      false
    [:w "word" #{:N}],    true
    [:w "word" #{:T}],    false
    [:w "word" #{:N :T}], true))

(t/deftest can-test-token-has-one-field?
  (t/are [token, output] (= (token-has-one-field? token) output)
    [:s "/"],                false
    [:w "word"],             false
    [:w "word" #{}],         false
    [:w "word" #{:N}],       true
    [:w "word" #{:T}],       true
    [:w "word" #{:N :T}],    false
    [:w "word" #{:N :T :P}], false))

(t/deftest can-test-token-has-many-fields?
  (t/are [token, output] (= (token-has-many-fields? token) output)
    [:s "/"],                false
    [:w "word"],             false
    [:w "word" #{}],         false
    [:w "word" #{:N}],       false
    [:w "word" #{:T}],       false
    [:w "word" #{:N :T}],    true
    [:w "word" #{:N :T :P}], true))

(t/deftest can-test-tokens-assignment-complete?
  (t/are [tokens, output] (= (tokens-assignment-complete? tokens) output)
    [[:s "/"]],                                        true
    [[:w "word" #{:N}]],                               true
    [[:w "word" #{:N :I}]],                            false
    [[:w "word" #{:N}] [:s "/"] [:w "next" #{:P}]],    true
    [[:w "word" #{:N :I}] [:s "/"] [:w "next" #{:P}]], false))

(t/deftest can-token-set-fields
  (t/are [token, output] (= (token-set-fields token #{:P}) output)
    [:s "/"],                [:s "/"]
    [:w "android" #{}],      [:w "android" #{:P}]
    [:w "android" #{:T}],    [:w "android" #{:P}]
    [:w "android" #{:P}],    [:w "android" #{:P}]
    [:w "android" #{:P :N}], [:w "android" #{:P}]
    [:w "android" #{:I :N}], [:w "android" #{:P}]))

(t/deftest can-map-token-word
  (let [tokens [[:w "a" #{:I :P}] [:s "."] [:w "1" #{:I :T}]]]
    (t/are [f, output] (= (map-token-word f tokens) output)
      (fn [t] nil), tokens ;; leave the token list unchanged
      (fn [t] (token-set-fields t #{:N})) [[:w "a" #{:N}] [:s "."] [:w "1" #{:N}]])))

(t/deftest can-tokenize-label
  (t/are [label, output] (= (tokenize label) output)
    "android/dogwin.a.gen", [[:w "android"] [:s "/"] [:w "dogwin"] [:s "."] [:w "a"] [:s "."] [:w "gen"]]
    "adware|linux (kwin)",  [[:w "adware"] [:s "|"] [:w "linux"] [:s " ("] [:w "kwin"] [:s ")"]]
    "tr:agent 1-hjk",       [[:w "tr"] [:s ":"] [:w "agent"] [:s " "] [:w "1"] [:s "-"] [:w "hjk"]]
    "!simple-adware!",      [[:s "!"] [:w "simple"] [:s "-"] [:w "adware"] [:s "!"]]))

(t/deftest can-domainize-tokens
  (t/are [tokens, output] (= (domainize tokens) output)
    [[:s "!"] [:w "ginger"] [:s "!"]], [[:s "!"] [:w "ginger" #{:N :T :P :I}] [:s "!"]]
    [[:w "1"] [:w "12"] [:w "123"]],   [[:w "1" #{:I}] [:w "12" #{:I}] [:w "123" #{:I}]]
    [[:w "1"] [:w "a1"] [:w "ab2"]],   [[:w "1" #{:I}] [:w "a1" #{:P :I}] [:w "ab2" #{:P :I}]]
    [[:w "a"] [:w "ab"] [:w "abc"]],   [[:w "a" #{:I}] [:w "ab" #{:T :P :I}] [:w "abc" #{:N :T :P :I}]]))

                                        ; LABELS

(t/deftest can-test-label-valid?
  (t/are [label, output] (= (label-valid? label) output)
    0,   false
    nil, false
    "",  false
    "a", true
    "é", false
    "0", true
    "-", false))

(t/deftest can-find-label-words-between-parenthesis
  (t/are [label, output] (= (label-words-between-parenthesis label) output)
    "android trojan", #{} ;; no parenthesis
    "android [trojan]", #{} ;; no parenthesis
    "android (tro!jan)", #{} ;; not a word
    "android (trojan", #{} ;; no closing parenthesis
    "android (trojan)", #{"trojan"}
    "(variant) android (trojan)", #{"variant" "trojan"}))

(t/deftest can-find-label-words-between-square-brackets
  (t/are [label, output] (= (label-words-between-square-brackets label) output)
    "android trojan",             #{} ;; no brackets
    "android (trojan]",           #{} ;; no brackets
    "android [tro!jan]",          #{} ;; not a word
    "android [trojan",            #{} ;; no closing bracket
    "android [trojan]",           #{"trojan"}
    "[variant] android [trojan]", #{"variant" "trojan"}))

                                        ; RESULTS

(t/deftest can-convert-result-to-id
  (t/is (thrown? AssertionError (result->id [nil "basebridge"])))
  (t/is (thrown? AssertionError (result->id ["" "basebridge"])))
  (t/is (thrown? AssertionError (result->id ["avast" nil])))
  (t/is (thrown? AssertionError (result->id ["avast" ""])))
  (t/are [result, output] (= (result->id result) output)
    ["eset" "dogwin.a"], "eset++dogwin.a"
    ["AVG" "koguo/1"], "avg++koguo/1"))

                                        ; PATTERNS

(t/deftest can-test-pattern-word?
  (t/are [part, output] (= (pattern-word? part) output)
    "TT", false
    "T",  true
    ":",  false))

(t/deftest can-test-pattern-sep?
  (t/are [part, output] (= (pattern-sep? part) output)
    "TT", false
    "T",  false
    ":",  true))

(t/deftest can-select-pattern-parts
  (t/is (= (pattern-parts "T:/P.N (I)") ["T" ":/" "P" "." "N" " (" "I" ")"]))
  (t/is (= (pattern-words "T:/P.N (I)") ["T" "P" "N" "I"]))
  (t/is (= (pattern-keywords "T:/P.N (I)") [:T :P :N :I])))

(t/deftest can-test-pattern-is-compatible-with-token-list?
  (t/are [tokens, output] (= (pattern-compatible? "P.T.N" tokens) output)
    [[:w "" #{:P}] [:w "" #{:T}]],                        false
    [[:w "" #{:P}] [:w "" #{:T}] [:s ""]],                false
    [[:w "" #{:P}] [:w "" #{:T}] [:w "" #{:N}]],          true
    [[:w "" #{:P}] [:w "" #{:T}] [:w "" #{:I}]],          false
    [[:w "" #{:P :I}] [:w "" #{:T :I}] [:w "" #{:N :I}]], true))

(t/deftest can-w-patternize-tokens
  (t/is (thrown? AssertionError (w-patternize [[:t "word"]])))
  (t/are [tokens, output] (= (w-patternize tokens) output)
    [[:s "/"]],                                     "/"
    [[:s "."]],                                     "."
    [[:w "word"]],                                  "w"
    [[:w "word"]],                                  "w"
    [[:w "word"]],                                  "w"
    [[:w "android"] [:s "."] [:w "adrd" #{:N}]],    "w.w"))

(t/deftest can-f-patternize-tokens
  (t/is (thrown? AssertionError (f-patternize [[:w "word" #{}]])))
  (t/is (thrown? AssertionError (f-patternize [[:w "word" #{:not}]])))
  (t/is (thrown? AssertionError (f-patternize [[:w "word" #{:N :P}]])))
  (t/is (thrown? AssertionError (f-patternize [[:w "android" #{:P}] [:s "."] [:w "adrd" #{:I :N}]])))
  (t/are [tokens, output] (= (f-patternize tokens) output)
    [[:s "/"]],                                           "/"
    [[:w "word" #{:N}]],                                  "N"
    [[:w "word" #{:T}]],                                  "T"
    [[:w "word" #{:P}]],                                  "P"
    [[:w "word" #{:I}]],                                  "I"
    [[:w "android" #{:P}] [:s "."] [:w "adrd" #{:N}]],    "P.N"))

(t/deftest can-pattern-set-tokens-fields
  (t/is (= (pattern-set-tokens-fields
            "P:N-I [I]"
            [[:w "android" #{:P}] [:s ":"] [:w "pirates" #{:I :N}]
             [:s "-"] [:w "a" #{:I}] [:s " ["] [:w "trj" #{:I}] [:s "]"]]),
           [[:w "android" #{:P}] [:s ":"] [:w "pirates" #{:N}]
            [:s "-"] [:w "a" #{:I}] [:s " ["] [:w "trj" #{:I}] [:s "]"]]))
  (t/are [tokens, output] (= (pattern-set-tokens-fields "T.P" tokens) output)
    ;; incompatible tokens: return the same token list
    [[:w "" #{}] [:s "."] [:w "" #{}]],           [[:w "" #{}] [:s "."] [:w "" #{}]]
    [[:w "" #{:N}] [:s "."] [:w "" #{:I}]],       [[:w "" #{:N}] [:s "."] [:w "" #{:I}]]
    ;; compatible tokens: set fields from pattern
    [[:w "" #{:T :I}] [:s "."] [:w "" #{:P}]],    [[:w "" #{:T}] [:s "."] [:w "" #{:P}]]
    [[:w "" #{:T :I :N}] [:s "."] [:w "" #{:P}]], [[:w "" #{:T}] [:s "."] [:w "" #{:P}]]))
