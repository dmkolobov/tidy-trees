(defproject tidy-trees "0.0.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}

  ;; We need to add src/cljs too, because cljsbuild does not add its
  ;; source-paths to the project source-paths
  :source-paths ["src/cljc" "src/cljs"]

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0-beta4"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async  "0.3.443"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.3-alpha2"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :hooks [leiningen.cljsbuild]

  :cljsbuild
  {:builds {;; This build is only used for including any cljs source
            ;; in the packaged jar when you issue lein jar command and
            ;; any other command that depends on it
            :tidy-trees
            {:source-paths ["src/cljs" "src/cljc"]
             ;; The :jar true option is not needed to include the CLJS
             ;; sources in the packaged jar. This is because we added
             ;; the CLJS source codebase to the Leiningen
             ;; :source-paths
             ;:jar true
             ;; Compilation Options
             :compiler
             {:output-to "dev-resources/public/js/tidy_trees.js"
              :optimizations :advanced
              :pretty-print false}}}})
