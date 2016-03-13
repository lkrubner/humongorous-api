(ns humongorous-api.server
  (:import
   [java.net URL])
  (:require
   [humongorous-api.tokens :as tokens]
   [humongorous-api.supervisor-message-queue :as queue]
   [humongorous-api.middleware :as middleware]
   [cheshire.core :refer :all]
   [clojure.java.io :as io]
   [clojure.string :as st]
   [cheshire.core :as cheshire]
   [manifold.deferred :as deferred])
  (:use
   [humongorous-api.problems]
   [humongorous-api.errors]
   [ring.middleware.params]
   [ring.middleware.keyword-params]
   [ring.middleware.multipart-params]
   [ring.middleware.nested-params]
   [ring.middleware.file]
   [ring.middleware.resource]
   [ring.middleware.content-type]
   [ring.adapter.jetty :only [run-jetty]]
   [ring.middleware.json]
   [clojure.walk]))



(defn walk-deep-structure [next-item function-to-transform-values]
  (postwalk
   (fn [%]
     (if (and (vector? %) (= (count %) 2) (keyword? (first %)))
       [(function-to-transform-values %) (second %)]
       %))
   next-item))

(defn walk-deep-structure-for-values [next-item function-to-transform-values]
  (postwalk
   (fn [%]
     (if (and (vector? %) (= (count %) 2) (keyword? (first %)))
       [(first %) (function-to-transform-values %)]
       %))
   next-item))

(defn prepare-for-json [seq-or-entry]
  (reduce
   (fn [vector-of-strings next-document]
     ;; need to avoid: 
     ;; java.lang.Exception: Don't know how to write JSON of class org.bson.types.ObjectId
     (let [next-document (assoc next-document "_id" (str (get next-document "_id")))]
       (conj vector-of-strings next-document)))
   []
   seq-or-entry))

(defn intro []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "See documentation here: https://github.com/lkrubner/humongorous-api"})

(defn generate []
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (cheshire/generate-string {:token  (tokens/create-token)})})

(defn not-found []
  {:status 404
   :headers {"Content-Type" "text/plain"}
   :body "We could not find the page you were looking for. See documentation here: https://github.com/lkrubner/humongorous-api"})

(defn service [simple-uri]
  (cond
    (= simple-uri "generate") (generate)
    (= simple-uri "") (intro)
    (= simple-uri nil) (intro)
    :default (not-found)))

(defn fetch [request]
  "We use a deferred to ensure that the database request does not run on the main thread, but then we block on the response and send back the end result."
  (let [eventual-result (deferred/deferred)]
    (queue/enqueue request eventual-result)
    (cheshire/generate-string @eventual-result {:pretty true})))

(defn handler [request]
  (clojure.pprint/pprint request)
  (let [uri-parts (clojure.string/split (:uri request) #"/")]
    (println "uri-parts" uri-parts)
    (if (#{"generate" "intro" "" nil} (get uri-parts 1))
      (service (get uri-parts 1))
      (fetch request))))

(def app
  (-> handler
      (middleware/wrap-transaction-id)
      (middleware/wrap-token)
      (middleware/wrap-token-check)
      (middleware/wrap-json-body)
      (middleware/wrap-cors-headers)
      (wrap-keyword-params)
      (wrap-multipart-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-json-params)
      (wrap-content-type)))

(def server (atom :no-server-stored-yet))

(defn start [args]
  (let [port (if (nil? (first args))
               34000
               (Integer/parseInt  (first args)))
        jetty (run-jetty #'app {:port port :join? false :max-threads 5000})]
    (swap! server
           (fn [old-server] jetty))))

(defn stop []
  (.stop @server))
