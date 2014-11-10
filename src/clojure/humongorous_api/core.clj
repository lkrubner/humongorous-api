(ns humongorous-api.core  
  (:import [org.apache.commons.daemon Daemon DaemonContext])
  (:gen-class
   :implements [org.apache.commons.daemon.Daemon])
  (:require
   [humongorous-api.supervisor :as su]
   [humongorous-api.server :as sr]
   [taoensso.timbre :as tb]))


(def command-line-arguments (atom []))

(defn init [args]
  (swap! command-line-arguments (fn [old-vector] (conj old-vector args))))

(defn start []
  (try 
    (sr/start (first @command-line-arguments))
    (catch Exception e (tb/log e))))


;; org.apache.commons.daemon implementation

(defn -init [this ^DaemonContext context]
  (init (.getArguments context)))
  
(defn -start [this]
  (future (start)))

(defn -stop [this]
  (sr/stop))


;; Enable command-line invocation

(defn -main [& args]
  (init args)
  (start))





