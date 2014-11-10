(ns humongorous-api.handling-queue
  (:require
   [humongorous-api.persistence :as persistence]
   [slingshot.slingshot :as ss]
   [lamina.core :as lamina]
   [clojure.pprint :as pp]))


(def ^:private persistence-channel (lamina/channel))
(def ^:private handled (atom {}))


(defn add-to-handled [context-wrapper-for-database-call]
  (swap! handled (fn [old-value-of-handled]
                   (assoc old-value-of-handled (get-in context-wrapper-for-database-call [:request :transaction-id])
                          {:status 0
                           :messages [{:message "Attempting to create document"}]
                           :data {
                                  :url (str "/v0.2/" (str (get-in context-wrapper-for-database-call [:request :params :name-of-collection])))
                                  :document (get-in context-wrapper-for-database-call [:request :json-params])
                                  }
                           }))))

(defn delete-from-handled [context-wrapper-for-database-call]
  (Thread/sleep 120000) 
  (swap! handled (fn [old-value-of-handled]
                   (dissoc old-value-of-handled (get-in context-wrapper-for-database-call [:request :transaction-id])))))

(defn get-from-handled [context-wrapper-for-database-call]
  (println " in handling_queue/get-from-handled, the handled atom:")
  (pp/pprint @handled)
  (get @handled (get-in context-wrapper-for-database-call [:request :transaction-id])))

(defn persist-this-item [context-wrapper-for-database-call]
  (lamina/enqueue persistence-channel
                  (fn []
                    (future (add-to-handled context-wrapper-for-database-call))
                    (future (delete-from-handled context-wrapper-for-database-call))
                    (ss/try+ 
                     (persistence/make-consistent context-wrapper-for-database-call)
                     (catch Object o (ss/throw+ {:type :humongorous-api.supervisor/problem
                                                 :message "Error in handling-queue/persist-this-itme."
                                                 :data o}))))))

(defn worker []
  (loop [closure-with-item-inside @(lamina/read-channel persistence-channel)]
    (ss/try+ 
     (closure-with-item-inside)
     (catch Object o (ss/throw+ {:type :humongorous-api.supervisor/problem
                                 :message "Error in handling-queue/worker."
                                 :closure closure-with-item-inside
                                 :data o})))
    (recur @(lamina/read-channel persistence-channel))))

(defn start-workers []
  (lamina/on-closed persistence-channel
                    (fn [] (ss/throw+
                            {:type :humongorous-api.supervisor/persistence-channel-closed
                             :message "In handling-queue, an error has closed the persistence-channel"}))) 
  (lamina/on-drained persistence-channel
                     (fn [] (ss/throw+
                             {:type :humongorous-api.supervisor/persistence-channel-drained
                              :message "In handling-queue, an error has closed the persistence-channel"})))
  (dotimes [_ 20]
    (println "Starting up the persist queue workers.")
    (future (worker))))


