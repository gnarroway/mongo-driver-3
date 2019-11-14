(ns mongo-driver-3.collection-test
  (:require [clojure.test :refer :all]
            [mongo-driver-3.client :as mg]
            [mongo-driver-3.collection :as mc])
  (:import (com.mongodb ReadConcern ReadPreference WriteConcern)
           (java.util.concurrent TimeUnit)
           (com.mongodb.client.model InsertOneOptions InsertManyOptions DeleteOptions FindOneAndUpdateOptions ReturnDocument FindOneAndReplaceOptions CountOptions UpdateOptions ReplaceOptions IndexOptions CreateCollectionOptions)
           (java.time ZoneId LocalDate LocalTime LocalDateTime)
           (java.util Date UUID)))

;;; Unit

(deftest test->ReadConcern
  (is (nil? (mc/->ReadConcern nil)))
  (is (thrown? IllegalArgumentException (mc/->ReadConcern "invalid")))
  (is (instance? ReadConcern (mc/->ReadConcern :available))))

(deftest test->ReadPreference
  (is (nil? (mc/->ReadPreference nil)))
  (is (thrown? IllegalArgumentException (mc/->ReadPreference "invalid")))
  (is (instance? ReadPreference (mc/->ReadPreference :primary))))

(deftest test->WriteConcern
  (is (= (WriteConcern/W1) (mc/->WriteConcern {:write-concern :w1})) "accepts kw")
  (is (= (WriteConcern/W1) (mc/->WriteConcern {:write-concern (WriteConcern/W1)})) "accepts WriteConcern")
  (is (= (WriteConcern/ACKNOWLEDGED) (mc/->WriteConcern {:write-concern "invalid"})) "defaults to acknowledged")
  (is (= 1 (.getW (mc/->WriteConcern {:write-concern/w 1}))) "set w")
  (is (= 2 (.getW (mc/->WriteConcern {:write-concern (WriteConcern/W2)}))))
  (is (= 1 (.getW (mc/->WriteConcern {:write-concern (WriteConcern/W2) :write-concern/w 1}))) "prefer granular option")
  (is (true? (.getJournal (mc/->WriteConcern {:write-concern/journal? true}))) "can set journal")
  (is (= 77 (.getWTimeout (mc/->WriteConcern {:write-concern/w-timeout-ms 77}) (TimeUnit/MILLISECONDS))) "can set timeout"))

(deftest test->InsertOneOptions
  (is (instance? InsertOneOptions (mc/->InsertOneOptions {})))
  (is (true? (.getBypassDocumentValidation (mc/->InsertOneOptions {:bypass-document-validation? true}))))
  (is (true? (.getBypassDocumentValidation (mc/->InsertOneOptions
                                            {:insert-one-options (.bypassDocumentValidation (InsertOneOptions.) true)})))
      "configure directly")
  (is (false? (.getBypassDocumentValidation (mc/->InsertOneOptions
                                             {:insert-one-options          (.bypassDocumentValidation (InsertOneOptions.) true)
                                              :bypass-document-validation? false})))
      "can override"))

(deftest test->ReplaceOptions
  (is (instance? ReplaceOptions (mc/->ReplaceOptions {})))
  (are [expected arg]
       (= expected (.isUpsert (mc/->ReplaceOptions {:upsert? arg})))
    true true
    false false
    false nil) (is (true? (.getBypassDocumentValidation (mc/->ReplaceOptions {:bypass-document-validation? true}))))
  (is (true? (.getBypassDocumentValidation (mc/->ReplaceOptions
                                            {:replace-options (.bypassDocumentValidation (ReplaceOptions.) true)})))
      "configure directly")
  (is (false? (.getBypassDocumentValidation (mc/->ReplaceOptions
                                             {:replace-options             (.bypassDocumentValidation (ReplaceOptions.) true)
                                              :bypass-document-validation? false})))
      "can override"))

(deftest test->UpdateOptions
  (is (instance? UpdateOptions (mc/->UpdateOptions {})))
  (are [expected arg]
       (= expected (.isUpsert (mc/->UpdateOptions {:upsert? arg})))
    true true
    false false
    false nil)
  (is (true? (.getBypassDocumentValidation (mc/->UpdateOptions {:bypass-document-validation? true}))))
  (is (true? (.getBypassDocumentValidation (mc/->UpdateOptions
                                            {:update-options (.bypassDocumentValidation (UpdateOptions.) true)})))
      "configure directly")
  (is (false? (.getBypassDocumentValidation (mc/->UpdateOptions
                                             {:update-options              (.bypassDocumentValidation (UpdateOptions.) true)
                                              :bypass-document-validation? false})))
      "can override"))

