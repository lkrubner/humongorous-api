(ns humongorous-api.supervisor-persistence
  (:require
   [humongorous-api.persistence :as persistence]
   [dire.core :as dire])
  (:use
   [humongorous-api.problems]
   [humongorous-api.errors]))



;; 2016-02-28 - I'm going to structure this app as layers of
;; namespaces that act as supervisors to other namespaces.
;; You can consider this a weak imitation of the Erlang style.



(handle-error java.lang.Object #'persistence/start)
(handle-error java.lang.Object #'persistence/stop)
(handle-error java.lang.Object #'persistence/make-consistent)



(defn start [map-of-config-info]
  (dire/supervise #'persistence/start map-of-config-info))

(defn stop [db-connection]
  (dire/supervise #'persistence/stop db-connection))

(defn make-consistent [message]
  (dire/supervise #'persistence/make-consistent message))








