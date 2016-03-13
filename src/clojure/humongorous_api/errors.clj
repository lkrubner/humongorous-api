(ns humongorous-api.errors
  (:require
   [slingshot.slingshot :as slingshot]
   [clj-stacktrace.core :as stack]
   [dire.core :as dire]
   [taoensso.timbre :as timbre]
   [clojure.test :as test]))


(defmacro handle-error [object-to-catch symbol-to-target & args]
  (let [result-fn (first args)]  
    `(dire/with-handler! ~symbol-to-target
       ~object-to-catch
       (fn [e# & args#]
         (let [error-key# (str ~symbol-to-target " " e#)
               error-message# (str (stack/parse-exception e#) " " e# " " args#)]
           (timbre/log :trace (str error-key# " " error-message#)))
         ~(if (test/function? result-fn)
            `(~result-fn))))))


;; examples of how this was used in another project: 

;; (handle-error java.lang.Exception #'salesslick.query/token
;;               (fn [] {:status 503
;;                       :body { :error "Service Unavailable: something went wrong with the token server." }
;;                       :headers {"Content-Type" "application/json"}}))

;; (handle-error java.lang.Exception #'salesslick.event-bus/add-to-events)
;; (handle-error java.lang.Exception #'salesslick.event-bus/update-events)
;; (handle-error java.lang.Exception #'salesslick.event-bus/publish)
;; (handle-error java.lang.Exception #'salesslick.event-bus/start)
