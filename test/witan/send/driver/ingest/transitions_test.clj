(ns witan.send.driver.ingest.transitions-test
  (:require [witan.send.driver.ingest.transitions :as sut]
            [clojure.test :as t]))

(def mini-census
  [{:anon-ref 0, :calendar-year 2016, :setting "SA", :need "N1", :academic-year 1} ;; needs a joiner record too
   {:anon-ref 0, :calendar-year 2017, :setting "SB", :need "N2", :academic-year 2}
   {:anon-ref 0, :calendar-year 2018, :setting "SA", :need "N1", :academic-year 3}

   {:anon-ref 1, :calendar-year 2015, :setting "SA", :need "N1", :academic-year 1} ;; leaver only

   {:anon-ref 2, :calendar-year 2018, :setting "SA", :need "N1", :academic-year 1} ;; joiner only

   {:anon-ref 3, :calendar-year 2016, :setting "SA", :need "N1", :academic-year 1} ;; joiner & leaver
   ])

(t/deftest test-transitions
  (t/testing "Happy Path"
    (t/is
     (= #{{:anon-ref 0, :calendar-year 2015, :setting-2 "SA", :need-2 "N1", :academic-year-2 1, :academic-year-1 0, :need-1 "NONSEND", :setting-1 "NONSEND"}
          {:anon-ref 0, :calendar-year 2016, :setting-2 "SB", :need-2 "N2", :academic-year-2 2, :setting-1 "SA", :need-1 "N1", :academic-year-1 1}
          {:anon-ref 0, :calendar-year 2017, :setting-1 "SB", :need-1 "N2", :academic-year-1 2, :setting-2 "SA", :need-2 "N1", :academic-year-2 3}

          {:anon-ref 1, :calendar-year 2015, :setting-1 "SA", :need-1 "N1", :academic-year-1 1, :need-2 "NONSEND", :setting-2 "NONSEND", :academic-year-2 2}

          {:anon-ref 2, :calendar-year 2017, :setting-2 "SA", :need-2 "N1", :academic-year-2 1, :academic-year-1 0, :need-1 "NONSEND", :setting-1 "NONSEND"}

          {:anon-ref 3, :calendar-year 2015, :setting-2 "SA", :need-2 "N1", :academic-year-2 1, :academic-year-1 0, :need-1 "NONSEND", :setting-1 "NONSEND"}
          {:anon-ref 3, :calendar-year 2016, :setting-1 "SA", :need-1 "N1", :academic-year-1 1, :need-2 "NONSEND", :setting-2 "NONSEND", :academic-year-2 2}}
        (sut/transitions mini-census)))))

(t/deftest side-1
  (t/testing "Happy Path"
    (t/is #{{:anon-ref 3, :calendar-year 2016, :setting-1 "SA", :need-1 "N1", :academic-year-1 1}
            {:anon-ref 0, :calendar-year 2017, :setting-1 "SB", :need-1 "N2", :academic-year-1 2}
            {:anon-ref 0, :calendar-year 2016, :setting-1 "SA", :need-1 "N1", :academic-year-1 1}
            {:anon-ref 1, :calendar-year 2015, :setting-1 "SA", :need-1 "N1", :academic-year-1 1}}
          (sut/*-1-side mini-census))))

(t/deftest side-2
  (t/testing "Happy Path"
    (t/is #{{:anon-ref 0, :calendar-year 2015, :setting-2 "SA", :need-2 "N1", :academic-year-2 1}
            {:anon-ref 0, :calendar-year 2016, :setting-2 "SB", :need-2 "N2", :academic-year-2 2}
            {:anon-ref 2, :calendar-year 2017, :setting-2 "SA", :need-2 "N1", :academic-year-2 1}
            {:anon-ref 3, :calendar-year 2015, :setting-2 "SA", :need-2 "N1", :academic-year-2 1}
            {:anon-ref 0, :calendar-year 2017, :setting-2 "SA", :need-2 "N1", :academic-year-2 3}}
          (sut/*-2-side mini-census))))
