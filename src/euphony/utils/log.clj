(ns euphony.utils.log
  (:require [taoensso.timbre :as log]))

                                        ; DEFAULTS

(def LEVELS #{:debug :info :warn :error :fatal})

                                        ; CONFIGURATIONS

(def set-level! log/set-level!)

(defmacro with-level [level & body]
  `(binding [log/*config* (assoc log/*config* :level ~level)]
     ~@body))

                                        ; MAIN FUNCTIONS

(defmacro log [level & args]
  `(log/log ~level ~@args))
