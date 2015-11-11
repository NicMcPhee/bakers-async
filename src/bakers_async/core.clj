(ns bakers-async.core
  (:require [clojure.core.async :as async :refer :all
             :exclude [map into reduce merge partition partition-by take]])
  (:gen-class))

(defrecord Customer
  [id n fib-of-n])

(defn set-fib-value
  [customer value]
  (reset! (:fib-of-n customer) value))

(defn add-single-customer-to-channel [customer-id customers-channel]
  (Thread/sleep (rand-int 5000))
  (let [customer (->Customer customer-id (rand-int 30) (atom nil))]
    (add-watch (:fib-of-n customer) customer-id
               (fn [key atom old-value new-value]
                 (println (str "Customer " key " changed from " old-value " to " new-value "."))))
    (go
      (>! customers-channel customer))))

(defn add-customers-to-channel
  [num-customers customers-channel]
  (doseq [customer-id (range num-customers)]
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
