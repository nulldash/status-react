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

(defn get-places [geolocation cur-loc-geocoded]
  (let [{:keys [latitude longitude]} (:coords geolocation)]
    (dispatch [:set-in [:debug :cur-loc-geocoded-flag] (str latitude "," longitude)])
    (when (and latitude longitude)
      (http-get (str "https://api.mapbox.com/geocoding/v5/mapbox.places/"
                     longitude "," latitude
                     ".json?access_token=pk.eyJ1Ijoic3RhdHVzaW0iLCJhIjoiY2oydmtnZjRrMDA3czMzcW9kemR4N2lxayJ9.Rz8L6xdHBjfO8cR3CDf3Cw")
                #(reset! cur-loc-geocoded (json->clj %))
                #(reset! cur-loc-geocoded nil))
      true)))

(defn place-item [title address]
  [touchable-highlight {:on-press #(do
                                     (dispatch [:set-command-argument [0 address false]])
                                     (dispatch [:send-seq-argument]))}
   [view {:height 74
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
                    :color "#000000"
                    :line-height 23}
            :font :medium}
      title]]
    [text {:number-of-lines 1
           :style {:font-size 15
                   :padding-left 22
                   :color "#000000"
                   :line-height 23}}
     address]]])

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
               (when (and @cur-loc-geocoded (> (count (:features @cur-loc-geocoded)) 0))
                 [view {:margin-top        11
                        :margin-horizontal 16}
                  [text {:style {:font-size 14
                                 :color "#939ba1"
                                 :letter-spacing -0.2}}
                   "Your current location"]
                  (let [{:keys [place_name] :as feature} (get-in @cur-loc-geocoded [:features 0])]
                    [place-item (:text feature) place_name])])))})))

(defn places-nearby-view []
  (let [geolocation      (subscribe [:get :geolocation])
        cur-loc-geocoded (r/atom nil)
        result (reaction (when @geolocation (get-places @geolocation cur-loc-geocoded)))]
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
                         [place-item text place_name])
                       (:features @cur-loc-geocoded)))])))})))


