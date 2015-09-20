(defproject ddellacosta/oauth2-client "0.2.0"
  :description "OAuth2/OpenID Connect Client Library for Clojure."

  :url "https://github.com/ddellacosta/oauth2-client"

  :license {:name "MIT License"
            :url "http://dd.mit-license.org"}

  :dependencies
  [[cheshire "5.5.0"]
   [clj-http "2.0.0"]
   [crypto-random "1.2.0"]
   [prismatic/schema "0.4.3"]
   [ring "1.4.0"]
   [ring/ring-codec "1.0.0"]
   [org.clojure/clojure "1.7.0"]]

  :plugins [[codox "0.8.13"]]

  :codox
  {:exclude [oauth2-client.examples-utils
             oauth2-client.github
             oauth2-client.google]}

  :profiles
  {:dev
   {:source-paths ["examples"]

    :plugins [[lein-environ "1.0.0"]]

    :dependencies
    [[compojure "1.4.0"]
     [environ "1.0.0"]
     [org.clojure/tools.nrepl "0.2.10"]
     [ring/ring-codec "1.0.0"]
     [ring/ring-mock "0.2.0"]]}})
