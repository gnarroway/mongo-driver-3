(ns mongo-driver-3.client
  (:refer-clojure :exclude [find])
  (:require [mongo-driver-3.model :as m])
  (:import (com.mongodb.client MongoClients MongoClient ClientSession MongoDatabase TransactionBody)
           (com.mongodb ConnectionString ClientSessionOptions TransactionOptions)
           (java.util.concurrent TimeUnit)))

(set! *warn-on-reflection* true)

;;; Core

(defn create
  "Creates a connection to a MongoDB

  `connection-string` is a mongo connection string, e.g. mongodb://localhost:27107

  If a connecting string is not passed in, it will connect to the default localhost instance."
  ([] (MongoClients/create))
  ([^String connection-string]
   (MongoClients/create connection-string)))

(defn get-db
  "Gets a database by name

  `client` is a MongoClient, e.g. resulting from calling `connect`
  `name` is the name of the database to get."
  [^MongoClient client ^String name]
  (.getDatabase client name))

(defn close
  "Close a MongoClient and release all resources"
  [^MongoClient client]
  (.close client))

(defn list-collections
  "Lists collections in a database, returning as a seq of maps unless otherwise configured.

  Arguments:

  - `db` a MongoDatabase
  - `opts` (optional), a map of:
    - `:name-only?` returns just the string names
    - `:keywordize?` keywordize the keys of return results, default: true. Only applicable if `:name-only?` is false.
    - `:raw?` return the mongo iterable directly instead of processing into a seq, default: false
    - `:session` a ClientSession"
  ([^MongoDatabase db] (list-collections db {}))
  ([^MongoDatabase db {:keys [raw? keywordize? ^ClientSession session] :or {keywordize? true}}]
   (let [it (if session
              (.listCollections db session)
              (.listCollections db))]
     (if-not raw?
       (map #(m/from-document % keywordize?) (seq it))
       it))))

(defn list-collection-names
  "Lists collection names in a database, returning as a seq of strings unless otherwise configured.

  Arguments:

  - `db` a MongoDatabase
  - `opts` (optional), a map of:
    - `:raw?` return the mongo MongoIterable directly instead of processing into a seq, default: false
    - `:session` a ClientSession"
  ([^MongoDatabase db] (list-collection-names db {}))
  ([^MongoDatabase db opts]
   (let [it (if-let [^ClientSession session (:session opts)]
              (.listCollectionNames db session)
              (.listCollectionNames db))]
     (if-not (:raw? opts)
       (seq it)
       it))))

(defn ->TransactionOptions
  "Coerces options map into a TransactionOptions. See `start-session` for usage."
  [{:keys [max-commit-time-ms] :as opts}]
  (let [rp (m/->ReadPreference opts)
        rc (m/->ReadConcern opts)
        wc (m/->WriteConcern opts)]

    (cond-> (TransactionOptions/builder)
      max-commit-time-ms (.maxCommitTime max-commit-time-ms (TimeUnit/MILLISECONDS))
      rp (.readPreference rp)
      rc (.readConcern rc)
      wc (.writeConcern wc)
      true (.build))))

(defn ->ClientSessionOptions
  "Coerces an options map into a ClientSessionOptions See `start-session` for usage.

  See `start-session` for usage"
  [{:keys [client-session-options causally-consistent?] :as opts}]
  (let [trans-opts (->TransactionOptions opts)]
    (cond-> (if client-session-options (ClientSessionOptions/builder client-session-options) (ClientSessionOptions/builder))
      trans-opts (.defaultTransactionOptions trans-opts)
      (some? causally-consistent?) (.causallyConsistent causally-consistent?)
      true (.build))))

(defn start-session
  "Creates a client session.

  Arguments:

  - `client` a MongoClient
  - `opts` (optional), a map of:
    - `:max-commit-time-ms` Max execution time for commitTransaction operation, in milliseconds
    - `:causally-consistent?` whether operations using session should be causally consistent with each other
    - `:read-preference` Accepts a ReadPreference or a kw corresponding to one:
       [:primary, :primaryPreferred, :secondary, :secondaryPreferred, :nearest]
       Invalid values will throw an exception.
    - `:read-concern` Accepts a ReadConcern or kw corresponding to one:
      [:available, :default, :linearizable, :local, :majority, :snapshot]
      Invalid values will throw an exception.
    - `:write-concern` A WriteConcern or kw corresponding to one:
      [:acknowledged, :journaled, :majority, :unacknowledged, :w1, :w2, :w3],
      defaulting to :acknowledged, if some invalid option is provided.
    - `:write-concern/w` an int >= 0, controlling the number of replicas to acknowledge
    - `:write-concern/w-timeout-ms` How long to wait for secondaries to acknowledge before failing,
      in milliseconds (0 means indefinite).
    - `:write-concern/journal?` If true, block until write operations have been committed to the journal.
    - `:client-session-options` a ClientSessionOptions, for configuring directly. If specified, any
      other [preceding] query options will be applied to it."
  ([^MongoClient client] (start-session client {}))
  ([^MongoClient client opts]
   (.startSession client (->ClientSessionOptions opts))))

(defn with-transaction
  "Executes `body` in a transaction.

  `body` should be a fn with one or more mongo operations in it.
  Ensure `session` is passed as an option to each operation.

  e.g.

  ```clojure
  (with-open [s (start-session client)]
    (with-transaction s
      (fn []
        (insert-one my-db \"coll\" {:name \"hello\"} {:session s})
        (insert-one my-db \"coll\" {:name \"world\"} {:session s}))))
  ```"
  ([^ClientSession session body] (with-transaction session body {}))
  ([^ClientSession session body opts]
   (.withTransaction session
                     (reify TransactionBody
                       (execute [_] (body)))
                     (->TransactionOptions opts))))

;;; Utility

(defn connect-to-db
  "Connects to a MongoDB database using a URI, returning the client and database as a map with :client and :db.

  This is useful to get a db from a single call, instead of having to create a client and get a db manually."
  [connection-string]
  (let [uri (ConnectionString. connection-string)
        client (MongoClients/create uri)]
    (if-let [db-name (.getDatabase uri)]
      {:client client :db (.getDatabase client db-name)}
      (throw (IllegalArgumentException. "No database name specified in URI. connect-to-db requires database to be explicitly configured.")))))