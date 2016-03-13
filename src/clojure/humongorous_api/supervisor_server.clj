(ns humongorous-api.supervisor-server
  (:require
   [humongorous-api.server :as server]
   [humongorous-api.temporal :as temporal]
   [dire.core :as dire]
   [clj-stacktrace.core :as stack]
   [taoensso.timbre :as timbre]
   [clojure.java.io :as io]
   [clojure.string :as st])
  (:use
   [humongorous-api.problems]
   [humongorous-api.errors]))



(dire/with-handler! #'server/start
  Object
  (fn [e & args]
    (timbre/log :trace  (str " server/start: The time of the error: " (temporal/current-time-as-string)  " " (str e) " " (str args)))))

(dire/with-handler! #'server/stop
  Object
  (fn [e & args]
    (timbre/log :trace  (str " server/start: The time of the error: " (temporal/current-time-as-string)  " " (str e) " " (str args)))))

(defn start []
  (dire/supervise #'server/start))

(defn stop []
  (dire/supervise #'server/stop))




