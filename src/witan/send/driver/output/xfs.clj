(ns witan.send.driver.output.xfs
  (:require [kixi.stats.core :as ksc]
            [kixi.stats.distribution :as ksd]))

(defn sorted-set-rf
  ([] (transient #{}))
  ([acc] (into (sorted-set) (persistent! acc)))
  ([acc new] (conj! acc new)))

(defn summary [histogram]
  {:min             (ksd/minimum histogram)
   :low-95pc-bound  (ksd/quantile histogram 0.025)
   :q1              (ksd/quantile histogram 0.25)
   :median          (ksd/quantile histogram 0.50)
   :q3              (ksd/quantile histogram 0.75)
   :high-95pc-bound (ksd/quantile histogram 0.975)
   :max             (ksd/maximum histogram)})

(defn histogram-summary [vs]
  (let [h (ksc/histogram (reduce ksc/histogram (ksc/histogram) vs))]
    (assoc (summary h) :histogram h)))

(defn histogram-summary-rf
  ([] (transient {}))
  ([acc]
   (into {}
         (comp
          (map (fn [[k vs]] [k (histogram-summary vs)])))
         (persistent! acc)))
  ([acc [k n]]
   (assoc! acc k (conj (acc k []) n))))

(defn histogram-rf
  ([] (transient {}))
  ([acc]
   (transduce
    (map (fn [[k ns]] [(dissoc k :simulation) ns]))
    histogram-summary-rf
    (persistent! acc)))
  ([acc [k n]]
   (assoc! acc k (+ (get acc k 0) n))))

(defn histogram-from-transition-counts
  ([xf simulated-transition-counts]
   (transduce
    xf
    histogram-rf
    simulated-transition-counts))
  ([simulated-transition-count-eduction]
   (histogram-rf
    (reduce histogram-rf simulated-transition-count-eduction))))

(comment

  (histogram-summary [1 2 3 2 1])
  ;;=> {:min 1.0, :low-95pc-bound 1.0, :q1 1.0, :median 2.0, :q3 2.25, :high-95pc-bound 3.0, :max 3.0, :histogram #object[kixi.stats.digest$t_digest$fn$reify__41329 0x24f4b0b "kixi.stats.digest$t_digest$fn$reify__41329@24f4b0b"]}
  )
