(ns witan.send.driver.output.settings
  (:require [clojure.core.async :as a]
            [witan.send.driver.output.xfs :as xfs]))

(defn chan-map
  ([{::keys [in-chan out-chan]}]
   {::in-chan in-chan
    ::out-chan out-chan})
  ([]
   (chan-map {::in-chan (a/chan 1024)
              ::out-chan (a/chan 1)})))

(defn key-by-setting-calendar-year
  [{:keys [simulation setting calendar-year transition-count]
    :or {transition-count 1}
    :as _census-record}]
  [{:simulation simulation :setting setting :calendar-year calendar-year} transition-count])

(def settings-xf
  (map key-by-setting-calendar-year))

(defn key-by-setting-calendar-year-need
  [{:keys [simulation setting calendar-year need transition-count]
    :or {transition-count 1}
    :as _census-record}]
  [{:simulation simulation :setting setting :calendar-year calendar-year :need need} transition-count])

(def settings-by-need-xf
  (map key-by-setting-calendar-year-need))


(defn key-by-setting-calendar-year-academic-year
  [{:keys [simulation setting calendar-year academic-year transition-count]
    :or {transition-count 1}
    :as _census-record}]
  [{:simulation simulation :setting setting :calendar-year calendar-year :academic-year academic-year} transition-count])

(def settings-by-academic-year-xf
  (map key-by-setting-calendar-year-academic-year))

(defn settings-counts
  ([{::keys [in-chan out-chan] :as chan-map} xf rf]
   (a/pipe
    (a/transduce xf rf (rf) in-chan)
    out-chan)
   chan-map)
  ([chan-map]
   (settings-counts chan-map settings-xf xfs/histogram-rf)))


(comment

  (transduce
   settings-by-need-xf
   xfs/histogram-rf
   [{:setting "a" :need "n-a" :calendar-year 2020 :simulation 1}
    {:setting "a" :need "n-b" :calendar-year 2020 :simulation 1}
    {:setting "a" :need "n-a" :calendar-year 2020 :simulation 2}
    {:setting "a" :need "n-a" :calendar-year 2021 :simulation 1}
    {:setting "a" :need "n-a" :calendar-year 2021 :simulation 2}

    {:setting "b" :need "n-a" :calendar-year 2020 :simulation 1}
    {:setting "b" :need "n-b" :calendar-year 2020 :simulation 1}
    {:setting "b" :need "n-a" :calendar-year 2020 :simulation 2}
    {:setting "b" :need "n-a" :calendar-year 2021 :simulation 1}
    {:setting "b" :need "n-b" :calendar-year 2021 :simulation 1}
    {:setting "b" :need "n-a" :calendar-year 2021 :simulation 2}])

  (transduce
   settings-xf
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
  ;; {{:setting "a", :calendar-year 2020}
  ;;  {:min 1.0, :low-95pc-bound 1.0, :q1 1.0, :median 1.5, :q3 2.0, :high-95pc-bound 2.0, :max 2.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__41329 0x5c16c15f "kixi.stats.digest$t_digest$fn$reify__41329@5c16c15f"]},

  ;;  {:setting "a", :calendar-year 2021}
  ;;  {:min 1.0, :low-95pc-bound 1.0, :q1 1.0, :median 1.0, :q3 1.0, :high-95pc-bound 1.0, :max 1.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__41329 0x6bf76cca "kixi.stats.digest$t_digest$fn$reify__41329@6bf76cca"]},

  ;;  {:setting "b", :calendar-year 2020}
  ;;  {:min 1.0, :low-95pc-bound 1.0, :q1 1.0, :median 1.0, :q3 1.0, :high-95pc-bound 1.0, :max 1.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__41329 0x7a03eac5 "kixi.stats.digest$t_digest$fn$reify__41329@7a03eac5"]},

  ;;  {:setting "b", :calendar-year 2021}
  ;;  {:min 1.0, :low-95pc-bound 1.0, :q1 1.0, :median 1.0, :q3 1.0, :high-95pc-bound 1.0, :max 1.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__41329 0x7f0f0c91 "kixi.stats.digest$t_digest$fn$reify__41329@7f0f0c91"]}}


  (transduce
   settings-xf
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
  ;; {{:setting "a", :calendar-year 2020}
  ;;  {:min 3.0, :low-95pc-bound 3.0, :q1 3.0, :median 6.5, :q3 10.0, :high-95pc-bound 10.0, :max 10.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__41329 0x511a17cc "kixi.stats.digest$t_digest$fn$reify__41329@511a17cc"]},

  ;;  {:setting "a", :calendar-year 2021}
  ;;  {:min 3.0, :low-95pc-bound 3.0, :q1 3.0, :median 3.0, :q3 3.0, :high-95pc-bound 3.0, :max 3.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__41329 0x130a39df "kixi.stats.digest$t_digest$fn$reify__41329@130a39df"]},

  ;;  {:setting "b", :calendar-year 2020}
  ;;  {:min 3.0, :low-95pc-bound 3.0, :q1 3.0, :median 3.0, :q3 3.0, :high-95pc-bound 3.0, :max 3.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__41329 0x6a82dc86 "kixi.stats.digest$t_digest$fn$reify__41329@6a82dc86"]},

  ;;  {:setting "b", :calendar-year 2021}
  ;;  {:min 3.0, :low-95pc-bound 3.0, :q1 3.0, :median 3.0, :q3 3.0, :high-95pc-bound 3.0, :max 3.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__41329 0x12193a67 "kixi.stats.digest$t_digest$fn$reify__41329@12193a67"]}}

  )
