(ns humongorous-api.server
  (:import
   [java.net URL]
   [java.util UUID])
  (:require
   [humongorous-api.controller :as controller]
   [humongorous-api.perpetual :as perpetual]
   [humongorous-api.startup :as startup]
   [humongorous-api.tokens :as tokens]
   [humongorous-api.dates :as dt]
   [compojure.core :refer :all]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [me.raynes.fs :as fs]
   [liberator.core :refer [resource defresource]]
   [ring.util.response :as rr]
   [taoensso.timbre :as timbre]
   [clojure.java.io :as io]
   [clojure.string :as st])
  (:use
   [ring.middleware.params]
   [ring.middleware.keyword-params]
   [ring.middleware.multipart-params]
   [ring.middleware.nested-params]
   [ring.middleware.file]
   [ring.middleware.resource]
   [ring.middleware.content-type]
   [ring.adapter.jetty :only [run-jetty]]
   [ring.middleware.json]))



(defn intro []
  (assoc
      (ring.util.response/response "See documentation here: https://github.com/lkrubner/humongorous-api")
    :headers {"Content-Type" "text/plain"}))

(defn get-token [request]
  (assoc
      (ring.util.response/response (str "{ token: " (tokens/create-token) " }"))
    :headers {"Content-Type" "application/json"}))

(defn invalid-token [request]
  (assoc
      (ring.util.response/response (str "{ error : \"Invalid token\", request: " (str request) " }"))
    :headers {"Content-Type" "application/json"}))

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
      (timbre/log :trace "In wrap-cors-headers, the origin is: " origin " and the request method is " (get-in request [:request-method])) 
      resp)))

(defn wrap-token-check
  "Enforcing a limit on how many requests a user can make before they get a new token"
  [handler & [opts]]
  (fn [request]
    (if (tokens/is-valid? request)
      (do
        (tokens/increment-token (:token request))
        (handler request))
      (invalid-token request))))

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

;; a helper to create an absolute url for the entry with the given id
(defn build-entry-url [request]
  (if (get-in request [:params :document-id])
    (URL. (format "%s://%s:%s%s/%s"
                  (name (:scheme request))
                  (:server-name request)
                  (:server-port request)
                  (:uri request)
                  (get-in request [:params :document-id])))
    (URL. (format "%s://%s:%s%s"
                  (name (:scheme request))
                  (:server-name request)
                  (:server-port request)
                  (:uri request)))))

;; For PUT and POST check if the content type is json.
(defn check-content-type [ctx content-types-we-allow]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (let [vector-of-headers-sent-by-client (st/split (get-in ctx [:request :headers "content-type"]) #";")]
      (or
       (not (every? nil? (map #(some #{%} content-types-we-allow) vector-of-headers-sent-by-client)))
       [false {:message "Unsupported Content-Type"}]))
    true))


(defresource collection-resource
  :available-media-types ["application/json"]
  :allowed-methods [:get :put]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(controller/request-malformed? % ::data)
  :location #(build-entry-url (get % :request))
  :respond-with-entity? true
  :multiple-representations? false
  :new? (fn [ctx]
          (controller/is-document-created? ctx))
  :handle-created (fn [ctx]
                    (controller/document-created ctx))
  :put! (fn [ctx]
          (timbre/log :trace " now we are in collection-resource :put! ")
          (controller/document-resource-put! ctx))
  :handle-ok (fn [ctx]
               (timbre/log :trace " now we are in collection-resource :handle-ok")
               (controller/collection-resource-handle-ok ctx)))

(defresource document-resource 
  :allowed-methods [:get :post :put :delete]
  :known-content-type? #(check-content-type % ["application/json"])
  :exists? (fn [ctx] true)
  :existed? (fn [ctx] (controller/document-resource-existed? ctx))
  :available-media-types ["application/json"]
  :respond-with-entity? true
  :malformed? #(controller/request-malformed? % ::data)
  :can-put-to-missing? true
  :multiple-representations? false
  :can-post-to-missing? true
  :new? (fn [ctx] (if (= (get-in ctx [:request :method]) :post) false true))
  :handle-ok (fn [ctx]
               (timbre/log :trace " now we are in document-resource :handle-ok")
               (controller/document-resource-handle-ok ctx))
  :put! (fn [ctx]
          (timbre/log :trace " now we are in document-resource put!")
          (controller/document-resource-put! ctx))
  :post! (fn [ctx]
           (timbre/log :trace " now we are in document-resource :post!")
           (controller/document-resource-post! ctx))
  :delete! (fn [ctx]
             (timbre/log :trace " now we are in document-resource :delete!")
             (controller/document-resource-delete! ctx)))


(defroutes app-routes
  (ANY "/" [] (intro))
  (GET "/token" request (get-token request))
  (ANY "/v0.2/:token/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many/match-field/:match-field/match-value/:match-value" [] collection-resource)  
  (ANY "/v0.2/:token/:name-of-collection/match-field/:match-field/match-value/:match-value" [] collection-resource)
  (ANY "/v0.2/:token/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many" [] collection-resource)
  (ANY "/v0.2/:token/:name-of-collection/:document-id" [] document-resource)
  (ANY "/v0.2/:token/:name-of-collection" [] collection-resource)
  (route/resources "/")
  (route/not-found "Page not found. Check the http verb that you used (GET, POST, PUT, DELETE) and make sure you put a collection name in the URL, and possbly also a document ID."))

(def app
  (-> app-routes
      (wrap-transaction-id)
      (wrap-token)
      (wrap-token-check)
      (wrap-cors-headers)
      (wrap-keyword-params)
      (wrap-multipart-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-json-params)
      (wrap-content-type)))

(defmacro start-perpetual-events []
  `(do ~@(for [i (map first (ns-publics 'humongorous-api.perpetual))]
           `(~(symbol (str "perpetual/" i))))))

(defmacro start-startup-events []
  `(do ~@(for [i (map first (ns-publics 'humongorous-api.startup))]
           `(~(symbol (str "startup/" i))))))

(defn start [args]
  (try
    (println "App 'humongorous-api' is starting.")
    (println "If no port is specified then we will default to port 34000.")
    (println "You can specify the port by starting it like this:")
    (println "java -jar target/humongorous-api-0.1-standalone.jar 80")
    (start-startup-events)
    (start-perpetual-events)
    (let [port (if (nil? (first args))
                 34000
                 (Integer/parseInt  (first args)))]
      (timbre/log :trace (str "Starting the app at: " (dt/current-time-as-string)))
      (def server (run-jetty #'app {:port port :join? false :max-threads 5000})))
    (catch Exception e (println e))))

(defn stop []
  (.stop server))
