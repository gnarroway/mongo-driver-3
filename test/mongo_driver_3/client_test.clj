(ns mongo-driver-3.client-test
  (:require [clojure.test :refer :all]
            [mongo-driver-3.client :as mg]
            [mongo-driver-3.collection :as mc])
  (:import (com.mongodb.client MongoClient MongoDatabase MongoIterable ListCollectionsIterable)
           (java.util UUID)
           (com.mongodb ClientSessionOptions ReadConcern ReadPreference)
           (java.util.concurrent TimeUnit)))

;;; Unit

(deftest test->ClientSessionOptions
  (is (instance? ClientSessionOptions (mg/->ClientSessionOptions {})))
  (are [expected arg]
       (= expected (.isCausallyConsistent (mg/->ClientSessionOptions {:causally-consistent? arg})))
    true true
    false false
    nil nil)
  (is (= 7 (.getMaxCommitTime (.getDefaultTransactionOptions
                               (mg/->ClientSessionOptions {:max-commit-time-ms 7})) (TimeUnit/MILLISECONDS))))
  (is (= (ReadConcern/AVAILABLE) (.getReadConcern (.getDefaultTransactionOptions
                                                   (mg/->ClientSessionOptions {:read-concern :available})))))
  (is (= (ReadPreference/primary) (.getReadPreference (.getDefaultTransactionOptions (mg/->ClientSessionOptions {:read-preference :primary})))))
  (is (nil? (.getWriteConcern (.getDefaultTransactionOptions (mg/->ClientSessionOptions {})))))
  (is (= 1 (.getW (.getWriteConcern (.getDefaultTransactionOptions (mg/->ClientSessionOptions {:write-concern/w 1}))))))
  (let [opts (.build (.causallyConsistent (ClientSessionOptions/builder) true))]
    (is (= opts (mg/->ClientSessionOptions {:client-session-options opts})) "configure directly")))

;;; Integration

; docker run -it --rm -p 27017:27017 mongo:4.2
(def mongo-host "mongodb://localhost:27017")

(deftest ^:integration test-create
  (is (instance? MongoClient (mg/create)))
  (is (instance? MongoClient (mg/create mongo-host))))

(deftest ^:integration test-connect-to-db
  (is (thrown? IllegalArgumentException (mg/connect-to-db mongo-host)))
  (let [res (mg/connect-to-db (str mongo-host "/my-db"))]
    (is (instance? MongoClient (:client res)))
    (is (instance? MongoDatabase (:db res)))
    (is (= "my-db" (.getName (:db res))))))

(def client (atom nil))

(defn- setup-connections [f]
  (reset! client (mg/create))
  (f)
  (mg/close @client))

(use-fixtures :once setup-connections)

(defn new-db
  [client]
  (mg/get-db client (.toString (UUID/randomUUID))))

(deftest ^:integration  test-list-collections
  (let [db (new-db @client)
        _ (mc/create db "test")]
    (is (= ["test"] (map :name (mg/list-collections db))))
    (is (= ["test"] (map #(get % "name") (mg/list-collections db {:keywordize? false}))))
    (is (instance? ListCollectionsIterable (mg/list-collections db {:raw? true})))))

(deftest ^:integration test-list-collection-names
  (let [db (new-db @client)
        _ (mc/create db "test")]
    (is (= ["test"] (mg/list-collection-names db)))
    (is (instance? MongoIterable (mg/list-collection-names db {:raw? true})))))

#_(deftest ^:integration test-start-session
    (is (instance? ClientSession (mg/start-session @client))))