(ns mongo-driver-3.iterable 
  (:require [mongo-driver-3.model :as m]))

(defn documents 
  "Given a MongoIterable <it>, returns an eduction which will
   eventually yield all the documents (per `m/from-document`)."
  [it keywordize?]
  (eduction (map #(m/from-document % keywordize?)) it))
