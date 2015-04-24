(ns spawn.core
  (:require [clojure.core.async :as a :refer [chan go go-loop <! >! <!! >!!]]))

(defn make-stream
  [handler-fn initial-state]
  (let [handler-fn (if (symbol? handler-fn)
                     (ns-resolve *ns* handler-fn)
                     handler-fn)
        ch         (chan 1)]
    (go-loop [state initial-state]
      (>! ch state)
      (recur (handler-fn state)))
    ch))

(defn compose-stream
  [handler-fn input-stream]
  (let [handler-fn (if (symbol? handler-fn)
                     (ns-resolve *ns* handler-fn)
                     handler-fn)
        ch         (chan 1)]
    (go
      (while true
        (>! ch (handler-fn (<! input-stream)))))
    ch))

(defmacro defstream
  "Returns a channel that is infinitely populated."
  [stream-name handler-fn initial-state]
  `(def ~stream-name ~(make-stream handler-fn initial-state)))

(defn consume-stream-sync
  ([stream]
   (for [_ (range)]
     (<!! stream)))
  ([stream len]
   (for [_ (range len)]
     (<!! stream))))

(defn consume-stream-async
  [stream len handler-fn]
  (dotimes [_ len]
    (go
      (handler-fn (<! stream))))
  :done)

(comment

  (->> (make-stream (partial * 2) 1)
       consume-stream-sync
       (take 20))
  
  (-> (make-stream (partial * 2) 10)
      (consume-stream-sync 10))

  
  (with-out-str
    (time
     (consume-stream-sync (make-stream inc 10) 10)))
  (<!! (make-stream inc 1))
  (consume-stream-async (make-stream inc 1) 100 println)
  (defstream adder inc 1)
  (consume-stream-sync (make-stream inc 10) 10)
  (<!! (make-stream inc 1))
  (->> 1
       (make-stream inc)
       (compose-stream (partial * 3))
       (compose-stream #(- % 2))
       <!!)
  
  )
