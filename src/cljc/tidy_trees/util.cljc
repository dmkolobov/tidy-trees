(ns tidy-trees.util)

(defn rand-keyword
  []
  (keyword
    (str
      #?(:cljs (random-uuid)
         :clj  (java.util.UUID/randomUUID)))))

(defn floor
  [x]
  #?(:cljs (.floor js/Math x)
     :clj  (.floor Math x)))

(defn ceil
  [x]
  #?(:cljs (.ceil js/Math x)
     :clj  (.ceil Math x)))