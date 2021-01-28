(ns witan.send.driver.transitions.csv
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [witan.send.driver.ingest :as i]))

(def header
  [:calendar-year :setting-1 :need-1 :academic-year-1 :setting-2 :need-2 :academic-year-2])

(defn convert-record-types [transition]
  (try
    (-> transition
        (update :calendar-year i/->int)
        (update :academic-year-1 i/->int)
        (update :academic-year-2 i/->int))
    (catch Exception e
      (throw (ex-info "Unable to parse transition." {:transition transition} e)))))

(defn base-transitions-xf [transitions-file]
  (comp
   (drop 1)
   (map #(zipmap header %))
   (map-indexed (fn [idx transition]
                  (assoc transition
                         ::file-name transitions-file
                         ::file-idx idx)))
   (map convert-record-types)))

(defn ->data
  ([transitions-file xf]
   (with-open [reader (io/reader transitions-file)]
     (into []
           xf
           (csv/read-csv reader))))
  ([transitions-file]
   (->data transitions-file
           (base-transitions-xf transitions-file))))

(comment

  (def historical-transitions (->data "../witan.send/data/demo/data/transitions.csv"))

  )
