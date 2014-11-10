(ns humongorous-api.controller
  (:require
   [humongorous-api.handling-queue :as hq]
   [humongorous-api.query :as query]
   [cheshire.core :as cheshire]
   [me.raynes.fs :as fs]
   [dire.core :as dire]
   [clojure.string :as st]
   [clojure.walk :as walk]
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [taoensso.timbre :as timbre]
   [clojure.java.io :as io]))


(defn walk-deep-structure [next-item function-to-transform-values]
  (walk/postwalk
   (fn [%]
     (if (and (vector? %) (= (count %) 2) (keyword? (first %)))
       [(function-to-transform-values %) (second %)]
       %))
   next-item))

(defn walk-deep-structure-for-values [next-item function-to-transform-values]
  (walk/postwalk
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

(defn request-malformed? [context key] 
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (get-in context [:request :json-params])]
        (if (or nil (= body {}))
          {:status 1 :messages [{:type "error" :message "Error: the body of your request was empty"}]}
          [false {key body}]))
      (catch Exception e
        (.printStackTrace e)
        {:message (str "Error: please re-check the document that you sent. " (format "IOException: %s" (.getMessage e)))}))))

(defn find-this-item [ctx]
  (let [ctx (assoc ctx :database-query-to-call "find-this-item")
        future-data-return (query/fetch ctx)
        results (walk-deep-structure @future-data-return (fn [%] (st/replace (name (first %)) "*" "$")))]
    (cheshire/generate-string (prepare-for-json results) {:pretty true})))

(defn set-database-query [ctx]
  (let [field-to-sort-by (get-in ctx [:request :params :field-to-sort-by])
        match-field (get-in ctx [:request :params :match-field])
        match-value (get-in ctx [:request :params :match-value])
        ctx (if (and (nil? field-to-sort-by) match-field)
              (assoc ctx :database-query-to-call "find-these-items")
              ctx)
        ctx (if (and (nil? field-to-sort-by) match-field)
              (assoc ctx :database-where-clause-map { (keyword match-field) match-value })
              ctx)]
    ctx))

(defn collection-resource-handle-ok [ctx]
  (let [field-to-sort-by (get-in ctx [:request :params :field-to-sort-by])
        match-field (get-in ctx [:request :params :match-field])
        match-value (get-in ctx [:request :params :match-value])
        offset-by-how-many (get-in ctx [:request :params :offset-by-how-many])
        return-how-many (get-in ctx [:request :params :return-how-many])
        document (if (get-in ctx [:request :json-params])
                   (get-in ctx [:request :json-params])
                   {})
        query-name (if return-how-many
                     :paginate-these-items
                     :find-these-items) 
        where-clause-map (if (and match-field match-value)
                           { (keyword match-field) match-value }
                           {})
        context-wrapper-for-database-call (if (= query-name :paginate-these-items)
                                            {
                                             :name-of-collection (get-in ctx [:request :params :name-of-collection])
                                             :where-clause-map where-clause-map
                                             :document document
                                             :query-name query-name
                                             :field-to-sort-by field-to-sort-by  
                                             :offset-by-how-many offset-by-how-many  
                                             :return-how-many return-how-many
                                             }
                                            {
                                             :name-of-collection (get-in ctx [:request :params :name-of-collection])
                                             :where-clause-map where-clause-map
                                             :document document
                                             :query-name query-name
                                             })
        future-data-return (query/fetch context-wrapper-for-database-call)
        results (walk-deep-structure @future-data-return (fn [%] (st/replace (name (first %)) "*" "$")))]
    (cheshire/generate-string (prepare-for-json results) {:pretty true})))

(defn document-resource-handle-ok [ctx]
  {:pre [(string?  (get-in ctx [:request :params :document-id]))]}
  (let [document-id (get-in ctx [:request :params :document-id])
        document (if (map? (get-in ctx [:request :json-params]))
                   (get-in ctx [:request :json-params])
                   {})
        context-wrapper-for-database-call {
                                           :name-of-collection (get-in ctx [:request :params :name-of-collection])
                                           :where-clause-map  {:_id document-id}
                                           :document document
                                           :query-name :find-these-items
                                           }
        future-data-return (query/fetch context-wrapper-for-database-call)
        entry (do
                @future-data-return)
        entry (if entry 
                (first entry)
                {})
        results (walk-deep-structure entry (fn [%] (st/replace (name (first %)) "*" "$")))
        results (if (get results "_id")
                  (assoc results "_id" (str (get results "_id")))
                  {:status 0
                   :messages [{
                               :type "information"
                               :message "That item no longer exists."
                               }]})]
    (cheshire/generate-string results {:pretty true})))

(defn document-resource-put! [ctx]
  "2014-07-08 - PUT leads us to create a new document. If the :document-id matches the :_id of an existing document, that other document needs to be removed and wholly over-written by this new document."
  (let [document-id (get-in ctx [:request :params :document-id])
        where-clause-map (if (st/blank? document-id)
                           {}
                           {:_id document-id})
        context-wrapper-for-database-call {
                                           :name-of-collection (get-in ctx [:request :params :name-of-collection])
                                           :where-clause-map where-clause-map
                                           :document (get-in ctx [:request :json-params])
                                           :query-name :create-this-item
                                           }]
    (hq/persist-this-item context-wrapper-for-database-call)
    {:status 0
     :messages [{:message "Attempting to create document"}]
     :data {
            :url (str "/v0.2/" (str (get-in ctx [:request :params :name-of-collection])))
            :document (get-in ctx [:request :json-params])
            }
     }))

(defn is-document-created? [ctx]
  (hq/get-from-handled ctx))

(defn document-created [ctx]
  (hq/get-from-handled ctx))

(defn document-resource-post! [ctx]
  {:pre [(string? (get-in ctx [:request :params :document-id]))]}
  (let [context-wrapper-for-database-call {
                                           :name-of-collection (get-in ctx [:request :params :name-of-collection])
                                           :where-clause-map {:_id (get-in ctx [:request :params :document-id])}
                                           :document (get-in ctx [:request :json-params])
                                           :query-name :persist-this-item
                                           }]
    (hq/persist-this-item context-wrapper-for-database-call)))

(defn document-resource-existed? [ctx]
  false)

(defn document-resource-delete! [ctx]
  (if-not (st/blank? (str (get-in ctx [:request :params :document-id])))
    (let [document-id (get-in ctx [:request :params :document-id])
          context-wrapper-for-database-call {
                                             :name-of-collection (get-in ctx [:request :params :name-of-collection])
                                             :where-clause-map {:_id document-id}
                                             :document {}
                                             :query-name :remove-this-item      
                                             }]
      (hq/persist-this-item context-wrapper-for-database-call))
    (do
      (timbre/log :trace "In document-resource-delete! we did not delete because there was no document id: " (:request ctx))
      "Could not delete anything because we did not receive a document-id")))







