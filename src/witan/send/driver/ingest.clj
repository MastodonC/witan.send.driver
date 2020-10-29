(ns witan.send.driver.ingest
  (:require [witan.send.constants :as const]
            [witan.send.driver.transition :as dt]))

(def NON-SEND (name const/non-send))

(defn ->int [x]
  (cond (int? x)
        x
        (double? x)
        (int x)
        (string? x)
        (int (Double/valueOf x))
        :else
        (throw (ex-info (format "Failed to parse supplied value '%s'" x)
                        {:value x}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Aliased for legacy code
(def joiner? dt/joiner?)

(def leaver? dt/leaver?)

(def mover? dt/mover?)

(def stayer? dt/stayer?)

(def outside-of-send? dt/outside-of-send?)

(def transition-type dt/transition-type)

(def advances-one-ay? dt/advances-one-ay?)
