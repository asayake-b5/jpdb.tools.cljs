(ns starter.browser
  (:require [reagent.core :as r] [reagent.dom :as rd]
            [fipp.edn :as fedn]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.controllers :as rfc]
            [reitit.coercion.spec :as rss]))

(def value (r/atom ""))
(defonce match (r/atom nil))

(defn api-key-input []
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e))}
   [:p "Enter your api key (can be found at the end of the " [:a {:href "https://jpdb.io/settings"} "settings page"] ")"
    [:input {:name "api-key"
             :value @value
             :on-change #(reset! value (-> % .-target .-value))}]
    [:button
     {:on-click #((rfe/replace-state ::main))}
     "Verify"]]])

(defn login []
  [:div
   [api-key-input]])

(defn main-page []
  [:div
   :p "baba"])

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
  (start))

;; this is called before any code is reloaded
(defn ^:dev/before-load stop []
  (js/console.log "stop"))
