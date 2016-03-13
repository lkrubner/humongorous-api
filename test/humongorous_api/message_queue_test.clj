(ns humongorous-api.message-queue-test
  (:require
   [humongorous-api.message-queue :as queue]
   [manifold.stream :as stream]
   [clojure.test :refer :all]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))



(deftest enqueue 
  (testing "enqueue's message stream is non-dynamic and can not be overridden with bindings"
    (is (thrown? IllegalStateException
                 (binding [queue/message-stream (atom (stream/stream))]))))

  ;; we need to start the message stream or we get a NullPointerException when
  ;; these functions hit the stream. 
  (queue/start {})

  (testing "enqueue always returns nil"
    (is (= nil 
           (queue/enqueue {} (fn [] (+ 1 1))))))

  (testing "enqueue always returns nil -- hashmap hashmap"
    (is (= nil 
           (queue/enqueue {} {}))))
  
  (testing "enqueue expects a map as the first argument"
    (is (thrown? AssertionError
                 (@#'queue/enqueue [] (fn [] (+ 1 1)))))))
