(ns euphony.commands.sub.heuristics-test
  (:require [clojure.test :as t]
            [euphony.commands.sub.heuristics :refer :all]
            [euphony.protocols.conn :as pc]
            [euphony.test-system :as ts]))

(t/use-fixtures :once ts/with-conn-initial)

(t/deftest can-deduce-known-words
  (t/are [tokens, output] (= (deduce-known-words (pc/db ts/*conn-initial*) "" tokens) output)
    ;; one word, unknown
    [[:w "cat" #{}]], [[:w "cat" #{}]]
    [[:w "cat" #{:T}]], [[:w "cat" #{:T}]]
    [[:w "cat" #{:P}]], [[:w "cat" #{:P}]]
    [[:w "cat" #{:T :P :I}]], [[:w "cat" #{:T :P :I}]]
    ;; one word, known (type)
    [[:w "trojan" #{}]], [[:w "trojan" #{:T}]]
    [[:w "trojan" #{:T}]], [[:w "trojan" #{:T}]]
    [[:w "trojan" #{:P}]], [[:w "trojan" #{:T}]]
    [[:w "trojan" #{:T :P :I}]], [[:w "trojan" #{:T}]]
    ;; token list with separators
    [[:w "trojan" #{:T :P}] [:s "!"] [:w "cat" #{:T :P}]],[[:w "trojan" #{:T}] [:s "!"] [:w "cat" #{:T :P}]]))

(t/deftest can-deduce-signature-tokens
  (t/are [tokens, output] (= (deduce-signature-tokens (pc/db ts/*conn-initial*) "" tokens) output)
    ;; ok
    [[:w "12341234" #{:I :P :N :T}]], [[:w "12341234" #{:I}]]
    [[:w "1234abcd" #{:I :P :N :T}]], [[:w "1234abcd" #{:I}]]
    [[:w "123412341234" #{:I :P :N :T}]], [[:w "123412341234" #{:I}]]
    [[:w "1234abcd1234" #{:I :P :N :T}]], [[:w "1234abcd1234" #{:I}]]
    ;; incompatible
    [[:w "abcdabcdabcd" #{:P :N :T}]], [[:w "abcdabcdabcd" #{:P :N :T}]]
    [[:w "123412341234" #{:P :N :T}]], [[:w "123412341234" #{:P :N :T}]]
    [[:w "1234abcd1234" #{:P :N :T}]], [[:w "1234abcd1234" #{:P :N :T}]]
    ;; no digits
    [[:w "abcdabcd" #{:I :P :N :T}]], [[:w "abcdabcd" #{:I :P :N :T}]]
    [[:w "abcdabcdabcd" #{:I :P :N :T}]], [[:w "abcdabcdabcd" #{:I :P :N :T}]]
    ;; not a sig
    [[:w "abczabczabcz" #{:I :P :N :T}]], [[:w "abczabczabcz" #{:I :P :N :T}]]
    [[:w "123z123z123z" #{:I :P :N :T}]], [[:w "123z123z123z" #{:I :P :N :T}]]
    ;; too small
    [[:w "1234" #{:I :P :N :T}]], [[:w "1234" #{:I :P :N :T}]]
    [[:w "abcd" #{:I :P :N :T}]], [[:w "abcd" #{:I :P :N :T}]]
    [[:w "ab12" #{:I :P :N :T}]], [[:w "ab12" #{:I :P :N :T}]]
    ;; too big
    [[:w "abcdabcdabcdabcd" #{:I :P :N :T}]], [[:w "abcdabcdabcdabcd" #{:I :P :N :T}]]
    [[:w "1234123412341234" #{:I :P :N :T}]], [[:w "1234123412341234" #{:I :P :N :T}]]
    [[:w "1234abcd1234abcd" #{:I :P :N :T}]], [[:w "1234abcd1234abcd" #{:I :P :N :T}]]
    ))

(t/deftest can-deduce-words-suffixed-by-ware
  (t/are [tokens, output] (= (deduce-words-suffixed-by-ware (pc/db ts/*conn-initial*) "" tokens) output)
    ;; one word, not suffixed by -ware
    [[:w "cat" #{}]], [[:w "cat" #{}]]
    [[:w "cat" #{:T}]], [[:w "cat" #{:T}]]
    [[:w "cat" #{:P}]], [[:w "cat" #{:P}]]
    [[:w "cat" #{:T :P :I}]], [[:w "cat" #{:T :P :I}]]
    ;; one word, suffixed by -ware
    [[:w "spyware" #{}]], [[:w "spyware" #{:T}]]
    [[:w "spyware" #{:T}]], [[:w "spyware" #{:T}]]
    [[:w "spyware" #{:P}]], [[:w "spyware" #{:T}]]
    [[:w "spyware" #{:T :P :I}]], [[:w "spyware" #{:T}]]
    ;; token list with separators
    [[:w "spyware" #{:T :P}] [:s "!"] [:w "cat" #{:T :P}]],[[:w "spyware" #{:T}] [:s "!"] [:w "cat" #{:T :P}]]))

(t/deftest can-deduce-words-between-parenthesis
  (t/are [label tokens, output] (= (deduce-words-between-parenthesis (pc/db ts/*conn-initial*) label tokens) output)
    ;; one word, not between parenthesis
    "test" [[:w "test" #{}]], [[:w "test" #{}]]
    "test" [[:w "test" #{:I}]], [[:w "test" #{:I}]]
    "test" [[:w "test" #{:I :P}]], [[:w "test" #{:I :P}]]
    ;; one word, between parenthesis
    "(test)" [[:s "("] [:w "test" #{}] [:s ")"]], [[:s "("] [:w "test" #{:I}] [:s ")"]]
    "(test)" [[:s "("] [:w "test" #{:I}] [:s ")"]], [[:s "("] [:w "test" #{:I}] [:s ")"]]
    "(test)" [[:s "("] [:w "test" #{:P}] [:s ")"]], [[:s "("] [:w "test" #{:I}] [:s ")"]]
    "(test)" [[:s "("] [:w "test" #{:I :P}] [:s ")"]], [[:s "("] [:w "test" #{:I}] [:s ")"]]
    ;; error cases
    "(test" [[:s "("] [:w "test" #{:I :P}] [:s ")"]], [[:s "("] [:w "test" #{:I :P}] [:s ")"]]
    "test)" [[:s "("] [:w "test" #{:I :P}] [:s ")"]], [[:s "("] [:w "test" #{:I :P}] [:s ")"]]
    "(te!st)" [[:s "("] [:w "test" #{:I :P}] [:s ")"]], [[:s "("] [:w "test" #{:I :P}] [:s ")"]]))

(t/deftest can-deduce-words-between-square-brackets
  (t/are [label tokens, output] (= (deduce-words-between-square-brackets (pc/db ts/*conn-initial*) label tokens) output)
    ;; one word, not between square brackets
    "test" [[:w "test" #{}]], [[:w "test" #{}]]
    "test" [[:w "test" #{:I}]], [[:w "test" #{:I}]]
    "test" [[:w "test" #{:I :P}]], [[:w "test" #{:I :P}]]
    ;; one word, between square brackets
    "[test]" [[:s "["] [:w "test" #{}] [:s "]"]], [[:s "["] [:w "test" #{:I :T}] [:s "]"]]
    "[test]" [[:s "["] [:w "test" #{:I}] [:s "]"]], [[:s "["] [:w "test" #{:I :T}] [:s "]"]]
    "[test]" [[:s "["] [:w "test" #{:P}] [:s "]"]], [[:s "["] [:w "test" #{:I :T}] [:s "]"]]
    "[test]" [[:s "["] [:w "test" #{:I :P}] [:s "]"]], [[:s "["] [:w "test" #{:I :T}] [:s "]"]]
    "[test]" [[:s "["] [:w "test" #{:I :T :P}] [:s "]"]], [[:s "["] [:w "test" #{:I :T}] [:s "]"]]
    ;; error cases
    "[test" [[:s "["] [:w "test" #{:I :P}] [:s "]"]], [[:s "["] [:w "test" #{:I :P}] [:s "]"]]
    "test]" [[:s "["] [:w "test" #{:I :P}] [:s "]"]], [[:s "["] [:w "test" #{:I :P}] [:s "]"]]
    "[te!st]" [[:s "["] [:w "test" #{:I :P}] [:s "]"]], [[:s "["] [:w "test" #{:I :P}] [:s "]"]]))

(t/deftest can-infer-words-in-english-sentence-at-beginning
  (t/are [tokens, output] (= (infer-words-in-english-sentence-at-beginning (pc/db ts/*conn-initial*) "" tokens) output)
    ;; OK (baseline)
    [[:w "variant" #{:I :N}] [:w "test" #{:I :P}]], [[:w "variant" #{:I}] [:w "test" #{:I}]]
    [[:w "variant" #{:I :N}] [:s " "] [:w "test" #{:I :P}]], [[:w "variant" #{:I}] [:s " "] [:w "test" #{:I}]]
    [[:w "variant" #{:I :N}] [:s "-"] [:w "test" #{:I :P}]], [[:w "variant" #{:I}] [:s "-"] [:w "test" #{:I}]]
    ;; not enough word
    [[:w "variant" #{:I :N}]], [[:w "variant" #{:I :N}]]
    ;; no information token at beginning
    [[:w "variant" #{:P :N}] [:w "test" #{:I :P}]], [[:w "variant" #{:P :N}] [:w "test" #{:I :P}]]
    ;; no alphabetic word at beginning
    [[:w "variant0" #{:I :N}] [:w "test" #{:I :P}]], [[:w "variant0" #{:I :N}] [:w "test" #{:I :P}]]
    ;; no English words at beginning
    [[:w "adrd" #{:I :N}] [:w "test" #{:I :P}]], [[:w "adrd" #{:I :N}] [:w "test" #{:I :P}]]
    ;; no correct separator
    [[:w "variant" #{:I :N}] [:s "/"] [:w "test" #{:I :P}]], [[:w "variant" #{:I :N}] [:s "/"] [:w "test" #{:I :P}]]))

(t/deftest can-infer-fields-by-elimination
  (t/are [tokens, output] (= (infer-fields-by-elimination (pc/db ts/*conn-initial*) "" tokens) output)
    ;; no known fields
    [[:w "1" #{}] [:w "2" #{}]], [[:w "1" #{}] [:w "2" #{}]]
    [[:w "1" #{:T :P}] [:w "2" #{:T :N}]], [[:w "1" #{:T :P}] [:w "2" #{:T :N}]]
    ;; ;; one known field
    [[:w "1" #{:N}] [:w "2" #{:N :T}]], [[:w "1" #{:N}] [:w "2" #{:T}]]
    [[:w "1" #{:T}] [:w "2" #{:T :P}]], [[:w "1" #{:T}] [:w "2" #{:P}]]
    [[:w "1" #{:P}] [:w "2" #{:P :N}]], [[:w "1" #{:P}] [:w "2" #{:N}]]
    [[:w "1" #{:I}] [:w "2" #{:I :N}]], [[:w "1" #{:I}] [:w "2" #{:I :N}]]
    ;; ;; two known field
    [[:w "1" #{:N}] [:w "2" #{:T}] [:w "3" #{:N :T :P}]], [[:w "1" #{:N}] [:w "2" #{:T}] [:w "3" #{:P}]]
    [[:w "1" #{:I}] [:w "2" #{:T}] [:w "3" #{:N :T :P :I}]], [[:w "1" #{:I}] [:w "2" #{:T}] [:w "3" #{:N :P :I}]]
    ;; cannot set to empty fields
    [[:w "1" #{:N}] [:w "2" #{:T}] [:w "3" #{:N :T}]], [[:w "1" #{:N}] [:w "2" #{:T}] [:w "3" #{:N :T}]]
    [[:w "1" #{:P}] [:w "2" #{:T}] [:w "3" #{:P :T}]], [[:w "1" #{:P}] [:w "2" #{:T}] [:w "3" #{:P :T}]]))

(t/deftest can-infer-fields-from-compatible-patterns
  (let [label "androidos.trojan.adrd"
        tokens [[:w "androidos" #{:P}] [:s "."] [:w "trojan" #{:T}] [:s "."] [:w "adrd" #{:N :I}]]
        found! [[:w "androidos" #{:P}] [:s "."] [:w "trojan" #{:T}] [:s "."] [:w "adrd" #{:N}]]
        not-found! tokens
        local-conn (pc/transact ts/*conn-initial*
                             [{:db/id "l1" :label/label "androidos.trojan.adrd" :label/words-pattern "w.w.w"}
                              {:db/id "av1" :antivirus/antivirus "av1"} {:result/antivirus "av1" :result/label "l1"}])]
    ;; TODO: one unknown token, two words in tokens
    (t/are [conn, output] (= (infer-fields-from-compatible-patterns (pc/db conn) label tokens) output)
      ;; no compatible patterns
      local-conn, tokens
      ;; one compatible pattern
      (pc/transact local-conn
                  [{:db/id "l2" :label/label "android.trojan.base"
                    :label/words-pattern "w.w.w" :label/fields-pattern "P.T.N"}
                   {:result/label "l2" :result/antivirus [:antivirus/antivirus "av1"]}]), found!
      ;; incompatible antivirus
      (pc/transact local-conn
                  [{:db/id "av2" :antivirus/antivirus "av2"}
                   {:db/id "l2" :label/label "android.trojan.base"
                    :label/words-pattern "w.w.w" :label/fields-pattern "P.T.N"}
                   {:result/label "l2" :result/antivirus "av2"}]), not-found!
      ;; incompatible words-pattern (separators)
      (pc/transact local-conn
                  [{:db/id "l2" :label/label "android:trojan:base"
                    :label/words-pattern "w:w:w" :label/fields-pattern "P.T.N"}
                   {:result/label "l2" :result/antivirus [:antivirus/antivirus "av1"]}]), not-found!
      ;; incompatible words-pattern (cardinality)
      (pc/transact local-conn
                  [{:db/id "l2" :label/label "android.base"
                    :label/words-pattern "w.w" :label/fields-pattern "P.N"}
                   {:result/label "l2" :result/antivirus [:antivirus/antivirus "av1"]}]), not-found!
      ;; incompatible fields-pattern
      (pc/transact local-conn
                  [{:db/id "l2" :label/label "trojan.android.base"
                    :label/words-pattern "w.w.w" :label/fields-pattern "T.P.N"}
                   {:result/label "l2" :result/antivirus [:antivirus/antivirus "av1"]}]), not-found!
      ;; multiple compatible pattern
      (pc/transact local-conn
                  [{:db/id "l2" :label/label "android.trojan.base"
                    :label/words-pattern "w.w.w" :label/fields-pattern "P.T.N"}
                   {:result/label "l2" :result/antivirus [:antivirus/antivirus "av1"]}
                   {:db/id "l3" :label/label "android.trojan.1"
                    :label/words-pattern "w.w.w" :label/fields-pattern "P.T.I"}
                   {:result/label "l3" :result/antivirus [:antivirus/antivirus "av1"]}]), not-found!)))

(t/deftest can-infer-name-from-last-one-unknown-token
  (t/are [tokens, output] (= (infer-name-from-last-one-unknown-token (pc/db ts/*conn-initial*) "" tokens) output)
    ;; OK (baseline)
    [[:w "T" #{:T}] [:w "P" #{:P}] [:w "adrd" #{:N :I}]], [[:w "T" #{:T}] [:w "P" #{:P}] [:w "adrd" #{:N}]]
    ;; not enough words to infer
    [[:w "T" #{:T}]], [[:w "T" #{:T}]]
    ;; not only one ambiguous token
    [[:w "T" #{:T}] [:w "P" #{:P :I}] [:w "adrd" #{:N :I}]], [[:w "T" #{:T}] [:w "P" #{:P :I}] [:w "adrd" #{:N :I}]]
    ;; the ambiguous token cannot be a name
    [[:w "T" #{:T}] [:w "P" #{:P}] [:w "adrd" #{:I :P}]], [[:w "T" #{:T}] [:w "P" #{:P}] [:w "adrd" #{:I :P}]]
    ;; ;; a name field is already set for another token
    [[:w "T" #{:N}] [:w "P" #{:P}] [:w "adrd" #{:N :I}]], [[:w "T" #{:N}] [:w "P" #{:P}] [:w "adrd" #{:N :I}]]
    ;; the ambiguous word does not contain a vowel
    [[:w "T" #{:T}] [:w "P" #{:P}] [:w "drrd" #{:N :I}]], [[:w "T" #{:T}] [:w "P" #{:P}] [:w "drrd" #{:N :I}]]
    ;; the ambiguous word is in the dictionnary
    [[:w "T" #{:T}] [:w "P" #{:P}] [:w "pirate" #{:N :I}]], [[:w "T" #{:T}] [:w "P" #{:P}] [:w "pirate" #{:N :I}]]))

(t/deftest can-infer-name-from-last-two-unknown-tokens
  (t/are [tokens, output] (= (infer-name-from-last-two-unknown-tokens (pc/db ts/*conn-initial*) "" tokens) output)
    ;; OK (baseline)
    [[:w "adrd" #{:I :N}] [:w "test" #{:I :N}]], [[:w "adrd" #{:N}] [:w "test" #{:I}]]
    [[:w "test" #{:I :N}] [:w "adrd" #{:I :N}]], [[:w "test" #{:I}] [:w "adrd" #{:N}]]
    ;; not two unknown tokens
    [[:w "adrd" #{:I :N}]], [[:w "adrd" #{:I :N}]]
    [[:w "adrd" #{:I :N}] [:w "test" #{:I :N}] [:w "abcd" #{:I :N}]], [[:w "adrd" #{:I :N}] [:w "test" #{:I :N}] [:w "abcd" #{:I :N}]]
    ;; not only one English word
    [[:w "star" #{:I :N}] [:w "test" #{:I :N}]], [[:w "star" #{:N :I}] [:w "test" #{:I :N}]]
    ;; not only one non English word
    [[:w "adrd" #{:I :N}] [:w "abcd" #{:I :N}]], [[:w "adrd" #{:N :I}] [:w "abcd" #{:I :N}]]
    ;; not a valid inference domain #{:I :N}
    [[:w "adrd" #{:I :P}] [:w "test" #{:I :N}]], [[:w "adrd" #{:I :P}] [:w "test" #{:I :N}]]
    ;; token text is too small
    [[:w "adr" #{:I :N}] [:w "test" #{:I :N}]], [[:w "adr" #{:N :I}] [:w "test" #{:N :I}]]))

(t/deftest can-infer-synonyms-from-known-platforms-and-types
  (t/are [tokens, output] (= (infer-synonyms-from-known-platforms-and-types (pc/db ts/*conn-initial*) "" tokens) output)
    ;; word is a synonym (baseline)
    [[:w "android" #{:P :T :I :N}]], [[:w "android" #{:P}]]       ;; with androidos
    [[:w "ransomware" #{:P :T :I :N}]], [[:w "ransomware" #{:T}]] ;; with ransom
    ;; word is not a synonym
    [[:w "andjoo" #{:P :T :I :N}]], [[:w "andjoo" #{:P :T :I :N}]]
    [[:w "ransoware" #{:P :T :I :N}]], [[:w "ransoware" #{:P :T :I :N}]]
    ;; word is too small
    [[:w "an" #{:P :T :I :N}]], [[:w "an" #{:P :T :I :N}]]
    ;; word is not compatible
    [[:w "android" #{:T :I :N}]], [[:w "android" #{:T :I :N}]]))
