;;; `kvstore` is a key-value store offering thread safety, and
;;; constant-time access in a addition to an expiration facility.
;;;
;;; Read performance is equivalent to performing two standard Clojure
;;; hash map access operations plus an atom dereference. Writes use
;;; Clojure's concurrency primitives and may be relatively expensive
;;; when used in a highly concurrent environment, but writes will not
;;; affect read performance.
;;;
;;; If you are storing a large number of keys or large values, you may
;;; need to adjust your JVM runtime options.
;;;
;;; According to the included unit tests, this library offers an
;;; average key access time of 0.0005ms on a high-end 2019 MacBook
;;; Air, averaging approximately 1.8e6 QPS on 10 million reads of a
;;; 1.0e6-element store using intergers as keys and values.
;;;
;;; Memory utilitzation is similar to using a standard hash map
;;; assuming you do not use key expiration. This store implements key
;;; expiration using a priority-map of keys to long ints for any key
;;; that is scheduled to expire.

(ns kvstore.core
  (:require [clojure.data.priority-map :refer [priority-map]]
            [clojure.core.async :as async :refer [chan timeout go alts! >!!]]))

;;; ## Implementation details

(defn- now-millis [] (System/currentTimeMillis))
(defn- wall-expiration [delta-millis] (+ (now-millis) delta-millis))

(defn- remove-expired
  "Grind through the `expirations` priority map and dissociate
  entries from it and the store data until no unexpired expirations
  remain. Return the updated store."
  [{:keys [data expirations] :as store}]
  (let [wall-time-millis (now-millis)]
    (loop [[[k wall-expiration-millis] & exps :as all-exps] expirations
           data data]
      (cond (not k)
            (assoc store :data data :expirations (priority-map))
            (>= wall-time-millis wall-expiration-millis)
            (recur exps (dissoc data k))
            :else
            (assoc store :data data :expirations all-exps)))))

(defn- handle-expired
  "Atomically remove expired keys from `store`."
  [store]
  (swap! store remove-expired))

;;; ## Public interface

(defn make-store
  "Return a new store having a default expiration of
  `default-expiration-millis` milliseconds and an expired key
  cleanup interval of `expiration-interval-millis`. An
  expiration value of `nil`, `false`, or zero or less means
  keys will not expire. Stores created with this function kick
  off a go routine; `store-close!` should be evaluated on a
  returned store before it is GCd to avoid a resource leak."
  [default-expiration-millis expiration-interval-millis]
  (let [closed? (atom false)
        close-ch (chan)
        store (atom {:data {} :expirations (priority-map)
                     :default-expiration-millis default-expiration-millis
                     :close-ch close-ch :closed? closed?})
        expire (timeout expiration-interval-millis)]
    (go (while (not @closed?)
          (let [expire-now (timeout expiration-interval-millis)
                [v ch] (alts! [close-ch expire-now])]
            (cond (= ch expire-now)
                  (handle-expired store)
                  (= ch close-ch)
                  (swap! closed? (fn [_] true))))))
    store))

(defn store-get
  "Return the value of key `k` in `store`. Returns `nil` if
  `k` does not exist or has been purged due to expiration."
  [store k]
  (get-in @store [:data k]))

(defn store-put!
  "Associate the value `v` with key `k` in `store`. Blocks
  until the value is successfully associated. If
  `expiration-millis` is provided the value associated with the
  key will be purged at the current wall time plus the specified
  number of milliseconds or later, overriding any existing
  key expiration value."
  ([store k v] (store-put! store k v (@store :default-expiration-millis)))
  ([store k v expiration-millis]
   (swap!
    store
    (fn [{:keys [data expirations] :as s}]
      (let [data (assoc data k v)
            s (assoc s :data data)]
        (if (and expiration-millis (> expiration-millis 0))
          (assoc s :expirations
                 (assoc expirations k (wall-expiration expiration-millis)))
          s))))))

(defn store-delete!
  "Purge the value associated with key `k` in `store` if one
   exists. Blocks until the value has been deleted."
  [store k]
  (swap! store
         (fn [{:keys [data expirations] :as s}]
           (assoc s
                  :data (dissoc data k)
                  :expirations
                  (dissoc expirations k)))))

(defn store-close!
  "Releases resources associated with `store`, preventing
  resource leaks."
  [store]
  (>!! (@store :close-ch) :close-store))
