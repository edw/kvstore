(defproject kvstore "0.1.0-SNAPSHOT"
  :description "KV Store"
  :url "http://github.com/edw/kvstore"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "1.2.603"]
                 [org.clojure/data.priority-map "1.0.0"]]
  :plugins [[lein-marginalia "0.9.1"]]
  :repl-options {:init-ns kvstore.core})
