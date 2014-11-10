(defproject humongorous-api "1.3"
  :description "humongorous-api  provides endpoints to our front Javascript to enable all CRUD operations via Ajax. It also provides some minimal data conversions, for instance, one of the frontenders wanted to save JSON objects with field names such as $$hashKey, and dollar signs are reserved in MongoDB, so we convert $ to * when saving and we convert * to $ when we are fetching."
  :url "https://github.com/lkrubner/humongorous-api"
  :license {:name "Copyright Lawrence Krubner 2014"
            :url "http://www.krubner.com/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.taoensso/timbre "3.2.1"]
                 [dire "0.5.1"]
                 [slingshot "0.10.3"]
                 [ring "1.2.1"]
                 [clj-time "0.6.0"]
                 [org.clojure/data.json "0.2.5"]
                 [compojure "1.1.6"]
                 [cheshire "5.3.1"]
                 [com.novemberain/monger "2.0.0"]
                 [org.clojure/tools.namespace "0.2.4"]
                 [formative "0.8.7"]
                 [lamina "0.5.0"]
                 [me.raynes/fs "1.4.4"]
                 [org.clojure/core.incubator "0.1.3"]
                 [clj-stacktrace "0.2.7"]
                 [liberator "0.11.0"]
                 [overtone/at-at "1.2.0"]
                 [ring/ring-json "0.3.1"]
                 [org.apache.commons/commons-daemon "1.0.9"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/java.classpath "0.2.0"]]}}
  :repositories [["central-proxy" "https://repository.sonatype.org/content/repositories/centralm1/"]]
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :disable-implicit-clean true
  :warn-on-reflection true
  :main humongorous-api.core
  :aot :all
  :jvm-opts ["-Xms100m" "-Xmx1000m" "-XX:-UseCompressedOops"])



