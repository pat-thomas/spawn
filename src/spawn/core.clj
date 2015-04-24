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
  (count (consume-stream adder 10))
  )
