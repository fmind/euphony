(ns euphony.components.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [euphony.utils.db :as Db]
            [euphony.utils.io :as io]
            [euphony.utils.log :as log]))

                                        ; HELPERS

(defn install-schema [conn schema-file]
  (log/log :info "** installing schema")
  (d/transact conn (io/read-edn! schema-file)))

(defn install-seeds [conn seeds-file]
  (log/log :info "** installing seeds")
  (d/transact conn (io/read-edn! seeds-file)))

                                        ; COMPONENT

(defrecord Datomic [conn uri
                    schema-file seeds-file
                    reset-on-start reset-on-stop]
  component/Lifecycle
  (start [this]
    (log/log :info "Starting Datomic at:" uri)
    (when reset-on-start
      (log/log :info "* deleting database")
      (d/delete-database uri))
    (let [new? (d/create-database uri)
          conn (d/connect uri)]
      (when new?
        (log/log :info "* creating database")
        (install-schema conn schema-file)
        (install-seeds conn seeds-file))
      (assoc this :conn conn)))
  (stop [this]
    (log/log :info "Stopping Datomic:" uri)
    (when reset-on-stop
      (log/log :info "* deleting database")
      (d/delete-database uri))
    (dissoc this :conn)))

(defn new-datomic [conf]
  (map->Datomic conf))
