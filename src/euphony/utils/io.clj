(ns euphony.utils.io
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as jio]
            [loom.io :as gio]))

                                        ; SYSTEM

(defn filepath [dir file]
  (jio/file dir file))

(defn mkdir! [dir]
  (jio/make-parents dir)
  (.mkdir (jio/as-file dir)))

                                        ; PARSERS

(def json-line->struct json/parse-string)

                                        ; READERS

(defn read-edn! [path]
  (with-open [r (java.io.PushbackReader. (jio/reader path))]
    (edn/read r)))

(defn read-json! [path]
  (with-open [r (jio/reader path)]
    (json/parse-stream r)))

                                        ; WRITERS

(defn write-dot! [path graph]
  (with-open [w (jio/writer path)]
    (.write w (gio/dot-str graph))))

(defn write-json! [path structure]
  (with-open [w (jio/writer path)]
    (json/generate-stream structure w {:pretty true})))
