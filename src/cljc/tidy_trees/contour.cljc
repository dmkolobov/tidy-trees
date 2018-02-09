(ns tidy-trees.contour
  "Implement contours as defined in ___'s paper 'Tidier Drawings of Trees'")

(defn part-contours
  [c1 c2]
  (let [n1 (count c1)
        n2 (count c2)
        k  (min n1 n2)
        i1 (- n1 k)
        i2 (- n2 k)]
    [(cond (< n1 n2) (subvec c2 0 i2)
           (> n1 n2) (subvec c1 0 i1)
           :default  [])
     [(subvec c1 i1)
      (subvec c2 i2)]]))

(defn find-overlap
  "Given the maximal controur lc of the left subtree and the minimal contour
  rc of the right subtree, return the number of units of distance that the
  two trees overlap."
  [rc lc]
  (or (->> (zipmap (rseq rc) (rseq lc))
           (map (partial apply -))
           (apply max))
        0))

(defn nudge
  "Moves the contour c to the right by delta units and returns the resulting
  contour."
  [c delta]
  (vec (map (partial + delta) c)))

(defn- find-contour
  "Helper used to define min-contour and max-contour."
  [f & cs]
  (reduce (fn [cprime c]
            (let [[cp [c1 c2]] (part-contours cprime c)]
              (into cp (map f c1 c2))))
          (first cs)
          (rest cs)))

(def min-contour
  "Given the sequence of min-contours cs, return the minimum contour which bounds
  every contour in cs."
  (partial find-contour min))

(def max-contour
  "Given the sequence of min-contours cs, return the maximum contour which bounds
  every contour in cs."
  (partial find-contour max))

(defn bound-tree
  "Given a number w, a minimal contour min-c, and a maximal contour max-c, return
  a new contour which bounds a tree whose root is w units wide and centered over
  the volume bounded by min-c and max-c."
  [w min-c max-c]
  (let [l        (peek min-c)
        r        (peek max-c)
        cx       (+ l (/ (- r l) 2))
        hw       (/ w 2)
        min-x    (- cx hw)
        max-x    (+ cx hw)]
    [(conj min-c min-x)
     (conj max-c max-x)]))

(def top
  "Return the topmost bound of the given contour."
  peek)