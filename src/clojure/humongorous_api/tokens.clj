(ns humongorous-api.tokens
  (:import
   (java.util UUID))
  (:require
   [clojure.pprint :as pp]))


(def t (atom {}))

(defn increment-token [token]
  (swap! t (fn [old-map-of-t]
             (pp/pprint old-map-of-t)
             (assoc old-map-of-t token (+ (get old-map-of-t token 0) 1)))))

(defn create-token []
  "2014-08-07 - if the user is logged in, we use their session id. If the frontend code can not use a session id for a user, then we create a new token."
  (let [token (str (java.util.UUID/randomUUID))]
    (increment-token token)
    token))

(defn is-valid? [request]
  "2014-08-07 - no doubt we will do something more complicated later on, but for now we just ask that a user have a token that's been used less than 1000 times."
  (let [token (:token request)]
    (pp/pprint @t)
    (if (or
         (or 
          (= (:uri request) "/")
          (= (:uri request) "/token"))
         (and
          (contains? @t token)
          (< (get @t token 0) 1000)))
      true
      false)))





