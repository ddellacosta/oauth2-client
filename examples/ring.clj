(ns oauth2-examples-ring
  (:require
   [cheshire.core :refer [parse-string]]
   [compojure.core :refer [GET defroutes]]
   [oauth2-client.ring :as oauth2-ring]
   [ring.adapter.jetty :as ring-jetty]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.response :as response]))

(def oauth2-config
  {:authorization-uri "https://github.com/login/oauth/authorize"
   :access-token-uri "https://github.com/login/oauth/access_token"
   :client-id ""
   :client-secret ""
   :redirect-uri "http://127.0.0.1:3000/github-callback"
   :scope nil})

(defn print-github-user-info
  [response]
  (-> response :body (parse-string true) pr-str))

(defroutes oauth2
  (GET "/" []
       (str "<a href=\"/github-user-info\">Github User Info</a>"))

  (GET "/github-user-info" request 
       (oauth2-ring/do-authorized
        oauth2-config
        request
        #(-> (oauth2-ring/authorized-get-request % "https://api.github.com/user")
             print-github-user-info)))

  (GET "/github-callback" request
       (oauth2-ring/oauth2-callback-handler oauth2-config request))

  (GET "/signout" []
       (assoc (response/redirect "/") :session {})))

(def handler
  (-> #'oauth2
      (wrap-session)
      (wrap-params)))

(defn run-jetty
  []
  (ring-jetty/run-jetty #'handler {:join? false :port 3000}))
