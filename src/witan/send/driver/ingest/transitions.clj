(ns witan.send.driver.ingest.transitions
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.set :as cs]
            [witan.send.constants :as const]))

;; This was inspired by this blog post:
;; https://statcompute.wordpress.com/2018/04/08/inner-and-outer-joins-in-clojure/
;; especially the outer join #1 part

(def NONSEND (name const/non-send))

(defn *-1-side
  "Produce the *-1 side of the transitions which will not have the last
  year."
  [census]
  (let [last-year (reduce max (map :calendar-year census))]
    (into #{}
          (comp
           (filter #(< (:calendar-year %) last-year))
           (map #(cs/rename-keys % {:setting :setting-1
                                    :need :need-1
                                    :academic-year :academic-year-1})))
          census)))

(defn *-2-side
  "Produce the *-2 side of the transitions which will not have the first
  year."
  [census]
  (let [first-year (reduce min (map :calendar-year census))]
    (into #{}
          (comp
           (filter #(< first-year (:calendar-year %)))
           (map #(cs/rename-keys % {:setting :setting-2
                                    :need :need-2
                                    :academic-year :academic-year-2}))
           (map #(update % :calendar-year dec)))
          census)))

(defn needs-joiner? [transition]
  (nil? (:need-1 transition)))

(defn needs-leaver? [transition]
  (nil? (:need-2 transition)))

(defn joiner-side
  "Fill in the missing joiner side."
  [{:keys [academic-year-2] :as transition}]
  (assoc transition
         :academic-year-1 (dec academic-year-2)
         :need-1 NONSEND
         :setting-1 NONSEND))

(defn leaver-side
  "Fill in the missing leaver side"
  [{:keys [academic-year-1] :as transition}]
  (assoc transition
         :need-2 NONSEND
         :setting-2 NONSEND
         :academic-year-2 (inc academic-year-1)))

(defn transitions [census]
  (let [s1 (*-1-side census)
        s2 (*-2-side census)]
    (into (sorted-set-by (fn [x y]
                           (compare
                            ((juxt :anon-ref :calendar-year :academic-year-1 :academic-year-2) x)
                            ((juxt :anon-ref :calendar-year :academic-year-1 :academic-year-2) y))))
          (comp
           (map #(apply merge %))
           (map #(if (needs-joiner? %) (joiner-side %) %))
           (map #(if (needs-leaver? %) (leaver-side %) %)))
          (vals (group-by (juxt :anon-ref :calendar-year) (cs/union s1 s2))))))

(defn ->csv [prefix transitions]
  (with-open [w (io/writer (str prefix "transitions.csv"))]
    (let [header [:calendar-year :setting-1 :need-1 :academic-year-1 :setting-2 :need-2 :academic-year-2]]
      (csv/write-csv
       w
       (into [(mapv name header)]
             (map (apply juxt header))
             transitions)))))

(defn transition-s1->census [{:keys [calendar-year setting-1 need-1 academic-year-1]}]
  {:anon-ref -1
   :calendar-year calendar-year
   :setting setting-1
   :need need-1
   :academic-year academic-year-1})

(defn transition-s2->census [{:keys [calendar-year setting-2 need-2 academic-year-2]}]
  {:anon-ref -1
   :calendar-year (inc calendar-year)
   :setting setting-2
   :need need-2
   :academic-year academic-year-2})

(defn ->census-like
  "Create a census shaped data structure for further charting output. It
  can never be a proper census file as we don't have the pseudo IDs
  that would allow us to trace longitudinally."
  [transitions]
  (let [last-transition-year (last (into (sorted-set) (map :calendar-year transitions)))]
    (println last-transition-year)
    (into
     (into []
           (map transition-s1->census)
           transitions)
     (comp
      (filter #(= (:calendar-year %) last-transition-year))
      (map transition-s2->census))
     transitions)))
