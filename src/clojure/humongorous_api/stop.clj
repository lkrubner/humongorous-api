(ns humongorous-api.stop
  (:require
    [humongorous-api.supervisor-server :as server]
    [humongorous-api.supervisor-message-queue :as queue]))

(defn stop []
  (server/stop)
  (queue/stop))
