(ns starter.browser
  (:require [reagent.core :as r] [reagent.dom :as rd]
            [starter.jpdb-client :as client]
            [starter.utils :as utils]
            [ajax.core :refer [GET POST]]
            [fipp.edn :as fedn]
            [testdouble.cljs.csv :as csv]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.controllers :as rfc]
            [reitit.coercion.spec :as rss]))

(def api-key (r/atom ""))
(defonce match (r/atom nil))

(defonce user-decks (r/atom nil))
(defonce special-decks (r/atom nil))
(defonce deck-vocab (r/atom nil))
(defonce lookedup-vocab (r/atom nil))

(defn handler-list-user [response]
  ;; (deref response))
  (reset! user-decks response)
  (.log js/console (str response)))

(defn handler-list-special [response]
  ;; (deref response))
  (reset! special-decks response)
  (.log js/console (str response)))

(defn handler-list-vocab [response]
  ;; (deref response))
  (reset! deck-vocab response)
  (.log js/console (str response)))

(defn handler-lookup-vocab [response]
  ;; (deref response))
  (reset! lookedup-vocab response)
  (.log js/console (str response)))

(defn handler-create-deck [response]
  ;; (deref response))
  (.log js/console (str response)))

(defn verify-on-click []
  (client/ping @api-key)
  (client/list-user-decks @api-key handler-list-user)
  (client/list-special-decks @api-key handler-list-special)

  (rfe/replace-state ::main))

(defn export-deck [value]
  (client/list-vocabulary @api-key
                          value
                          true
                          (fn [response]
                            (let [merged (mapv conj (:vocabulary response) (:occurences response))
                                  csv (csv/write-csv merged)
                                  name "test.csv"]
                              (utils/download-data csv name "text/csv")))))

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

(defn main-page []
  (when (= "" @api-key)
    (rfe/replace-state ::frontpage))
  ;; (let [selected (r/atom (((:decks @user-decks) 0) 0))]
  (let [selected (r/atom nil)]
    [:div
     [:div#export
      [:h3 "Export" [:form
                     {:on-submit (fn [e] (.preventDefault e))}
                     [:select
                      {:on-change #(reset! selected (-> % .-target .-value js/parseInt))}
                      [:option {:value 0 :selected true :disabled true :hidden true} "Choose a deck to export"]
                      (for [deck (:decks @user-decks)]
                        [:option
                         {:value (deck 0) :key (deck 0)}
                         (str (deck 1) " (" (deck 2) " words)")])]
                     [:button
                      {:type "submit"
                       :on-click #(export-deck @selected)}
                      "Export!"]]]]]))

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
