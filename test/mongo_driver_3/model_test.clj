(ns mongo-driver-3.model-test
  (:require [clojure.test :refer :all]
            [mongo-driver-3.model :as m])
  (:import (com.mongodb ReadConcern ReadPreference WriteConcern)
           (java.util.concurrent TimeUnit)
           (com.mongodb.client.model InsertOneOptions InsertManyOptions DeleteOptions FindOneAndUpdateOptions ReturnDocument FindOneAndReplaceOptions CountOptions UpdateOptions ReplaceOptions IndexOptions CreateCollectionOptions RenameCollectionOptions BulkWriteOptions DeleteManyModel DeleteOneModel InsertOneModel ReplaceOneModel UpdateManyModel UpdateOneModel)))

;;; Unit

(deftest test->ReadConcern
  (is (nil? (m/->ReadConcern {})))
  (is (thrown? IllegalArgumentException (m/->ReadConcern {:read-concern "invalid"})))
  (is (instance? ReadConcern (m/->ReadConcern {:read-concern :available}))))

(deftest test->ReadPreference
  (is (nil? (m/->ReadPreference {})))
  (is (thrown? IllegalArgumentException (m/->ReadPreference {:read-preference "invalid"})))
  (is (instance? ReadPreference (m/->ReadPreference {:read-preference :primary}))))

(deftest test->WriteConcern
  (is (= (WriteConcern/W1) (m/->WriteConcern {:write-concern :w1})) "accepts kw")
  (is (= (WriteConcern/W1) (m/->WriteConcern {:write-concern (WriteConcern/W1)})) "accepts WriteConcern")
  (is (= (WriteConcern/ACKNOWLEDGED) (m/->WriteConcern {:write-concern "invalid"})) "defaults to acknowledged")
  (is (= 1 (.getW (m/->WriteConcern {:write-concern/w 1}))) "set w")
  (is (= 2 (.getW (m/->WriteConcern {:write-concern (WriteConcern/W2)}))))
  (is (= 1 (.getW (m/->WriteConcern {:write-concern (WriteConcern/W2) :write-concern/w 1}))) "prefer granular option")
  (is (true? (.getJournal (m/->WriteConcern {:write-concern/journal? true}))) "can set journal")
  (is (= 77 (.getWTimeout (m/->WriteConcern {:write-concern/w-timeout-ms 77}) (TimeUnit/MILLISECONDS))) "can set timeout"))

(deftest test->InsertOneOptions
  (is (instance? InsertOneOptions (m/->InsertOneOptions {})))
  (is (true? (.getBypassDocumentValidation (m/->InsertOneOptions {:bypass-document-validation? true}))))
  (is (true? (.getBypassDocumentValidation (m/->InsertOneOptions
                                            {:insert-one-options (.bypassDocumentValidation (InsertOneOptions.) true)})))
      "configure directly")
  (is (false? (.getBypassDocumentValidation (m/->InsertOneOptions
                                             {:insert-one-options          (.bypassDocumentValidation (InsertOneOptions.) true)
                                              :bypass-document-validation? false})))
      "can override"))

(deftest test->ReplaceOptions
  (is (instance? ReplaceOptions (m/->ReplaceOptions {})))
  (are [expected arg]
       (= expected (.isUpsert (m/->ReplaceOptions {:upsert? arg})))
    true true
    false false
    false nil)
  (is (true? (.getBypassDocumentValidation (m/->ReplaceOptions {:bypass-document-validation? true}))))
  (is (true? (.getBypassDocumentValidation (m/->ReplaceOptions
                                            {:replace-options (.bypassDocumentValidation (ReplaceOptions.) true)})))
      "configure directly")
  (is (false? (.getBypassDocumentValidation (m/->ReplaceOptions
                                             {:replace-options             (.bypassDocumentValidation (ReplaceOptions.) true)
                                              :bypass-document-validation? false})))
      "can override"))

