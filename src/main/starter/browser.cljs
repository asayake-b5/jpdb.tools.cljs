(ns starter.browser
  (:require
   [portal.shadow.remote :as p]
   [reagent.core :as r]
   [reagent.dom :as rd]
   [reitit.coercion.spec :as rss]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [starter.decks :refer [main-page]]
   [starter.jpdb-client :as client :refer [api-key]]))

(defonce match (r/atom nil))

(defn verify-on-click []
  (client/ping @api-key)
  (starter.decks/reload-decks)
  (rfe/replace-state :starter.browser/main))

(defn api-key-input []
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e))}
   [:p "Enter your api key (can be found at the end of the " [:a {:href "https://jpdb.io/settings"} "settings page"] ")"
    [:input {:name "api-key"
             :value @api-key
             :on-change #(reset! api-key (-> % .-target .-value))}]
    [:button
     {:on-click verify-on-click}
     "Verify"]]])

(defn login []
  [:div
   [api-key-input]])

(defn current-page []
  [:div
   (when @match
     (let [view (:view (:data @match))]
       [view @match]))
   ;; [:pre (with-out-str (fedn/pprint @match))]
   ])

(def routes
  [["/" {:name ::frontpage
         :view login}]
   ["/logged" {:name ::main
               :view main-page
               :controllers [{:start (js/console.log "pipou")}]}]])

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start []
  (rfe/start! (rf/router routes {:data {:coercion rss/coercion}})
              (fn [m] (reset! match m))
              {:use-fragment true})

  (rd/render [current-page] (js/document.getElementById "app"))
  (js/console.log "start"))

(defn init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (js/console.log "init")
  (add-tap p/submit)
  (start))

;; this is called before any code is reloaded
(defn ^:dev/before-load stop []
  (js/console.log "stop"))
