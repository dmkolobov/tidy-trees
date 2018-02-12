(ns tidy-trees.ui-util
  "Defines some higher-order components and utility functions for
  working with css values."
  (:require [reagent.core :refer [create-class dom-node argv]]))

(defn measured-class
  [{:keys [reagent-render on-mount]}]
  (create-class
    {:component-did-mount
     (fn [owner]
       (let [dom  (dom-node owner)
             rect (.getBoundingClientRect dom)]
         (on-mount [(.floor js/Math (.-width rect))
                    (.floor js/Math (.-height rect))])))
     :reagent-render reagent-render}))

(defn diffed-class
  [{:keys [reagent-render on-diff]}]
  (create-class
    {:component-will-update #(when-not (= (argv %1) %2) (on-diff (rest %2)))
     :reagent-render        reagent-render}))

(defn translate
  [x y]
  (str "translate("(.round js/Math x)"px,"(.round js/Math y)"px)"))