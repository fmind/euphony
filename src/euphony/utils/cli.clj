(ns euphony.utils.cli
  (:require [clojure.string :as Str]
            [clojure.tools.cli :refer [parse-opts]]))

                                        ; MESSAGE FUNCTIONS

(defn argc=-error
  "Build an error message about the number of command-line arguments."
  [argc] (str "Failed to start: except exactly " argc " arguments."))

(defn error-message
  "Build a single error message from a list of errors."
  [errors] (str "ERRORS:" \newline (Str/join \newline errors) \newline))


(defn usage-message
  "Build a usage message from a usage summary and an option summary."
  [usage options] (str "USAGE: " usage \newline \newline
                       "OPTIONS: " \newline options))

                                        ; MAIN FUNCTIONS

(defn parse
  "Parse a list of command line arguments given the possible options."
  [args options & {:keys [argc=]}]
  (let [result (parse-opts args options)]
    (cond-> result
      ;; check that the number of arguments matches argc= (optional)
      (and argc= (not= (count (:arguments result)) argc=)) (update :errors conj (argc=-error argc=)))))

(defn feedback!
  "Display a feedback message to the user."
  ([usage options]
   (println (usage-message usage options)))
  ([usage options errors]
   (binding [*out* *err*]
     (when (not-empty errors)
       (println (error-message errors))))
   (feedback! usage options)))
