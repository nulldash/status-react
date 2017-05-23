(ns status-im.chat.views.geolocation.views
  (:require-macros [status-im.utils.views :refer [defview]]
                   [reagent.ratom :refer [reaction]])
  (:require [status-im.components.react :refer [view image text touchable-highlight]]
            [reagent.core :as r]
            [status-im.utils.utils :refer [http-get]]
            [status-im.utils.types :refer [json->clj]]
            [status-im.chat.views.geolocation.styles :as st]
            [status-im.components.mapbox :refer [mapview]]
            [re-frame.core :refer [dispatch subscribe]]
            [status-im.i18n :refer [label]]
            [status-im.components.react :as components]))

(def mapbox-api "https://api.mapbox.com/geocoding/v5/mapbox.places/")
(def access-token "pk.eyJ1Ijoic3RhdHVzaW0iLCJhIjoiY2oydmtnZjRrMDA3czMzcW9kemR4N2lxayJ9.Rz8L6xdHBjfO8cR3CDf3Cw")

(defn get-places [coords cur-loc-geocoded & [poi?]]
  (let [{:keys [latitude longitude]} coords]
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
   [view (st/place-item-container address)
    [view st/place-item-title-container
     [view st/place-item-circle-icon]
     [text {:style st/place-item-title
            :number-of-lines 1
            :font :medium}
      title]]
    (when address
      [text {:style st/place-item-address
             :number-of-lines 1}
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
                  :logoIsHidden true
                  :style st/map-view}]]]]
     [view st/map-activity-indicator-container
      [components/activity-indicator {:animating true}]])])

(defn current-location-view []
  (let [geolocation      (subscribe [:get :geolocation])
        cur-loc-geocoded (r/atom nil)
        result (reaction (when @geolocation (get-places (:coords @geolocation) cur-loc-geocoded)))]
      (r/create-class
        {:component-will-mount #(dispatch [:request-geolocation-update])
         :render
           (fn []
             (let [_ @result]
               (dispatch [:set-in [:debug :cur-loc-geocoded] @cur-loc-geocoded])
               (when (and @cur-loc-geocoded (> (count (:features @cur-loc-geocoded)) 0))
                 [view st/location-container
                  [text {:style st/location-container-title}
                   (label :t/your-current-location)]
                  (let [{:keys [place_name] :as feature} (get-in @cur-loc-geocoded [:features 0])]
                    [place-item (:text feature) place_name])])))})))

(defn places-nearby-view []
  (let [geolocation      (subscribe [:get :geolocation])
        cur-loc-geocoded (r/atom nil)
        result (reaction (when @geolocation (get-places (:coords @geolocation) cur-loc-geocoded true)))]
    (r/create-class
      {:component-will-mount #(dispatch [:request-geolocation-update])
       :render
         (fn []
           (let [_ @result]
             (when (and @cur-loc-geocoded (> (count (:features @cur-loc-geocoded)) 0))
               [view st/location-container
                [text {:style st/location-container-title}
                 (label :t/places-nearby)]
                (doall
                  (map (fn [{:keys [text place_name] :as feature}]
                         ^{:key feature}
                         [view
                          [place-item text place_name]
                          (when (not= feature (last (:features @cur-loc-geocoded)))
                            [view st/separator])])
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
            [view st/location-container
             [text {:style st/location-container-title}
              (label :t/search-results) " " features-count]
             (doall
               (map (fn [{:keys [place_name] :as feature}]
                      ^{:key feature}
                      [view
                       [place-item place_name nil]
                       (when (not= feature (last (:features @places)))
                         [view st/separator])])
                    (:features @places)))]))))))

(defn dropped-pin []
  (let [geolocation     @(subscribe [:get :geolocation])
        pin-location    (r/atom nil)
        pin-geplocation (r/atom nil)
        result          (reaction (when @pin-location (get-places @pin-location pin-geplocation)))]
    (fn []
      (dispatch [:set-in [:debug :pin-location] @pin-location])
      (let [_ @result]
        [view
         [mapview {:initial-center-coordinate (select-keys (:coords geolocation) [:latitude :longitude])
                   :initialZoomLevel 10
                   :onRegionDidChange #(reset! pin-location (js->clj % :keywordize-keys true))
                   :logoIsHidden true
                   :style {:height 265}}]
         (when (and @pin-geplocation (> (count (:features @pin-geplocation)) 0))
           [view st/location-container
            [text {:style st/location-container-title}
             (label :t/dropped-pin)]
            (let [{:keys [place_name] :as feature} (get-in @pin-geplocation [:features 0])]
              [place-item place_name nil])])]))))