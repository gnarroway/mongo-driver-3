(ns mongo-driver-3.data-literals
  (:import (org.bson.types ObjectId)
           (java.io Writer)))


(defmethod print-method ObjectId [c ^Writer w] (.write w ^String (str "#mongo/id \"" (.toHexString c) "\"")))
(defmethod print-dup ObjectId [c ^Writer w] (.write w ^String (str "#mongo/id \"" (.toHexString c) "\"")))

(defn mongo-id [o]
  (ObjectId. o))