(deftest test->UpdateOptions
  (is (instance? UpdateOptions (m/->UpdateOptions {})))
  (are [expected arg]
       (= expected (.isUpsert (m/->UpdateOptions {:upsert? arg})))
    true true
    false false
    false nil)
  (is (true? (.getBypassDocumentValidation (m/->UpdateOptions {:bypass-document-validation? true}))))
  (is (true? (.getBypassDocumentValidation (m/->UpdateOptions
                                            {:update-options (.bypassDocumentValidation (UpdateOptions.) true)})))
      "configure directly")
  (is (false? (.getBypassDocumentValidation (m/->UpdateOptions
                                             {:update-options              (.bypassDocumentValidation (UpdateOptions.) true)
                                              :bypass-document-validation? false})))
      "can override"))

(deftest test->InsertManyOptions
  (is (instance? InsertManyOptions (m/->InsertManyOptions {})))
  (are [expected arg]
       (= expected (.getBypassDocumentValidation (m/->InsertManyOptions {:bypass-document-validation? arg})))
    true true
    false false
    nil nil)
  (are [expected arg]
       (= expected (.isOrdered (m/->InsertManyOptions {:ordered? arg})))
    true true
    false false
    true nil)
  (is (true? (.getBypassDocumentValidation (m/->InsertManyOptions
                                            {:insert-many-options (.bypassDocumentValidation (InsertManyOptions.) true)})))
      "configure directly")
  (is (false? (.getBypassDocumentValidation (m/->InsertManyOptions
                                             {:insert-many-options          (.bypassDocumentValidation (InsertManyOptions.) true)
                                              :bypass-document-validation? false})))
      "can override"))

(deftest test->DeleteOptions
  (is (instance? DeleteOptions (m/->DeleteOptions {})))
  (let [opts (DeleteOptions.)]
    (is (= opts (m/->DeleteOptions {:delete-options opts})) "configure directly")))

(deftest test->RenameCollectionOptions
  (is (instance? RenameCollectionOptions (m/->RenameCollectionOptions {})))
  (are [expected arg]
       (= expected (.isDropTarget (m/->RenameCollectionOptions {:drop-target? arg})))
    true true
    false false
    false nil)
  (let [opts (RenameCollectionOptions.)]
    (is (= opts (m/->RenameCollectionOptions {:rename-collection-options opts})) "configure directly")))

(deftest test->FindOneAndUpdateOptions
  (is (instance? FindOneAndUpdateOptions (m/->FindOneAndUpdateOptions {})))
  (let [opts (FindOneAndUpdateOptions.)]
    (is (= opts (m/->FindOneAndUpdateOptions {:find-one-and-update-options opts})) "configure directly"))
  (are [expected arg]
       (= expected (.isUpsert (m/->FindOneAndUpdateOptions {:upsert? arg})))
    true true
    false false
    false nil)
  (are [expected arg]
       (= expected (.getReturnDocument (m/->FindOneAndUpdateOptions {:return-new? arg})))
    (ReturnDocument/AFTER) true
    (ReturnDocument/BEFORE) false
    (ReturnDocument/BEFORE) nil)

  (is (= {"_id" 1} (.getSort (m/->FindOneAndUpdateOptions {:sort {:_id 1}}))))
  (is (= {"_id" 1} (.getProjection (m/->FindOneAndUpdateOptions {:projection {:_id 1}})))))

(deftest test->FindOneAndReplaceOptions
  (is (instance? FindOneAndReplaceOptions (m/->FindOneAndReplaceOptions {})))
  (let [opts (FindOneAndReplaceOptions.)]
    (is (= opts (m/->FindOneAndReplaceOptions {:find-one-and-replace-options opts})) "configure directly"))
  (are [expected arg]
       (= expected (.isUpsert (m/->FindOneAndReplaceOptions {:upsert? arg})))
    true true
    false false
    false nil)
  (are [expected arg]
       (= expected (.getReturnDocument (m/->FindOneAndReplaceOptions {:return-new? arg})))
    (ReturnDocument/AFTER) true
    (ReturnDocument/BEFORE) false
    (ReturnDocument/BEFORE) nil)

  (is (= {"_id" 1} (.getSort (m/->FindOneAndReplaceOptions {:sort {:_id 1}}))))
  (is (= {"_id" 1} (.getProjection (m/->FindOneAndReplaceOptions {:projection {:_id 1}})))))

