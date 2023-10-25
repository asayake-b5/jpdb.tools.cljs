(ns starter.decks
  (:require
   ["@mui/material" :as mui]
   [goog.object :as gobj]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [starter.jpdb-client :as client :refer [api-key]]
   [cljs-jpdb.core :as jpdb]
   [starter.utils :as utils :refer [TabPanel]]
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
  (jpdb/list-user-decks @api-key handler-list-user)
  (jpdb/list-special-decks @api-key handler-list-special))

(defn export-deck [value]
  (jpdb/list-vocabulary @api-key
                        value
                        true
                        (fn [response]
                          (let [merged (mapv conj (:vocabulary response) (:occurences response))
                                csv (csv/write-csv merged)
                                deck-name (get (first (filter #(= (% 0) value) (:decks @user-decks))) 1)
                                name (str deck-name ".csv")]
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
  (jpdb/create-deck @api-key deck-name (fn [response]
                                         (let [deck (csv->jpdb (csv/read-csv deck-contents))
                                               id (:id response)]
                                           (jpdb/add-vocab-to-deck @api-key id deck (fn [response] (js/console.log response)))))))

(defn sleep [ms] (new js/Promise (fn [resolve] (js/setTimeout resolve ms))))

(defn import-component []
  (let [total (r/atom 0)
        done (r/atom 0)]
    (fn []
      [:div#import
       [:div
        (when (not= 0 @total)
          [:p (str "Done: " @done "/" @total)])]
       [:form
        {:on-submit
         (fn [e] (doseq [[i file] (map-indexed vector (prim-seq (-> e .-target (aget 0) .-files)))]
                   (let [basename (.substr (.-name file) 0 (.lastIndexOf (.-name file) "."))]
                     (-> (js/Promise.resolve (sleep (* (+ 1 i) 2000)))
                         (.then (fn [x]
                                  (-> (js/Promise.resolve (.text file))
                                      (.then (fn [f] (.then (js/Promise.resolve (import-deck basename f))
                                                            #(swap! done inc))))))))))
           (.preventDefault e))}
        [:input {:type "file"
                 :multiple true
                 :accept ".csv"
                 :name "file-input"
                 :on-change (fn [e]
                              (reset! total (-> e .-target .-files .-length)))}]
        [:button
         {:type "submit"}
         "Import!"]]])))

(defn deck-list->str [decks]
  (->> decks
       (map #(.-innerHTML %))
       (reduce #(str %1 "\n" %2))))

(defn delete-decks [decks]
  (when (js/confirm (str "The following decks will be deleted: \n" (deck-list->str decks) "\nAre you sure you want to delete them all? All active cards not in an deck will be temporarily disabled until you add a deck with that word in it again, delete operation is definitive."))
    (doseq [id (prim-seq decks)]
      (jpdb/delete-deck @api-key (-> id .-value js/parseInt) #(js/console.log %)))
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
             (mapv (fn [deck] (-> (jpdb/list-vocabulary-fetch @api-key (-> deck .-value js/parseInt) true)
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

                 (jpdb/lookup-vocabulary @api-key (keys @temp)  (fn [r] (->> (:vocabulary_info r)
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
;; due on
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

(defn viewer-review-component [filters contents]
  (let [page (r/atom 0)
        rows-per-page (r/atom 50)]
    (fn [filters contents]
      [:div
       [:> mui/TableContainer
    ;; [:> mui/TableHead
    ;;  [:> mui/TableRow
    ;;   [:> mui/TableCell "Name"]
    ;;   [:> mui/TableCell "Occurences"]
    ;;   [:> mui/TableCell "Definitions"]
    ;;   [:> mui/TableCell "Top"]
    ;;   [:> mui/TableCell "Level?idk"]]]
        [:> mui/TableBody
         (map (fn [row] ^{:key (get row 0)}
                [:> mui/TableRow
                 [:> mui/TableCell {:vid (get row 0) :sid (get row 1)} (str (get row 2))] ;;TODO ruby and shit
                 ])
              (get (vec (partition @rows-per-page @rows-per-page nil @contents)) @page))]
        [:> mui/TableFooter
         [:> mui/TableRow
          [:> mui/TablePagination
           {:count (count @contents) :rows-per-page @rows-per-page
            :on-page-change (fn [_ newpage]  (reset! page newpage)) :page @page
            :on-rows-per-page-change (fn [event] (reset! rows-per-page (-> event .-target .-value)))}]]]]])))

(defn export-data [contents]
  (utils/download-data (csv/write-csv contents :separator \tab) "export.csv" "text/csv"))
  ;; (js/console.log (clj->js contents)))

(defn viewer-component []
  (let [selected (r/atom nil)
        filters (r/atom {:test "test"})
        contents (r/atom nil)
        tab (r/atom 0)]
    (fn []
      [:div#viewer
       [:p "TODOs: a tab for a 'review mode' where words are stacked together, etc etc, also, infinite scrolling instead of pagination?"]
       [viewer-select-component selected contents]
       [viewer-filter-component filters]
       [:button
        {:type "submit"
         :on-click #(export-data @contents)}
        "Save to CSV (tentative for now, click when the table loaded)"]

       [:div
        [:> mui/Tabs {:value @tab :on-change #(reset! tab %2)}
         [:> mui/Tab {:label "Viewing"} :id 0]
         [:> mui/Tab {:label "Reviewing"} :id 1]]
        [TabPanel {:value @tab :index 0}
         [viewer-list-component filters contents]]
        [TabPanel {:value @tab :index 1}
         [viewer-review-component filters contents]]]])))

(defn main-page []
  (let [tab (r/atom 0)]
    (fn []
      (when (= "" @api-key)
        (rfe/replace-state :starter.browser/frontpage))
      [:div
       [:> mui/Tabs {:value @tab :on-change #(reset! tab %2)}
        [:> mui/Tab {:label "Decks"} :id 0]]
       [TabPanel {:value @tab :index 0}
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
         [:> mui/AccordionDetails [viewer-component]]]]])))
