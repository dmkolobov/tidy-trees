(ns tidy-trees.reagent
  (:require [reagent.core
             :as    reagent
             :refer [atom]]

            [re-frame.core
             :refer [subscribe
                     dispatch
                     reg-sub
                     reg-event-db
                     reg-event-fx]]

            [tidy-trees.layout
             :refer [new-drawing
                     record-dimensions
                     ready-for-layout?
                     layout-drawing]]

            [tidy-trees.util
             :refer [rand-keyword]]

            [tidy-trees.ui-util :as ui]))

;; ---- events ------------------------------------------------

(reg-event-db
  ::init
  (fn [db [_ did tree opts]]
    (assoc-in db [:nursery did] (new-drawing tree opts))))

(reg-event-fx
  ::measure
  (fn [{:keys [db]} [_ did node-id dimensions]]
    (let [drawing-prime (record-dimensions (get-in db [:nursery did]) node-id dimensions)
          db-prime      (assoc-in db [:nursery did] drawing-prime)]
      (if (ready-for-layout? drawing-prime)
        {:db db-prime :dispatch [::layout did]}
        {:db db-prime}))))

(reg-event-db
  ::layout
  (fn [db [_ did]] (assoc db did (layout-drawing (get-in db [:nursery did])))))

;; ---- subscriptions ----------------------------------------------

(reg-sub ::drawing (fn [db [_ did]] (get db did)))
(reg-sub ::nursery (fn [db [_ did]] (get-in db [:nursery did])))

;; --- rendering -----------------------------------------------------

(defn child-anchor
  [{:keys [x y width height]}]
  [(+ x (/ width 2)) (+ y height)])

(defn parent-anchor
  [{:keys [x y width]}]
  [(+ x (/ width 2)) y])

(defn line
  [x1 y1 x2 y2]
  [:line {:class-name "tidy-tree-edge"
          :x1 (.round js/Math x1)
          :y1 (.round js/Math y1)
          :x2 (.round js/Math x2)
          :y2 (.round js/Math y2)}])

(defn diagonal-edges
  [[x y] anchors]
  [:g
   (map (fn [[cx cy]] ^{:key [cx cy]} [line x y cx cy]) anchors)])

(defn straight-edges
  [[x y] anchors]
  (let [min-x     (apply min (map first anchors))
        max-x     (apply max (map first anchors))
        min-y     (apply min (map last anchors))
        my        (+ y (/ (- min-y y) 2))]
    [:g
     [line x y x my]
     [line min-x my max-x my]
     (for [[cx cy] anchors] ^{:key [cx cy]} [line cx my cx cy])]))

(defn is-placed?
  [t]
  (and t
       (number? (:x t))
       (number? (:y t))))

(defn draw-edges
  [_ {:keys [nodes width height opts]}]
  [:svg {:style {:position "absolute"}
         :width    width
         :height   height
         :view-box (str "0 0 "width" "height)}
   (doall
     (for [{:keys [children id] :as node} nodes]
       (when (and (is-placed? node) (seq children))
         ^{:key id}
         [(condp = (:edge-style opts)
            :straight straight-edges
            :diagonal diagonal-edges
            straight-edges)
          (child-anchor node)
          (->> children
               (filter is-placed?)
               (map parent-anchor))])))])

(defn draw-node
  [did {:keys [id]}]
  (ui/measured-class
    {:on-mount #(dispatch [::measure did id %])
     :reagent-render
      (fn [_ {:keys [label x y] :or {x 0 y 0}}]
        [:div {:class-name "tidy-tree-node"
               :style {:position  "absolute"
                       :transform (ui/translate x y)}}
         label])}))

(defn draw-nodes
  [did {:keys [nodes]}]
  [:div {:style {:position "absolute"}}
   (doall
     (for [{:keys [id] :as node} nodes]
       ^{:key id} [draw-node did node]))])

(defn draw-tree
  [did
   {:keys [width height] :as drawing}]
  [:div {:style {:position   "relative"
                 :overflow-y "hidden"
                 :overflow-x "visible"
                 :width      (+ 2 (.ceil js/Math width))
                 :height     (+ 2 (.ceil js/Math height))}}
   [draw-edges did drawing]
   [draw-nodes did drawing]])

(defn tidy-tree
  [tree opts]
  (let [did     (rand-keyword)
        drawing (subscribe [::drawing did])
        nursery (subscribe [::nursery did])
        init!   #(dispatch (into [::init did] %))]

    (dispatch [::init did tree opts])

    (ui/diffed-class
      {:on-diff init!
       :reagent-render
       (fn [_ _]
         [:div.tidy-tree
          (when-let [dw @drawing] [draw-tree did dw])
          (when-let [new-dw @nursery] [:div {:style {:position "absolute"
                                                     :width    0
                                                     :height   0}}
                                       [draw-tree did new-dw]])])})))