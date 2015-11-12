(ns bakers-async.core
  (:require [clojure.core.async :as async :refer :all
             :exclude [map into reduce merge partition partition-by take]])
  (:gen-class))

(defn fib [n]
  (if (< n 2)
    n
    (+ (fib (- n 1)) (fib (- n 2)))))

(defrecord Customer
  [id n fib-of-n server])

(defn make-customer [id]
  (let [n (+ 30 (rand-int 10))
        customer (->Customer id n (atom nil) (atom nil))]
    (add-watch (:fib-of-n customer) customer-id
               (fn [key atom old-value new-value]
                 (println (str "Customer " key " changed from " old-value " to " new-value "."))))
    customer))

(defn set-fib-value
  [customer value]
  (reset! (:fib-of-n customer) value))

(defn add-single-customer-to-channel [customer-id customers-channel]
  (Thread/sleep (rand-int 1000))
  (let [customer (make-customer customer-id)]
    (println (str "Constructured customer: " customer))
    (>!! customers-channel customer)))

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

(defrecord Server
  [id customers-served])

(defn make-server [id]
  (let [server (->Server id (atom []))]
    (add-watch (:customers-served server) id
               (fn [key atom old-value new-value]
                 (println (str "Server " key " has now served " new-value "."))))
    server))

(defn add-served-customer [server customer-id]
  (set! (:customers-served server) conj customer-id))

(defn make-servers [num-servers]
  (let [servers-channel (chan)]
    (doseq [id (range num-servers)]
      (>!! servers-channel (make-server id)))))

(defn map-example []
  (let [size 1000
        ca (chan)
        cb (chan)]
    (onto-chan ca (range size))
    (onto-chan cb (range size))
    (let [cm (async/map #(rem (+ %1 %2) 40) [ca cb])
          results (chan)]
      (loop []
        (when-let [n (<!! cm)]
          (go
           (println "About to insert")
           (>! results (fib n)))
          (recur)))
      results)))

;;        (doseq [_ (range size)
;;                :let [n (<! cm)]]
;;          (println n))))))
;;     (go
;;      (loop []
;;        (let [n (<! cm)]
;;          (if n
;;            (do
;;              (println n)
;;              (recur)))))))))



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

; I'm not sure I need the thread thing. The following
; pegs all the cores quite nicely, which suggests that
; go blocks provide parallelism.

;; (def hi-chan (chan))
;; (doseq [n (range 1000)]
;;   (go (>! hi-chan (str "hi " (fib (rem n 40))))))
;; ; Then wait a while for everything to compute
;; (close! hi-chan)
;; (<!! (async/into [] hi-chan))
