(ns witan.send.driver.output.academic-years
  (:require [clojure.core.async :as a]
            [witan.send.driver.async :as da]
            [witan.send.driver.output.xfs :as xfs]))

(defn chan-map
  ([{::keys [in-chan out-chan]}]
   {::in-chan in-chan
    ::out-chan out-chan})
  ([]
   (chan-map {::in-chan (a/chan 1024)
              ::out-chan (a/chan 1)})))

(defn key-by-academic-year-calendar-year
  "This should work with census records created from transitions or
  transition counts. If it is from transitions without the count it
  presumes that the count, which will then be summed, is 1"
  [{:keys [simulation academic-year calendar-year transition-count]
    :or {transition-count 1}
    :as _census-record}]
  [{:simulation simulation :academic-year academic-year :calendar-year calendar-year} transition-count])

(def academic-years-xf
  (map key-by-academic-year-calendar-year))

(defn academic-years-counts
  "This returns a chan containing a map of
  simulation/setting/calendar-year -> sum of setting"
  ([{::keys [in-chan out-chan] :as chan-map} xf rf]
   (da/transduce-pipe in-chan out-chan xf rf)
   chan-map)
  ([chan-map]
   (academic-years-counts chan-map academic-years-xf xfs/histogram-rf)))
