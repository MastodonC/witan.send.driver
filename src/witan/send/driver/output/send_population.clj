(ns witan.send.driver.output.send-population
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

(defn key-by-calendar-year
  [{:keys [simulation calendar-year transition-count]
    :or {transition-count 1}
    :as _census-record}]
  [{:simulation simulation :calendar-year calendar-year} transition-count])

(def send-population-xf
  (map key-by-calendar-year))

(defn send-population-counts
  ([{::keys [in-chan out-chan] :as chan-map} xf rf]
   (da/transduce-pipe in-chan out-chan xf rf)
   chan-map)
  ([chan-map]
   (send-population-counts chan-map send-population-xf xfs/histogram-rf)))

(comment

  (transduce
   send-population-xf
   xfs/histogram-rf
   [{:setting "a" :calendar-year 2020 :simulation 1}
    {:setting "a" :calendar-year 2020 :simulation 1}
    {:setting "a" :calendar-year 2020 :simulation 2}
    {:setting "a" :calendar-year 2021 :simulation 1}
    {:setting "a" :calendar-year 2021 :simulation 2}

    {:setting "b" :calendar-year 2020 :simulation 1}
    {:setting "b" :calendar-year 2020 :simulation 2}
    {:setting "b" :calendar-year 2021 :simulation 1}
    {:setting "b" :calendar-year 2021 :simulation 2}])
  ;; {{:calendar-year 2020}
  ;; {:min 2.0, :low-95pc-bound 2.0, :q1 2.0, :median 2.5, :q3 3.0, :high-95pc-bound 3.0, :max 3.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__24569 0x5f684856 "kixi.stats.digest$t_digest$fn$reify__24569@5f684856"]},
  ;;
  ;; {:calendar-year 2021}
  ;; {:min 2.0, :low-95pc-bound 2.0, :q1 2.0, :median 2.0, :q3 2.0, :high-95pc-bound 2.0, :max 2.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__24569 0x4c2b5c96 "kixi.stats.digest$t_digest$fn$reify__24569@4c2b5c96"]}}

  (transduce
   send-population-xf
   xfs/histogram-rf
   [{:setting "a" :calendar-year 2020 :simulation 1 :transition-count 5}
    {:setting "a" :calendar-year 2020 :simulation 1 :transition-count 5}
    {:setting "a" :calendar-year 2020 :simulation 2 :transition-count 3}
    {:setting "a" :calendar-year 2021 :simulation 1 :transition-count 3}
    {:setting "a" :calendar-year 2021 :simulation 2 :transition-count 3}

    {:setting "b" :calendar-year 2020 :simulation 1 :transition-count 3}
    {:setting "b" :calendar-year 2020 :simulation 2 :transition-count 3}
    {:setting "b" :calendar-year 2021 :simulation 1 :transition-count 3}
    {:setting "b" :calendar-year 2021 :simulation 2 :transition-count 3}])
  ;; {{:calendar-year 2020}
  ;; {:min 6.0, :low-95pc-bound 6.0, :q1 6.0, :median 9.5, :q3 13.0, :high-95pc-bound 13.0, :max 13.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__24569 0x8409e00 "kixi.stats.digest$t_digest$fn$reify__24569@8409e00"]},
  ;; {:calendar-year 2021}
  ;; {:min 6.0, :low-95pc-bound 6.0, :q1 6.0, :median 6.0, :q3 6.0, :high-95pc-bound 6.0, :max 6.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__24569 0x6850aa56 "kixi.stats.digest$t_digest$fn$reify__24569@6850aa56"]}}
  )
