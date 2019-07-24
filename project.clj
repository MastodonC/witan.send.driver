(defproject witan.send.driver "0.1.0-SNAPSHOT"
  :description "witan.send.drier - Connect incoming data and drive witan.send"
  :url "http://github.com/MastodonC/witan.send.ingest"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/math.combinatorics "0.1.6"]
                 [tick "0.4.14-alpha"]
                 [cljplot "0.0.1-SNAPSHOT"]
                 [dk.ative/docjure "1.13.0"]
                 [net.cgrand/xforms "0.19.0"]
                 [witan.send "0.1.0-SNAPSHOT"]])
