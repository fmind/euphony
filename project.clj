(defproject euphony "0.1.0"
  :description "A friendly translator in a world full of dangerous malware."
  :license {:name "AGPLv3" :url "https://opensource.org/licenses/agpl-3.0"}
  :url "https://github.com/fmind/euphony"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [com.datomic/datomic-free "0.9.5561"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.clojure/tools.cli "0.3.5"]
                 [com.taoensso/timbre "4.8.0"]
                 [aysylu/loom "0.6.0"]
                 [instaparse "1.4.5"]
                 [clj-fuzzy "0.3.3"]
                 [cheshire "5.7.0"]
                 [medley "0.8.4"]]
  :codox {:output-path "codox"}
  :main ^:skip-aot euphony.core
  :uberjar-name "euphony.jar"
  :jar-name "lib-euphony.jar"
  :target-path "target/%s"
  :jar-exclusions [#"dev.clj"]
  :profiles {:uberjar {:aot :all}})
