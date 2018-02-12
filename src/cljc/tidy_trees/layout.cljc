(ns tidy-trees.layout
  (:require [clojure.zip :as zip :refer [edit root]]
            [tidy-trees.zip :refer [ff rw edit-walk reduce-walk node-seq]]
            [tidy-trees.contour :refer [top bound-tree find-overlap nudge min-contour max-contour]]
            [tidy-trees.util :refer [rand-keyword]]))

(defrecord LayoutNode
  [id label x y width height min-c max-c children delta shift])

(def branch? (comp seq :children))
(defn mk-node [node child-seq] (assoc node :children child-seq))

(defn zipper
  [tidy]
  (zip/zipper branch? :children mk-node tidy))

(defn convert
  [tree {:keys [branch? children label-branch label-term]}]
  (map->LayoutNode
    {:id       (rand-keyword)
     :label    (if (branch? tree) (label-branch tree) (label-term tree))
     :children (when (branch? tree) (children tree))}))

(defn convert-tree
  [tree opts]
  (-> (convert tree opts)
      (zipper)
      (edit-walk ff update :children (partial map #(convert % opts)))
      (root)))

(defn right-of
  "Places node gap units to the right of prev-node at their closest points."
  [gap node prev-node]
  (let [lc      (:min-c node)
        prev-rc (:max-c prev-node)
        overlap (find-overlap prev-rc lc)
        delta   (+ overlap gap)]
    (-> node
        (update :delta + delta)
        (update :min-c nudge delta)
        (update :max-c #(max-contour (nudge % delta) prev-rc)))))

(defn spread-trees
  "Spaces a sequence of subtrees so that each subtree is gap units from
  its left and right sibling at their closest points."
  [children {:keys [h-gap]}]
  (reduce (fn [row child]
            (if (seq row)
              (conj row (right-of h-gap child (last row)))
              (conj row child)))
          []
          children));

(defn layout-branch
  "Space the subtrees of node so that they don't overlap and center the node
  between its first and last subtree."
  [{:keys [children width] :as node} args]
  (let [children      (spread-trees children args)
        [min-c max-c] (bound-tree width
                                  (apply min-contour (map :min-c children))
                                  (:max-c (last children)))
        min-x         (top min-c)]
    (assoc node
      :delta    min-x
      :shift    min-x
      :min-c    min-c
      :max-c    max-c
      :children children)))

(defn layout-node
  "Perform the layout algorithm on the node and return a node which
  satisfies our drawing requirements."
  [node opts]
  (if (branch? node)
    (layout-branch node opts)
    (assoc node
      :min-c [0]
      :max-c [(:width node)])))

(defn add-dimensions
  "Add the node dimensions as attributes on the node itself."
  [node measures]
  (let [[width height] (get measures (:id node))]
    (assoc node
      :width  width
      :height height)))

(defn place-node
  "Assign x,y coordinates to the node and propage 'delta' and 'shift' variables
  to its children."
  [{:keys [id delta shift] :as node} slots depths]
  (-> node
      (assoc :x delta)
      (assoc :y (slots (get depths id)))
      (update :children
              (fn [children]
                (map #(update % :delta + delta (- shift))
                     children)))))

(defn ensure-bounds
  "Ensures that the leftmost node of the tree has an x coordinate of zero."
  [{:keys [min-c] :as tree-root}]
  (update tree-root :delta - (apply min min-c)))

(defn h-and-d
  [[heights depths] depth {:keys [id height]}]
  [(update heights depth max height) (assoc depths id depth)])

(defn compute-slots
  [heights {:keys [v-gap]}]
  (loop [top 0
         ss  []
         hs  heights]
    (if (seq hs)
      (recur (+ top v-gap (first hs))
             (conj ss top)
             (rest hs))
      (conj ss top))))

(defn tree->nodes [tree] (node-seq (zipper tree)))

;; ---- public api ----

(defrecord Drawing [tree nodes opts measures width height edges])

(defn new-drawing
  [tree opts]
  (let [tree (convert-tree tree opts)]
    (Drawing. tree
              (tree->nodes tree)
              opts
              {}
              0
              0
              nil)))

(defn record-dimensions
  [drawing id dimensions]
  (update drawing :measures assoc id dimensions))

(defn ready-for-layout?
  [{:keys [nodes measures]}]
  (= (count nodes) (count measures)))

(defn layout-drawing
  [{:keys [tree opts measures] :as drawing}]
  (let [tail             (edit-walk (zipper tree)
                                    ff
                                    add-dimensions
                                    measures)
        [heights depths] (reduce-walk tail [(sorted-map) {}] rw h-and-d)
        slots            (compute-slots (vals heights) opts)
        tree-prime       (-> tail
                             (edit-walk rw layout-node opts)
                             (edit ensure-bounds)
                             (edit-walk ff place-node slots depths)
                             (root))
        nodes            (tree->nodes tree-prime)]
    (assoc drawing
      :tree   tree-prime
      :nodes  nodes
      :width  (- (apply max (:max-c tree-prime))
                 (apply min (:min-c tree-prime)))
      :height (last slots))))