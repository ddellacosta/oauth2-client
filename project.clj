(defproject oauth2-client "0.1.0"
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
   [org.clojure/clojure "1.7.0"]
   [compojure "1.4.0"]]

  :plugins [[codox "0.8.10"]]

  :profiles
  {:dev
   {:dependencies [[ring/ring-mock "0.2.0"]
                   [org.clojure/tools.nrepl "0.2.10"]]}})
