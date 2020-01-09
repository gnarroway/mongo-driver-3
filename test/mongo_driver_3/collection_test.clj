(ns mongo-driver-3.collection-test
  (:require [clojure.test :refer :all]
            [mongo-driver-3.client :as mg]
            [mongo-driver-3.collection :as mc])
  (:import (java.time ZoneId LocalDate LocalTime LocalDateTime)
           (java.util Date UUID)
           (com.mongodb.client FindIterable)))

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
          res (mc/find db "test" {})]
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
          res (mc/find-one db "test" {} {:projection {:_id 0}})]
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
              :localtime     (Date/from (.toInstant (.atZone (LocalDateTime/of (LocalDate/ofEpochDay 0) (:localtime doc)) (ZoneId/of "UTC"))))} res)))))

(deftest ^:integration test-insert-many
  (testing "basic insertions"
    (let [db (new-db @client)
          _ (mc/insert-many db "test" [{:id 1} {:id 2}])
          res (mc/find db "test" {})]
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

(deftest ^:integration test-find
  (let [db (new-db @client)
        _ (mc/insert-many db "test" [{:id 1 :a 1 :v 2} {:id 2 :a 1 :v 3} {:id 3 :v 1}])]

    (testing "query"
      (are [ids q] (= ids (map :id (mc/find db "test" q)))
        [1 2 3] {}
        [1] {:id 1}
        [1 2] {:a {:$exists true}}
        [2] {:v {:$gt 2}}))

    (testing "sort"
      (are [ids s] (= ids (map :id (mc/find db "test" {} {:sort s})))
        [1 2 3] {}
        [3 1 2] {:v 1}
        [2 1 3] {:v -1}))

    (testing "skip"
      (are [cnt n] (= cnt (count (mc/find db "test" {} {:skip n})))
        3 0
        2 1
        1 2
        0 3))

    (testing "limit"
      (are [cnt n] (= cnt (count (mc/find db "test" {} {:limit n})))
        1 1
        2 2
        3 3
        3 4))

    (testing "projection"
      (are [ks p] (= ks (keys (first (mc/find db "test" {} {:projection p}))))
        [:_id :id :a :v] {}
        [:_id :a] {:a 1}
        [:id :a :v] {:_id 0}))

    (testing "raw"
      (is (instance? FindIterable (mc/find db "test" {} {:raw? true}))))

    (testing "keywordize"
      (is (= [{"id" 1}] (mc/find db "test" {} {:keywordize? false :projection {:_id 0 :id 1} :limit 1}))))))

(deftest ^:integration test-find-one
  (let [db (new-db @client)
        _ (mc/insert-many db "test" [{:id 1 :a 1 :v 2} {:id 2 :a 1 :v 3} {:id 3 :v 1}])]

    (testing "query"
      (are [id q] (= id (:id (mc/find-one db "test" q)))
        1 {}
        2 {:id 2}
        1 {:a {:$exists true}}
        2 {:v {:$gt 2}}))

    (testing "sort"
      (are [id s] (= id (:id (mc/find-one db "test" {} {:sort s})))
        1 {}
        3 {:v 1}
        2 {:v -1}))

    (testing "projection"
      (are [ks p] (= ks (keys (mc/find-one db "test" {} {:projection p})))
        [:_id :id :a :v] {}
        [:_id :a] {:a 1}
        [:id :a :v] {:_id 0}))

    (testing "keywordize"
      (is (= {"id" 1} (mc/find-one db "test" {} {:keywordize? false :projection {:_id 0 :id 1}}))))))

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

(deftest ^:integration test-bulk-write
  (testing "existing docs"
    (let [db (new-db @client)
          _ (mc/insert-many db "test" [{:id 1} {:id 2} {:id 3} {:id 4}])
          _ (mc/bulk-write db "test" [[:replace-one {:filter {:id 2} :replacement {:id 2.1}}]
                                      [:update-many {:filter {:id 3} :update {:$set {:a "b"}}}]
                                      [:update-one {:filter {:id 4} :update {:$set {:a "b"}}}]])]

      (is (= [{:id 1} {:id 2.1} {:id 3 :a "b"} {:id 4 :a "b"}]
             (mc/find db "test" {} {:projection {:_id 0}})))))

  (testing "upsert"
    (let [db (new-db @client)
          res (mc/bulk-write db "test" [[:insert-one {:document {:id 1}}]
                                        [:replace-one {:filter {:id 2} :replacement {:id 2.1} :upsert? true}]
                                        [:update-many {:filter {:id 3} :update {:$set {:a "b"}} :upsert? true}]
                                        [:update-one {:filter {:id 4} :update {:$set {:a "b"}} :upsert? true}]])]

      (is (= 4 (mc/count-documents db "test")))
      (is (= 1 (.getInsertedCount res)))
      (is (= 3 (count (.getUpserts res)))))))

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

(defn- coll-exists?
  "Returns true if collection exists"
  [db coll]
  (some #(= coll %) (.listCollectionNames db)))

(deftest ^:integration test-create
  (testing "not existing"
    (let [db (new-db @client)
          _ (mc/create db "my-coll")]
      (is (true? (coll-exists? db "my-coll")))))

  (testing "existing"
    (let [db (new-db @client)
          _ (mc/create db "my-coll")]
      (is (thrown? Exception (mc/create db "my-coll"))))))

(deftest ^:integration test-rename
  (testing "not existing"
    (let [db (new-db @client)
          _ (mc/create db "old")
          _ (mc/rename db "old" "new")]
      (is (not (coll-exists? db "old")))
      (is (true? (coll-exists? db "new")))))

  (testing "existing"
    (let [db (new-db @client)
          _ (mc/create db "old")
          _ (mc/create db "new")]
      (is (thrown? Exception (mc/rename db "old" "new")))
      (mc/rename db "old" "new" {:drop-target? true})
      (is (not (coll-exists? db "old")))
      (is (true? (coll-exists? db "new"))))))

(deftest ^:integration test-drop
  (testing "existing"
    (let [db (new-db @client)
          _ (mc/create db "my-coll")
          _ (mc/drop db "my-coll")]
      (is (not (coll-exists? db "my-coll")))))

  (testing "not existing"
    (let [db (new-db @client)]
      (is (nil? (mc/drop db "my-coll"))))))

(deftest ^:integration test-list-indexes
  (let [db (new-db @client)
        _ (mc/create db "test")]
    (is (= 1 (count (mc/list-indexes db "test"))) "has default index")))

(deftest ^:integration test-create-index
  (let [db (new-db @client)
        _ (mc/create-index db "test" {:a 1})]
    (is (= [{:_id 1} {:a 1}] (map :key (mc/list-indexes db "test"))))))

(deftest ^:integration test-create-indexes
  (let [db (new-db @client)
        _ (mc/create-indexes db "test" [{:keys {:a 1}} {:keys {:b 1}}])]
    (is (= [{:_id 1} {:a 1} {:b 1}] (map :key (mc/list-indexes db "test"))))))