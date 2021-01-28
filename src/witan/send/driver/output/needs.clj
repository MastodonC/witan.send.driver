(ns witan.send.driver.output.needs
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

(defn key-by-need-calendar-year
  [{:keys [simulation need calendar-year transition-count]
    :or {transition-count 1}
    :as _census-record}]
  [{:simulation simulation :need need :calendar-year calendar-year} transition-count])

(def needs-xf
  (map key-by-need-calendar-year))

(defn needs-counts
  ([{::keys [in-chan out-chan] :as chan-map} xf rf]
   (da/transduce-pipe in-chan out-chan xf rf)
   chan-map)
  ([chan-map]
   (needs-counts chan-map needs-xf xfs/histogram-rf)))
