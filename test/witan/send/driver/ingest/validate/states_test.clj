(ns witan.send.driver.ingest.validate.states-test
  (:require [witan.send.driver.validate.states :as sut]
            [clojure.test :as t]))

(def valid-states [{:setting "a"
                    :min-academic-year 0 :max-academic-year 5
                    :needs #{"1" "2" "3"}
                    :setting->setting #{"a" "b"}}
                   {:setting "b"
                    :min-academic-year 0 :max-academic-year 5
                    :needs #{"1" "2"}
                    :setting->setting #{"a" "b"}}
                   {:setting "c"
                    :min-academic-year 6 :max-academic-year 10
                    :needs #{"1" "2" "3"}
                    :setting->setting #{"c"}}])

(t/deftest test-validate-transition
  (t/testing "Happy path"
    (t/is (= (sut/validate-transition
              valid-states
              {:setting-1 "a",
               :need-1 "1",
               :academic-year-1 0,
               :setting-2 "a",
               :need-2 "1",
               :academic-year-2 1})
             {:setting-1 "a",
              :need-1 "1",
              :academic-year-1 0,
              :setting-2 "a",
              :need-2 "1",
              :academic-year-2 1}))
    (t/is (= (sut/validate-transition
              valid-states
              {:setting-1 "a",
               :need-1 "1",
               :academic-year-1 -1,
               :setting-2 "a",
               :need-2 "1",
               :academic-year-2 0})
             {:setting-1 "a",
              :need-1 "1",
              :academic-year-1 -1,
              :setting-2 "a",
              :need-2 "1",
              :academic-year-2 0,
              :anomalies '(:ay-need-setting-1-invalid)}))))
