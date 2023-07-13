(ns starter.jpdb-client
  (:require
   [ajax.core :refer [GET json-response-format POST]]
   [reagent.core :as r]))

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
   (list-vocabulary api-key deck-id false))
  ([api-key deck-id fetch-occurences? handler-list-vocab]
   (POST "https://jpdb.io/api/v1/deck/list-vocabulary"
     {:format :json
      :response-format (json-response-format {:keywords? true})
      :handler handler-list-vocab
      :params {:id deck-id :fetch_occurences fetch-occurences?}
      :headers {:Authorization (str "Bearer " api-key)}})))

(defn lookup-vocabulary
  [api-key vocabulary handler-lookup-vocab]
  (POST "https://jpdb.io/api/v1/lookup-vocabulary"
    {:format :json
     :response-format (json-response-format {:keywords? true})
     :handler handler-lookup-vocab
     :params {:list (:vocabulary vocabulary)
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

;;TODO verify with deck
(defn merge-vocab-occurences [deck-vocab]
  (let [everything (deref deck-vocab)
        vocab (:vocabulary everything)
        occurences (:occurences everything)]
    (map conj vocab occurences)))

(defn mass-delete-decks [api-key decks]
  (map (partial delete-deck api-key) decks))

;; (comment
;;   (delete-deck api-key 38)
;;   (mass-delete-decks api-key (range 42 47))
;;   (list-decks api-key)
;;   (create-deck api-key "baba")
;;   (list-special-decks api-key)
;;   (list-vocabulary api-key 48 true)
;;   (lookup-vocabulary api-key (deref deck-vocab))
;;   (add-vocab-to-deck api-key 47 (deref deck-vocab))
;;   (merge-vocab-occurences)
;;   (deref special-decks)
;;   (deref deck-vocab)
;;   (deref user-decks))

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
