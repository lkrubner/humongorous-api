(ns humongorous-api.middleware
  (:import
   [java.util UUID])
  (:require
   [humongorous-api.tokens :as tokens]
   [ring.util.response :as rr]
   [clj-stacktrace.core :as stack]
   [cheshire.core :as cheshire]))


(defn wrap-json-body
  "I think this exists so we can fail fast and send back the message in the Exception?"
  [handler & [opts]]
  (fn [request]
    (handler (assoc request :body (cheshire/parse-string (slurp (get request :body {})) true)))))

(defn wrap-cors-headers
  "Adding CORS headers to all responses, so the frontenders can make cross-domain requests"
  [handler & [opts]]
  (fn [request]
    (let [resp (handler request)
          origin (get-in request [:headers "origin"])
          origin (if (or (= origin "null") (nil? origin))
                   "*"
                   origin)
          resp (rr/header resp "Content-Type" "application/json")
          resp (rr/header resp "Access-Control-Allow-Origin" (str origin))
          resp (rr/header resp "Access-Control-Allow-Methods" "PUT, DELETE, POST, GET, OPTIONS, XMODIFY")
          resp (rr/header resp "Access-Control-Max-Age" "4440")
          resp (rr/header resp "Access-Control-Allow-Credentials" "true")
          resp (rr/header resp "Access-Control-Allow-Headers" "Authorization, X-Requested-With, Content-Type, Origin, Accept")] 
      resp)))

(defn wrap-token-check
  "Enforcing a limit on how many requests a user can make before they get a new token"
  [handler & [opts]]
  (fn [request]
    (if (tokens/is-valid? request)
      (do
        (tokens/increment-token (:token request))
        (handler request))
    (cheshire/generate-string {:status 401
                               :headers {}
                               :body "Error: no token. We can not allow your request"}
                              {:pretty true}))))

(defn wrap-token
  "Let's make sure that the token is in the request, even before the middleware is done processing, so the token is available to other middleware."
  [handler & [opts]]
  (fn [request]
    (let [uri (:uri request)
          uri-parts (clojure.string/split uri #"/")
          token (get uri-parts 2)
          request (assoc-in request [:json-params :token] token)]
      (handler request))))

(defn wrap-transaction-id
  "Each request to this app needs to have a unique-id, due in part to the many forms of indirection that the Liberator library forces upon us. We use this transaction id to find a document we might be saving to the database, so we can return it to the user."
  [handler & [opts]]
  (fn [request]
    (handler (assoc-in request  [:json-params :transaction-id] (str (java.util.UUID/randomUUID))))))
