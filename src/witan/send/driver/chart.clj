(ns witan.send.driver.chart
  (:require [clojure.string :as s]
            [net.cgrand.xforms :as x]
            [cljplot.render :as plotr]
            [cljplot.build :as plotb]
            [cljplot.core :as plot]
            [clojure2d.color :as color]
            [witan.send.driver.chart :as chart]
            [witan.send.driver.ingest :as i]
            [witan.send.driver.ingest.transitions :as it]))

(defn map-kv [f coll]
  (reduce-kv (fn [m k v] (assoc m k (f v))) (empty coll) coll))

(defn default-domain-map [k data default-value]
  (zipmap
   (x/into (sorted-set) (map k) data)
   (repeat default-value)))

(defn pivot-table
  "Pivot a vector of maps into a map of idx0 -> vector of idx1, rf value
  pairs all sorted by key and reduced according to rf."
  ([idx0 idx1 rf data]
   (x/into (sorted-map)
           (x/by-key idx0
                     (comp
                      (x/by-key idx1 rf)
                      (x/reduce (fn
                                  ([]
                                   (default-domain-map idx1 data 0)) ;; should default-domain-map be passed in?
                                  ([a]
                                   (sort-by first a))
                                  ([m [k v]]
                                   (assoc m k v))))))
           data))
  ([idx0 rf data]
   (x/into (default-domain-map idx0 data 0)
           (x/by-key idx0 rf)
           data)))

