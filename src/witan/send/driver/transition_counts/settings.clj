(ns witan.send.driver.transition-counts.settings
  (:require [witan.send.driver.output.xfs :as oxfs]))

(defn key-by-setting-transition-ncy-and-calendar-year [{:keys [transition-count]
                                                        :or {transition-count 0}
                                                        :as tc}]
  [(select-keys tc [:simulation :calendar-year
                    :setting-1 :academic-year-1
                    :setting-2 :academic-year-2])
   transition-count])

(defn setting-transition-counts [simulated-transition-counts]
  (transduce
   (map key-by-setting-transition-ncy-and-calendar-year)
   oxfs/histogram-rf
   simulated-transition-counts))
