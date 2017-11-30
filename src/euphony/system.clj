(ns euphony.system
  (:require [clojure.java.io :as jio]
            [com.stuartsierra.component :as component]
            [euphony.components.datomic :refer [new-datomic]]))

                                        ; DEFAULTS

(def CONF {:datomic {:schema-file (jio/resource "schema.edn")
                     :seeds-file (jio/resource "seeds.edn")
                     :uri "datomic:mem://euphony"}})

                                        ; ALIASES

(def start component/start-system)
(def stop component/stop-system)

                                        ; CONSTRUCTORS

(defn- components
  [conf]
  (component/system-map
   :datomic (new-datomic (:datomic conf))))

(defn- dependencies
  [conf] {})

(defn new-system
  [conf]
  (let [conf (merge-with merge CONF conf)]
    (component/system-using (components conf)
                            (dependencies conf))))

(defmacro with-system [[binding conf] & body]
  `(let [~binding (new-system ~conf)
         ~binding (start ~binding)]
     (try
       ~@body
       (finally
         (stop ~binding)))))