(defn pivot-table-vals [data]
  (map-kv #(map second %) data))

(defn sum-key-rf [k]
  (x/reduce (fn
              ([] 0)
              ([a] a)
              ([a x] (+ a (k x))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; charts

;; {need/setting [y1 y2 y3 y4]
;;  ,,,}

;; use pivot-table and pivot-table-values
(defn grouped-column
  [title domain colours data]
  (let [palette (map colours domain)
        legend-spec (mapv
                     (fn [sd p]
                       [:rect sd {:color p}])
                     domain palette)]
    (-> (plotb/series [:stack-vertical [:bar data {:palette palette}]])
        (plotb/preprocess-series)
        (plotb/update-scale :y :ticks 20)
        (plotb/update-scale :y :fmt "%,.0f")
        ;; (plotb/update-scale :x :fmt name)
        (plotb/add-axes :left)
        (plotb/add-axes :bottom)
        ;;(plotb/add-label :bottom domain-name)
        ;;(plotb/add-label :left range-name)
        (plotb/add-label :top title {:font-size 24 :font "Open Sans Bold" :margin 36})
        (plotb/add-legend "" legend-spec)
        (plotr/render-lattice {:width 1539 :height 1037}))))

(defn grouped-column-mini
  [title domain colours data]
  (let [palette (map colours domain)
        legend-spec (mapv
                     (fn [sd p]
                       [:rect sd {:color p}])
                     domain palette)]
    (-> (plotb/series [:stack-vertical [:bar data {:palette palette}]])
        (plotb/preprocess-series)
        (plotb/update-scale :y :ticks 5)
        (plotb/update-scale :y :fmt "%,.0f")
        ;; (plotb/update-scale :x :fmt name)
        (plotb/add-axes :left)
        (plotb/add-axes :bottom)
        ;;(plotb/add-label :bottom domain-name)
        ;;(plotb/add-label :left range-name)
        (plotb/add-label :top title {:font-size 12 :font "Open Sans" :margin 16})
        (plotb/add-legend "" legend-spec)
        (plotr/render-lattice {:width 300 :height 200}))))

(defn stacked-column
  [title domain colours data]
  (let [palette (map colours domain)
        legend-spec (mapv
                     (fn [sd p]
                       [:rect sd {:color p}])
                     domain palette)]
    (-> (plotb/series
         [:stack-vertical [:sbar data {:palette palette}]])
        (plotb/preprocess-series)
        (plotb/update-scale :x :scale [:bands {:padding-in 0.0 :padding-out 0.2}])
        (plotb/update-scale :y :ticks 5)
        (plotb/update-scale :y :fmt "%,.0f")
        (plotb/add-axes :left)
        (plotb/add-axes :bottom)
        (plotb/add-label :top title {:font-size 24 :font "Open Sans Bold" :margin 36})
        ;; (plotb/add-label :bottom domain-name)
        ;; (plotb/add-label :left range-name)
        (plotb/add-legend "" legend-spec)
        (plotr/render-lattice {:width 1539 :height 1037}))))

(defn stacked-column-mini
  [title domain colours data]
  (let [palette (map colours domain)
        legend-spec (mapv
                     (fn [sd p]
                       [:rect sd {:color p}])
                     domain palette)]
    (-> (plotb/series
         [:stack-vertical [:sbar data {:palette palette}]])
        (plotb/preprocess-series)
        (plotb/update-scale :x :scale [:bands {:padding-in 0.0 :padding-out 0.2}])
        (plotb/update-scale :y :ticks 10)
        (plotb/update-scale :y :fmt "%,.0f")
        (plotb/add-axes :left)
        (plotb/add-axes :bottom)
        (plotb/add-label :top title {:font-size 12 :font "Open Sans" :margin 16})
        ;; (plotb/add-label :bottom domain-name)
        ;; (plotb/add-label :left range-name)
        (plotb/add-legend "" legend-spec)
        (plotr/render-lattice {:width 300 :height 200}))))

;; use pivot-table
(defn multi-line [data]
  (let [pal (color/palette-presets :tableau-20-2)
        pt-types [\o \x \v]
        legend (map #(vector :line (name %2) {:color %1 :shape %3}) pal (keys data) (cycle pt-types))]
    (-> (plotb/series [:grid])
        (plotb/add-multi :line data {:stroke {:size 1}} {:color pal
                                                         :point (cycle (map (fn [c] {:type c :size 12}) pt-types))})
        (plotb/preprocess-series)
        (plotb/update-scale :x :fmt int)
        (plotb/update-scale :y :fmt "%,.0f")
        (plotb/add-axes :bottom)
        (plotb/add-axes :left)
        (plotb/add-label :bottom "bottom label")
        (plotb/add-label :left "left label")
        (plotb/add-legend "symbol" legend)
        (plotr/render-lattice {:width 1539 :height 1037}))))

(defn save
  [prefix {:keys [chart file-name] :as chart-spec}]
  (plot/save chart (str prefix file-name))
  chart-spec)

(defn wrapped-stacked-column-chart
  [title idx0 idx1 idx1-domain idx1-palette rf data]
  {:chart
   (stacked-column
    title
    idx1-domain
    idx1-palette
    (-> (pivot-table idx0 ;; :calendar-year
                     idx1 ;; domain-key
                     rf
                     data)
        (pivot-table-vals)))
   :file-name (s/lower-case (str (s/replace title " " "_") ".png"))
   :title title})

(defn wrapped-grouped-column-chart
  [title idx0 idx1 idx1-domain idx1-palette rf data]
  {:chart
   (grouped-column
    title
    idx1-domain
    idx1-palette
    (-> (pivot-table idx0 ;; :calendar-year
                     idx1 ;; domain-key
                     rf
                     data)
        (pivot-table-vals)))
   :file-name (s/lower-case (str (s/replace title " " "_") ".png"))
   :title title})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Census based charts

(defn total-population-per-calendar-year-broken-down-by-need
  [{:keys [needs needs-palette census]}]
  (wrapped-stacked-column-chart
   "Total population per calendar year broken down by need"
   :calendar-year
   :need
   needs
   needs-palette
   x/count
   census))

(defn total-population-per-calendar-year-broken-down-by-setting
  [{:keys [settings settings-palette census]}]
  (wrapped-stacked-column-chart
   "Total population per calendar year broken down by setting"
   :calendar-year
   :setting
   settings
   settings-palette
   x/count
   census))

(defn needs-by-calendar-year
  [{:keys [calendar-years calendar-years-palette census]}]
  (wrapped-grouped-column-chart
   "Needs by calendar year"
   :need
   :calendar-year
   calendar-years
   calendar-years-palette
   x/count
   census))

(defn settings-by-calendar-year
  [{:keys [calendar-years calendar-years-palette census]}]
  (wrapped-grouped-column-chart
   "Settings by calendar year"
   :setting
   :calendar-year
   calendar-years
   calendar-years-palette
   x/count
   census))

(defn total-population-per-calendar-year-by-academic-year
  [{:keys [calendar-years calendar-years-palette census]}]
  (wrapped-grouped-column-chart
   "Total population per calendar year by academic year"
   :academic-year
   :calendar-year
   calendar-years
   calendar-years-palette
   x/count
   census))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transitions based charts
(defn count-of-joiners-per-calendar-year-by-academic-year
  [{:keys [transition-years transition-years-palette transitions]}]
  (let [filter-pred i/joiner?]
    (wrapped-grouped-column-chart
     "Count of joiners per calendar year by academic year"
     :academic-year-2
     :calendar-year
     transition-years
     transition-years-palette
                               x/count
     (filter filter-pred transitions))))

(defn count-of-joiners-per-calendar-year-by-need
  [{:keys [transition-years transition-years-palette transitions]}]
  (let [filter-pred i/joiner?]
    (wrapped-grouped-column-chart
     "Count of joiners per calendar year by need"
     :need-2
     :calendar-year
     transition-years
     transition-years-palette
     x/count
                                      (filter filter-pred transitions))))

(defn count-of-joiners-per-calendar-year-by-setting
  [{:keys [transition-years transition-years-palette transitions]}]
  (let [filter-pred i/joiner?]
    (wrapped-grouped-column-chart
     "Count of joiners per calendar year by setting"
     :setting-2
     :calendar-year
     transition-years
     transition-years-palette
     x/count
     (filter filter-pred transitions))))

(defn count-of-leavers-per-calendar-year-by-academic-year
  [{:keys [transition-years transition-years-palette transitions]}]
  (let [filter-pred i/leaver?]
    (wrapped-grouped-column-chart
     "Count of leavers per calendar year by academic year"
     :academic-year-1
     :calendar-year
     transition-years
     transition-years-palette
     x/count
     (filter filter-pred transitions))))

(defn count-of-leavers-per-calendar-year-by-need
  [{:keys [transition-years transition-years-palette transitions]}]
  (let [filter-pred i/leaver?]
    (wrapped-grouped-column-chart
     "Count of leavers per calendar year by need"
     :need-1
     :calendar-year
     transition-years
     transition-years-palette
     x/count
     (filter filter-pred transitions))))

(defn count-of-leavers-per-calendar-year-by-setting
  [{:keys [transition-years transition-years-palette transitions]}]
  (let [filter-pred i/leaver?]
    (wrapped-grouped-column-chart
     "Count of leavers per calendar year by setting"
     :setting-1
     :calendar-year
     transition-years
     transition-years-palette
     x/count
     (filter filter-pred transitions))))

(defn count-of-movers-per-calendar-year-by-academic-year
  [{:keys [transition-years transition-years-palette transitions]}]
  (let [filter-pred i/mover?]
    (wrapped-grouped-column-chart
     "Count of movers per calendar year by academic year"
     :academic-year-1
     :calendar-year
     transition-years
     transition-years-palette
     x/count
     (filter filter-pred transitions))))

(defn count-of-stayers-per-calendar-year-by-academic-year
  [{:keys [transition-years transition-years-palette transitions]}]
  (let [filter-pred i/stayer?]
    (wrapped-grouped-column-chart
     "Count of stayers per calendar year by academic year"
     :academic-year-1
     :calendar-year
     transition-years
     transition-years-palette
     x/count
     (filter filter-pred transitions))))

;; #nofilter
(defn count-of-all-transitions-by-type
  [{:keys [transition-years transition-years-palette transitions]}]
  (wrapped-grouped-column-chart
   "Count of all transitions by type"
   i/transition-type
   :calendar-year
   transition-years
   transition-years-palette
   x/count
   transitions))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; all validation charts
(defn validation-charts [census]
  (let [calendar-years (x/into (sorted-set) (map :calendar-year) census)
        settings (x/into (sorted-set) (map :setting) census)
        needs (x/into (sorted-set) (map :need) census)
        transition-years (->> calendar-years
                              (drop-last)
                              (map (fn [y] (str y "-" (inc y)))))
        data {:census census
              :transitions (it/transitions census)
              :calendar-years calendar-years
              :settings settings
              :needs needs
              :transition-years transition-years
              :settings-palette (zipmap settings (color/palette-presets :tableau-20))
              :needs-palette (zipmap needs (reverse (color/palette-presets :tableau-20)))
              :calendar-years-palette (zipmap calendar-years (color/palette-presets :green-orange-teal))
              :transition-years-palette (zipmap transition-years (color/palette-presets :green-orange-teal))}]

    ((juxt
      total-population-per-calendar-year-broken-down-by-need
      total-population-per-calendar-year-broken-down-by-setting
      needs-by-calendar-year
      settings-by-calendar-year
      total-population-per-calendar-year-by-academic-year
      count-of-joiners-per-calendar-year-by-academic-year
      count-of-joiners-per-calendar-year-by-need
      count-of-joiners-per-calendar-year-by-setting
      count-of-leavers-per-calendar-year-by-academic-year
      count-of-leavers-per-calendar-year-by-need
      count-of-leavers-per-calendar-year-by-setting
      count-of-movers-per-calendar-year-by-academic-year
      count-of-stayers-per-calendar-year-by-academic-year
      count-of-all-transitions-by-type)
     data)))

(defn transition-charts [transitions]
  (let [transition-years (x/into (sorted-set) (map :calendar-year) transitions)
        calendar-years (conj transition-years (dec (first transition-years)))
        settings (x/into (sorted-set) (map :setting-1) transitions)
        needs (x/into (sorted-set) (map :need-1) transitions)
        data {:census (it/->census-like transitions)
              :transitions transitions
              :calendar-years calendar-years
              :settings settings
              :needs needs
              :transition-years transition-years
              :settings-palette (zipmap settings (color/palette-presets :tableau-20))
              :needs-palette (zipmap needs (reverse (color/palette-presets :tableau-20)))
              :calendar-years-palette (zipmap calendar-years (color/palette-presets :green-orange-teal))
              :transition-years-palette (zipmap transition-years (color/palette-presets :green-orange-teal))}]
    ((juxt
      ;; census based
      total-population-per-calendar-year-broken-down-by-need
      total-population-per-calendar-year-broken-down-by-setting
      needs-by-calendar-year
      settings-by-calendar-year
      total-population-per-calendar-year-by-academic-year

      ;; transitions based
      count-of-joiners-per-calendar-year-by-academic-year
      count-of-joiners-per-calendar-year-by-need
      count-of-joiners-per-calendar-year-by-setting
      count-of-leavers-per-calendar-year-by-academic-year
      count-of-leavers-per-calendar-year-by-need
      count-of-leavers-per-calendar-year-by-setting
      count-of-movers-per-calendar-year-by-academic-year
      count-of-stayers-per-calendar-year-by-academic-year
      count-of-all-transitions-by-type)
     data)))
