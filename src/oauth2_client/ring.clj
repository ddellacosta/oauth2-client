(ns oauth2-client.ring
  (:require
   [clojure.string :as string]
   [clojure.walk :refer [keywordize-keys]]
   [clj-http.client :as http-client]
   [ring.util.response :as response]
   [oauth2-client.core :as oauth2-client]))

;; Should handle expired tokens
(defn do-authorized
  "Makes an authorized request via authorized-fn, passing in the
  access_token stored in the session at the path [:oauth2 <provider>
  :access_token], or alternatively, if no access_token exists, it will
  initiate the process of requesting an authorization grant per

  (<provider> will either be the value explicitly set at the :provider
  key in the oauth2-config map, or the authorization-uri will be used
  as a placeholder for this.)

  RFC6749 section 4.1
  (https://tools.ietf.org/html/rfc6749#section-4.1).

  TODO: Handle refresh tokens (https://tools.ietf.org/html/rfc6749#section-1.5)"
  [{:keys [provider authorization-uri] :as oauth2-config} request authorized-fn]
  {:pre [(oauth2-client/validate-oauth2-config oauth2-config)]}
  (let [session      (:session request)
        provider-key (or provider authorization-uri)]
    (if-let [access_token (get-in session [:oauth2 provider-key :access_token])]
      (authorized-fn access_token)
      (let [state (oauth2-client/generate-anti-forgery-token)]
        (-> (assoc oauth2-config :state state)
            oauth2-client/authorization-redirect
            response/redirect
            (assoc-in [:session :oauth2 provider-key :state] state)
            (assoc-in [:session :oauth2 provider-key :redirect-on-auth] (:uri request)))))))

(defn store-access-token
  "Extracts the values returned in the access token response and
  returns the Ring response passed in with these values stored in
  the session."
  [access-token-response response {:keys [provider authorization-uri] :as oauth2-config}]
  ;; state is accepted by Github, for example, but not part of RFC6749
  (->> access-token-response
       oauth2-client/parse-json-access-token-response
       (update-in response [:session :oauth2 (or provider authorization-uri)] merge)))

(defn oauth2-redirect-response
  "Accepts a session map and returns a redirect response for the path
  at the session key-path [:oauth2 :redirect-on-auth], with the
  session added back into the response."
  [{:keys [provider authorization-uri] :as oauth2-config} session]
  (let [redirect-on-auth (get-in session [:oauth2 (or provider authorization-uri) :redirect-on-auth])]
    (-> (response/redirect redirect-on-auth)
        (assoc :session session))))

(defn default-state-mismatch-response
  "Returns 500 error response for state (for CSRF protection) mismatch."
  []
  (assoc (ring.util.response/response "State does not match") :status 500))

(defn oauth2-callback-handler
  "Takes an oauth2-config and the resource owner's redirect Ring
  request back to the client after the user has authenticated.

  The state value returned by the resource owner is compared to the
  state value set and stored in the session at the path [:oauth
  :state]. If there is a mismatch, the state-mismatch-response is
  returned. Otherwise, the access token is requested from the resource
  owner, and the user is redirected to the route originally requested,
  stored in the session at the path [:oauth <provider> :redirect-on-auth].

  (<provider> will either be the value explicitly set at the :provider
  key in the oauth2-config map, or the authorization-uri will be used
  as a placeholder for this.)

  By default the function default-state-mismatch-response is used
  to generate the state-mismatch response, but an alternative response
  can optionally be passed in as the third argument.

  This function implements functionality corresponding to sections
  4.1.2 through 4.1.4 of RFC6749.

  https://tools.ietf.org/html/rfc6749#section-4.1.2
  
  TODO: implement error handling (per section 4.1.2.1)"
  ([oauth2-config request]
   (oauth2-callback-handler oauth2-config request (default-state-mismatch-response)))
  ([{:keys [provider authorization-uri] :as oauth2-config}
    {:keys [session params] :as request}
    state-mismatch-response]
   (let [{provider-state :state code :code} (keywordize-keys params)
         session-state                      (get-in session [:oauth2 (or provider authorization-uri) :state])
         redirect-response                  (oauth2-redirect-response oauth2-config session)]
     (if (= session-state provider-state)
       ;; Make access token request and add access token
       ;; to session in redirect response
       (-> (assoc oauth2-config :state session-state :code code)
           oauth2-client/access-token-post-request
           (store-access-token redirect-response oauth2-config))
       state-mismatch-response))))
