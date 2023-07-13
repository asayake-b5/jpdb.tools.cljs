(ns starter.decks
  (:require
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [starter.jpdb-client :as client :refer [api-key]]
   [starter.utils :as utils]
   [testdouble.cljs.csv :as csv]))

(defonce user-decks (r/atom nil))
(defonce special-decks (r/atom nil))
(defonce deck-vocab (r/atom nil))
(defonce lookedup-vocab (r/atom nil))

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

(defn export-deck [value]
  (client/list-vocabulary @api-key
                          value
                          true
                          (fn [response]
                            (let [merged (mapv conj (:vocabulary response) (:occurences response))
                                  csv (csv/write-csv merged)
                                  name "test.csv"]
                              (utils/download-data csv name "text/csv")))))

(defn main-page []
  (when (= "" @api-key)
    (rfe/replace-state :starter.browser/frontpage))
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
