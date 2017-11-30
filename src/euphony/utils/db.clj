(ns euphony.utils.db
  (:require [datomic.api :as d]))

                                        ; MAIN FUNCTIONS

(def q d/q)
(def db d/db)
(def pull d/pull)
(def entity d/entity)
(def with (comp :db-after d/with))

(defn transact [conn tx-data]
  (d/transact conn tx-data)
  conn)
