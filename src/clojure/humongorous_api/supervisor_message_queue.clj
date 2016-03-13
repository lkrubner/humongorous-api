(ns humongorous-api.supervisor-message-queue
  (:import
   [java.net URL])
  (:require
   [humongorous-api.message-queue :as queue]
   [humongorous-api.temporal :as temporal]
   [dire.core :as dire]
   [clj-stacktrace.core :as stack]
   [taoensso.timbre :as timbre])
  (:use
   [humongorous-api.problems]
   [humongorous-api.errors]))



(handle-error Object #'queue/enqueue)

(handle-error Object #'queue/start)

(handle-error Object #'queue/stop)


(defn enqueue [message eventual-result]
  (queue/enqueue message eventual-result))

(defn start [map-of-config-info]
  (queue/start map-of-config-info))

(defn stop []
  (queue/stop))



