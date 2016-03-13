(ns humongorous-api.core
  (:gen-class)
  (:require
   [humongorous-api.supervisor-stop :as stop]
   [humongorous-api.supervisor-start :as start]))



;; what you would call from the REPL to re-initate the app
(defn start []
  (start/start))

(defn stop []
  (stop/stop))


;; Enable command-line invocation
(defn -main [& args]
  (try
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread.
                       #(do (println "humongorous-api is shutting down")
                            (stop))))
    (start)
    (catch Exception e (println e))))




