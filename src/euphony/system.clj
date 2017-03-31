(ns euphony.system
  (:require [clojure.java.io :as jio]
            [com.stuartsierra.component :as component]
            [euphony.components.datomic :refer [new-datomic]]))

                                        ; DEFAULTS

(def CONF {:conn {:uri "datomic:mem://euphony"
                  :schema-file (jio/resource "schema.edn")
                  :seeds-file (jio/resource "seeds-max.edn")}})

                                        ; ALIASES

(def start component/start-system)
(def stop component/stop-system)

                                        ; CONSTRUCTORS

(defn- components
  [conf]
  (component/system-map
   :conn (new-datomic (:conn conf))))

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
