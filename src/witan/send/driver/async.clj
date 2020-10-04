(ns witan.send.driver.async
  (:require [clojure.core.async :as a]))

;; Channels are called ::<compoenent-name>/in-chan and ::<component-name>/out-chan
;; Each component should have a xf and rf that can be used separately
;; from the core.async stuff so that we can test.
;;
;; Channels are wired together using mults and pubs in assembler
;; namespace

(defn full? [c]
  (.full? (.buf c)))

(defn msgs-on-chan [c]
  (.count (.buf c)))

;; Thank you Dan Compton https://danielcompton.net/2015/08/04/core-async-channel-size
(defn chan-status [chan-map chan-keys]
  (into {}
        (map (fn [[k c]]
               [k {:full? (full? c)
                   :backlog (msgs-on-chan c)}]))
        (select-keys chan-map chan-keys)))

(defn collect-results [chan-map chan-keys]
  (into {}
        (map (fn [k] [k
                      (a/<!! (chan-map k))]))
        chan-keys))

(defn pipe [chan-map src-key dest-key]
  (a/pipe (chan-map src-key) (chan-map dest-key))
  chan-map)

(defn mult [chan-map k src-chan]
  (assoc chan-map k (a/mult (chan-map src-chan))))

(defn tap-mult [chan-map src-mult-key dest-key]
  (a/tap (chan-map src-mult-key) (chan-map dest-key))
  chan-map)

(defn transduce-pipe
  "Create a transduce that puts its result on the passed in out-chan"
  [in-chan out-chan xf rf]
  (a/pipe
   (a/transduce xf rf (rf) in-chan)
   out-chan))


(comment

  ;; From hiredman at https://clojurians.slack.com/archives/C05423W6H/p1603818068116500
  (deftype MeteredChannel [take-meter put-meter ch]
    impl/Channel
    (close! [_]
      (impl/close! ch))
    (closed? [_]
      (impl/closed? ch))
    impl/ReadPort
    (take! [_ fn1]
      (take-meter)
      (impl/take! ch fn1))
    impl/WritePort
    (put! [_ val fn1]
      (put-meter)
      (impl/put! ch val fn1)))

  )
