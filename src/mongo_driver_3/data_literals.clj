(ns mongo-driver-3.data-literals
  (:import (org.bson.types ObjectId)
           (java.io Writer)
           (java.util Date)
           (java.nio ByteBuffer)))


(defmethod print-method ObjectId [^ObjectId c ^Writer w] (.write w ^String (str "#mongo/id \"" (.toHexString c) "\"")))
(defmethod print-dup ObjectId [^ObjectId c ^Writer w] (.write w ^String (str "#mongo/id \"" (.toHexString c) "\"")))

(defprotocol AsObjectId 
  (oid-from [this]))

(extend-protocol AsObjectId 
  (Class/forName "[B") 
  (oid-from [this] (ObjectId. ^bytes this))
  nil 
  (oid-from [_]    (ObjectId.))
  String 
  (oid-from [this] (ObjectId. this))
  Date 
  (oid-from [this] (ObjectId. this))
  ByteBuffer
  (oid-from [this] (ObjectId. this))
  )



(defn mongo-id ;; https://mongodb.github.io/mongo-java-driver/4.8/apidocs/bson/org/bson/types/ObjectId.html
  (^ObjectId []  (ObjectId.))
  (^ObjectId [o] (oid-from o))
  ([o1 o2] 
   (if (and (int? o1) 
            (int? o2))
     (ObjectId. (int o1) (int o2))
     (ObjectId. ^Date o1 (int o2)))))
