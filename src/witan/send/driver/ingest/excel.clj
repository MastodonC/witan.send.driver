(ns witan.send.driver.ingest.excel
  (:require [dk.ative.docjure.spreadsheet :as xl])
  (:import [org.apache.poi.ss.usermodel Row]))

(defn read-row [^org.apache.poi.ss.usermodel.Row row]
  (into []
        (map xl/read-cell)
        (xl/cell-seq row)))

(defn rows [file-name sheet-name]
  (let [row-seq (->> (xl/load-workbook file-name)
                     (xl/select-sheet sheet-name)
                     xl/row-seq)]
    (map read-row row-seq)))

(defn row-maps [file-name sheet-name cols-map]
  (->> (xl/load-workbook file-name)
       (xl/select-sheet sheet-name)
       (xl/select-columns cols-map)))

(defn file->sheets [file-name]
  (into []
        (-> file-name
            xl/load-workbook
            xl/sheet-seq)))

(defn files->sheets [files]
  (into []
        (mapcat file->sheets)
        files))

(def file-names->workbook-xf
  (map (fn [file-name] {::file-name file-name
                        ::workbook (xl/load-workbook file-name)})))

(defn sheets-with-metadata [{:keys [::file-name ::workbook]}]
  {::file-name file-name
   ::sheets (xl/sheet-seq workbook)})

(defn rows-with-metadata [{:keys [::file-name ::sheets]}]
  (into []
        (comp
         (map (fn [sheet]
                {::file-name file-name
                 ::sheet-name (xl/sheet-name sheet)
                 ::rows (into [] (xl/row-seq sheet))}))
         (mapcat (fn [{:keys [::file-name ::sheet-name ::rows]}]
                   (into []
                         (comp
                          (map (fn [^Row row]
                                 {::file-name file-name
                                  ::sheet-name sheet-name
                                  ::row row
                                  ::row-index (inc (.getRowNum row))
                                  ::cells (read-row row)}))
                          (filter #(some some? (::cells %))))
                         rows))))
        sheets))

(def workbook->data-xf
  (comp
   (map sheets-with-metadata)
   (mapcat rows-with-metadata)))

(def files->data-xf
  (comp
   file-names->workbook-xf
   workbook->data-xf))

(defn files->data [file-names]
  (into []
        files->data-xf
        file-names))
