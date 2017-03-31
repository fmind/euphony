(ns euphony.test-helpers
  (:require [clojure.set :as Set]))

(defn set= [& sets]
  (apply = (map set sets)))

(defn subset? [a b]
  (Set/subset? (set a) (set b)))
