(defproject gh-contributors-cli "0.1.0-SNAPSHOT"
  :description "CLI created to help hiring managers to reach out to the most active OpenSource contributors."
  :url "https://github.com/cesarcneto/gh-contributors-cli"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.csv "1.0.0"]
                 [org.clojure/tools.cli "1.0.206"]
                 [babashka/babashka.curl "0.1.1"]
                 [cheshire "5.10.2"]]
  :main ^:skip-aot gh-contributors-cli.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
