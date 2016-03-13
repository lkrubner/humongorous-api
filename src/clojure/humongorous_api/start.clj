(ns humongorous-api.start
  (:require
   [humongorous-api.monitoring :as monitoring]
   [humongorous-api.supervisor-message-queue :as queue]
   [humongorous-api.supervisor-server :as server]
   [slingshot.slingshot :as slingshot]
   [me.raynes.fs :as fs]
   [overtone.at-at :as at]
   [taoensso.timbre :as timbre]
   [taoensso.timbre.appenders.core :as appenders])
  (:use
   [humongorous-api.problems]
   [humongorous-api.errors]))



(defn- set-timbre-level []
  (timbre/set-config!
    {:level :trace ; e/o #{:trace :debug :info :warn :error :fatal :report}
     :timestamp-opts timbre/default-timestamp-opts
     :output-fn timbre/default-output-fn ; (fn [data]) -> string
     :appenders
       {:example-println-appender ; Appender id
       ;; Appender definition (just a map):
       {:enabled?   true
        :async?     false
        :min-level  nil
        :rate-limit [[1 250] [10 5000]] ; 1/250ms, 10/5s
        :output-fn  :inherit
        :fn ; Appender's fn
        (fn [data]
          (let [{:keys [output-fn]} data
                formatted-output-str (output-fn data)]
            (println formatted-output-str)))}}}))

(defn- resource-usage []
  "Every 5 minutes, we print some stats about resource use."
  (let [my-pool (at/mk-pool)]
    (at/every 300000
              (fn []
                  (timbre/log :trace "Resource usage: "
                              (monitoring/show-stats-regarding-resources-used-by-this-app))
                  (doseq [x (monitoring/thread-top)]
                    (timbre/log :trace x))))
              my-pool))

(defn- repopulate-list-of-collections []
  "Every 15 minutes, we fetch the complete list of all collections in MongoDB, and we put the list in a collection that frontenders can use to figure out what collections are available."
  (let [my-pool (at/mk-pool)]
    (at/every 1800000
              (fn []
                ;; 2016-03-05 - what should we put here? 
                (queue/enqueue {} {})))
              my-pool))

(defn- print-start-info []
  (timbre/log :trace "STARTING!!!!")
  (timbre/log :trace "App 'humongorous' is starting.")
  (timbre/log :trace "If no port is specified in the config file then we will default to port 34000.")
  (timbre/log :trace "Starting the app"))

(defn- check-that-config-file-exists[]
  (if-not (fs/exists? "/etc/humongorous")
    (slingshot/throw+ {:type :humongorous-api.supervisor/no-config-file :message "In start/check-that-config-file-exists, we could not find the config file /etc/humongorous" })))

(defn- check-that-log-file-exists[]
  (if-not (fs/exists? "/var/log/humongorous")
    (slingshot/throw+ {:type :humongorous-api.supervisor/no-log-file :message "In start/check-that-log-file-exists, we could not find the log file /var/log/humongorous" })))

(defn- start-message-queue []
  (queue/start (read-string (slurp "/etc/humongorous"))))

(defn- start-server []
  (server/start (read-string (slurp "/etc/humongorous"))))

(defn launch []
  (set-timbre-level)
  (print-start-info)
  (check-that-config-file-exists)
  (check-that-log-file-exists)
  (resource-usage)
  (start-message-queue)
  (start-server))



