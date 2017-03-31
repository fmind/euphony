(ns euphony.components.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as datomic]
            [euphony.protocols.conn :as pc]
            [euphony.utils
             [io :as io]
             [log :as log]]))

                                        ; HELPERS

(def transact* (comp :db-after datomic/with))

(defn install-schema [conn schema-file]
  (pc/transact conn (io/read-edn! schema-file)))

(defn install-seeds [conn seeds-file]
  (pc/transact conn (io/read-edn! seeds-file)))

                                        ; COMPONENT

(defrecord Datomic [reset-on-stop reset-on-start
                    uri schema-file seeds-file
                    no-side-effect conn]
  component/Lifecycle
  (start [this]
    (log/log :info "Starting Datomic at:" uri)
    (when reset-on-start
      (log/log :info "* deleting database")
      (datomic/delete-database uri))
    (let [created (datomic/create-database uri), conn (datomic/connect uri)
          this (assoc this :conn (if no-side-effect (datomic/db conn) conn))]
      (if (not created) this
          (do (log/log :info "* creating database")
              (-> this (install-schema schema-file) (install-seeds seeds-file))))))
  (stop [this]
    (log/log :info "Stopping Datomic:" uri)
    (when reset-on-stop
      (log/log :info "* deleting database")
      (datomic/delete-database uri))
    (dissoc this :conn))
  pc/Conn
  (db [this]
    (if no-side-effect
      (:conn this) ;; datomic.db
      (datomic/db (:conn this))))
  (transact [this datoms]
    (if no-side-effect
      (update this :conn transact* datoms)
      (do (datomic/transact (:conn this) datoms) this))))

(defn new-datomic [conf]
  (map->Datomic conf))
