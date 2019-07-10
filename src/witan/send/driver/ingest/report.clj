(ns witan.send.driver.ingest.report
  (:require [clojure.java.io :as io]
            [net.cgrand.xforms :as x]
            [witan.send.driver.chart :as chart]
            [witan.send.driver.ingest :as i]
            [witan.send.driver.ingest.transitions :as it]
            [witan.send.driver.ingest.valid-states :as ivs]
            [witan.send.driver.validate.costs :as vc]
            [witan.send.driver.validate.states :as vs]
            [witan.send.driver.report.org :as org]
            [witan.send.driver.report.excel :as xl]))

(defn save-org-report [prefix txt]
  (with-open [w (io/writer (str prefix "report.org"))]
    (.write w txt)))

(defn org-report [settings-map {:keys [census transitions validation-charts]}]
  (str
   (org/h1 "Overview")

   (org/para ["Data was provided for calendar years "
              (apply str (interpose ", " (i/calendar-years census)))
              "."])

   (org/para ["Invalid transitions and need/setting pairs without costs can be found "
              (org/link "./invalid-transitions.xls" "here")
              "."])

   (org/h1 "Setting Mappings")

   (org/table
    {:escc "Client Name" :mc "MC Name"}
    [:escc :mc]
    (map (fn [[k v]] {:escc k :mc v}) settings-map))

   (org/h1 "SEND population per year")
   (org/table
    {:year "Year" :count "Count"}
    [:year :count]
    (map (fn [[k v ]] {:year k :count v})
         (into (sorted-map)
               (x/by-key :calendar-year x/count)
               census)))

   (org/h1 "Transitions per year")
   (org/table
    {:year "Year" :count "Count"}
    [:year :count]
    (map (fn [[k v]] {:year (str k "-" (inc k)) :count v})
         (into (sorted-map)
               (x/by-key :calendar-year x/count)
               transitions)))


   (org/h1 "Leavers per year")
   (org/table
    {:year "Year" :count "Count"}
    [:year :count]
    (map (fn [[k v]] {:year (str k "-" (inc k)) :count v})
         (into (sorted-map)
               (comp
                (filter i/leaver?)
                (x/by-key :calendar-year x/count))
               transitions)))

   (org/h1 "Joiners per year")
   (org/table
    {:year "Year" :count "Count"}
    [:year :count]
    (map (fn [[k v]] {:year (str k "-" (inc k)) :count v})
         (into (sorted-map)
               (comp
                (filter i/joiner?)
                (x/by-key :calendar-year x/count))
               transitions)))

   (org/h1 "Charts")
   (apply str
          (interpose "\n"
                     (map (fn [{:keys [title file-name]}]
                            (str (org/h2 title) (org/img (str "./" file-name))))
                          validation-charts)))))

(defn transitions-data [transitions]
  (into [["anon-ref" "calendar-year" "setting-1" "need-1" "academic-year-1" "setting-2" "need-2" "academic-year-2"]]
        (mapv (juxt :anon-ref :calendar-year :setting-1 :need-1 :academic-year-1 :setting-2 :need-2 :academic-year-2)
              transitions)))

(defn validation-report [census valid-states costs]
  (let [transitions (it/transitions census)
        header ["Anonymous Reference" "Calendar Year" "Setting 1" "Need 1" "NCY 1" "Setting 2" "Need 2" "NCY 2" "anomalies"]]
    (xl/workbook
     [{:tab-name "Invalid Transitions"
       :data (into [header]
                   (comp
                    (keep identity)
                    (map (partial vs/validate-transition valid-states))
                    (remove #(empty? (:anomalies %)))
                    (remove #(some #{:outside-of-send :does-not-advance-1-ay} (:anomalies %))) ;; get rid of things we don't model
                    (map (juxt :anon-ref :calendar-year :setting-1 :need-1 :academic-year-1 :setting-2 :need-2 :academic-year-2
                               (fn [x] (->> x
                                            :anomalies
                                            (map name)
                                            ((partial clojure.string/join " ")))))))
                   transitions)}
      {:tab-name "Does not advance 1 academic year"
       :data (into [header]
                   (comp
                    (keep identity)
                    (map (partial vs/validate-transition valid-states))
                    (remove #(empty? (:anomalies %)))
                    (filter #(some #{:does-not-advance-1-ay} (:anomalies %))) ;; get rid of things we don't model
                    (map (juxt :anon-ref :calendar-year :setting-1 :need-1 :academic-year-1 :setting-2 :need-2 :academic-year-2
                               (fn [x] (->> x
                                            :anomalies
                                            (map name)
                                            ((partial clojure.string/join " ")))))))
                   transitions)}
      {:tab-name "Missing Costs"
       :data (into [["Need" "Setting"]]
                   (->> (vc/valid-costs transitions costs)
                        :no-defined-cost
                        (sort-by (juxt first second))))}
      {:tab-name "Records with No Cost Defined"
       :data (transitions-data
              (vc/transitions-with-missing-costs transitions costs))}])))

(defn save-validation-workbook [prefix wkbk]
  (xl/save
   (str prefix "validation-report.xlsx")
   wkbk))

(defn report-all [output-prefix {:keys [census costs valid-states settings-map] :as data}]
  (merge data
         {:settings-map settings-map
          :invalid-transition-report (validation-report census valid-states costs)
          :validation-charts (chart/validation-charts output-prefix census)
          :transitions (it/transitions census)
          :census census
          :costs costs
          :valid-states valid-states}))

(defn save-all [output-prefix {:keys [invalid-transition-report settings-map transitions valid-states validation-charts] :as data}]
  (save-validation-workbook output-prefix invalid-transition-report)
  (run! (partial chart/save output-prefix) validation-charts)
  (save-org-report output-prefix (org-report settings-map data))
  (it/->csv output-prefix transitions)
  (ivs/->csv output-prefix valid-states)
  data)