(deftest test->InsertManyOptions
  (is (instance? InsertManyOptions (mc/->InsertManyOptions {})))
  (are [expected arg]
       (= expected (.getBypassDocumentValidation (mc/->InsertManyOptions {:bypass-document-validation? arg})))
    true true
    false false
    nil nil)
  (are [expected arg]
       (= expected (.isOrdered (mc/->InsertManyOptions {:ordered? arg})))
    true true
    false false
    true nil)
  (is (true? (.getBypassDocumentValidation (mc/->InsertManyOptions
                                            {:insert-many-options (.bypassDocumentValidation (InsertManyOptions.) true)})))
      "configure directly")
  (is (false? (.getBypassDocumentValidation (mc/->InsertManyOptions
                                             {:insert-one-options          (.bypassDocumentValidation (InsertManyOptions.) true)
                                              :bypass-document-validation? false})))
      "can override"))

(deftest test->DeleteOptions
  (is (instance? DeleteOptions (mc/->DeleteOptions {})))
  (let [opts (DeleteOptions.)]
    (is (= opts (mc/->DeleteOptions {:delete-options opts})) "configure directly")))

(deftest test->FindOneAndUpdateOptions
  (is (instance? FindOneAndUpdateOptions (mc/->FindOneAndUpdateOptions {})))
  (let [opts (FindOneAndUpdateOptions.)]
    (is (= opts (mc/->FindOneAndUpdateOptions {:find-one-and-update-options opts})) "configure directly"))
  (are [expected arg]
       (= expected (.isUpsert (mc/->FindOneAndUpdateOptions {:upsert? arg})))
    true true
    false false
    false nil)
  (are [expected arg]
       (= expected (.getReturnDocument (mc/->FindOneAndUpdateOptions {:return-new? arg})))
    (ReturnDocument/AFTER) true
    (ReturnDocument/BEFORE) false
    (ReturnDocument/BEFORE) nil)

  (is (= {"_id" 1} (.getSort (mc/->FindOneAndUpdateOptions {:sort {:_id 1}}))))
  (is (= {"_id" 1} (.getProjection (mc/->FindOneAndUpdateOptions {:projection {:_id 1}})))))

(deftest test->FindOneAndReplaceOptions
  (is (instance? FindOneAndReplaceOptions (mc/->FindOneAndReplaceOptions {})))
  (let [opts (FindOneAndReplaceOptions.)]
    (is (= opts (mc/->FindOneAndReplaceOptions {:find-one-and-replace-options opts})) "configure directly"))
  (are [expected arg]
       (= expected (.isUpsert (mc/->FindOneAndReplaceOptions {:upsert? arg})))
    true true
    false false
    false nil)
  (are [expected arg]
       (= expected (.getReturnDocument (mc/->FindOneAndReplaceOptions {:return-new? arg})))
    (ReturnDocument/AFTER) true
    (ReturnDocument/BEFORE) false
    (ReturnDocument/BEFORE) nil)

  (is (= {"_id" 1} (.getSort (mc/->FindOneAndReplaceOptions {:sort {:_id 1}}))))
  (is (= {"_id" 1} (.getProjection (mc/->FindOneAndReplaceOptions {:projection {:_id 1}})))))

(deftest test->CountOptions
  (is (instance? CountOptions (mc/->CountOptions {})))
  (let [opts (CountOptions.)]
    (is (= opts (mc/->CountOptions {:count-options opts})) "configure directly"))
  (is (= {"a" 1} (.getHint (mc/->CountOptions {:hint {:a 1}}))))
  (is (= 7 (.getLimit (mc/->CountOptions {:limit 7}))))
  (is (= 2 (.getSkip (mc/->CountOptions {:skip 2}))))
  (is (= 42 (.getMaxTime (mc/->CountOptions {:max-time-ms 42}) (TimeUnit/MILLISECONDS)))))

(deftest test->IndexOptions
  (is (instance? IndexOptions (mc/->IndexOptions {})))
  (are [expected arg]
       (= expected (.isSparse (mc/->IndexOptions {:sparse? arg})))
    true true
    false false
    false nil)
  (are [expected arg]
       (= expected (.isUnique (mc/->IndexOptions {:unique? arg})))
    true true
    false false
    false nil)
  (let [opts (IndexOptions.)]
    (is (= opts (mc/->IndexOptions {:index-options opts})) "configure directly")))

