(ns tidy-trees.zip
  "From Clojure docs:

    '...with zippers you can write code that looks like an imperative,
     destructive walk through a tree, call root when you are done'

   That's great! Lets hide the imperative, destructive, recursive details of
   the walks in this namespace."
  (:require [clojure.zip :refer [node path edit up next prev end?]]))

(defn ff
  "Defines a depth first walk starting from the root and ending at the tail."
  [loc]
  (let [nloc (next loc)] (when-not (end? nloc) nloc)))

(defn rw
  "Defines a reverse depth-first walk from the root to the tail of the tree, meaning
  we start at the tail and end up at the root."
  [loc]
  (if-let [prev (prev loc)] prev (up loc)))

(defn node-seq
  "Given a zipper location, return a sequence of its nodes in depth-first order."
  [loc]
  (loop [nodes []
         loc   loc]
    (if (end? loc)
      nodes
      (recur (conj nodes (node loc))
             (next loc)))))

(defn walk
  [loc step-fn walk-fn]
  (let [loc-prime (walk-fn loc)]
    (if-let [next-loc (step-fn loc-prime)]
      (recur next-loc step-fn walk-fn)
      loc-prime)))

(defn reduce-walk
  "Starting at loc, walk the tree in the direction defined by step-fn, reducing
  over value v by applying compute-fn with the current value, current node depth, and
  the current node at each point in the walk.

  Returns the value."
  [loc v step-fn compute-fn]
  (let [vprime (compute-fn v
                           (count (path loc))
                           (node loc))]
    (if-let [next-loc (step-fn loc)]
      (recur next-loc vprime step-fn compute-fn)
      vprime)))

(defn edit-walk
  "Starting at loc, walk the tree in the direction defined by step-fn, editing
  each location along the way.

  Returns walk end location, which is either the root or tail of the tree."
  [loc step-fn edit-fn & args]
  (walk loc step-fn #(apply edit % edit-fn args)))
