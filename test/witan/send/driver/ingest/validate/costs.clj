(ns witan.send.driver.ingest.validate.costs
  (:require [witan.send.driver.validate.costs :as sut]
            [clojure.test :as t]))

(t/deftest test-duplicate-costs
  (t/testing "Happy path"
    (t/is (= (sut/duplicate-costs [{:need "foo" :setting "bar"}
                                   {:need "foo" :setting "bar"}
                                   {:need "foo" :setting "quux"}])
             '([["foo" "bar"] 2])))
    (t/is (= (sut/duplicate-costs [{:need "foo" :setting "bar"}
                                   {:need "foo" :setting "quux"}])
             '()))))
