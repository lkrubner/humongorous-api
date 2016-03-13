(ns humongorous-api.message-queue
  (:require
   [humongorous-api.supervisor-persistence :as persistence]
   [manifold.stream :as stream]
   [clojure.test :as test]))


(def ^:private message-stream (atom nil))

(defn enqueue
  "2015-07-26 - this is an unusual situation where I deliberately return nil so that anyone
   calling enqueue is aware that they must not expect a return value from that function.
   Rather, if they want to get data from the datbase, they must pass in a deferred, and wait
   till a value is delivered to that deferred."
  [message eventual-result]
  {:pre [(map? message)
         (do (println  (type eventual-result)) true)]
   :post [(nil? %)]}
  (stream/put! @message-stream
    [(fn [db-connection] (persistence/make-consistent message db-connection))
     eventual-result])
    nil)

(defn- worker
  [db-connection]
  (loop [message-vector @(stream/take! @message-stream)]
    (let [message-fn (first message-vector)
          eventual-result (second message-vector)]
      (deliver eventual-result (message-fn db-connection)))
    (if (= @message-stream ::stop)
      (persistence/stop db-connection)
      (recur @(stream/take! @message-stream)))))

(defn start
  [config]
  {:pre [(map? config)]}
  (swap! message-stream (fn [old-stream] (stream/stream)))
  (dotimes [_ (get config :how-many-message-queue-workers 5)]
    (future (worker (persistence/start config)))))

(defn stop []
  (swap! message-stream (fn [old-stream] ::stop)))

