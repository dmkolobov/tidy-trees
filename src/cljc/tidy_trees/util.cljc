(ns tidy-trees.util)

(defn rand-keyword
  []
  (keyword
    (str
      #?(:cljs (random-uuid)
         :clj  (java.util.UUID/randomUUID)))))
