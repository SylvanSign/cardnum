(ns meccg.main
  (:require [om.core :as om :include-macros true]
            [sablono.core :as sab :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [meccg.appstate :refer [app-state]]
            [meccg.gameboard :as gameboard]
            [meccg.gamelobby :as gamelobby]
            [cardnum.nav :as nav])
  (:import goog.history.Html5History))

(enable-console-print!)

(def tokens #js ["/" "/cards" "/deckbuilder" "/play" "/help" "/account" "/stats" "/about" "/rules"])

(def history (Html5History.))

(defn navigate [token]
  (let [page-number (.indexOf tokens token)]
    (.carousel (js/$ ".carousel") page-number))
  (try (js/ga "send" "pageview" token) (catch js/Error e))
  (.setToken history token)
  (swap! app-state assoc :active-page [token]))

(events/listen history EventType/NAVIGATE #(navigate (.-token %)))
(.setUseFragment history false)
(.setPathPrefix history "")
(.setEnabled history true)

(defn navbar [cursor owner]
  (om/component
   (sab/html
    [:ul.carousel-indicator {}
     (for [[name route ndx show-fn?] nav/navbar-links]
       (when (or (not show-fn?) (show-fn? @app-state))
         [:li {:class (if (= (first (:active-page cursor)) route) "active" "")
               :on-click #(.setToken history route)
               :data-target "#main" :data-slide-to ndx}
          [:a {:href route} name]]))])))

(defn status [cursor owner]
  (om/component
    (sab/html
      [:div
       [:div.float-right
        (let [c (count (gamelobby/filter-blocked-games (:user cursor) (:games cursor)))]
          (str c " Game" (when (not= c 1) "s")))]
       (if-let [game (some #(when (= (:gameid cursor) (:gameid %)) %) (:games cursor))]
         (let [user-id (-> cursor :user :_id)
               is-player (some #(= user-id (-> % :user :_id)) (:players game))]
           (when (:started game)
             [:div.float-right
              (when is-player
                [:a.concede-button {:on-click gameboard/concede} "Concede"])
              [:a.leave-button {:on-click gamelobby/leave-game} "Leave game"]
              (when is-player
                [:a.save-button {:on-click gameboard/save-game} "Save game"])
              (when is-player
                [:a.mute-button {:on-click #(gameboard/mute-spectators (not (:mute-spectators game)))}
                 (if (:mute-spectators game) "Unmute spectators" "Mute spectators")])
              ]))
         (when (not (nil? (:gameid cursor)))
           [:div.float-right [:a {:on-click gamelobby/leave-game} "Leave game"]]))
       (when-let [game (some #(when (= (:gameid cursor) (:gameid %)) %) (:games cursor))]
         (when (:started game)
           (let [c (count (:spectators game))]
             (when (pos? c)
               [:div.spectators-count.float-right (str c " Spectator" (when (> c 1) "s"))
                [:div.blue-shade.spectators (om/build-all gamelobby/player-view
                                                          (map (fn [%] {:player % :game game})
                                                               (:spectators game)))]]))))])))

(om/root navbar app-state {:target (. js/document (getElementById "left-menu"))})
(om/root status app-state {:target (. js/document (getElementById "status"))})
