(ns mongo-driver-3.collection
  (:refer-clojure :exclude [find empty? drop])
  (:require [mongo-driver-3.model :refer :all]
            [mongo-driver-3.client :refer [*session*]])
  (:import (com.mongodb MongoNamespace)
           (com.mongodb.client MongoDatabase MongoCollection ClientSession)
           (com.mongodb.client.model IndexModel)
           (java.util List)
           (org.bson Document)))

(set! *warn-on-reflection* true)

;;; Collection

(defn ^MongoCollection collection
  "Coerces `coll` to a MongoCollection with some options.

  Arguments:

  - `db`   is a MongoDatabase
  - `coll` is a collection name or a MongoCollection. This is to provide flexibility, either in reuse of
    instances or in some more complex configuration we do not directly support.
  - `opts` (optional), a map of:
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
    - `:write-concern/journal?` If true, block until write operations have been committed to the journal."
  ([^MongoDatabase db coll]
   (collection db coll {}))
  ([^MongoDatabase db coll opts]
   (let [^MongoCollection coll' (if (instance? MongoCollection coll) coll (.getCollection db coll))
         rp (->ReadPreference opts)
         rc (->ReadConcern opts)
         wc (->WriteConcern opts)]
     (cond-> ^MongoCollection coll'
       rp (.withReadPreference rp)
       rc (.withReadConcern rc)
       wc (.withWriteConcern wc)))))

;;; CRUD functions

(defn aggregate
  "Aggregates documents according to the specified aggregation pipeline and return a seq of maps,
  unless configured otherwise..

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection namee
  - `q` is a map representing a query.
  - `opts` (optional), a map of:
    - `:allow-disk-use?` whether to allow writing temporary files
    - `:batch-size` Documents to return per batch, e.g. 1
    - `:bypass-document-validation?` Boolean
    - `:keywordize?` keywordize the keys of return results, default: true
    - `:raw?` return the mongo AggregateIterable directly instead of processing into a seq, default: false
    - `:session` a ClientSession"
  ([^MongoDatabase db coll pipeline]
   (aggregate db coll pipeline {}))
  ([^MongoDatabase db coll pipeline opts]
   (let [{:keys [^ClientSession session allow-disk-use? ^Integer batch-size bypass-document-validation? keywordize? raw?] :or {keywordize? true raw? false}} opts
         ^ClientSession session (or session *session*)
         it                     (cond-> (if session
                                          (.aggregate (collection db coll opts) session ^List (map document pipeline))
                                          (.aggregate (collection db coll opts) ^List (map document pipeline)))
                                        (some? allow-disk-use?) (.allowDiskUse allow-disk-use?)
                                        (some? bypass-document-validation?) (.bypassDocumentValidation bypass-document-validation?)
                                        batch-size (.batchSize batch-size))]

     (if-not raw?
       (map (fn [x] (from-document x keywordize?)) (seq it))
       it))))

