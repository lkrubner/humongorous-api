(ns humongorous-api.supervisor-stop
  (:require
   [humongorous-api.stop :as stop]
   [clj-stacktrace.core :as stack]
   [dire.core :as dire])
  (:use
   [humongorous-api.problems]
   [humongorous-api.errors]))



;; 2016-02-28 - I'm going to structure this app as layers of
;; namespaces that act as supervisors to other namespaces.
;; You can consider this a weak imitation of the Erlang style.


(handle-error java.lang.Object #'stop/stop)

(defn stop []
  (dire/supervise #'stop/stop))








