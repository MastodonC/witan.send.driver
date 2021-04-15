(ns witan.send.driver.transition-counts.settings
  (:require [witan.send.domain.academic-years :as ay]
            [witan.send.driver.output.xfs :as oxfs]))

(defn key-by-setting-transition-ncy-and-calendar-year [{:keys [transition-count]
                                                        :or {transition-count 0}
                                                        :as tc}]
  [(select-keys tc [:simulation :calendar-year
                    :setting-1 :academic-year-1
                    :setting-2 :academic-year-2])
   transition-count])

(def key-by-setting-transition-ncy-and-calendar-year-xf
  (map key-by-setting-transition-ncy-and-calendar-year))

(defn setting-transition-counts [simulated-transition-counts]
  (transduce
   key-by-setting-transition-ncy-and-calendar-year-xf
   oxfs/histogram-rf
   simulated-transition-counts))

(defn key-census-by-ncy-and-setting [{:keys [transition-count]
                                      :or {transition-count 0}
                                      :as census-record}]
  [(select-keys census-record [:simulation :calendar-year :academic-year :setting])
   transition-count])

(defn key-census-by-keystage-and-setting [{:keys [transition-count academic-year]
                                           :or {transition-count 0}
                                           :as census-record}]
  [(-> census-record
       (select-keys [:simulation :calendar-year :setting])
       (assoc :keystage (-> academic-year
                            ay/national-curriculum-stage
                            ay/key-stage-names)))
   transition-count])
