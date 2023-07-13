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

(defn export-component []
  (let [selected (r/atom nil)]
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
                     "Export!"]]]]))

(defonce deck-name (r/atom "New deck name"))
(defonce csv-content (r/atom nil))

;;TODO ask on the internet if can be done better
(defn csv->jpdb [deck]
    (->> deck
        (mapv (fn [e] (let [[vid sid occ] e]
           [[(js/parseInt vid) (js/parseInt sid)] (js/parseInt occ)]
           )))
        (reduce #(let [vocabs-old (%1 0)
                   vocabs-new (%2 0)
                   occs-old (%1 1)
                   occs-new (%2 1)
                   ]
               [(conj vocabs-old vocabs-new) (conj occs-old occs-new)]
               ) [[] []])
        (zipmap [:vocabulary :occurences])
  ))

(comment
  (let [contents [[1 2 3] [4 5 6] [7 8 9]]]
    (csv->jpdb contents)
  ))


(defn import-deck [deck-name deck-contents]
  (client/create-deck @api-key deck-name (fn [response]
                                           (let [deck (csv->jpdb (csv/read-csv deck-contents))
                                                 id (:id response)]
                                             ;;TODO split
                                             ;; (map (fn [entry]
                                             ;;        (let [[vid sid occ]])
                                             ;;        ) deck )
                                             (client/add-vocab-to-deck @api-key id deck (fn [response] (js/console.log response)))

                                             )
                                           )))

(defn import-component []
  [:div#import
   [:h3 "Import" [:form
                  {:on-submit (fn [e] (.preventDefault e))}
                  [:p "Name of the deck after importing (can't be empty): (TODO do the react verify thing idk)"]
                  [:input {:name "deck-name"
                           :value @deck-name
                           :on-change #(reset! deck-name (-> % .-target .-value))}]
                  [:input {:type "file"
                           :name "file-input"
                           :on-change (fn [e]
                                        (when (not (= "" (-> e .-target .-value)))
                                          (let [file (-> e .-target .-files (aget 0))]
                                            (.then (js/Promise.resolve (.text file)) #(reset! csv-content %)))))}]
                  [:button
                   {:type "submit"
                    :on-click #(import-deck @deck-name @csv-content)}
                   "Import!"]]]])

(defn main-page []
  (when (= "" @api-key)
    (rfe/replace-state :starter.browser/frontpage))
  [:div
   (export-component)
   (import-component)])
