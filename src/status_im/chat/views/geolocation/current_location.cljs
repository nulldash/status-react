(ns status-im.chat.views.geolocation.current-location
  (:require-macros [status-im.utils.views :refer [defview]]
                   [reagent.ratom :refer [reaction]])
  (:require [status-im.components.react :refer [view image text touchable-highlight]]
            [reagent.core :as r]
            [status-im.utils.utils :refer [http-get]]
            [status-im.utils.types :refer [json->clj]]
            [status-im.components.mapbox :refer [mapview]]
            [re-frame.core :refer [dispatch subscribe]]
            [status-im.components.react :as components]))

(def mapbox-api "https://api.mapbox.com/geocoding/v5/mapbox.places/")
(def access-token "pk.eyJ1Ijoic3RhdHVzaW0iLCJhIjoiY2oydmtnZjRrMDA3czMzcW9kemR4N2lxayJ9.Rz8L6xdHBjfO8cR3CDf3Cw")

(defn get-places [geolocation cur-loc-geocoded & [poi?]]
  (let [{:keys [latitude longitude]} (:coords geolocation)]
    (when (and latitude longitude)
      (http-get (str mapbox-api longitude "," latitude
                     ".json?" (when poi? "types=poi&") "access_token=" access-token)
                #(reset! cur-loc-geocoded (json->clj %))
                #(reset! cur-loc-geocoded nil))
      true)))

(defn place-item [title address]
  [touchable-highlight {:on-press #(do
                                     (dispatch [:set-command-argument [0 (or address title) false]])
                                     (dispatch [:send-seq-argument]))}
   [view {:height (if address 74 52)
          :justify-content :center}
    [view {:flex-direction :row
           :align-items :center}
     [view {:border-color "#628fe3"
            :border-width 3
            :border-radius 7
            :height 13
            :width 13}]
     [text {:style {:font-size 15
                    :padding-left 9
                    :padding-right 16
                    :color "#000000"
                    :line-height 23}
            :number-of-lines 1
            :font :medium}
      title]]
    (when address
      [text {:number-of-lines 1
             :style {:font-size 15
                     :padding-left 22
                     :padding-right 16
                     :color "#000000"
                     :line-height 23}}
       address])]])

(defview current-location-map-view []
  [geolocation [:get :geolocation]
   command     [:selected-chat-command]]
  {:component-will-mount #(dispatch [:request-geolocation-update])}
  [view
   (if geolocation
     [view
      [touchable-highlight {:on-press #(do
                                         (dispatch [:set-command-argument [0 "Dropped pin" false]])
                                         (dispatch [:set-chat-seq-arg-input-text "Dropped pin"])
                                         (dispatch [:load-chat-parameter-box (:command command)]))}
       [view
        [mapview {:initial-center-coordinate (select-keys (:coords geolocation) [:latitude :longitude])
                  :showsUserLocation true
                  :initialZoomLevel 10
                  :style {:height 100}}]]]]
     [view {:align-items :center
            :justify-content :center
            :height 100}
      [components/activity-indicator {:animating true}]])])

(defn current-location-view []
  (let [geolocation      (subscribe [:get :geolocation])
        cur-loc-geocoded (r/atom nil)
        result (reaction (when @geolocation (get-places @geolocation cur-loc-geocoded)))]
      (r/create-class
        {:component-will-mount #(dispatch [:request-geolocation-update])
         :render
           (fn []
             (let [_ @result]
               (dispatch [:set-in [:debug :cur-loc-geocoded] @cur-loc-geocoded])
               (when (and @cur-loc-geocoded (> (count (:features @cur-loc-geocoded)) 0))
                 [view {:margin-top        11
                        :margin-horizontal 16}
                  [text {:style {:font-size 14
                                 :color "#939ba1"
                                 :letter-spacing -0.2}}
                   "Your current location"]
                  (let [{:keys [place_name] :as feature} (get-in @cur-loc-geocoded [:features 0])]
                    [place-item (:text feature) place_name])])))})))

(defn separator []
  [view {:height 1 :margin-left 22 :opacity 0.5 :background-color "#c1c7cbb7"}])

(defn places-nearby-view []
  (let [geolocation      (subscribe [:get :geolocation])
        cur-loc-geocoded (r/atom nil)
        result (reaction (when @geolocation (get-places @geolocation cur-loc-geocoded true)))]
    (r/create-class
      {:component-will-mount #(dispatch [:request-geolocation-update])
       :render
         (fn []
           (let [_ @result]
             (when (and @cur-loc-geocoded (> (count (:features @cur-loc-geocoded)) 0))
               [view {:margin-top        11
                      :margin-horizontal 16}
                [text {:style {:font-size 14
                               :color "#939ba1"
                               :letter-spacing -0.2}}
                 "Places nearby"]
                (doall
                  (map (fn [{:keys [text place_name] :as feature}]
                         ^{:key feature}
                         [view
                          [place-item text place_name]
                          (when (not= feature (last (:features @cur-loc-geocoded)))
                            [separator])])
                       (:features @cur-loc-geocoded)))])))})))

(defn places-search []
  (let [seq-arg-input-text (subscribe [:chat :seq-argument-input-text])
        places             (r/atom nil)
        result             (reaction (http-get (str mapbox-api @seq-arg-input-text
                                                    ".json?access_token=" access-token)
                                               #(reset! places (json->clj %))
                                               #(reset! places nil)))]
    (fn []
      (dispatch [:set-in [:debug :places-search] @places])
      (let [_ @result]
        (when @places
          (let [features-count (count (:features @places))]
            [view {:margin-top        12
                   :margin-horizontal 16}
             [text {:style {:font-size 14
                            :color "#939ba1"
                            :letter-spacing -0.2}}
              "Search results " features-count]
             (doall
               (map (fn [{:keys [place_name] :as feature}]
                      ^{:key feature}
                      [view
                       [place-item place_name nil]
                       (when (not= feature (last (:features @places)))
                         [separator])])
                    (:features @places)))]))))))

