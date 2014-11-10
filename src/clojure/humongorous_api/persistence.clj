(ns humongorous-api.persistence
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId]
           [org.joda.time.DateTimeZone])
  (:require
   [humongorous-api.dates :as tyme]
   [me.raynes.fs :as fs]
   [dire.core :as dire]
   [slingshot.slingshot :as slingshot]
   [monger.core :as mg]
   [monger.collection :as mc]
   [monger.conversion :as convert]
   [monger.operators :as operators]
   [monger.joda-time]
   [monger.query :refer :all]
   [taoensso.timbre :as timbre]
   [clojure.java.io :as io]
   [clojure.walk :as walk]
   [clojure.string :as st])
  (:refer-clojure :exclude [sort find]))



;; 2014-08-07 - I dislike how I wrote the database.clj file. I intend to eventually
;; remove it and replace it with this file. I am using this file in faces.clj


;; in production, when we start using username/password for MongoDB:
;; uri (str "mongodb://" (:username credentials) ":" (:password credentials) "@" (:host credentials) "/" (:db credentials))
(defn establish-database-connection []
  (slingshot/try+ 
   (let [credentials (read-string (slurp "/etc/humongorous"))]
     (timbre/log :trace credentials)
     (if (nil? credentials)
       (slingshot/throw+ {:type :humongorous-api.supervisor/no-config :message "We could not find /etc/humongorous"})
       (let [uri (str "mongodb://" (:host credentials) "/" (:db credentials))
             { :keys [conn db] } (mg/connect-via-uri uri)]
         (if (nil? db)
           (slingshot/throw+ {:type :humongorous-api.supervisor/unable-to-connect-to-database :message (str "The uri " uri " failed to connect to the database")})
             (def current-database db)))))))

(defn get-where-clause-map
  [context-wrapper-for-database-call]
  (let [where-clause-map (:where-clause-map context-wrapper-for-database-call)
        document-id (str (:_id where-clause-map))
        where-clause-map (if-not (st/blank? document-id)
                           (assoc where-clause-map :_id (ObjectId. document-id))
                           where-clause-map)]
    where-clause-map))

(defn walk-deep-structure
  [next-item function-to-transform-values]
  (walk/postwalk
   (fn [%]
     (if (and (vector? %) (= (count %) 2) (string? (first %)))
       [(function-to-transform-values %) (second %)]
       %))
   next-item))

(defn- which-query [context-wrapper-for-database-call]
  {:pre [
         (map? context-wrapper-for-database-call)
         (string? (:name-of-collection context-wrapper-for-database-call))
         (map? (:where-clause-map context-wrapper-for-database-call))
         (map? (:document context-wrapper-for-database-call))
         (keyword? (:query-name context-wrapper-for-database-call))
         (if (:_id (:document context-wrapper-for-database-call))
           (string? (:_id (:document context-wrapper-for-database-call)))
           true)
         (if (:_id (:where-clause-map context-wrapper-for-database-call))
           (string? (:_id (:where-clause-map context-wrapper-for-database-call)))
           true)
         (if (:created-at (:document context-wrapper-for-database-call))
           (= (type (:created-at (:document context-wrapper-for-database-call)) org.joda.time.DateTime))
           true)
         (if (:updated-at (:document context-wrapper-for-database-call))
           (= (type (:updated-at (:document context-wrapper-for-database-call)) org.joda.time.DateTime))
           true)
         (if (:database-fields-to-return-vector context-wrapper-for-database-call)
           (vector? (:database-fields-to-return-vector context-wrapper-for-database-call))
           true)
         ]}
  ;; (slingshot/throw+
  ;;  {:type :humongorous-api.supervisor/database-logging
  ;;   :message "This is the data used to make this database call:"
  ;;   :data [context-wrapper-for-database-call]})
  (timbre/log :trace  "In persistence/which-query " context-wrapper-for-database-call)
  (:query-name context-wrapper-for-database-call))

(defmulti make-consistent
  (fn [context-wrapper-for-database-call] (which-query context-wrapper-for-database-call)))

(defmethod make-consistent :remove-this-item
  [context-wrapper-for-database-call]
  (timbre/log :trace  " now we are in make-consistent :remove-this-item")
  (let [where-clause-map (get-where-clause-map context-wrapper-for-database-call)]
  (if (:_id where-clause-map)
    (mc/remove current-database
               (:name-of-collection context-wrapper-for-database-call) where-clause-map)
    (timbre/log :trace "ERROR: in make-consistent :remove-this-item, we are unable to get the where-clause-map " context-wrapper-for-database-call))))

