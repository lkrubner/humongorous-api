(ns humongorous-api.supervisor-start
  (:require
   [humongorous-api.start :as start]
   [dire.core :as dire])
  (:use
   [humongorous-api.problems]
   [humongorous-api.errors]))



;; 2016-02-28 - I'm going to structure this app as layers of
;; namespaces that act as supervisors to other namespaces.
;; You can consider this a weak imitation of the Erlang style.


(handle-error clojure.lang.ExceptionInfo #'start/launch)

(defn start []
  (start/launch))








