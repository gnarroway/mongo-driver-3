(ns mongo-driver-3.data-literals
  (:import (org.bson.types ObjectId)
           (java.io Writer)
           (java.util Date)
           (java.nio ByteBuffer)))


(defmethod print-method ObjectId [^ObjectId c ^Writer w] (.write w ^String (str "#mongo/id \"" (.toHexString c) "\"")))
(defmethod print-dup ObjectId [^ObjectId c ^Writer w] (.write w ^String (str "#mongo/id \"" (.toHexString c) "\"")))

(defn mongo-id ;; https://mongodb.github.io/mongo-java-driver/4.8/apidocs/bson/org/bson/types/ObjectId.html
  (^ObjectId [] (ObjectId.))
  (^ObjectId [o]
   (cond
     (string? o)        (ObjectId. ^String o)
     (bytes? o)         (ObjectId. ^bytes o)
     (instance? Date o) (ObjectId. ^Date o)
     (instance? ByteBuffer o) (ObjectId. ^ByteBuffer o)
     :else
     (throw
      (IllegalArgumentException. 
       (str "Can not construct an ObjectId from class: " (type o))))))
  ([o1 o2] 
   (if (and (int? o1) 
            (int? o2))
     (ObjectId. (int o1) (int o2))
     (ObjectId. ^Date o1 (int o2)))))
