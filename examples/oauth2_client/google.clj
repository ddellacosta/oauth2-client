;; Google OAuth2:
;; https://developers.google.com/identity/protocols/OAuth2WebServer

(ns oauth2-client.google
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
  {:authorization-uri "https://accounts.google.com/o/oauth2/auth"
   :access-token-uri "https://www.googleapis.com/oauth2/v3/token" 
   :client-id (:google-client-id env)
   :client-secret (:google-client-secret env)
   :redirect-uri "http://127.0.0.1:3000/google-callback"
   :scope "profile"})

(defroutes oauth2
  (GET "/" []
       (str "<a href=\"/google-user-info\">Google User Info</a>"))

  (GET "/google-user-info" request 
       (oauth2-ring/do-authorized
        oauth2-config
        request
        #(-> (oauth2/authorized-request :get % (str "https://www.googleapis.com/oauth2/v2/userinfo"))
             pprint-response-body)))

  (GET "/google-callback" request
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
