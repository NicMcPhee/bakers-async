(ns bakers-async.core
  (:require [clojure.core.async :as async :refer :all
             :exclude [map into reduce merge partition partition-by take]])
  (:gen-class))

(defn fib [n]
  (if (< n 2)
    n
    (+ (fib (- n 1)) (fib (- n 2)))))

(defrecord Customer
  [id n fib-of-n])

(defn set-fib-value
  [customer value]
  (reset! (:fib-of-n customer) value))

(defn add-single-customer-to-channel [customer-id customers-channel]
  ; (Thread/sleep (rand-int 1000))
  (let [n (+ 30 (rand-int 10))
        customer (->Customer customer-id n (atom (fib n)))]
    (println (str "Constructured customer: " customer))
    (add-watch (:fib-of-n customer) customer-id
               (fn [key atom old-value new-value]
                 (println (str "Customer " key " changed from " old-value " to " new-value "."))))
    (go
      (>! customers-channel customer))))

(defn add-customers-to-channel
  [num-customers customers-channel]
  (doseq [customer-id (range num-customers)]
    (println (str "Adding customer " customer-id))
    (add-single-customer-to-channel customer-id customers-channel))
  (close! customers-channel))

(defn make-customers [num-customers]
  (let [customers-channel (chan)]
    (future (add-customers-to-channel num-customers customers-channel))
    customers-channel))



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

; I'm not sure I need the thread thing. The following
; pegs all the cores quite nicely, which suggests that
; go blocks provide parallelism.

(def hi-chan (chan))
(doseq [n (range 1000)]
  (go (>! hi-chan (str "hi " (fib (rem n 40))))))
; Then wait a while for everything to compute
(close! hi-chan)
(<!! (async/into [] hi-chan))
