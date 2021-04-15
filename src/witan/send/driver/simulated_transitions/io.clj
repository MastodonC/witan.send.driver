(ns witan.send.driver.simulated-transitions.io
  (:require [clojure.core.async :as a]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            ;; [cognitect.transit :as transit]
            [taoensso.nippy :as nippy]
            [reducibles.core :as r]
            [witan.send.driver.ingest :as i]
            [clojure.string :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; nippy io
(defn thaw-file [f]
  (tap> {:file f :msg (format "Processing: %s" (.getName f))})
  (into []
        (map-indexed
         (fn [idx transition]
           (assoc transition
                  ::filename (.getName f)
                  ::file-idx idx)))
        (nippy/thaw-from-file f)))

(def thaw-file-xf
  (mapcat thaw-file))

(defn stringify-needs-and-settings [transtion-count]
  (-> transtion-count
      transient
      (assoc! :need-1 (name (:need-1 transtion-count)))
      (assoc! :need-2 (name (:need-2 transtion-count)))
      (assoc! :setting-1 (name (:setting-1 transtion-count)))
      (assoc! :setting-2 (name (:setting-2 transtion-count)))
      persistent!))

(def nippy->data-xf
  (comp
   (filter (fn [f] (re-find #"npy$" (.getName f))))
   thaw-file-xf
   (map stringify-needs-and-settings)))

(defn file-list [dirname]
  (sort (.listFiles (io/file dirname))))

(defn nippy->eduction [dirname]
  (eduction
   nippy->data-xf
   (file-list dirname)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; async base
(defn async-nippy->data [{::keys [out-chan] :as chan-map} simulated-transitions-data-dir]
  (let [in-chan (a/to-chan!! (sort-by #(.getName %) (file-list simulated-transitions-data-dir)))
        _ (a/pipeline-blocking
           3
           out-chan
           nippy->data-xf
           in-chan
           true
           (fn [e]
             (a/close! out-chan)
             (a/close! in-chan)))]
    chan-map))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; csv ingestion
(def header
  [:simulation
   :calendar-year
   :setting-1 :need-1 :academic-year-1
   :setting-2 :need-2 :academic-year-2
   :transition-count])

(defn scrub-transition-count [transition-count]
  (-> transition-count
      (update :simulation i/->int)
      (update :calendar-year i/->int)
      ;; (update :setting-1 keyword)
      ;; (update :need-1 keyword)
      (update :academic-year-1 i/->int)
      (update :academic-year-2 i/->int)
      ;; (update :setting-2 keyword)
      ;; (update :need-2 keyword)
      (update :transition-count i/->int)))

(defn csv->data
  [filename]
  (with-open [reader (io/reader filename)]
    (into []
          (comp
           (drop 1)
           (map #(zipmap header %))
           (map scrub-transition-count))
          (csv/read-csv reader))))

(defn csv->nippy
  ([simulations-per-partition in-file out-dir]
   (with-open [reader (io/reader in-file)]
     (run!
      (fn [data]
        (let [idx (-> data first :simulation)]
          (nippy/freeze-to-file
           (format "%ssimulated-transitions-%05d.npy" out-dir idx)
           data)))
      (eduction
       (drop 1)
       (map #(zipmap header %))
       (map scrub-transition-count)
       (partition-by (fn [{:keys [simulation]}] (quot simulation simulations-per-partition)))
       (csv/read-csv reader)))))
  ([in-file out-dir]
   (csv->nippy 100 in-file out-dir)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transit Ingestion
(defn split-need-setting [need-setting]
  (let [need-setting-string (name need-setting)]
    (if (= need-setting-string "NONSEND")
      ["NONSEND" "NONSEND"]
      (s/split need-setting-string #"-"))))

(defn format-count-key [[calendar-year-2 academic-year-2 need-setting-1 need-setting-2]]
  (let [[need-1 setting-1] (split-need-setting need-setting-1)
        [need-2 setting-2] (split-need-setting need-setting-2)]
    {:calendar-year (dec calendar-year-2)
     :academic-year-1 (dec academic-year-2)
     :need-1 need-1
     :setting-1 setting-1
     :academic-year-2 academic-year-2
     :need-2 need-2
     :setting-2 setting-2}))


(defn simulated-transition->transition-count [sim-number [count-key transition-count]]
  (-> count-key
      format-count-key
      (assoc :simulation sim-number
             :transition-count transition-count)))

(def simulated-transitions->transition-counts-xf
  (comp
   (mapcat (fn [[simulation-number year-projections]]
             (map (fn [yp] [simulation-number yp])
                  year-projections)))
   (mapcat (fn [[simulation-number year-projection]]
             (map (fn [transition-count]
                    (simulated-transition->transition-count simulation-number transition-count))
                  year-projection)))))

(defn transit-file->eduction [filename]
  (eduction
   simulated-transitions->transition-counts-xf
   (r/transit-reducible :msgpack (-> filename
                                     io/file
                                     io/input-stream
                                     java.util.zip.GZIPInputStream.))))

(defn gzipped-transit-file-eduction [filename]
  (eduction
   (r/transit-reducible :msgpack (-> filename
                                     io/file
                                     io/input-stream
                                     java.util.zip.GZIPInputStream.))))

(def gzipped-transit-file-paths-xf
  (comp
   (filter #(re-find #"transit\.gz$" (.getName %)))
   (map #(.getPath %))))

(def transit-files-xf
  (comp
   gzipped-transit-file-paths-xf
   (map gzipped-transit-file-eduction)
   cat
   simulated-transitions->transition-counts-xf))

(defn sorted-file-list [directory]
  (-> directory
      io/file
      .listFiles
      sort))

(defn transit-files->eduction [directory]
  (eduction
   transit-files-xf
   (sorted-file-list directory)))
