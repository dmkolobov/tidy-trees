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
             :refer [floor rand-keyword]]))

;; ---- events ------------------------------------------------

(reg-event-db
  ::init
  (fn [db [_ did tree opts]]
    (assoc db did (new-drawing tree opts))))

(reg-event-fx
  ::measure
  (fn [{:keys [db]} [_ did node-id dimensions]]
    (let [drawing-prime (record-dimensions (get db did) node-id dimensions)
          db-prime      (assoc db did drawing-prime)]
      (if (ready-for-layout? drawing-prime)
        {:db db-prime :dispatch [::layout did]}
        {:db db-prime}))))

(reg-event-db
  ::layout
  (fn [db [_ did]] (update db did layout-drawing)))

;; ---- subscriptions ----------------------------------------------

(reg-sub ::drawing (fn [db [_ did]] (get db did)))

;; --- rendering -----------------------------------------------------

(defn draw-diagonal
  [e]
  (let [[[x1 y1] [x2 y2]] e]
    [:line {:class-name "tidy-tree-edge"
            :x1 x1 :y1 y1
            :x2 x2 :y2 y2}]))

(defn diagonal-edges
  [node-edges]
  [:g
   (map (fn [edge] ^{:key edge} [draw-diagonal edge])
        node-edges)])

(defn straight-edges
  [node-edges]
  (let [[[x y] _] (first node-edges)
        min-x     (apply min (map (comp first last) node-edges))
        max-x     (apply max (map (comp first last) node-edges))
        min-y     (apply min (map (comp last last) node-edges))
        my        (+ y (/ (- min-y y) 2))]
    [:g
     [draw-diagonal [[x y]      [x my]]]
     [draw-diagonal [[min-x my] [max-x my]]]
     (for [[_ [x :as child]] node-edges]
       ^{:key child}
       [draw-diagonal [[x my] child]])]))

(defn draw-edges
  [_ {:keys [edges width height opts]}]
  [:svg {:style {:position "absolute"}
         :width    width
         :height   height
         :view-box (str "0 0 "width" "height)}
   (doall
     (for [[id node-edges] edges]
       ^{:key id}
       [(condp = (:edges opts)
          :straight straight-edges
          :diagonal diagonal-edges
          straight-edges) node-edges]))])

(defn draw-node
  [did {:keys [id]}]
  (reagent/create-class
    {:component-did-mount
     (fn [owner]
       (let [dom  (reagent/dom-node owner)
             rect (.getBoundingClientRect dom)]
         (dispatch [::measure did id [(floor (.-width rect)) (floor (.-height rect))]])))
     :reagent-render
     (fn [_ {:keys [label x y] :or {x 0 y 0}}]
       [:div {:class-name "tidy-tree-node"
              :style {:display   "inline-block"
                      :position  "absolute"
                      :transform (str "translate("x"px,"y"px)")}}
        label])}))

(defn draw-nodes
  [did {:keys [nodes]}]
  [:div {:style {:position "absolute"}}
   (doall
     (for [{:keys [id] :as node} nodes]
       ^{:key id} [draw-node did node]))])

(defn draw-tree
  [did
   {:keys [width height]
    :as drawing}]
  [:div {:class-name "tidy-tree"
         :style {:position "relative"
                 :overflow "hidden"
                 :width     width
                 :height    height}}
   [draw-edges did drawing]
   [draw-nodes did drawing]])

(defn tidy-tree
  [tree opts]
  (let [did     (rand-keyword)
        drawing (subscribe [::drawing did])]
    (dispatch [::init did tree opts])
    (reagent/create-class
      {:component-will-update
       (fn [this new-argv]
         (when-not (= new-argv (reagent/argv this))
           (dispatch (into [::init did] (rest new-argv)))))
       :reagent-render
       (fn [_ _]
         (when-let [dw @drawing] [draw-tree did dw]))})))