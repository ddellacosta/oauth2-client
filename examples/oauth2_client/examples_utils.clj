(ns oauth2-client.examples-utils
  (:import java.io.StringWriter)
  (:require
   [cheshire.core :refer [parse-string]]
   [clojure.pprint :refer [pprint]]))

(defn pprint-response-body
  [response]
  (let [sw (StringWriter.)]
    (pprint (-> response :body (parse-string true)) sw)
    (str "<pre>" (.toString sw) "</pre>")))
