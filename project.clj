(defproject middlesphere/clj-compress "0.1.0"
  :description "A Clojure library designed to compress/decompress data."
  :url "https://github.com/mikeananev/clj-compress.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-tools-deps "0.4.3"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}

  :profiles {:kaocha {:dependencies [[lambdaisland/kaocha "0.0-389"]]}}

  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner" ]})
