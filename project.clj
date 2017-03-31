(defproject euphony "0.1.0"
  :description "A friendly translator in a world full of dangerous malware."
  :license {:name "MIT Licence" :url "https://opensource.org/licenses/MIT"}
  :url "https://github.com/fmind/euphony"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [clj-fuzzy "0.3.3"]
                 [cheshire "5.7.0"]
                 [medley "0.8.4"]
                 [instaparse "1.4.5"]
                 [com.datomic/datomic-free "0.9.5554"]
                 [com.taoensso/timbre "4.8.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [aysylu/loom "0.6.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]]
  :jvm-opts ["-Xmx30G"]
  :codox {:output-path "codox"}
  :main ^:skip-aot euphony.core
  :uberjar-name "euphony.jar"
  :jar-name "lib-euphony.jar"
  :target-path "target/%s"
  :jar-exclusions [#"dev.clj"]
  :profiles {:uberjar {:aot :all}})
