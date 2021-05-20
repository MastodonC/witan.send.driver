(ns witan.send.driver.ingest
  (:require [clojure.string :as s]
            [witan.send.constants :as const]
            [witan.send.driver.transitions :as dt]))

(def NON-SEND (name const/non-send))

(defn safe-trim [s]
  (when (string? s) (s/trim s)))

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

(defn calendar-years [census-data]
  (->> census-data
       (map :calendar-year)
       distinct
       count))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Aliased for legacy code
(def joiner? dt/joiner?)

(def leaver? dt/leaver?)

(def mover? dt/mover?)

(def stayer? dt/stayer?)

(def outside-of-send? dt/outside-of-send?)

(def transition-type dt/transition-type)

(def advances-one-ay? dt/advances-one-ay?)
