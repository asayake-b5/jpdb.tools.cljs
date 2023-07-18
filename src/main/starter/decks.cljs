(ns starter.decks
  (:require
   [reagent.core :as r]
   ["@mui/material" :as mui]
   [reitit.frontend.easy :as rfe]
   [starter.jpdb-client :as client :refer [api-key]]
   [starter.utils :as utils]
   [goog.object :as gobj]
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

(defn handler-list-user [response]
  ;; (deref response))
  (reset! user-decks response)
  (.log js/console (str response)))

(defn handler-list-special [response]
  ;; (deref response))
  (reset! special-decks response)
  (.log js/console (str response)))

(defn reload-decks []
  (client/list-user-decks @api-key handler-list-user)
  (client/list-special-decks @api-key handler-list-special))

(defn export-deck [value]
  (client/list-vocabulary @api-key
                          value
                          true
                          (fn [response]
                            (let [merged (mapv conj (:vocabulary response) (:occurences response))
                                  csv (csv/write-csv merged)
                                  name "test.csv"]
                              (utils/download-data csv name "text/csv")))))

(defn gen-options []
  (for [deck (:decks @user-decks)]
    [:option
     {:value (deck 0) :key (deck 0)}
     (str (deck 1) " (" (deck 2) " words)")]))

(defn export-component []
  (let [selected (r/atom nil)]
    [:div#export
     [:form
      {:on-submit (fn [e] (.preventDefault e))}
      [:select
       {:on-change #(reset! selected (-> % .-target .-value js/parseInt))}
       [:option {:value 0 :selected true :disabled true :hidden true} "Choose a deck to export"]
       (gen-options)]
      [:button
       {:type "submit"
        :on-click #(export-deck @selected)}
       "Export!"]]]))

(defonce deck-name (r/atom "New deck name"))
(defonce csv-content (r/atom nil))

;;TODO ask on the internet if can be done better
(defn csv->jpdb [deck]
  (->> deck
       (mapv (fn [e] (let [[vid sid occ] e]
                       [[(js/parseInt vid) (js/parseInt sid)] (js/parseInt occ)])))
       (reduce #(let [vocabs-old (%1 0)
                      vocabs-new (%2 0)
                      occs-old (%1 1)
                      occs-new (%2 1)]
                  [(conj vocabs-old vocabs-new) (conj occs-old occs-new)]) [[] []])
       (zipmap [:vocabulary :occurences])))

(comment
  (let [contents [[1 2 3] [4 5 6] [7 8 9]]]
    (csv->jpdb contents)))

(defn import-deck [deck-name deck-contents]
  (client/create-deck @api-key deck-name (fn [response]
                                           (let [deck (csv->jpdb (csv/read-csv deck-contents))
                                                 id (:id response)]
                                             (client/add-vocab-to-deck @api-key id deck (fn [response] (js/console.log response)))))))

(defn import-component []
  [:div#import
   [:form
    {:on-submit (fn [e] (.preventDefault e))}
    [:p "Name of the deck after importing (can't be empty): (TODO do the react verify thing, also importe multiple?)"]
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
     "Import!"]]])