(deftest test->CreateCollectionOptions
  (are [expected arg]
       (= expected (.isCapped (mc/->CreateCollectionOptions {:capped? arg})))
    true true
    false false
    false nil)
  (is (= 7 (.getMaxDocuments (mc/->CreateCollectionOptions {:max-documents 7}))))
  (is (= 42 (.getSizeInBytes (mc/->CreateCollectionOptions {:max-size-bytes 42}))))
  (let [opts (-> (CreateCollectionOptions.) (.maxDocuments 5))]
    (is (= opts (mc/->CreateCollectionOptions {:create-collection-options opts})) "configure directly")
    (is (= 5 (.getMaxDocuments (mc/->CreateCollectionOptions {:create-collection-options opts}))))
    (is (= 7 (.getMaxDocuments (mc/->CreateCollectionOptions {:create-collection-options opts :max-documents 7})))
        "can override")))

;;; Integration

; docker run -it --rm -p 27017:27017 mongo:4.2

(def client (atom nil))

(defn- setup-connections [f]
  (reset! client (mg/create))
  (f)
  (mg/close @client))

(use-fixtures :once setup-connections)

(defn new-db
  [client]
  (mg/get-db client (.toString (UUID/randomUUID))))

(deftest ^:integration test-insert-one
  (testing "basic insertion"
    (let [db (new-db @client)
          doc {:hello "world"}
          _ (mc/insert-one db "test" doc)
          res (mc/find-maps db "test" {})]
      (is (= 1 (count res)))
      (is (= doc (select-keys (first res) [:hello])))))

  (testing "conversion round trip"
    (let [db (new-db @client)
          doc {:nil           nil
               :string        "string"
               :int           1
               :float         1.1
               :ratio         1/2
               :list          ["moo" "cow"]
               :set           #{1 2}
               :map           {:moo "cow"}
               :kw            :keyword
               :bool          true
               :date          (Date.)
               :localdate     (LocalDate/now)
               :localdatetime (LocalDateTime/now)
               :localtime     (LocalTime/now)}
          _ (mc/insert-one db "test" doc)
          res (mc/find-one-as-map db "test" {} {:projection {:_id 0}})]
      (is (= {:nil           nil
              :string        "string"
              :int           1
              :float         1.1
              :ratio         0.5
              :list          ["moo" "cow"]
              :set           [1 2]
              :map           {:moo "cow"}
              :kw            "keyword"
              :bool          true
              :date          (:date doc)
              :localdate     (Date/from (.toInstant (.atStartOfDay (:localdate doc) (ZoneId/of "UTC"))))
              :localdatetime (Date/from (.toInstant (.atZone (:localdatetime doc) (ZoneId/of "UTC"))))
              :localtime     (Date/from (.toInstant (.atZone (LocalDateTime/of (LocalDate/EPOCH) (:localtime doc)) (ZoneId/of "UTC"))))} res)))))

(deftest ^:integration test-insert-many
  (testing "basic insertions"
    (let [db (new-db @client)
          _ (mc/insert-many db "test" [{:id 1} {:id 2}])
          res (mc/find-maps db "test" {})]
      (is (= 2 (count res)))
      (is (= [1 2] (map :id res)))))

  (testing "no docs"
    (let [db (new-db @client)]
      (is (thrown? IllegalArgumentException (mc/insert-many db "test" [])))
      (is (thrown? IllegalArgumentException (mc/insert-many db "test" nil))))))

(deftest ^:integration test-delete-one
  (testing "not exist"
    (let [db (new-db @client)
          res (mc/delete-one db "test" {:id :a})]
      (is (= 0 (.getDeletedCount res)))))

  (testing "one at a time"
    (let [db (new-db @client)
          _ (mc/insert-many db "test" [{:v 1} {:v 1}])]
      (is (= 1 (.getDeletedCount (mc/delete-one db "test" {:v 1}))))
      (is (= 1 (.getDeletedCount (mc/delete-one db "test" {:v 1}))))
      (is (= 0 (.getDeletedCount (mc/delete-one db "test" {:v 1})))))))

(deftest ^:integration test-delete-many
  (testing "not exist"
    (let [db (new-db @client)
          res (mc/delete-many db "test" {:id :a})]
      (is (= 0 (.getDeletedCount res)))))

  (testing "all together"
    (let [db (new-db @client)
          _ (mc/insert-many db "test" [{:v 1} {:v 1}])]
      (is (= 2 (.getDeletedCount (mc/delete-many db "test" {:v 1}))))
      (is (= 0 (.getDeletedCount (mc/delete-many db "test" {:v 1})))))))

