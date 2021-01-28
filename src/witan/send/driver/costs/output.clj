(ns witan.send.driver.costs.output
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [dk.ative.docjure.spreadsheet :as dkxl]))

(defn cost-vec [cost-maps]
  (into [["need" "setting" "cost"]]
        (map (juxt :need :setting :cost))
        (sort-by (juxt first second) cost-maps)))

(defn ->xlsx [out-file-name cost-maps]
  (dkxl/save-workbook! out-file-name
                       (dkxl/create-workbook
                        "Costs"
                        (cost-vec cost-maps))))

(defn ->csv [out-file-name cost-maps]
  (with-open [w (io/writer out-file-name)]
    (csv/write-csv w (cost-vec cost-maps))))
