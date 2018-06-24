(ns ui.core
  (:require
    [reagent.core :as reagent :refer [atom]])
  (:import
    [goog.async Debouncer]))

(enable-console-print!)

(defonce state (atom {:left {:highlighted false
                             :on true}
                      :center {:highlighted false
                               :on true}
                      :right {:highlighted false
                              :on true}}))
(def colors {:left :#23A087 :center :#F95C6E :right :#004D63})
(def gray-color :#333)

(def piano (.createInstrument js/Synth "piano"))

(defn debounce [f]
  (let [dbnc (Debouncer. f 200)]
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn panel [panel-key]
  (let [{:keys [highlighted on]} (panel-key @state)
        bg (if highlighted
             :#fff
             (if on
               (panel-key colors)
               gray-color))]
    [:div {:key panel-key
           :style {:transition "all 0.2s linear"
                   :flex "1 0 0"
                   :background bg}}]))

(defn full-size [& content]
  [:div {:style {:display :flex
                 :height :100vh}}
   content])

(defn panels [& content]
  [:div {:key 1
         :style {:flex "1 1 0"
                 :display :flex
                 :flexDirection :row}}
   content])

(defn root-component []
  (full-size
    (panels
      (panel :left)
      (panel :center)
      (panel :right))))

(reagent/render
  [root-component]
  (js/document.getElementById "app-container"))

(def left? #{"a" "s" "z" "`" "Meta" "Control" "Tab" "§" "1" "2" "3" "4"
             "q" "w" "e" "d" "x" "c"})
(def center? #{"b" "g" "h" "v" "f" "r" "5" "6" "7" "8" "t" "y" "u" "j" "n" " " "m"})
(def right? #{"," "k" "l" "i" "9" "0" "-" "=" "Backspace" "[" "]" ";" "'" "\\"
              "." "/" "ArrowLeft" "ArrowDown" "ArrowRight" "ArrowUp" "Enter"})

(defn side-for-key-pressed [k]
  (cond
    (left? k) :left
    (center? k) :center
    (right? k) :right))

(def switch-on-off
  (debounce
    (fn [side]
      (swap! state
             #(update-in % [side :on] not)))))

(defn set-highlight [side status]
  (swap! state
         #(assoc-in % [side :highlighted] status)))

(def sound-octave-for-side
  (let [octave 3]
    {:left ["C" octave]
     :center ["D" octave]
     :right ["E" octave]}))

(def play
  (fn [side]
    (let [[sound octave] (sound-octave-for-side side)]
      (.play piano sound octave 1))))

(defn on-key-down [k]
  (let [side (side-for-key-pressed k)]
    (when-not (get-in @state [side :highlighted])
      (play side))
    (set-highlight side true)))

(defn on-key-up [k]
  (let [side (side-for-key-pressed k)]
    (set-highlight side false)
    #_(switch-on-off side)))

(defn keydown-handler [e]
  (.preventDefault e)
  (.stopPropagation e)
  (on-key-down (.-key e)))

(defn keyup-handler [e]
  (.preventDefault e)
  (.stopPropagation e)
  (on-key-up (.-key e)))

(js/setTimeout
  #(do
     (.addEventListener js/document
                        "keydown"
                        keydown-handler)
     (.addEventListener js/document
                        "keyup"
                        keyup-handler))
  2000)
