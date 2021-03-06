(ns witan.send.driver.transitions
  (:require [witan.send.constants :as const]))

(def NON-SEND (name const/non-send))

(defn joiner? [{:keys [setting-1 need-1 setting-2 need-2] :as rec}]
  (when (and (= NON-SEND setting-1)
             (= NON-SEND need-1)
             (not= NON-SEND setting-2)
             (not= NON-SEND need-2))
    rec))

(defn leaver? [{:keys [setting-1 need-1 setting-2 need-2] :as rec}]
  (when (and (= NON-SEND setting-2)
             (= NON-SEND need-2)
             (not= NON-SEND setting-1)
             (not= NON-SEND need-1))
    rec))

(defn mover? [{:keys [setting-1 need-1 setting-2 need-2] :as rec}]
  (when (and (not (or (joiner? rec)
                      (leaver? rec)))
             (not= setting-1 setting-2)
             #_(or (not= setting-1 setting-2)
                   (not= need-1 need-2)))
    rec))

(defn stayer? [{:keys [setting-1 need-1 setting-2 need-2] :as rec}]
  (when (and (not (or (joiner? rec)
                      (leaver? rec)))
             (= setting-1 setting-2)
             #_(and (= setting-1 setting-2)
                    (= need-1 need-2)))
    rec))

(defn outside-of-send? [transition]
  (every? #(= % NON-SEND)
          ((juxt :setting-1 :need-1 :setting-2 :need-2) transition)))


(defn transition-type [rec]
  (cond (joiner? rec) :joiner
        (leaver? rec) :leaver
        (stayer? rec) :stayer
        (mover? rec)  :mover))

(defn advances-one-ay? [{:keys [academic-year-1 academic-year-2] :as rec}]
  (when (= 1 (- academic-year-2  academic-year-1))
    rec))

(defn skips-one-or-more-years? [{:keys [academic-year-1 academic-year-2] :as rec}]
  (when (< 1 (- academic-year-2  academic-year-1))
    rec))

(defn held-back-year? [{:keys [academic-year-1 academic-year-2] :as rec}]
  (when (zero? (- academic-year-2  academic-year-1))
    rec))

(defn goes-back-one-or-more-years? [{:keys [academic-year-1 academic-year-2] :as rec}]
  (when (neg? (- academic-year-2  academic-year-1))
    rec))

(defn observed-settings [recs]
  (into (sorted-set)
        (mapcat (juxt :setting-1 :setting-2))
        recs))

(defn observed-needs [recs]
  (into (sorted-set)
        (mapcat (juxt :need-1 :need-2))
        recs))

(defn calendar-years [recs]
  (into (sorted-set)
        (map :calendar-year)
        recs))
