(ns witan.send.driver.ingest.valid-states
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [witan.send.driver.ingest.excel :as ie]
            [witan.send.driver.ingest :as i]))

(defn clip [n lower-bound upper-bound]
  (cond
    (< n lower-bound) lower-bound
    (> n upper-bound) upper-bound
    :else n))

(def valid-states-xf
  (comp (map #(update % :min-academic-year i/->int))
        (map #(update % :max-academic-year i/->int))
        (map #(assoc % :setting-group (:setting %)))
        (map #(assoc % :max-academic-year (clip (:max-academic-year %) -5 21)))))

(defn load-valid-states [file-name fixup-xf]
  (let [header [:setting :setting-group :min-academic-year :max-academic-year :needs :setting->setting]]
    (into []
          (comp
           (drop 1)
           fixup-xf)
          (ie/rows file-name "Sheet1"))))

(defn ->csv [output-prefix valid-states]
  (with-open [w (io/writer (str output-prefix "valid-states.csv"))]
    (let [header [:setting :setting-group :min-academic-year :max-academic-year :needs :setting->setting]]
      (csv/write-csv
       w
       (into [(mapv name header)]
             (comp
              (map (fn [m] (update m :needs #(s/join "," %))))
              (map (fn [m] (update m :setting->setting #(s/join "," %))))
              (map (apply juxt header)))
             valid-states)))))
