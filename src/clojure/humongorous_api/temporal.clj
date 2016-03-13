(ns humongorous-api.temporal
  (:import
   (java.text SimpleDateFormat)
   (java.util Date)
   (org.joda.time.format DateTimeFormat)
   (org.joda.time.format ISODateTimeFormat))
  (:require [clojure.string :as st]
            [clj-time.core :as tyme]
            [clj-time.format :as tyme-format])
  (:use [taoensso.timbre :as timbre :only (trace debug info warn error fatal spy)]))


;; 2013-08-28 - setting default time zone
(org.joda.time.DateTimeZone/setDefault org.joda.time.DateTimeZone/UTC)




(defn current-time-as-datetime []
  (tyme/now))

(defn datetime-days-ago [how-many-days-ago]
  (tyme/minus (tyme/now) (tyme/days how-many-days-ago)))

(defn current-time-as-string []
  (let [date-formatter (tyme-format/formatters :date-hour-minute-second)
        current-time-as-string (apply str (tyme-format/unparse date-formatter (tyme/now)))]
    current-time-as-string))

(defn this-time-given-as-string-but-parsed-to-datetime [time-as-string]
  (let [date-formatter (tyme-format/formatters :date-hour-minute-second)
        time-as-datetime (tyme-format/parse date-formatter time-as-string)]
    time-as-datetime))

(defn three-days-ago-as-a-string-for-the-database []
  (let [three-days-ago  (tyme/minus (tyme/now) (tyme/days 4))
        date-formatter (tyme-format/formatters :date-hour-minute-second)
        three-days-ago-as-string (apply str (tyme-format/unparse date-formatter three-days-ago))]
    three-days-ago-as-string))

(defn now-as-a-string-for-the-database []
  (let [date-formatter (tyme-format/formatters :year-month-day)
        now-as-a-string-for-the-database (apply str (tyme-format/unparse date-formatter (tyme/now)))]
    now-as-a-string-for-the-database))

(defn one-month-ago-as-a-string-for-the-database []
  (let [one-month-ago (tyme/minus (tyme/now) (tyme/months 1))
        date-formatter (tyme-format/formatters :year-month-day)
        one-month-ago-as-a-string-for-the-database (apply str (tyme-format/unparse date-formatter one-month-ago))]
    one-month-ago-as-a-string-for-the-database))

(defn one-year-ago-as-a-string-for-the-database []
  (let [one-year-ago (tyme/minus (tyme/now) (tyme/months 12))
        date-formatter (tyme-format/formatters :year-month-day)
        one-month-ago-as-a-string-for-the-database (apply str (tyme-format/unparse date-formatter one-year-ago))]
    one-year-ago-as-a-string-for-the-database))

(defn make-created-at [item]
  (when (and (not (clojure.string/blank? (get-in item ["created-at[year]"])))
             (not (clojure.string/blank? (get-in item ["created-at[month]"])))
             (not (clojure.string/blank? (get-in item ["created-at[day]"])))
             (not (clojure.string/blank? (get-in item ["created-at[h]"])))
             (not (clojure.string/blank? (get-in item ["created-at[m]"]))))
    (tyme/date-time (Integer/parseInt (get-in item ["created-at[year]"])) (Integer/parseInt (get-in item ["created-at[month]"])) (Integer/parseInt (get-in item ["created-at[day]"])) (Integer/parseInt (get-in item ["created-at[h]"])) (Integer/parseInt (get-in item ["created-at[m]"])))))

(defn make-updated-at [item]
  (when (and (not (clojure.string/blank? (get-in item ["updated-at[year]"])))
             (not (clojure.string/blank? (get-in item ["updated-at[month]"])))
             (not (clojure.string/blank? (get-in item ["updated-at[day]"])))
             (not (clojure.string/blank? (get-in item ["updated-at[h]"])))
             (not (clojure.string/blank? (get-in item ["updated-at[m]"]))))
    (tyme/date-time (Integer/parseInt (get-in item ["updated-at[year]"])) (Integer/parseInt (get-in item ["updated-at[month]"])) (Integer/parseInt (get-in item ["updated-at[day]"])) (Integer/parseInt (get-in item ["updated-at[h]"])) (Integer/parseInt (get-in item ["updated-at[m]"])))))

(defn make-start-at [item]
  (when (and (not (clojure.string/blank? (get-in item ["start-at[year]"])))
             (not (clojure.string/blank? (get-in item ["start-at[month]"])))
             (not (clojure.string/blank? (get-in item ["start-at[day]"])))
             (not (clojure.string/blank? (get-in item ["start-at[h]"])))
             (not (clojure.string/blank? (get-in item ["start-at[m]"]))))
    (tyme/date-time (Integer/parseInt (get-in item ["start-at[year]"])) (Integer/parseInt (get-in item ["start-at[month]"])) (Integer/parseInt (get-in item ["start-at[day]"])) (Integer/parseInt (get-in item ["start-at[h]"])) (Integer/parseInt (get-in item ["start-at[m]"])))))

(defn make-end-at [item]
  (when (and (not (clojure.string/blank? (get-in item ["end-at[year]"])))
             (not (clojure.string/blank? (get-in item ["end-at[month]"])))
             (not (clojure.string/blank? (get-in item ["end-at[day]"])))
             (not (clojure.string/blank? (get-in item ["end-at[h]"])))
             (not (clojure.string/blank? (get-in item ["end-at[m]"]))))
    (tyme/date-time (Integer/parseInt (get-in item ["end-at[year]"])) (Integer/parseInt (get-in item ["end-at[month]"])) (Integer/parseInt (get-in item ["end-at[day]"])) (Integer/parseInt (get-in item ["end-at[h]"])) (Integer/parseInt (get-in item ["end-at[m]"])))))
