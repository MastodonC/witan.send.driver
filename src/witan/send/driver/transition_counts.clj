(ns witan.send.driver.transition-counts
  (:require [net.cgrand.xforms :as x]))

(defn transition->transition-count
  "Assoc's a :transition-count of 1 onto each transition that can be
  rolled up by other functions."
  [transition]
  (assoc transition :transition-count 1))

(def transitions->transition-counts-xf
  (map transition->transition-count))

(defn transition-count->transitions
  "Turns a single transition count into a vector of the right number of
  transitions."
  [transition-count]
  (let [t-count (:transition-count transition-count)
        t (dissoc transition-count :transition-count)]
    (into [] (repeat t-count t))))

(def transition-counts->transitions-xf
  (mapcat transition-count->transitions))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; to census
(defn ->s1-census-count [{:keys [simulation
                                 calendar-year
                                 transition-count
                                 academic-year-1 setting-1 need-1]
                          :as transition}]
  (try
    {:simulation       simulation
     :calendar-year    calendar-year
     :transition-count transition-count
     :academic-year    academic-year-1
     :setting          setting-1
     :need             need-1}
    (catch Exception e
      (throw (ex-info "Could not create census record." {:transition transition} e)))))

(defn ->s2-census-count [{:keys [simulation
                                 calendar-year
                                 transition-count
                                 academic-year-2 setting-2 need-2]
                          :as transition}]
  (try
    {:simulation       simulation
     :calendar-year    (inc calendar-year)
     :transition-count transition-count
     :academic-year    academic-year-2
     :setting          setting-2
     :need             need-2}
    (catch Exception e
      (throw (ex-info "Could not create census record." {:transition transition} e)))))

(defn first-calendar-year [sts]
  (let [dummy-sim-number (-> sts first :simulation)]
    (first (x/into (sorted-set)
                   (comp
                    (filter #(= dummy-sim-number (:simulation %)))
                    (map :calendar-year))
                   sts))))

(defn transition-counts->census-counts-xf [first-calendar-year]
  (mapcat (fn [tc]
            (if (= (:calendar-year tc) first-calendar-year)
              [(->s1-census-count tc) (->s2-census-count tc)]
              [(->s2-census-count tc)]))))


(defn transition-counts->census [sts]
  (let [first-calendar-year (first-calendar-year sts)]
    (x/into []
            (transition-counts->census-counts-xf first-calendar-year)
            sts)))