(deftest ^:integration test-find-maps
  (let [db (new-db @client)
        _ (mc/insert-many db "test" [{:id 1 :a 1 :v 2} {:id 2 :a 1 :v 3} {:id 3 :v 1}])]

    (testing "query"
      (are [ids q] (= ids (map :id (mc/find-maps db "test" q)))
        [1 2 3] {}
        [1] {:id 1}
        [1 2] {:a {:$exists true}}
        [2] {:v {:$gt 2}}))

    (testing "sort"
      (are [ids s] (= ids (map :id (mc/find-maps db "test" {} {:sort s})))
        [1 2 3] {}
        [3 1 2] {:v 1}
        [2 1 3] {:v -1}))

    (testing "limit"
      (are [cnt n] (= cnt (count (mc/find-maps db "test" {} {:limit n})))
        1 1
        2 2
        3 3
        3 4))

    (testing "projection"
      (are [ks p] (= ks (keys (first (mc/find-maps db "test" {} {:projection p}))))
        [:_id :id :a :v] {}
        [:_id :a] {:a 1}
        [:id :a :v] {:_id 0}))))

(deftest ^:integration test-find-one-as-map
  (let [db (new-db @client)
        _ (mc/insert-many db "test" [{:id 1 :a 1 :v 2} {:id 2 :a 1 :v 3} {:id 3 :v 1}])]

    (testing "query"
      (are [id q] (= id (:id (mc/find-one-as-map db "test" q)))
        1 {}
        2 {:id 2}
        1 {:a {:$exists true}}
        2 {:v {:$gt 2}}))

    (testing "sort"
      (are [id s] (= id (:id (mc/find-one-as-map db "test" {} {:sort s})))
        1 {}
        3 {:v 1}
        2 {:v -1}))

    (testing "projection"
      (are [ks p] (= ks (keys (mc/find-one-as-map db "test" {} {:projection p})))
        [:_id :id :a :v] {}
        [:_id :a] {:a 1}
        [:id :a :v] {:_id 0}))))

(deftest ^:integration test-count-documents
  (let [db (new-db @client)
        _ (mc/insert-many db "test" [{:id 1 :a 1 :v 2} {:id 2 :a 1 :v 3} {:id 3 :v 1}])]

    (testing "all"
      (is (= 3 (mc/count-documents db "test"))))

    (testing "query"
      (are [id q] (= id (mc/count-documents db "test" q))
        3 {}
        1 {:id 2}
        2 {:a {:$exists true}}
        1 {:v {:$gt 2}}))

    (testing "skip"
      (are [id s] (= id (mc/count-documents db "test" {} {:skip s}))
        3 nil
        3 0
        2 1
        1 2
        0 3
        0 4))

    (testing "limit"
      (are [id s] (= id (mc/count-documents db "test" {} {:limit s}))
        3 nil
        3 0
        1 1
        2 2
        3 3
        3 4))))

(deftest ^:integration test-find-one-and-update
  (testing "return new"
    (let [db (new-db @client)
          _ (mc/insert-many db "test" [{:id 1 :v 1} {:id 2 :v 1}])]

      (is (= {:id 1 :v 1} (dissoc (mc/find-one-and-update db "test" {:id 1} {:$set {:r 1} :$inc {:v 1}}) :_id)))
      (is (= {:id 2 :v 2 :r 1} (dissoc (mc/find-one-and-update db "test" {:id 2} {:$set {:r 1} :$inc {:v 1}} {:return-new? true}) :_id)))))

  (testing "upsert"
    (let [db (new-db @client)]

      (is (nil? (dissoc (mc/find-one-and-update db "test" {:id 1} {:$set {:r 1}} {:return-new? true}) :_id)))
      (is (= {:id 1 :r 1} (dissoc (mc/find-one-and-update db "test" {:id 1} {:$set {:r 1}} {:return-new? true :upsert? true}) :_id))))))

