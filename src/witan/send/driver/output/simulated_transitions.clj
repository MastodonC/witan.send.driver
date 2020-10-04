(ns witan.send.driver.output.simulated-transitions
  (:require [witan.send.driver.transition-counts :as tc]))

(defn simulated-transitions->census [sts]
  (tc/transition-counts->census sts))