(deftest test->CountOptions
  (is (instance? CountOptions (m/->CountOptions {})))
  (let [opts (CountOptions.)]
    (is (= opts (m/->CountOptions {:count-options opts})) "configure directly"))
  (is (= {"a" 1} (.getHint (m/->CountOptions {:hint {:a 1}}))))
  (is (= 7 (.getLimit (m/->CountOptions {:limit 7}))))
  (is (= 2 (.getSkip (m/->CountOptions {:skip 2}))))
  (is (= 42 (.getMaxTime (m/->CountOptions {:max-time-ms 42}) (TimeUnit/MILLISECONDS)))))

(deftest test->IndexOptions
  (is (instance? IndexOptions (m/->IndexOptions {})))
  (are [expected arg]
       (= expected (.isSparse (m/->IndexOptions {:sparse? arg})))
    true true
    false false
    false nil)
  (are [expected arg]
       (= expected (.isUnique (m/->IndexOptions {:unique? arg})))
    true true
    false false
    false nil)
  (let [opts (IndexOptions.)]
    (is (= opts (m/->IndexOptions {:index-options opts})) "configure directly")))

(deftest test->CreateCollectionOptions
  (are [expected arg]
       (= expected (.isCapped (m/->CreateCollectionOptions {:capped? arg})))
    true true
    false false
    false nil)
  (is (= 7 (.getMaxDocuments (m/->CreateCollectionOptions {:max-documents 7}))))
  (is (= 42 (.getSizeInBytes (m/->CreateCollectionOptions {:max-size-bytes 42}))))
  (let [opts (-> (CreateCollectionOptions.) (.maxDocuments 5))]
    (is (= opts (m/->CreateCollectionOptions {:create-collection-options opts})) "configure directly")
    (is (= 5 (.getMaxDocuments (m/->CreateCollectionOptions {:create-collection-options opts}))))
    (is (= 7 (.getMaxDocuments (m/->CreateCollectionOptions {:create-collection-options opts :max-documents 7})))
        "can override")))

(deftest test->BulkWriteOptions
  (is (instance? BulkWriteOptions (m/->BulkWriteOptions {})))
  (are [expected arg]
       (= expected (.getBypassDocumentValidation (m/->BulkWriteOptions {:bypass-document-validation? arg})))
    true true
    false false
    nil nil)
  (are [expected arg]
       (= expected (.isOrdered (m/->BulkWriteOptions {:ordered? arg})))
    true true
    false false
    true nil)
  (is (true? (.getBypassDocumentValidation (m/->BulkWriteOptions
                                            {:bulk-write-options (.bypassDocumentValidation (BulkWriteOptions.) true)})))
      "configure directly")
  (is (false? (.getBypassDocumentValidation (m/->BulkWriteOptions
                                             {:bulk-write-options         (.bypassDocumentValidation (BulkWriteOptions.) true)
                                              :bypass-document-validation? false})))
      "can override"))

(deftest test-write-model
  (testing "delete many"
    (is (instance? DeleteManyModel (m/write-model [:delete-many {:filter {:a "b"}}]))))

  (testing "delete one"
    (is (instance? DeleteOneModel (m/write-model [:delete-one {:filter {:a "b"}}]))))

  (testing "insert one"
    (is (instance? InsertOneModel (m/write-model [:insert-one {:document {:a "b"}}]))))

  (testing "replace one"
    (is (instance? ReplaceOneModel (m/write-model [:replace-one {:filter {:a "b"} :replacement {:a "c"}}])))
    (are [expected arg]
         (= expected (.isUpsert (.getOptions (m/write-model [:replace-one {:filter {:a "b"} :replacement {:a "c"} :upsert? arg}]))))
      true true
      false false
      false nil))

  (testing "update many"
    (is (instance? UpdateManyModel (m/write-model [:update-many {:filter {:a "b"} :update {"$set" {:a "c"}}}])))
    (are [expected arg]
         (= expected (.isUpsert (.getOptions (m/write-model [:update-many {:filter {:a "b"} :update {"$set" {:a "c"}} :upsert? arg}]))))
      true true
      false false
      false nil))

  (testing "update one"
    (is (instance? UpdateOneModel (m/write-model [:update-one {:filter {:a "b"} :update {"$set" {:a "c"}}}])))
    (are [expected arg]
         (= expected (.isUpsert (.getOptions (m/write-model [:update-one {:filter {:a "b"} :update {"$set" {:a "c"}} :upsert? arg}]))))
      true true
      false false
      false nil)))