(deftest ^:integration test-find-one-and-replace
  (testing "return new"
    (let [db (new-db @client)
          _ (mc/insert-many db "test" [{:id 1 :v 1} {:id 2 :v 1}])]

      (is (= {:id 1 :v 1} (dissoc (mc/find-one-and-replace db "test" {:id 1} {:id 1 :v 2}) :_id)))
      (is (= {:id 2 :v 2} (dissoc (mc/find-one-and-replace db "test" {:id 2} {:id 2 :v 2} {:return-new? true}) :_id)))))

  (testing "upsert"
    (let [db (new-db @client)]

      (is (nil? (dissoc (mc/find-one-and-replace db "test" {:id 1} {:id 1 :v 2} {:return-new? true}) :_id)))
      (is (= {:id 1 :v 2} (dissoc (mc/find-one-and-replace db "test" {:id 1} {:id 1 :v 2} {:return-new? true :upsert? true}) :_id))))))

(deftest ^:integration test-replace-one
  (testing "existing doc"
    (let [db (new-db @client)
          _ (mc/insert-many db "test" [{:id 1 :v 1} {:id 2 :v 1}])
          r1 (mc/replace-one db "test" {:v 1} {:v 2} {})
          r2 (mc/replace-one db "test" {:v 1} {:v 2} {})
          r3 (mc/replace-one db "test" {:v 1} {:v 2} {})]
      ;; replace-one will match at most 1
      (is (= 1 (.getMatchedCount r1)))
      (is (= 1 (.getModifiedCount r1)))
      (is (= 1 (.getMatchedCount r2)))
      (is (= 1 (.getModifiedCount r2)))
      (is (= 0 (.getMatchedCount r3)))
      (is (= 0 (.getModifiedCount r3)))))

  (testing "upsert"
    (let [db (new-db @client)]

      (let [res (mc/replace-one db "test" {:id 1} {:v 2} {})]
        (is (= 0 (.getMatchedCount res)))
        (is (= 0 (.getModifiedCount res)))
        (is (nil? (.getUpsertedId res))))

      (let [res (mc/replace-one db "test" {:id 1} {:v 2} {:upsert? true})]
        (is (= 0 (.getMatchedCount res)))
        (is (= 0 (.getModifiedCount res)))
        (is (some? (.getUpsertedId res)))))))

(deftest ^:integration test-update-one
  (testing "existing doc"
    (let [db (new-db @client)
          _ (mc/insert-many db "test" [{:id 1 :v 1} {:id 2 :v 1}])
          r1 (mc/update-one db "test" {:v 1} {:$set {:v 2}} {})
          r2 (mc/update-one db "test" {:v 1} {:$set {:v 2}} {})
          r3 (mc/update-one db "test" {:v 1} {:$set {:v 2}} {})]
      ;; update-one will match at most 1
      (is (= 1 (.getMatchedCount r1)))
      (is (= 1 (.getModifiedCount r1)))
      (is (= 1 (.getMatchedCount r2)))
      (is (= 1 (.getModifiedCount r2)))
      (is (= 0 (.getMatchedCount r3)))
      (is (= 0 (.getModifiedCount r3)))))

  (testing "upsert"
    (let [db (new-db @client)]

      (let [res (mc/update-one db "test" {:id 1} {:$set {:r 1}} {})]
        (is (= 0 (.getMatchedCount res)))
        (is (= 0 (.getModifiedCount res)))
        (is (nil? (.getUpsertedId res))))

      (let [res (mc/update-one db "test" {:id 1} {:$set {:r 1}} {:upsert? true})]
        (is (= 0 (.getMatchedCount res)))
        (is (= 0 (.getModifiedCount res)))
        (is (some? (.getUpsertedId res)))))))

(deftest ^:integration test-update-many
  (testing "existing doc"
    (let [db (new-db @client)
          _ (mc/insert-many db "test" [{:id 1 :v 1} {:id 2 :v 1}])
          r1 (mc/update-many db "test" {:v 1} {:$set {:v 2}} {})
          r2 (mc/update-many db "test" {:v 1} {:$set {:v 2}} {})]
      (is (= 2 (.getMatchedCount r1)))
      (is (= 2 (.getModifiedCount r1)))
      (is (= 0 (.getMatchedCount r2)))
      (is (= 0 (.getModifiedCount r2)))))

  (testing "upsert"
    (let [db (new-db @client)]

      (let [res (mc/update-many db "test" {:id 1} {:$set {:r 1}} {})]
        (is (= 0 (.getMatchedCount res)))
        (is (= 0 (.getModifiedCount res)))
        (is (nil? (.getUpsertedId res))))

      (let [res (mc/update-many db "test" {:id 1} {:$set {:r 1}} {:upsert? true})]
        (is (= 0 (.getMatchedCount res)))
        (is (= 0 (.getModifiedCount res)))
        (is (some? (.getUpsertedId res)))))))