(ns oauth2-client.ring
  (:require
   [clojure.string :as string]
   [clojure.walk :refer [keywordize-keys]]
   [clj-http.client :as http-client]
   [ring.util.response :as response]
   [oauth2-client.core :as oauth2-client]))

;; Todo--have it throw an exception detailing what it expects like in
;; http://stackoverflow.com/a/24874961 (?)
(defn validate-oauth2-config
  "Basic validation function for oauth2-config, simply confirms that
  all the required keys have non-blank (per clojure.string/blank?)
  values. These include :authorization-uri, :access-token-uri,
  :client-id, :client-secret and :redirect-uri.
  Returns boolean value."
  [oauth2-config]
  (->> oauth2-config
       ((juxt :authorization-uri :access-token-uri :client-id :client-secret :redirect-uri))
       (every? (complement string/blank?))))

(defn authorization-redirect
  "Helper to generate a ring redirect response for an OAuth2
   authorization request from an oauth2-config hash-map."
  [oauth2-config]
  (->> oauth2-config
       oauth2-client/authorization-query-args
       (oauth2-client/add-params-to-url (:authorization-uri oauth2-config))
       response/redirect))

;; Should handle expired tokens
(defn do-authorized
  "Makes an authorized request via authorized-fn, passing in the
  access_token stored in the session at the path [:oauth2
  :access_token], or alternatively, if no access_token exists, it will
  initiate the process of requesting an authorization grant per
  RFC6749 section 4.1
  (https://tools.ietf.org/html/rfc6749#section-4.1).

  TODO: Handle refresh tokens (https://tools.ietf.org/html/rfc6749#section-1.5)"
  [oauth2-config request authorized-fn]
  {:pre [(validate-oauth2-config oauth2-config)]}
  (let [session (:session request)]
    (if-let [access_token (get-in session [:oauth2 :access_token])]
      (authorized-fn access_token)
      (let [state (oauth2-client/generate-anti-forgery-token)]
        (-> (assoc oauth2-config :state state)
            authorization-redirect
            (assoc-in [:session :oauth2 :state] state)
            (assoc-in [:session :oauth2 :redirect-on-auth] (:uri request)))))))

(defn authorized-get-request
  "Makes a GET request to url using the access_token stored in the
  request headers at the 'Authorization' key, as suggested by RFC6749
  in section 7 (https://tools.ietf.org/html/rfc6749#section-7)."
  [access_token url]
  (let [auth-headers {:headers {"Authorization" (str "token " access_token)}}]
    (-> (http-client/get url auth-headers))))

(defn store-access-token
  "Extracts the values returned in the access token response and
  returns the Ring response passed in with these values stored in
  the session."
  [access-token-response response]
  ;; state is accepted by Github, for example, but not part of RFC6749
  (->> access-token-response
       oauth2-client/parse-json-access-token-response
       (update-in response [:session :oauth2] merge)))

(defn oauth2-redirect-response
  "Accepts a session map and returns a redirect response for the path
  at the session key-path [:oauth2 :redirect-on-auth], with the
  session added back into the response."
  [session]
  (let [redirect-on-auth (get-in session [:oauth2 :redirect-on-auth])]
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
  stored in the session at the path [:oauth :redirect-on-auth].

  By default the function default-state-mismatch-response is used
  to generate the state-mismatch response, but an alternative response
  can optionally be passed in as the third argument.

  This function implements functionality corresponding to sections
  4.1.2 through 4.1.4 of RFC6749.

  https://tools.ietf.org/html/rfc6749#section-4.1.2
 
  TODO: implement error handling (per section 4.1.2.1)"
  ([oauth2-config request]
   (oauth2-callback-handler oauth2-config request (default-state-mismatch-response)))
  ([oauth2-config {:keys [session params]} state-mismatch-response]
   (let [{response-state :state code :code} (keywordize-keys params)
         {session-state :state}             (:oauth2 session)
         redirect-response                  (oauth2-redirect-response session)]
     (if (= session-state response-state)
       ;; Make access token request and add access token
       ;; to session in redirect response
       (-> (assoc oauth2-config :state session-state :code code)
           oauth2-client/access-token-post-request
           (store-access-token redirect-response))
       state-mismatch-response))))