(defmethod make-consistent :create-this-item
  [context-wrapper-for-database-call]
  "2014-07-08 - if this app is called via a PUT then a new item should be created. If a document id is present, we want to overwrite the old document, so we delete it and create a new item."
  (let [item (:document context-wrapper-for-database-call)
        item (walk-deep-structure item (fn [%] (keyword (st/replace (first %) #"\$" "*"))))
        item (assoc item :created-at (tyme/current-time-as-datetime))
        item (assoc item :updated-at (tyme/current-time-as-datetime))
        where-clause-map (get-where-clause-map context-wrapper-for-database-call)
        document-id (:_id where-clause-map)]
    (try 
      (if document-id
        (mc/update current-database
                   (:name-of-collection context-wrapper-for-database-call)
                   where-clause-map
                   (assoc item :_id (ObjectId. (str document-id))))
        (mc/insert current-database
                   (:name-of-collection context-wrapper-for-database-call)
                   (assoc item :_id (ObjectId.))))
      (catch Exception e (timbre/log :trace e)))))

(defmethod make-consistent :persist-this-item
  [context-wrapper-for-database-call]
  "2014-07-08 - this function is called when the app receives a POST request. If there is a document-id, the new
   document should be merged with the old document. If the client code calling this app wants to over-write an
   existing document, they call with PUT instead of POST. 2014-09-23 - adding in 'dissoc item :password' because
   we have had problems with some code, somewhere on the frontend, saving the non-encrypted password to the database
   via humongorous-api."
  (let [item (:document context-wrapper-for-database-call)
        where-clause-map (get-where-clause-map context-wrapper-for-database-call)
        item (if (nil? (:created-at item))
               (assoc item :created-at (tyme/current-time-as-datetime))
               item)
        item (assoc item :updated-at (tyme/current-time-as-datetime))
        item (walk-deep-structure item (fn [%] (keyword (st/replace (first %) #"\$" "*"))))
        document-id (:_id where-clause-map)
        item (if document-id
               (assoc item :_id (ObjectId. (str document-id)))
               (assoc item :_id (ObjectId.)))
        old-item (if document-id
                   (first (make-consistent
                           (assoc
                               (assoc context-wrapper-for-database-call :where-clause-map {:_id (str (:_id item))})
                             :query-name :find-these-items))))
        item (if old-item
               (merge old-item item)
               item)
        name-of-collection (:name-of-collection context-wrapper-for-database-call)
        item (dissoc item :password)]
    (timbre/log :trace "Merger of old and new items: " item)
    (if document-id
      (mc/update current-database
                 name-of-collection
                 where-clause-map
                 item)
      (mc/insert current-database
                 name-of-collection
                 item))))

(defmethod make-consistent :find-these-items
  [context-wrapper-for-database-call]
  (if (:database-fields-to-return-vector context-wrapper-for-database-call)
    (mc/find-maps current-database
                  (:name-of-collection context-wrapper-for-database-call)
                  (get-where-clause-map context-wrapper-for-database-call)
                  (:database-fields-to-return-vector context-wrapper-for-database-call))
    (mc/find-maps current-database
                  (:name-of-collection context-wrapper-for-database-call)
                  (get-where-clause-map context-wrapper-for-database-call))))

(defmethod make-consistent :get-count
  [context-wrapper-for-database-call]
  (mc/count current-database
            (:name-of-collection context-wrapper-for-database-call)
            (get-where-clause-map context-wrapper-for-database-call)))

(defmethod make-consistent :paginate-these-items
  [context-wrapper-for-database-call]
  {:pre [
         (string? (:field-to-sort-by context-wrapper-for-database-call)) 
         (string? (:offset-by-how-many context-wrapper-for-database-call)) 
         (string? (:return-how-many context-wrapper-for-database-call))
         (number? (Integer/parseInt (:offset-by-how-many context-wrapper-for-database-call))) 
         (number? (Integer/parseInt (:return-how-many context-wrapper-for-database-call)))         
         ]}
  (let [field-to-sort-by (keyword (:field-to-sort-by context-wrapper-for-database-call))
        offset-by-how-many (Integer/parseInt (:offset-by-how-many context-wrapper-for-database-call))
        return-how-many (Integer/parseInt ( :return-how-many context-wrapper-for-database-call))]
    (with-collection current-database (:name-of-collection context-wrapper-for-database-call)
      (find (get-where-clause-map context-wrapper-for-database-call))
      ;;    (fields [:item-name :item-type :user-item-name :created-at])
      (sort (array-map field-to-sort-by 1 ))
      (limit return-how-many)
      (skip offset-by-how-many))))

(defmethod make-consistent :default
  [context-wrapper-for-database-call]
  (str "Error: we are in the default method of persistence/make-consistent"))







