(ns oauth2-client.core
  (:require
   [cheshire.core :refer [parse-string]]
   [clj-http.client :as client]
   [clojure.string :as string]
   [crypto.random :as random]
   [ring.util.codec :as ring-codec]))

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

(defn token-request-query-args
  "TODO make better description -
   https://tools.ietf.org/html/rfc6749#section-4.1.3"
  [{:keys [client-id client-secret redirect-uri scope state code] :as config}]
  {:grant_type "authorization_code"
   :code code
   :client_id client-id
   :client_secret client-secret
   :redirect_uri redirect-uri
   :state state})

(defn add-params-to-url
  "Generates an url with a query string from a url string
   and hash-map for the query parameters."
  [url query-params]
  (->> query-params
       (remove #(-> % val nil?))
       (into {})
       ring-codec/form-encode
       (str url "?")))

(defn access-token-post-request
  "Helper to POST an OAuth2 access token request using the
   configuration in the oauth2-config hash-map."
  ([oauth2-config]
   (access-token-post-request oauth2-config :json))
  ([oauth2-config accept-type]
   (->> oauth2-config
        token-request-query-args
        (hash-map :accept accept-type :form-params)
        (client/post (:access-token-uri oauth2-config)))))

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
