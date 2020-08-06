(ns witan.send.driver.validate.states
  (:require [clojure.math.combinatorics :as combo]
            [witan.send.check-inputs :as ci]
            [witan.send.driver.ingest :as i]
            [witan.send.domain.academic-years :as ay]))

(defn valid-states-for-setting [{:keys [needs setting min-academic-year max-academic-year]}]
  (into []
        (map vec)
        (combo/cartesian-product (range min-academic-year (inc max-academic-year))
                                 needs
                                 (vector setting))))

(def set-of-valid-states
  (memoize
   (fn [valid-states]
     (into #{}
           (mapcat valid-states-for-setting)
           valid-states))))

(def set-of-valid-setting-transitions
  (memoize
   (fn [valid-states]
     (into #{}
           (comp (mapcat (fn [{:keys [setting setting->setting]}]
                           (combo/cartesian-product (vector setting)
                                                    setting->setting)))
                 (map vec))
           valid-states))))

(defn valid-ay-need-setting? [valid-states ay need setting]
  (let [vs (set-of-valid-states valid-states)]
    (vs [ay need setting])))

(defn ay-need-setting-1-valid? [valid-settings
                                {:keys [academic-year-1 need-1 setting-1]
                                 :as transition}]
  (when (or (i/joiner? transition)
            (valid-ay-need-setting? valid-settings
                                    academic-year-1 need-1 setting-1))
    transition))

(defn ay-need-setting-2-valid? [valid-settings
                                {:keys [academic-year-2 need-2 setting-2]
                                 :as transition}]
  (when (or (i/leaver? transition)
            (valid-ay-need-setting? valid-settings
                                    academic-year-2 need-2 setting-2))
    transition))

(defn valid-setting-transition? [valid-states
                                 {:keys [setting-1 setting-2]
                                  :as transition}]
  (or (i/joiner? transition)
      (i/leaver? transition)
      ((set-of-valid-setting-transitions valid-states) [setting-1 setting-2])))

(defn validate [entity predicate anomaly]
  (if (predicate entity)
    entity
    (update entity :anomalies conj anomaly)))

(defn validate-transition
  "Take a transition and return it or a map with the original data and a
  vector of all the reasons why it has failed validation under the
  key :anomalies"
  [valid-states transition]
  (reduce (fn [a [predicate anomaly]]
            (validate a predicate anomaly))
          transition
          [[(complement i/outside-of-send?) :outside-of-send]
           [(complement ci/miscoded-nonsend?) :miscoded-non-send]
           [i/advances-one-ay? :does-not-advance-1-ay]
           [(partial ay-need-setting-1-valid? valid-states) :ay-need-setting-1-invalid]
           [(partial ay-need-setting-2-valid? valid-states) :ay-need-setting-2-invalid]
           [(partial valid-setting-transition? valid-states) :invalid-setting-transition]]))

(defn valid-transition? [valid-states transition]
  (when-not (:anomalies (validate-transition valid-states transition))
    transition))

(defn keystage-min-ay [y]
  (cond
    (ay/early-years y) (apply min ay/early-years)
    (ay/key-stage-1 y) (apply min ay/key-stage-1)
    (ay/key-stage-2 y) (apply min ay/key-stage-2)
    (ay/key-stage-3 y) (apply min ay/key-stage-3)
    (ay/key-stage-4 y) (apply min ay/key-stage-4)
    (ay/key-stage-5 y) (apply min ay/key-stage-5)
    (ay/ncy-15+ y) (apply min ay/ncy-15+)))

(defn keystage-max-ay [y]
  (cond
    (ay/early-years y) (apply max ay/early-years)
    (ay/key-stage-1 y) (apply max ay/key-stage-1)
    (ay/key-stage-2 y) (apply max ay/key-stage-2)
    (ay/key-stage-3 y) (apply max ay/key-stage-3)
    (ay/key-stage-4 y) (apply max ay/key-stage-4)
    (ay/key-stage-5 y) (apply max ay/key-stage-5)
    (ay/ncy-15+ y) (apply max ay/ncy-15+)))

(defn get-grouping [setting setting-groups]
  (get setting-groups setting setting))

(defn build-dummy-states [needs settings setting-groups census-data]
  (map (fn [setting] (let [ays (into #{} (comp
                                          (filter #(= setting (:setting %)))
                                          (filter #(< (:academic-year %) 21))
                                          (map :academic-year))
                                     census-data)]
                       (assoc {}
                              :setting setting
                              :setting-group (get-grouping setting setting-groups)
                              :min-academic-year (keystage-min-ay (apply min ays))
                              :max-academic-year (keystage-max-ay (apply max ays))
                              :needs needs
                              :setting->setting settings))) settings))
