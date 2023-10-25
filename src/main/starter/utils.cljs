(ns starter.utils
  (:require
   ["@mui/material" :as mui]
   [reagent.core :as r]))

;; Thanks to http://blog.find-method.de/index.php?/archives/218-File-download-with-ClojureScript.html
;; for saving me the trouble of converting myself

(defn file-blob [datamap mimetype]
  (js/Blob. [datamap] {"type" mimetype}))

(defn link-for-blob [blob filename]
  (doto (.createElement js/document "a")
    (set! -download filename)
    (set! -href (.createObjectURL js/URL blob))))

(defn click-and-remove-link [link]
  (let [click-remove-callback
        (fn []
          (.dispatchEvent link (js/MouseEvent. "click"))
          (.removeChild (.-body js/document) link))]
    (.requestAnimationFrame js/window click-remove-callback)))

(defn add-link [link]
  (.appendChild (.-body js/document) link))

(defn download-data [data filename mimetype]
  (-> data
      (file-blob mimetype)
      (link-for-blob filename)
      add-link
      click-and-remove-link))

;; (defn export-data []
;;   (download-data (:data @some-state) "exported-data.txt" "text/plain"))

(defn TabPanel []
  (let [this (r/current-component)
        props (r/props this)
        value (:value props)
        index (:index props)]
    [:div {:role "tabpanel" :hidden (not= value index)}
     (when (= value index)
       (into [:> mui/Box {:p 3}] (r/children this)))]))
