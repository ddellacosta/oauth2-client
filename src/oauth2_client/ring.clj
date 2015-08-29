(ns oauth2-client.ring
  (:require
   [cheshire.core :refer [parse-string]]
   [clojure.string :as string]
   [clojure.walk :refer [keywordize-keys]]
   [clj-http.client :as http-client]
   [ring.util.response :as response]
   [ring.util.request :refer [body-string urlencoded-form?]]
   [oauth2-client.core :as oauth2-client]))

(defn set-session-path
  "Helper to produce a valid path for OAuth2 data which needs to be
  stored in the session.  Heuristics are: if we have a vector for
  session-path, we return that; if we have a keyword we return a vector
  prefixed with the :oauth2 keyword; otherwise we return a vector in
  the format [:oauth2 <authorization-url>]."
  [{:keys [session-path authorization-uri] :as oauth2-config}]
  (cond
    (and session-path (vector? session-path) (seq session-path))
    session-path

    (and session-path (keyword? session-path))
    [:oauth2 session-path]

    :else
    [:oauth2 authorization-uri]))

(defn oauth2-authorization-redirect
  "Creates a ring redirect response with the authoriation-uri for the
  provider. Adds a state value to the session for confirmation when
  the callback endpoint is hit, and sets the original request uri for
  later use, upon receiving the access_token."
  [oauth2-config session-path original-request-uri]
  (let [state             (oauth2-client/generate-anti-forgery-token)
        full-session-path (apply conj [:session] session-path)]
    (-> (assoc oauth2-config :state state)
        oauth2-client/authorization-redirect
        response/redirect
        (assoc-in (conj full-session-path :state) state)
        (assoc-in (conj full-session-path :redirect-on-auth) original-request-uri))))

(defn default-authorization-failure-response
  [{:keys [data] :as clj-http-exception}]
  (ring.util.response/response (pr-str clj-http-exception)))

;; UPDATE DOC-STRING

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

  ([oauth2-config request resource-request-fn]
   (do-authorized oauth2-config request resource-request-fn identity))
  ([oauth2-config request resource-request-fn format-response-fn]
   (do-authorized oauth2-config request resource-request-fn format-response-fn default-authorization-failure-response))
  ([oauth2-config request resource-request-fn format-response-fn authorization-failure-response]
   {:pre [(oauth2-client/validate-oauth2-config oauth2-config)]}

   (let [session-path' (set-session-path oauth2-config)]
     (if-let [access_token (get-in (:session request) (conj session-path' :access_token))]
       (try
         ;; Return the resource from the provider, or whatever comes
         ;; in the 200 response--user needs to handle contingencies
         ;; here.
         (let [authorized-fn-resp (resource-request-fn access_token)]
           (format-response-fn authorized-fn-resp))

         ;; non-2xx response, per how clj-http does it
         ;; (https://github.com/dakrone/clj-http#exceptions):
         (catch Exception e
           (authorization-failure-response e)))

       ;; Otherwise, initiate OAuth2 authorization process
       (oauth2-authorization-redirect oauth2-config session-path' (:uri request))))))

(defn store-access-token
  "Extracts the values returned in the access token response and
  returns the Ring response passed in with these values stored in
  the session."
  [access-token-response response oauth2-config]
  ;; state is accepted by Github, for example, but not part of RFC6749
  (let [session-path (apply conj [:session] (set-session-path oauth2-config))]
    (->> access-token-response
         oauth2-client/parse-json-access-token-response
         (update-in response session-path merge))))

(defn oauth2-redirect-response
  "Accepts a session map and returns a redirect response for the path
  at the session key-path [:oauth2 :redirect-on-auth], with the
  session added back into the response."
  [oauth2-config session]
  (let [redirect-on-auth (get-in session (conj (set-session-path oauth2-config) :redirect-on-auth))]
    (-> (response/redirect redirect-on-auth)
        (assoc :session session))))

(defn default-state-mismatch-response
  "Returns 500 error response for state (for CSRF protection) mismatch."
  [redirect-response original-request]
  (assoc (ring.util.response/response "State does not match") :status 500))

(defn default-authorization-error-response
  "Returns 500 error response for a problem with the authorization
  request to the provider. See:
  https://tools.ietf.org/html/rfc6749#section-4.1.2.1"
  [{:keys [params] :as request}]
  (-> (str "<pre>There was a problem with the authorization request.\n"
           "  Error: " (get params "error") "\n"
           (if-let [desc (get params "error_description")]
             (str "  Error Description: " desc)))
      ring.util.response/response
      (assoc :status 500)))

(defprotocol IParseBody
  (parse-body [body]))

(extend-type java.lang.String
  IParseBody
  (parse-body [body] (parse-string body)))

(extend-type clojure.lang.PersistentArrayMap
  IParseBody
  (parse-body [body] body))

(defn default-token-request-error-response
  "Returns 500 error response for a problem with the token request to
  the provider. See: https://tools.ietf.org/html/rfc6749#section-5.2"
  [{:keys [body] :as request}]
  (let [body' (parse-body body)]
    (-> (str "<pre>There was a problem with the access token request.\n"
             "  Error: " (get body' "error") "\n"
             (if-let [desc (get body' "error_description")]
               (str "  Error Description: " desc)))
        ring.util.response/response
        (assoc :status 500))))

;; UPDATE DOC-STRING

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
  
  TODO: implement error handling for authorization code grant error
   response (per section 4.1.2.1) https://tools.ietf.org/html/rfc6749#section-4.1.2.1

  TODO: implement error handling for token error response
   (per section 5.2) https://tools.ietf.org/html/rfc6749#section-5.2"
  ([oauth2-config request]
   (oauth2-callback-handler
    oauth2-config
    request
    {:state-mismatch-response default-state-mismatch-response
     :authorization-error-response default-authorization-error-response
     :token-request-error-response default-token-request-error-response}))

  ([oauth2-config
    {:keys [session params] :as request}
    {:keys [state-mismatch-response authorization-error-response token-request-error-response]}]

   (if (get params "error")
     (authorization-error-response request)

     (let [{provider-state :state code :code} (keywordize-keys params)
           session-state                      (get-in session (conj (set-session-path oauth2-config) :state))
           redirect-response                  (oauth2-redirect-response oauth2-config session)]

       (if (not= session-state provider-state)
         (state-mismatch-response request redirect-response)

         (let [token-request-resp (oauth2-client/access-token-post-request (assoc oauth2-config :state session-state :code code))]
           (if (-> token-request-resp :body parse-body (get "error"))
             (default-token-request-error-response token-request-resp)
             ;; Make access token request and add access token
             ;; to session in redirect response
             (store-access-token token-request-resp redirect-response oauth2-config))))))))
