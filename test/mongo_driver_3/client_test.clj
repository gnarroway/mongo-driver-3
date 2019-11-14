(ns mongo-driver-3.client-test
  (:require [clojure.test :refer :all]
            [mongo-driver-3.client :as mg])
  (:import (com.mongodb.client MongoClient MongoDatabase)))

;;; Integration

; docker run -it --rm -p 27017:27017 mongo:4.2

(def mongo-host (or (System/getenv "MONGO_HOST") "mongodb://localhost:27017"))

(deftest test-create
  (is (instance? MongoClient (mg/create)))
  (is (instance? MongoClient (mg/create mongo-host))))

(deftest test-connect-to-db
  (is (thrown? IllegalArgumentException (mg/connect-to-db mongo-host)))
  (let [res (mg/connect-to-db (str mongo-host "/my-db"))]
    (is (instance? MongoClient (:client res)))
    (is (instance? MongoDatabase (:db res)))
    (is (= "my-db" (.getName (:db res))))))