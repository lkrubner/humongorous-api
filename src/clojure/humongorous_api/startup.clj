(ns humongorous-api.startup
  (:require
   [humongorous-api.handling-queue :as hq]
   [humongorous-api.persistence :as ps]
   [slingshot.slingshot :as ss]
   [me.raynes.fs :as fs]
   [taoensso.timbre :as timbre]))


(defn connect-to-persistence []
  (println " in startup/connect-to-persistence")
  (try
    (ps/establish-database-connection)
    (catch Exception e (println e))))

(defn start-persist-queue []
  (hq/start-workers))

(defn check-that-log-file-exists[]
  (if-not (fs/exists? "/var/log/humongorous-api.log")
    (ss/throw+ {:type :humongorous-api.supervisor/no-log-file :message "In startup/check-that-log-file-exists, we could not find the log file /var/log/humongorous-api.log" })))

(defn check-that-config-file-exists[]
  (if-not (fs/exists? "/etc/humongorous")
    (ss/throw+ {:type  :humongorous-api.supervisor/no-config-file :message "In startup/check-that-config-file-exists, we could not find the config file /etc/humongorous" })))

(defn set-timbre-level[]
  (println " setup for timbre ")
  (timbre/set-level! :trace)
  (timbre/set-config! [:appenders :spit :enabled?] true)
  (timbre/set-config! [:shared-appender-config :spit-filename] "/var/log/humongorous-api.log"))
