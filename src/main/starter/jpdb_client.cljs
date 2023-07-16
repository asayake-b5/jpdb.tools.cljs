(ns starter.jpdb-client
  (:require
   [ajax.core :refer [GET json-response-format POST]]
   [lambdaisland.fetch :as fetch]
   [reagent.core :as r]))

(def api-key (r/atom ""))

(defn ping [api-key]
  (GET "https://jpdb.io/api/v1/ping"
    {:format :json
     :response-format :json
     ;; :handler handler
     :headers {:Authorization (str "Bearer " api-key)}}))

(defn list-user-decks [api-key handler-list-user]
  (POST "https://jpdb.io/api/v1/list-user-decks"
    {:format :json
     :response-format (json-response-format {:keywords? true})
     :handler handler-list-user
     ;; :error-handler handler
     :params {:fields [:id :name :vocabulary_count]}
     :headers {:Authorization (str "Bearer " api-key)}}))

(defn list-special-decks [api-key handler-list-special]
  (POST "https://jpdb.io/api/v1/list-special-decks"
    {:format :json
     :response-format (json-response-format {:keywords? true})
     :handler handler-list-special
     ;; :error-handler handler
     :params {:fields [:name :vocabulary_count]}
     :headers {:Authorization (str "Bearer " api-key)}}))

(defn list-vocabulary
  ([api-key deck-id handler-list-vocab]
   (list-vocabulary api-key deck-id false) handler-list-vocab)
  ([api-key deck-id fetch-occurences? handler-list-vocab]
   (POST "https://jpdb.io/api/v1/deck/list-vocabulary"
     {:format :json
      :response-format :json
      :keywords? true
      :handler handler-list-vocab
      :params {:id deck-id :fetch_occurences fetch-occurences?}
      :headers {:Authorization (str "Bearer " api-key)}})))

(defn lookup-vocabulary
  [api-key vocabulary handler-lookup-vocab]
  (POST "https://jpdb.io/api/v1/lookup-vocabulary"
    {:format :json
     :response-format (json-response-format {:keywords? true})
     :handler handler-lookup-vocab
     :params {:list vocabulary
              :fields [:vid
                       :sid
                       :spelling
                       :reading
                       :meanings
                       :card_state
                       :frequency_rank]}
     :headers {:Authorization (str "Bearer " api-key)}}))

(defn delete-deck [api-key deck-id handler-delete-deck]
  (POST "https://jpdb.io/api/v1/deck/delete"
    {:format :json
     :response-format (json-response-format {:keywords? true})
     :handler handler-delete-deck
     :params {:id deck-id}
     :headers {:Authorization (str "Bearer " api-key)}}))

(defn create-deck [api-key name handler-create-deck]
  (POST "https://jpdb.io/api/v1/deck/create-empty"
    {:format :json
     :response-format (json-response-format {:keywords? true})
     :handler handler-create-deck
     :params {:name name}
     :headers {:Authorization (str "Bearer " api-key)}}))

(defn add-vocab-to-deck [api-key deck-id vocab handler-add-vocab]
  (POST "https://jpdb.io/api/v1/deck/add-vocabulary"
    {:format :json
     :response-format (json-response-format {:keywords? true})
     :handler handler-add-vocab
     :params {:id deck-id :vocabulary (:vocabulary vocab) :occurences (:occurences vocab)}
     :headers {:Authorization (str "Bearer " api-key)}}))

(defn list-vocabulary-fetch [api-key deck-id fetch-occurences?]
  (fetch/post "https://jpdb.io/api/v1/deck/list-vocabulary"
              {:content-type :json
               :body {:id deck-id :fetch_occurences fetch-occurences?}
               :headers {"Authorization"  (str "Bearer " api-key)}}))

(defn map-special-id
  [deck]
  (let [[name vocab-count] deck]
    (print name)
    (condp = name
      "All vocabulary" nil
      "Blacklisted vocabulary" ["blacklist" name vocab-count]
      "Vocabulary I'll never forget" ["never-forget" name vocab-count]
      nil)))

;; (let [decks (:decks (deref special-decks))]
;;   (filterv some? (map map-special-id decks)))
