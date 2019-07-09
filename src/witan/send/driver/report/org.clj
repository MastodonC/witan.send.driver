(ns witan.send.driver.report.org)

(defn h1 [s]
  (format "* %s\n\n" s))

(defn h2 [s]
  (format "** %s\n\n" s))

(defn link [url text]
  (format "[[%s][%s]]" url text))

(defn img [url]
  (format "[[%s]]" url))

(defn para [strs]
  (str (apply str strs) "\n\n"))

;; guts of this modified from clojure.pprint/print-table
(defn table [header-map ks rows]
  (when (seq rows)
    (let [widths (map
                  (fn [k]
                    (apply max (count (str k)) (map #(count (str (get % k))) rows)))
                  ks)
          spacers (map #(apply str (repeat % "-")) widths)
          fmts (map #(str "%" % "s") widths)
          fmt-row (fn [leader divider trailer row]
                    (str leader
                         (apply str (interpose divider
                                               (for [[col fmt] (map vector (map #(get row %) ks) fmts)]
                                                 (format fmt (str col)))))
                         trailer))]
      (str
       "\n"
       (fmt-row "| " " | " " |" header-map)
       "\n"
       (fmt-row "|-" "-+-" "-|" (zipmap ks spacers))
       "\n"
       (apply str (interpose "\n" (mapv #(fmt-row "| " " | " " |" %) rows)))
       "\n\n"))))
