(ns oauth2-client.core
  (:require
   [cheshire.core :refer [parse-string]]
   [clj-http.client :as http]
   [clojure.string :as string]
   [crypto.random :as random]
   [ring.util.codec :as ring-codec]))

;; Todo--maybe have it throw an exception detailing what it expects like in
;; http://stackoverflow.com/a/24874961 (?)
(defn validate-oauth2-config
  "Basic validation function for oauth2-config, simply confirms that
  all the required keys have non-blank (per clojure.string/blank?)
  values. These include :authorization-uri, :access-token-uri,
  :client-id and :client-secret. Returns boolean value."
  [oauth2-config]
  (->> oauth2-config
       ((juxt :authorization-uri :access-token-uri :client-id :client-secret))
       (every? (complement string/blank?))))

(defn remove-nils-from-map
  [m]
  (->> m (remove #(-> % val nil?)) (into {})))

(defn add-params-to-url
  "Generates an url with a query string from a url string
   and hash-map for the query parameters."
  [url query-params]
  (->> query-params
       remove-nils-from-map
       ring-codec/form-encode
       (str url "?")))

(defn generate-anti-forgery-token
  "Generates random string for anti-forgery-token."
  []
  (string/replace (random/base64 60) #"[\+=/]" "-"))

;; https://tools.ietf.org/html/rfc6749#section-4.1.1
(defn authorization-query-args
  "Generates a hash-map representation of the default arguments
  for a authorization query as specified in section 4.1.1 in RFC 6749."
  [{:keys [client-id redirect-uri scope state] :as config}]

  ;; required fields
  {:response_type "code"
   :client_id client-id

   ;; optional fields
   :redirect_uri redirect-uri
   :scope scope

   ;; optional, but recommended. Added by default in this library for security's sake.
   :state (or state (generate-anti-forgery-token))})

(defn authorization-redirect
  "Helper to generate a response url for an OAuth2 authorization
  request from an oauth2-config hash-map."
  [{:keys [authorization-query-args-fn]
    :or {authorization-query-args-fn authorization-query-args}
    :as oauth2-config}]
  (->> oauth2-config
       authorization-query-args-fn
       (add-params-to-url (:authorization-uri oauth2-config))))

(defn token-request-query-args
  "Extracts default arguments from the oauth2-config hash-map
  according to the OAuth2 RFC6749.  Expects the following keys:
  :code :client_id :redirect-uri :state
  :redirect-uri is optional depending on whether or not it was passed
  initially during the authorization step.
  :state is optional and not described in the RFC for the token
  request step, but used by some providers (i.e. Github).
  (see https://tools.ietf.org/html/rfc6749#section-4.1.3)"
  [{:keys [client-id client-secret redirect-uri state code] :as config}]

  ;; required fields
  {:grant_type "authorization_code"
   :code code
   :client_id client-id
   :client_secret client-secret

   ;; optional fields
   :redirect_uri redirect-uri
   :state state})

(defn access-token-post-request
  "Helper to POST an OAuth2 access token request using the
   configuration in the oauth2-config hash-map.

  By default, the token-request-query-args function is used to format
  the arguments for posting to the token request url, but can be
  overridden by providing a :token-request-query-args-fn member in the
  oauth2-config map.

  You can also toggle debugging of the clj-http.client/post call by
  passing in :debug-http-client? true in the oauth2-config map."
  ;;
  ;; TODO make it easier to add clj-http args to the http/post call.
  ;;
  ([oauth2-config]
   (access-token-post-request oauth2-config :json))
  ([{:keys [debug-http-client? token-request-query-args-fn]
     :or {token-request-query-args-fn token-request-query-args}
     :as oauth2-config} accept-type]
   (->> oauth2-config
        token-request-query-args-fn
        (hash-map :accept accept-type
                  :debug debug-http-client?
                  :debug-body debug-http-client?
                  :throw-entire-message? true
                  :form-params)
        (http/post (:access-token-uri oauth2-config)))))

(defn parse-json-access-token-response
  "Returns the access token from a JSON response body per RFC6749
  (https://tools.ietf.org/html/rfc6749#section-5.1)."
  [{body :body}]
  (parse-string body true))

(defn parse-form-access-token-response
  "Alternate function to allow retrieve
   access_token when passed in form-encoded."
  [{body :body}]
  (ring.util.codec/form-decode body))

(defn auth-headers
  "Returns a hash-map with Authorization request headers per section 7
  of RFC6749 (https://tools.ietf.org/html/rfc6749#section-7)."
  [method access_token]
  {:headers {"Authorization" (str method " " access_token)}})

(defn authorized-request
  "Makes a GET request to url using the access_token stored in the
  request headers at the 'Authorization' key, as suggested by RFC6749
  in section 7 (https://tools.ietf.org/html/rfc6749#section-7).

  The third, optional argument is the configuration for the get/post
  functions provided by clj-http.client.  By default only the
  Authorization header is set with the authorization method of
  'Bearer,' using the provided auth-headers function.

  To override this or any other get/post configuration parameters
  simply pass in a map formatted per clj-http's conventions
  (https://github.com/dakrone/clj-http). For example, to add debugging
  to an authorized GET request using a Github access_token:

  (let [clj-http-config (-> (auth-headers \"token\" access_token)
                            (assoc :debug true :debug-body true))]
    (authorized-request :get \"https://api.github.com/users\" clj-http-config))

  Passed-in authorization headers will override the default, and any other
  arguments will get added to the clj-http get or post function's
  final second argument.
  "
  ([method access_token url]
   (authorized-request method access_token url {})) 
  ([method access_token url clj-http-config]
   (let [clj-http-config' (merge (auth-headers "Bearer" access_token) clj-http-config)]
     (case method
       :get    (http/get url clj-http-config')
       :post   (http/post url clj-http-config')
       :put    (http/put url clj-http-config')
       :delete (http/delete url clj-http-config')))))
