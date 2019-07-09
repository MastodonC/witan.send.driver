(ns witan.send.driver.validate.costs
  (:require [clojure.data :as d]
            [witan.send.constants :as const]))

(def NON-SEND (name const/non-send))

(def defined-costs
  (memoize
   (fn [costs]
     (into #{} (map (juxt :need :setting)) costs))))

(defn transition-has-cost? [costs transition]
  (and
   ((defined-costs costs) ((juxt :need-1 :setting-1) transition))
   ((defined-costs costs) ((juxt :need-2 :setting-2) transition))))

(defn census-record-has-cost? [costs record]
  ((defined-costs costs) ((juxt :need :setting) record)))

(defn valid-costs
  "Returns
    - which need setting pairs occur in the transitions file that are uncosted
    - which costs have no matching need setting pair in the transtions
    - which need settnigs pairs do have defined costs"
  [transitions costs]
  (let [trans-need-settings (into #{}
                                  (comp
                                   (mapcat (juxt (juxt :need-1 :setting-1)
                                                 (juxt :need-2 :setting-2)))
                                   (remove #(some (fn [x] (= NON-SEND x)) %)))
                                  transitions)
        defined-costs (into #{} (map (juxt :need :setting)) costs)
        [ndc nmns dc] (d/diff trans-need-settings defined-costs)]
    {:no-defined-cost ndc
     :no-matching-need-setting nmns
     :defined-cost dc}))

(defn duplicate-costs
  "Returns any duplicate need setting pair and the number of times it
  occurs"
  [costs]
  (->> costs
       (map (juxt :need :setting))
       frequencies
       (keep (fn [[pair count]]
               (when (< 1 count)
                 [pair count])))))

(defn transitions-with-missing-costs [costs transitions]
  (into #{}
        (remove (partial transition-has-cost? costs))
        transitions))
