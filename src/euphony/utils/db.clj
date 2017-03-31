(ns euphony.utils.db
  (:require [datomic.api :as d]))

                                        ; MAIN FUNCTIONS

(def q d/q)
(def pull d/pull)
(def entity d/entity)
