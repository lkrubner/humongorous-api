(ns humongorous-api.perpetual
  (:require
   [humongorous-api.monitoring :as mn]
   [overtone.at-at :as at]
   [taoensso.timbre :as tb]))


(defn resource-usage []
  (let [my-pool (at/mk-pool)]
    (at/every 3110000
              (fn []
                (tb/log :trace "Resource usage: "
                       (mn/thread-top)
                       (mn/show-stats-regarding-resources-used-by-this-app)))
              my-pool)))


