(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.repl :as repl]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))




;; 2014-07-01 - this file is part of my attempt to use Stuart Sierra's workflow:
;; http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded




(defn is-the-user-namespace-loaded? []
  (println "yes, the user namespace has been loaded"))


