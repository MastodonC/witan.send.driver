(ns witan.send.driver.report.excel
  (:require [dk.ative.docjure.spreadsheet :as xl]))

(defn workbook
  "Given a map of :tab-name and data create a workbood with all the right
  sheets."
  [data-seq]
  (apply xl/create-workbook (mapcat #((juxt :tab-name :data) %) data-seq)))

(defn save [file-name wkbk]
  (xl/save-workbook-into-file! file-name wkbk))
