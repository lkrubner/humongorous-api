(ns humongorous-api.query
  (:require
   [humongorous-api.persistence :as ps]
   [taoensso.timbre :as timbre]))



(defn fetch [context-wrapper-for-database-call]
  {:pre [
         (map? context-wrapper-for-database-call)
         (string? (:name-of-collection context-wrapper-for-database-call))
         (map? (:where-clause-map context-wrapper-for-database-call))
         (map? (:document context-wrapper-for-database-call))
         (keyword? (:query-name context-wrapper-for-database-call))
        ]
   :post [(future? %)]}
  (future (ps/make-consistent context-wrapper-for-database-call)))
  