(defn bulk-write
  "Executes a mix of inserts, updates, replaces, and deletes.

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `operations` a list of 2-tuples in the form `[op config]`,
    - `op` is one of :insert-one :update-one :update-many :delete-one :delete-many :replace-one
    - `config` the configuration map for the operation
      - `insert` takes `:document`
      - `update` takes `:filter`, `:update`, and any options in the corresponding update function
      - `delete` takes `:filter`, and any options in the corresponding delete function
      - `replace` takes `:filter`, `:replacement`, and any options in the corresponding replace function
  - `opts` (optional), a map of:
    - `:bypass-document-validation?` Boolean
    - `:ordered?` Boolean whether serve should insert documents in order provided (default true)
    - `:bulk-write-options` A BulkWriteOptions for configuring directly. If specified,
    any other [preceding] query options will be applied to it.
    - `:session` A ClientSession

  Additionally takes options specified in `collection`"
  ([^MongoDatabase db coll operations]
   (bulk-write db coll operations {}))
  ([^MongoDatabase db coll operations opts]
   (let [opts' (->BulkWriteOptions opts)]
     (if-let [session (or (:session opts) *session*)]
       (.bulkWrite (collection db coll opts') ^ClientSession session ^List (map write-model operations))
       (.bulkWrite (collection db coll opts') (map write-model operations))))))

(defn count-documents
  "Count documents in a collection, optionally matching a filter query `q`.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `q` is a map representing a query.
  - `opts` (optional), a map of:
    - `:hint` an index name (string) hint or specification (map)
    - `:max-time-ms` max amount of time to allow the query to run, in milliseconds
    - `:skip` number of documents to skip before counting
    - `:limit` max number of documents to count
    - `:count-options` a CountOptions, for configuring directly. If specified, any
       other [preceding] query options will be applied to it.
    - `:session` a ClientSession

  Additionally takes options specified in `collection`."
  ([^MongoDatabase db coll]
   (.countDocuments (collection db coll {})))
  ([^MongoDatabase db coll q]
   (count-documents db coll q {}))
  ([^MongoDatabase db coll q opts]
   (let [opts' (->CountOptions opts)]
     (if-let [session (or (:session opts) *session*)]
       (.countDocuments (collection db coll opts) session (document q) opts')
       (.countDocuments (collection db coll opts) (document q) opts')))))

(defn delete-one
  "Deletes a single document from a collection and returns a DeleteResult.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `q` is a map representing a query.
  - `opts` (optional), a map of:
    - `:delete-options` A DeleteOptions for configuring directly.
    - `:session` A ClientSession

  Additionally takes options specified in `collection`"
  ([^MongoDatabase db coll q]
   (delete-one db coll q {}))
  ([^MongoDatabase db coll q opts]
   (if-let [session (or (:session opts) *session*)]
     (.deleteOne (collection db coll opts) session (document q) (->DeleteOptions opts))
     (.deleteOne (collection db coll opts) (document q) (->DeleteOptions opts)))))

(defn delete-many
  "Deletes multiple documents from a collection and returns a DeleteResult.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `q` is a map representing a query.
  - `opts` (optional), a map of:
    - `:delete-options` A DeleteOptions for configuring directly.
    - `:session` A ClientSession

  Additionally takes options specified in `collection`"
  ([^MongoDatabase db coll q]
   (delete-many db coll q {}))
  ([^MongoDatabase db coll q opts]
   (if-let [session (or (:session opts) *session*)]
     (.deleteMany (collection db coll opts) session (document q) (->DeleteOptions opts))
     (.deleteMany (collection db coll opts) (document q) (->DeleteOptions opts)))))

(defn find
  "Finds documents and returns a seq of maps, unless configured otherwise.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `q` is a map representing a query.
  - `opts` (optional), a map of:
    - `:limit` Max number of documents to return, e.g. 1
    - `:skip` Number of documents to skip, e.g. 1
    - `:sort` document representing sort order, e.g. {:timestamp -1}
    - `:projection` document representing fields to return, e.g. {:_id 0}
    - `:keywordize?` keywordize the keys of return results, default: true
    - `:raw?` return the mongo FindIterable directly instead of processing into a seq, default: false
    - `:session` a ClientSession

  Additionally takes options specified in `collection`."
  ([^MongoDatabase db coll q]
   (find db coll q {}))
  ([^MongoDatabase db coll q opts]
   (let [{:keys [limit skip sort projection ^ClientSession session keywordize? raw?] :or {keywordize? true raw? false}} opts]
     (let [^ClientSession session (or session *session*)
           it                     (cond-> (if session
                                            (.find (collection db coll opts) session (document q))
                                            (.find (collection db coll opts) (document q)))
                                          limit (.limit limit)
                                          skip (.skip skip)
                                          sort (.sort (document sort))
                                          projection (.projection (document projection)))]

       (if-not raw?
         (map (fn [x] (from-document x keywordize?)) (seq it))
         it)))))

(defn find-one
  "Finds a single document and returns it as a clojure map, or nil if not found.

  Takes the same options as `find`."
  ([^MongoDatabase db coll q]
   (find-one db coll q {}))
  ([^MongoDatabase db coll q opts]
   (first (find db coll q (assoc opts :limit 1 :raw? false)))))

(defn find-one-and-update
  "Atomically find a document (at most one) and modify it.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `q` is a map representing a query.
  - `update` is either a map representing a document update or a vector
    representing an 'aggregation pipeline'. A document update must include only
    update operators, while an 'aggregation pipeline' can contain multiple
    stages of `$set`, `$unset` and `$replaceWith`.
  - `opts` (optional), a map of:
    - `:upsert?` whether to insert a new document if nothing is found, default: false
    - `:return-new?` whether to return the document after update (insead of its state before the update), default: false
    - `:sort` map representing sort order, e.g. {:timestamp -1}
    - `:projection` map representing fields to return, e.g. {:_id 0}
    - `:find-one-and-update-options` A FindOneAndUpdateOptions for configuring directly. If specified,
    any other [preceding] query options will be applied to it.
    - `:keywordize?` keywordize the keys of return results, default: true
    - `:session` a ClientSession

  Additionally takes options specified in `collection`."
  ([^MongoDatabase db coll q update]
   (find-one-and-update db coll q update {}))
  ([^MongoDatabase db coll q update opts]
   (let [{:keys [keywordize? ^ClientSession session] :or {keywordize? true}} opts
         ^ClientSession session (or session *session*)
         opts'                  (->FindOneAndUpdateOptions opts)]
     (-> (if (instance? List update)
           (let [pipeline ^List (map document update)]
             (if session
               (.findOneAndUpdate (collection db coll opts) session (document q) pipeline opts')
               (.findOneAndUpdate (collection db coll opts) (document q) pipeline opts')))
           (if session
             (.findOneAndUpdate (collection db coll opts) session (document q) (document update) opts')
             (.findOneAndUpdate (collection db coll opts) (document q) (document update) opts')))
         (from-document keywordize?)))))

(defn find-one-and-replace
  "Atomically find a document (at most one) and replace it.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `q` is a map representing a query.
  - `doc` is a new document to add.
  - `opts` (optional), a map of:
    - `:upsert?` whether to insert a new document if nothing is found, default: false
    - `:return-new?` whether to return the document after update (insead of its state before the update), default: false
    - `:sort` map representing sort order, e.g. {:timestamp -1}
    - `:projection` map representing fields to return, e.g. {:_id 0}
    - `:find-one-and-replace-options` A FindOneAndReplaceOptions for configuring directly. If specified,
    any other [preceding] query options will be applied to it.
    - `:keywordize?` keywordize the keys of return results, default: true
    - `:session` a ClientSession

  Additionally takes options specified in `collection`."
  ([^MongoDatabase db coll q doc]
   (find-one-and-replace db coll q doc {}))
  ([^MongoDatabase db coll q doc opts]
   (let [{:keys [keywordize? session] :or {keywordize? true}} opts
         session (or session *session*)
         opts'   (->FindOneAndReplaceOptions opts)]
     (-> (if session
           (.findOneAndReplace (collection db coll opts) session (document q) (document doc) opts')
           (.findOneAndReplace (collection db coll opts) (document q) (document doc) opts'))
         (from-document keywordize?)))))

(defn insert-one
  "Inserts a single document into a collection, and returns nil.
  If the document does not have an _id field, it will be auto-generated by the underlying driver.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `doc` is a map to insert.
  - `opts` (optional), a map of:
    - `:bypass-document-validation?` Boolean
    - `:insert-one-options` An InsertOneOptions for configuring directly. If specified,
       any other [preceding] query options will be applied to it.
    - `:session` A ClientSession

  Additionally takes options specified in `collection`."
  ([^MongoDatabase db coll doc]
   (insert-one db coll doc {}))
  ([^MongoDatabase db coll doc opts]
   (let [opts' (->InsertOneOptions opts)]
     (if-let [session (or (:session opts) *session*)]
       (.insertOne (collection db coll opts) session (document doc) opts')
       (.insertOne (collection db coll opts) (document doc) opts')))))

(defn insert-many
  "Inserts multiple documents into a collection.
  If a document does not have an _id field, it will be auto-generated by the underlying driver.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `docs` is a collection of maps to insert
  - `opts` (optional), a map of:
    - `:bypass-document-validation?` Boolean
    - `:ordered?` Boolean whether serve should insert documents in order provided (default true)
    - `:insert-many-options` An InsertManyOptions for configuring directly. If specified,
      any other [preceding] query options will be applied to it.
    - `:session` A ClientSession

  Additionally takes options specified in `collection`"
  ([^MongoDatabase db coll docs]
   (insert-many db coll docs {}))
  ([^MongoDatabase db coll docs opts]
   (let [opts' (->InsertManyOptions opts)]
     (if-let [^ClientSession session (or (:session opts) *session*)]
       (.insertMany (collection db coll opts) session ^List (map document docs) opts')
       (.insertMany (collection db coll opts) ^List (map document docs) opts')))))

(defn replace-one
  "Replace a single document in a collection and returns an UpdateResult.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `q` is a map representing a query.
  - `doc` is a new document to add.
  - `opts` (optional), a map of:
    - `:upsert?` whether to insert a new document if nothing is found, default: false
    - `:bypass-document-validation?` Boolean
    - `:replace-options` A ReplaceOptions for configuring directly. If specified,
    any other [preceding[ query options will be applied to it.
    - `:session` a ClientSession

  Additionally takes options specified in `collection`"
  ([^MongoDatabase db coll q doc]
   (find-one-and-replace db coll q doc {}))
  ([^MongoDatabase db coll q doc opts]
   (if-let [^ClientSession session (or (:session opts) *session*)]
     (.replaceOne (collection db coll opts) session (document q) (document doc) (->ReplaceOptions opts))
     (.replaceOne (collection db coll opts) (document q) (document doc) (->ReplaceOptions opts)))))

(defn update-one
  "Updates a single document in a collection and returns an UpdateResult.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `q` is a map representing a query.
  - `update` is a map representing an update. The update to apply must include only update operators.
  - `opts` (optional), a map of:
    - `:upsert?` whether to insert a new document if nothing is found, default: false
    - `:bypass-document-validation?` Boolean
    - `:update-options` An UpdateOptions for configuring directly. If specified,
    any other [preceding[ query options will be applied to it.
    - `:session` a ClientSession

  Additionally takes options specified in `collection`"
  ([^MongoDatabase db coll q update]
   (update-one db coll q update {}))
  ([^MongoDatabase db coll q update opts]
   (if-let [^ClientSession session (or (:session opts) *session*)]
     (.updateOne (collection db coll opts) session (document q) (document update) (->UpdateOptions opts))
     (.updateOne (collection db coll opts) (document q) (document update) (->UpdateOptions opts)))))

(defn update-many
  "Updates many documents in a collection and returns an UpdateResult.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `q` is a map representing a query.
  - `update` is a map representing an update. The update to apply must include only update operators.
  - `opts` (optional), a map of:
    - `:upsert?` whether to insert a new document if nothing is found, default: false
    - `:bypass-document-validation?` Boolean
    - `:update-options` An UpdateOptions for configuring directly. If specified,
    any other [preceding[ query options will be applied to it.
    - `:session` a ClientSession

  Additionally takes options specified in `collection`"
  ([^MongoDatabase db coll q update]
   (update-many db coll q update {}))
  ([^MongoDatabase db coll q update opts]
   (if-let [^ClientSession session (or (:session opts) *session*)]
     (.updateMany (collection db coll opts) session (document q) (document update) (->UpdateOptions opts))
     (.updateMany (collection db coll opts) (document q) (document update) (->UpdateOptions opts)))))

;;; Admin functions

(defn create
  "Creates a collection

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `opts` (optional), a map of:
    - `:capped?` Boolean whether to create a capped collection
    - `:max-documents` max documents for a capped collection
    - `:max-size-bytes` max collection size in bytes for a capped collection
    - `:create-collection-options` A CreateCollectionOptions for configuring directly. If specified,
    any other [preceding] query options will be applied to it"
  ([^MongoDatabase db ^String coll]
   (create db coll {}))
  ([^MongoDatabase db ^String coll opts]
   (let [opts' (->CreateCollectionOptions opts)]
     (.createCollection db coll opts'))))

(defn rename
  "Renames `coll` to `new-coll` in the same DB.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `new-coll` is the target collection name
  - `opts` (optional), a map of:
    - `:drop-target?` Boolean drop tne target collection if it exists. Default: false
    - `:rename-collection-options` A RenameCollectionOptions for configuring directly. If specified,
    any other [preceding] query options will be applied to it"
  ([^MongoDatabase db coll new-coll]
   (rename db coll new-coll {}))
  ([^MongoDatabase db coll new-coll opts]
   (let [opts' (->RenameCollectionOptions opts)]

     (.renameCollection (collection db coll opts)
                        (MongoNamespace. (.getName db) new-coll)
                        opts'))))

(defn drop
  "Drops a collection from a database."
  [^MongoDatabase db coll]
  (.drop (collection db coll)))

(defn create-index
  "Creates an index

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `keys` is a document representing index keys, e.g. {:a 1}
  - `opts` (optional), a map of:
    - `:name`
    - `:sparse?`
    - `:unique?`
    - `:index-options` An IndexOptions for configuring directly. If specified,
    any other [preceding] query options will be applied to it"
  ([^MongoDatabase db coll keys]
   (create-index db coll keys {}))
  ([^MongoDatabase db coll keys opts]
   (.createIndex (collection db coll opts) (document keys) (->IndexOptions opts))))

(defn create-indexes
  "Creates many indexes.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `indexes` is a collection of maps with the following keys:
    - `:keys` (mandatory) a document representing index keys, e.g. {:a 1}
    - `:name`
    - `:sparse?`
    - `:unique?`"
  ([^MongoDatabase db coll indexes]
   (create-indexes db coll indexes {}))
  ([^MongoDatabase db coll indexes opts]
   (->> indexes
        (map (fn [x] (IndexModel. (document (:keys x)) (->IndexOptions x))))
        (.createIndexes (collection db coll opts)))))

(defn list-indexes
  "Lists indexes."
  ([^MongoDatabase db coll]
   (list-indexes db coll {}))
  ([^MongoDatabase db coll opts]
   (->> (.listIndexes (collection db coll opts))
        (map #(from-document % true)))))