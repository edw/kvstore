(ns kvstore.core-test
  (:require [clojure.test :refer :all]
            [kvstore.core :refer :all]))

(deftest create-and-put
  (testing "Basic store creation, putting, and getting."
    (let [s (make-store 0 5000)]
      (store-put! s :foo 42)
      (let [result (store-get s :foo)]
        (store-close! s)
        (is (= 42 result))))))

(defn populated-store [kvs]
  (let [s (make-store 0 5000)]
    (doseq [[k v] kvs]
      (store-put! s k v))
    s))

(defn- now-millis [] (System/currentTimeMillis))

;; Let's not run this all the time...
;; (deftest insert-ten-million-items
;;   (testing "Inserting 1.0e7 items"
;;     (let [s (make-store 0 5000)]
;;       (doseq [i (range 10000000)]
;;         (store-put! s i i))
;;       (store-close! s)
;;       ;; Survival is victory
;;       (is (= true true)))))

(deftest readfest
  (testing "Reading randomly"
    (let [n 100000]
      (print (format "Building store of %d items..." n))
      (let [kvs (map #(vector % (rand-int 100)) (range n))
            s (populated-store kvs)]
        (println "done.")
        (let [then (now-millis)
              j 10000000]
          (print (format "Doing %d reads..." j))
          (doseq [k (map (fn [_] rand-int 10) (range j))]
            (store-get s k))
          (let [elapsed (- (now-millis) then)
                qps (* 1000 (/ j elapsed))
                one-hundred-tx-millis (+ (* 95 1) (* 4 5) (* 1 10))
                tx-per-ms (/ 100 one-hundred-tx-millis)
                goal-qps (* 1000 tx-per-ms)]
            (println (format "...in %.2f s (%.2f qps, %.5f ms/key mean)"
                             (/ elapsed 1000.0)
                             (float qps)
                             (float (/ elapsed j))))
            (is (> qps (/ j 100)))))))))

(deftest expiring
  (testing "Expiring key"
    (let [s (make-store 2000 1000)]
      (store-put! s :foo 42)
      (is (= (store-get s :foo) 42))
      (Thread/sleep 5000)
      (is (= (store-get s :foo) nil)))))
