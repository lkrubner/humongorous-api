(ns humongorous-api.supervisor
  (:require
   [humongorous-api.perpetual :as perpetual]
   [humongorous-api.startup :as startup]
   [humongorous-api.server :as server]
   [humongorous-api.controller :as controller]
   [humongorous-api.dates :as dates]
   [humongorous-api.query :as query]
   [clj-stacktrace.core :as stack]
   [dire.core :as dire]
   [taoensso.timbre :as timbre]
   [humongorous-api.handling-queue :as pq]))


;; 2014-08-07 - I dislike cluttering my code (the part that's visible to human eyes)
;; with try/catch blocks, but I need all functions wrapped in try/catch blocks, so we
;; use Dire, which uses macros to wrap functions in try/catch blocks at compile time.
;; Thus, 'supervisor' is the namespace for catching anything thrown in other namespaces.


(derive ::persistence-channel-closed ::channel-problem)
(derive ::persistence-channel-drained ::channel-problem)
(derive ::unable-to-connect-to-database ::database-problem)  
(derive ::datbase-problem ::problem)
(derive ::no-config-file ::startup-problem)
(derive ::no-log-file ::startup-problem)
(derive ::startup-problem ::problem)
(derive java.lang.Exception ::problem)
(derive ::database-logging ::logging)


(dire/with-handler! #'server/start
  java.lang.Exception
  (fn [e & args]
    (println "We are in the server/start handler")
    (timbre/log :trace  (str " server/start: The time of the error: " (dates/current-time-as-string) " " (stack/parse-exception e) " " (str e) " " (str args)))))

(dire/with-handler! #'server/start
  Object
  (fn [e & args]
    (println "We are in the server/start handler")
    (timbre/log :trace  (str " server/start: The time of the error: " (dates/current-time-as-string)  " " (str e) " " (str args)))))

(dire/with-handler! #'humongorous-api.handling-queue/persist-this-item
  java.lang.Exception
  (fn [e & args]
    (timbre/log :trace  (str " pq/persist-this-item The time of the error: " (dates/current-time-as-string) " " (stack/parse-exception e) " " (str e) " " (str args)))
    ))

(dire/with-handler! #'humongorous-api.handling-queue/worker
  Object
  (fn [e & args]
    (timbre/log :trace  (str " dire/with-handler! #'humongorous-api.handling-queue/worker: The time of the error: " (dates/current-time-as-string)" " (str e) " " (str args)))))

(dire/with-handler! #'humongorous-api.handling-queue/start-workers
  Object
  (fn [e & args]
    (println "We are in the start-workers handler")
    (timbre/log :trace  (str " start-worker: The time of the error: " (dates/current-time-as-string) " " (stack/parse-exception e) " " (str e) " " (str args)))))

(dire/with-handler! #'humongorous-api.query/fetch
  "I am suprised by this error:
        2014-08-11 12:45:09.370:WARN:oejs.AbstractHttpConnection:/v0.1/User/sort/username/0/100/match-field/role/match-value/parent
        java.util.concurrent.ExecutionException: java.lang.AssertionError: Assert failed: (string? (:field-to-sort-by context-wrapper-for-database-call))
so I am adding logging."
  java.util.concurrent.ExecutionException
  (fn [e & args]
    (timbre/log :trace "Error in humongorous-api.query/fetch: " e " " args)))

(dire/with-post-hook! #'humongorous-api.persistence/get-where-clause-map
  "An optional docstring."
  (fn [result]
    (timbre/log :trace "Return value of humongorous-api.persistence/get-where-clause-map:  " result)))

(dire/with-handler! #'humongorous-api.persistence/get-where-clause-map
  java.lang.Exception
  (fn [e & args]
    (timbre/log :trace "Return value of humongorous-api.persistence/get-where-clause-map:  " e " and args: ")
    (doseq [x args]
      (timbre/log :trace "in humongorous-api.persistence/get-where-clause-map, this arg:" x " had type: " (type x)))
    (if (seq args)
      (if (map? (first args))
        (doseq [[k v] (first args)]
          (timbre/log :trace "in humongorous-api.persistence/get-where-clause-map, this key:" k " had value of type: " (type v)))))))

(dire/with-handler! #'humongorous-api.controller/document-resource-handle-ok
  "2014-08-26 - I am surprised that this: 

curl --verbose -X DELETE -H 'Content-Type: application/json' http://localhost/v0.1/User/553fa5cace4b03a899c746acb

gave me this: 

WARN:oejs.AbstractHttpConnection:/v0.1/User/553fa5cace4b03a899c746acb
java.util.concurrent.ExecutionException: java.lang.IllegalArgumentException: invalid ObjectId [553fa5cace4b03a899c746acb]

adding a handler to catch the exception.
"
  java.util.concurrent.ExecutionException
  (fn [e & args]
    (timbre/log :trace "Return value of humongorous-api.controller/document-resource-handle-ok:  " e " args: " args)))
