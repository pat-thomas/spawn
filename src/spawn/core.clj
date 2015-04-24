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
  [stream len]
  (for [_ (range len)]
    (<!! stream)))

(comment
  (defstream adder inc 1)
  (consume-stream-sync (make-stream inc 10) 10)
  (let [adder (make-stream inc 1)]
    (<!! (compose-stream #(* % 3) adder)))
  )
