(ns rid3.scatter-simple
  (:require
   [reagent.core :as reagent]
   [rid3.core :as rid3]
   [rid3.examples-util :as util]
   [goog.object :as gobj]
   ))

(def cursor-key :scatter-simple)

(def height 140)
(def width 260)

(def margin {:top    8
             :left   32
             :right  8
             :bottom 40})

(defn translate [left top]
  (str "translate("
       (or left 0)
       ","
       (or top 0)
       ")"))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dataset

(defn ->mock-dataset []
  (drop 1
        (reductions (fn [m d]
                      (hash-map
                       :label d
                       :value (+ (get m :value)
                                 (rand-int 5))))
                    {}
                    ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])))

(defn prepare-dataset [ratom]
  (-> @ratom
      (get :dataset)
      clj->js))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scales

(defn ->x-scale [ratom]
  (let [{:keys [dataset]} @ratom
        labels            (mapv :label dataset)]
    (-> js/d3
        .scaleBand
        (.rangeRound #js [0 width])
        (.padding 0.1)
        (.domain (clj->js labels)))))

(defn ->y-scale [ratom]
  (let [{:keys [dataset]} @ratom
        values            (mapv :value dataset)
        max-value         (apply max values)]
    (-> js/d3
        .scaleLinear
        (.rangeRound #js [height 0])
        (.domain #js [0 max-value]))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Example

(defn example [app-state]
  (let [viz-ratom (reagent/cursor app-state [cursor-key])]
    (reset! viz-ratom {:dataset (->mock-dataset)})
    (fn [app-state]
      [:div
       [:h4 "Simple Scatter Plot"]
       [util/link-source (name cursor-key)]
       [:button
        {:on-click #(swap! viz-ratom assoc :dataset (->mock-dataset))}
        "Randomize data"]

       [rid3/viz
        {:id    "scatter-simple"
         :ratom viz-ratom
         :svg   {:did-mount
                 (fn [node ratom]
                   (-> node
                       (.attr "width" (+ width
                                         (get margin :left)
                                         (get margin :right)))
                       (.attr "height" (+ height
                                          (get margin :top)
                                          (get margin :bottom)))))}

         :main-container {:did-mount
                          (fn [node ratom]
                            (-> node
                                (.attr "transform" (translate
                                                    (get margin :left)
                                                    (get margin :right)))))}
         :pieces
         [{:kind  :container
           :class "x-axis"
           :did-mount
           (fn [node ratom]
             (let [x-scale (->x-scale ratom)]
               (-> node
                   (.attr "transform" (translate 0 height))
                   (.call (-> (.axisBottom js/d3 x-scale)))
                   (.selectAll "text")
                   (.attr "dx" "-1.7em")
                   (.attr "dy" "-0.1em")
                   (.attr "transform" "rotate(-60)"))))}

          {:kind  :container
           :class "y-axis"
           :did-mount
           (fn [node ratom]
             (let [y-scale (->y-scale ratom)]
               (-> node
                   (.call (-> (.axisLeft js/d3 y-scale)
                              (.ticks 4))))))}

          {:kind            :elem-with-data
           :class           "bars"
           :tag             "circle"
           :prepare-dataset prepare-dataset
           :did-mount
           (fn [node ratom]
             (let [y-scale (->y-scale ratom)
                   x-scale (->x-scale ratom)]
               (-> node
                   (.attr "cx" (fn [d]
                                (let [label (gobj/get d "label")]
                                  (x-scale label))))
                   (.attr "cy" (fn [d]
                                (let [value (gobj/get d "value")]
                                  (y-scale value))))
                   (.attr "r" 4)
                   (.attr "fill" "#3366CC")
                   )))
           }]
         }]
       ])))
