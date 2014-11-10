(ns humongorous-api.monitoring
  (:import
   java.lang.management.ManagementFactory)
  (:require [clojure.string :as st]))


;; 2012-12-08 - the next 4 functions are from this post: http://lifeisagraph.com/2011/04/24/debugging-clojure.html

(defn threads
  "Get a seq of the current threads."
  []
  (let [grp (.getThreadGroup (Thread/currentThread))
        cnt (.activeCount grp)
        ary (make-array Thread cnt)]
    (.enumerate grp ary)
    (seq ary)))

(defn thread-grep
  "Find a thread by name."
  [name]
  (let [ptn (re-pattern name)]
    (filter #(re-find ptn (.getName %)) (threads))))

(defn get-thread
  "Lookup a thread by its numeric ID."
  [id]
  (first (filter #(= id (.getId %)) (threads))))

(defn thread-top
  "Return a seq of threads sorted by their total userland CPU usage."
  []
  (let [mgr (ManagementFactory/getThreadMXBean)
        cpu-times (map (fn [t]
                         [(.getThreadCpuTime mgr (.getId t)) t])
                    (threads))]
    (map
      (fn [[cpu t]] [cpu (.getName t) (.getId t) t])
      (reverse (sort-by first cpu-times)))))














(defn- as-megabytes 
  "Given a sequence of byte amounts, return megabyte amounts 
   as string, with an M suffix." 
  [memory] 
  (map #(str (int (/ % 1024 1024)) "M") memory)) 

(defn- as-percentage 
  "Given a pair of values, return the percentage as a string." 
  [[a b]] 
  (str (int (* 100 (/ a b))) "%")) 

(defn- memory-bean 
  "Return the MemoryMXBean." 
  [] 
  (java.lang.management.ManagementFactory/getMemoryMXBean)) 

(defn- heap-usage 
  "Given a MemoryMXBean, return the heap memory usage." 
  [^java.lang.management.MemoryMXBean bean] 
  (.getHeapMemoryUsage bean)) 

(defn- heap-used-max 
  "Given heap memory usage, return a pair of used/max values." 
  [^java.lang.management.MemoryUsage usage] 
  [(.getUsed usage) (.getMax usage)]) 

(defn memory-usage 
  "Return percentage, used, max heap as strings." 
  [] 
  (let [used-max (-> (memory-bean) (heap-usage) (heap-used-max))] 
    (cons (as-percentage used-max) 
          (as-megabytes used-max)))) 

(defn- operating-system-bean 
  "Return the OperatingSystemMXBean." 
  [] 
  (java.lang.management.ManagementFactory/getOperatingSystemMXBean)) 

(defn- cpus 
  "Given an OSMXBean, return the number of processors." 
  [^java.lang.management.OperatingSystemMXBean bean] 
  (.getAvailableProcessors bean)) 

(defn- load-average 
  "Given an OSMXBean, return the load average for the last minute." 
  [^java.lang.management.OperatingSystemMXBean bean] 
  (.getSystemLoadAverage bean)) 

(defn- cpu-percentage 
  "Given the number of CPUs and the load-average, return the 
   percentage utilization as a string." 
  [[cpus load-average]] 
  (str (int (* 100 (/ load-average cpus))) "%")) 

(defn cpu-usage 
  "Return utilization (as a string) and number of CPUs and load average." 
  [] 
  (let [bean (operating-system-bean) 
        data ((juxt cpus load-average) bean)] 
    (cons (cpu-percentage data) 
          data)))

(defn cpu-load-usage 
  "Returns load average." 
  [] 
  (let [bean (operating-system-bean) 
        data ((juxt cpus load-average) bean)] 
    data))

(defn free-memory-in-jvm []
  (let [runtime (Runtime/getRuntime)
        free-memory (. runtime freeMemory)]
    free-memory))

(defn show-stats-regarding-resources-used-by-this-app []
  "2012-12-08 - We use this for debugging purposes. We print this info to the terminal so we can keep track of how many resources this app uses, and when."
  (str
   "Memory in use (percentage/used/max-heap): " (memory-usage)
   "\n\nCPU usage (how-many-cpu's/load-average):  " (cpu-load-usage)
   "\n\nFree memory in jvm: " (conj [] (free-memory-in-jvm)))) 
