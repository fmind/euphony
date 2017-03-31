(ns euphony.protocols.conn)

(defprotocol Conn
  "Based on datomic.api"
  (db [this])
  (transact [this datoms]))
