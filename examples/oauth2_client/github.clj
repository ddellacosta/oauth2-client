;; Github OAuth2:
;; https://developer.github.com/v3/oauth/

(ns oauth2-client.github
  (:require
   [compojure.core :refer [GET defroutes]]
   [oauth2-client.core :as oauth2]
   [oauth2-client.examples-utils :refer [pprint-response-body]]
   [oauth2-client.ring :as oauth2-ring]
   [environ.core :refer [env]]
   [ring.adapter.jetty :as ring-jetty]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.response :as response]))

(def oauth2-config
  {:authorization-uri "https://github.com/login/oauth/authorize"
   :access-token-uri "https://github.com/login/oauth/access_token"
   :client-id (:github-client-id env)
   :client-secret (:github-client-secret env)
   :redirect-uri "http://127.0.0.1:3000/github-callback"
   :scope nil})

(defroutes oauth2
  (GET "/" []
       (str "<a href=\"/github-user-info\">Github User Info</a>"))

  (GET "/github-user-info" request 
       (oauth2-ring/do-authorized
        oauth2-config
        request
        #(-> (oauth2/authorized-request :get % "https://api.github.com/user" (oauth2/auth-headers "token" %))
             pprint-response-body)))

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