(defn deck-list->str [decks]
  (->> decks
       (map #(.-innerHTML %))
       (reduce #(str %1 "\n" %2))))

(defn delete-decks [decks]
  (when (js/confirm (str "The following decks will be deleted: \n" (deck-list->str decks) "\nAre you sure you want to delete them all? All active cards not in an deck will be temporarily disabled until you add a deck with that word in it again, delete operation is definitive."))
    (doseq [id (prim-seq decks)]
      (client/delete-deck @api-key (-> id .-value js/parseInt) #(js/console.log %)))
    (reload-decks)))

;;TODO factorize the multiple?
(defn delete-component []
  (let [selected (r/atom nil)]
    [:div#delete
     [:p "Select the decks to delete (use shift/control/dragging for multiple)"]
     [:form
      {:on-submit (fn [e] (.preventDefault e))}
      [:select
     ;;TODO process the thing later
       {:size 10 :multiple true :on-change #(reset! selected (-> % .-target .-selectedOptions))}
       (gen-options)]
      [:br]
      [:button
       {:type "submit"
        :on-click #(delete-decks @selected)}
       "Delete!"]]]))

(defn vid-occ-merge
  [decks]
  (->> decks
       (mapv (fn [deck] (into {} (map (fn [e f] [e [f]]) (get deck 0) (get deck 1)))))
       (apply merge-with (fn [a [b]] (conj a b)))))

(defn merge-view-decks [selected contents]
  (let [temp (r/atom nil)]
    (-> (->> selected
             (prim-seq)
             (mapv (fn [deck] (-> (client/list-vocabulary-fetch @api-key (-> deck .-value js/parseInt) true)
                               ;; (.then (fn [r] [[(-> r :body :vocabulary js->clj)] (-> r :body :occurences js->clj)])))))
                                  (.then (fn [r] (-> r :body))))))
             (js/Promise.all))
        (.then (fn [a] (->> a
                          ;; (map clj->js)
                            (mapv (fn [deck] [(gobj/get deck "vocabulary") (gobj/get deck "occurences")]))
                            (mapv js->clj)
                          ;; (mapv (fn [deck] [1]))
                            (vid-occ-merge)
                            (reset! temp))
               ;; (prn (clj->js (keys @contents)))

                 (client/lookup-vocabulary @api-key (keys @temp)  (fn [r] (->> (:vocabulary_info r)
                                                                               (mapv
                                                                                (fn [info] (conj info (get @temp [(info 0) (info 1)]))))
                                                                               (reset! contents)))))))))

(defn viewer-select-component [selected contents]
  [:form
   {:on-submit (fn [e] (.preventDefault e))}
   [:select
     ;;TODO process the thing later
    {:size 10 :multiple true :on-change #(reset! selected (-> % .-target .-selectedOptions))}
    (gen-options)]
   [:br]
   [:button
    {:type "submit"
     :on-click #(merge-view-decks @selected contents)}
    "View!"]])

(defn viewer-filter-component [filters]
;; New/due/machin bidule
;; vid/sid/kanji/furi/etc
;; occurences, highest occurence in any deck total occurences
;; freqs, highest freq, jpdb freq
;; cuttofs?
  [:div#filters [:label {:htmlFor "new"} "New"] [:input {:type "checkbox" :id "new" :name "New"}]])

(defn viewer-list-component [filters contents]
  (let [rows-per-page (r/atom 10)
        page (r/atom 0)]
    (fn [filters contents]

      [:div#list
       [:> mui/TableContainer
        [:> mui/TableHead
         [:> mui/TableRow
          [:> mui/TableCell "Name"]
          [:> mui/TableCell "Occurences"]
          [:> mui/TableCell "Definitions"]
          [:> mui/TableCell "Top"]
          [:> mui/TableCell "Level?idk"]]]
        [:> mui/TableBody
         (map (fn [row] ^{:key (get row 0)}
                [:> mui/TableRow
                 [:> mui/TableCell (str (get row 2))] ;;TODO ruby and shit
                 [:> mui/TableCell (str (reduce + (peek row)) "  " (flatten (peek row)))]
                 [:> mui/TableCell (str (get row 4))]
                 [:> mui/TableCell (str (get row 6))]
                 [:> mui/TableCell (str (peek row))]])
              (get (vec (partition @rows-per-page @rows-per-page nil @contents)) @page))] ;;TODO optimize this? does it get recalculated? but it should be lazy anyway
        [:> mui/TableFooter
         [:> mui/TableRow
          [:> mui/TablePagination
           {:count (count @contents) :rows-per-page @rows-per-page
            :on-page-change (fn [_ newpage]  (reset! page newpage)) :page @page
            :on-rows-per-page-change (fn [event] (reset! rows-per-page (-> event .-target .-value)))}]]]]])))

(defn viewer-component []
  (let [selected (r/atom nil)
        filters (r/atom {:test "test"})
        contents (r/atom nil)]
    (fn []
      [:div#viewer
       [:p "TODOs: a tab for a 'review mode' where words are stacked together, etc etc"]
       [viewer-select-component selected contents]
       [viewer-filter-component filters]
       [viewer-list-component filters contents]])))

(defn main-page []
  (when (= "" @api-key)
    (rfe/replace-state :starter.browser/frontpage))
  [:div
   [:> mui/Accordion
    [:> mui/AccordionSummary "Export A Deck"]
    [:> mui/AccordionDetails [export-component]]]
   [:> mui/Accordion
    [:> mui/AccordionSummary "Import Decks"]
    [:> mui/AccordionDetails [import-component]]]
   [:> mui/Accordion
    [:> mui/AccordionSummary "Delete Decks"]
    [:> mui/AccordionDetails [delete-component]]]
   [:> mui/Accordion
    [:> mui/AccordionSummary "View Decks"]
    [:> mui/AccordionDetails [viewer-component]]]])
