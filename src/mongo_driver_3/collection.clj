(ns mongo-driver-3.collection
  (:refer-clojure :exclude [find empty? drop])
  (:import (clojure.lang Ratio Keyword Named IPersistentMap)
           (com.mongodb ReadConcern ReadPreference WriteConcern MongoNamespace)
           (com.mongodb.client MongoDatabase MongoCollection ClientSession)
           (com.mongodb.client.model InsertOneOptions InsertManyOptions DeleteOptions FindOneAndUpdateOptions ReturnDocument FindOneAndReplaceOptions CountOptions CreateCollectionOptions RenameCollectionOptions IndexOptions IndexModel UpdateOptions ReplaceOptions)
           (java.util List Collection)
           (java.util.concurrent TimeUnit)
           (org.bson Document)
           (org.bson.types Decimal128)))

(set! *warn-on-reflection* true)

;;; Conversions

(defprotocol ConvertToDocument
  (^Document document [input] "Convert from clojure to Mongo Document"))

(extend-protocol ConvertToDocument
  nil
  (document [_]
    nil)

  Ratio
  (document [^Ratio input]
    (double input))

  Keyword
  (document [^Keyword input]
    (.getName input))

  Named
  (document [^Named input]
    (.getName input))

  IPersistentMap
  (document [^IPersistentMap input]
    (let [o (Document.)]
      (doseq [[k v] input]
        (.append o (document k) (document v)))
      o))

  Collection
  (document [^Collection input]
    (map document input))

  Object
  (document [input]
    input))

(defprotocol ConvertFromDocument
  (from-document [input keywordize?] "Converts Mongo Document to Clojure"))

(extend-protocol ConvertFromDocument
  nil
  (from-document [input _]
    input)

  Object
  (from-document [input _] input)

  Decimal128
  (from-document [^Decimal128 input _]
    (.bigDecimalValue input))

  List
  (from-document [^List input keywordize?]
    (vec (map #(from-document % keywordize?) input)))

  Document
  (from-document [^Document input keywordize?]
    (reduce (if keywordize?
              (fn [m ^String k]
                (assoc m (keyword k) (from-document (.get input k) true)))
              (fn [m ^String k]
                (assoc m k (from-document (.get input k) false))))
            {} (.keySet input))))


;;; Collection


(def kw->ReadConcern
  {:available    (ReadConcern/AVAILABLE)
   :default      (ReadConcern/DEFAULT)
   :linearizable (ReadConcern/LINEARIZABLE)
   :local        (ReadConcern/LOCAL)
   :majority     (ReadConcern/MAJORITY)
   :snapshot     (ReadConcern/SNAPSHOT)})

(defn ->ReadConcern
  "Coerce `rc` into a ReadConcern if not nil. See `collection` for usage."
  [{:keys [read-concern]}]
  (when read-concern
    (if (instance? ReadConcern read-concern)
      read-concern
      (or (kw->ReadConcern read-concern) (throw (IllegalArgumentException.
                                                 (str "No match for read concern of " (name read-concern))))))))

(defn ->ReadPreference
  "Coerce `rp` into a ReadPreference if not nil. See `collection` for usage."
  [{:keys [read-preference]}]
  (when read-preference
    (if (instance? ReadPreference read-preference)
      read-preference
      (ReadPreference/valueOf (name read-preference)))))

(defn ^WriteConcern ->WriteConcern
  "Coerces write-concern related options to a WriteConcern. See `collection` for usage."
  [{:keys [write-concern ^Integer write-concern/w ^Long write-concern/w-timeout-ms ^Boolean write-concern/journal?]}]
  (when (some some? [write-concern w w-timeout-ms journal?])
    (let [^WriteConcern wc (when write-concern
                             (if (instance? WriteConcern write-concern)
                               write-concern
                               (WriteConcern/valueOf (name write-concern))))]
      (cond-> (or wc (WriteConcern/ACKNOWLEDGED))
        w (.withW w)
        w-timeout-ms (.withWTimeout w-timeout-ms (TimeUnit/MILLISECONDS))
        (some? journal?) (.withJournal journal?)))))

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
         it (cond-> (if session
                      (.aggregate (collection db coll opts) session ^List (map document pipeline))
                      (.aggregate (collection db coll opts) ^List (map document pipeline)))
              (some? allow-disk-use?) (.allowDiskUse allow-disk-use?)
              (some? bypass-document-validation?) (.bypassDocumentValidation bypass-document-validation?)
              batch-size (.batchSize batch-size))]

     (if-not raw?
       (map (fn [x] (from-document x keywordize?)) (seq it))
       it))))

(defn ^CountOptions ->CountOptions
  "Coerce options map into CountOptions. See `count-documents` for usage."
  [{:keys [count-options hint limit max-time-ms skip]}]
  (let [^CountOptions opts (or count-options (CountOptions.))]
    (cond-> opts
      hint (.hint (document hint))
      limit (.limit limit)
      max-time-ms (.maxTime max-time-ms (TimeUnit/MILLISECONDS))
      skip (.skip skip))))

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
     (if-let [session (:session opts)]
       (.countDocuments (collection db coll opts) session (document q) opts')
       (.countDocuments (collection db coll opts) (document q) opts')))))

(defn ^DeleteOptions ->DeleteOptions
  "Coerce options map into DeleteOptions. See `delete-one` and `delete-many` for usage."
  [{:keys [delete-options]}]
  (let [^DeleteOptions opts (or delete-options (DeleteOptions.))]
    opts))

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
   (if-let [session (:session opts)]
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
   (if-let [session (:session opts)]
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
     (let [it (cond-> (if session
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

(defn ^FindOneAndUpdateOptions ->FindOneAndUpdateOptions
  "Coerce options map into FindOneAndUpdateOptions. See `find-one-and-update` for usage."
  [{:keys [find-one-and-update-options upsert? return-new? sort projection]}]
  (let [^FindOneAndUpdateOptions opts (or find-one-and-update-options (FindOneAndUpdateOptions.))]
    (cond-> opts
      (some? upsert?) (.upsert upsert?)
      return-new? (.returnDocument (ReturnDocument/AFTER))
      sort (.sort (document sort))
      projection (.projection (document projection)))))

(defn find-one-and-update
  "Atomically find a document (at most one) and modify it.

  Arguments:

  - `db` is a MongoDatabase
  - `coll` is a collection name
  - `q` is a map representing a query.
  - `update` is a map representing an update. The update to apply must include only update operators.
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
         opts' (->FindOneAndUpdateOptions opts)]
     (-> (if session
           (.findOneAndUpdate (collection db coll opts) session (document q) (document update) opts')
           (.findOneAndUpdate (collection db coll opts) (document q) (document update) opts'))
         (from-document keywordize?)))))

(defn ^FindOneAndReplaceOptions ->FindOneAndReplaceOptions
  "Coerce options map into FindOneAndReplaceOptions. See `find-one-and-replace` for usage."
  [{:keys [find-one-and-replace-options upsert? return-new? sort projection]}]
  (let [^FindOneAndReplaceOptions opts (or find-one-and-replace-options (FindOneAndReplaceOptions.))]
    (cond-> opts
      (some? upsert?) (.upsert upsert?)
      return-new? (.returnDocument (ReturnDocument/AFTER))
      sort (.sort (document sort))
      projection (.projection (document projection)))))

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
         opts' (->FindOneAndReplaceOptions opts)]
     (-> (if session
           (.findOneAndReplace (collection db coll opts) session (document q) (document doc) opts')
           (.findOneAndReplace (collection db coll opts) (document q) (document doc) opts'))
         (from-document keywordize?)))))

(defn ^InsertOneOptions ->InsertOneOptions
  "Coerce options map into InsertOneOptions. See `insert-one` for usage."
  [{:keys [insert-one-options bypass-document-validation?]}]
  (let [^InsertOneOptions opts (or insert-one-options (InsertOneOptions.))]
    (when (some? bypass-document-validation?) (.bypassDocumentValidation opts bypass-document-validation?))

    opts))

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
     (if-let [session (:session opts)]
       (.insertOne (collection db coll opts) session (document doc) opts')
       (.insertOne (collection db coll opts) (document doc) opts')))))

(defn ^InsertManyOptions ->InsertManyOptions
  "Coerce options map into InsertManyOptions. See `insert-many` for usage."
  [{:keys [insert-many-options bypass-document-validation? ordered?]}]
  (let [^InsertManyOptions opts (or insert-many-options (InsertManyOptions.))]
    (cond-> opts
      (some? bypass-document-validation?) (.bypassDocumentValidation bypass-document-validation?)
      (some? ordered?) (.ordered ordered?))))

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
     (if-let [^ClientSession session (:session opts)]
       (.insertMany (collection db coll opts) session ^List (map document docs) opts')
       (.insertMany (collection db coll opts) ^List (map document docs) opts')))))

(defn ^ReplaceOptions ->ReplaceOptions
  "Coerce options map into ReplaceOptions. See `replace-one` and `replace-many` for usage."
  [{:keys [replace-options upsert? bypass-document-validation?]}]
  (let [^ReplaceOptions opts (or replace-options (ReplaceOptions.))]
    (cond-> opts
      (some? upsert?) (.upsert upsert?)
      (some? bypass-document-validation?) (.bypassDocumentValidation bypass-document-validation?))))

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
   (if-let [^ClientSession session (:session opts)]
     (.replaceOne (collection db coll opts) session (document q) (document doc) (->ReplaceOptions opts))
     (.replaceOne (collection db coll opts) (document q) (document doc) (->ReplaceOptions opts)))))

(defn ^UpdateOptions ->UpdateOptions
  "Coerce options map into UpdateOptions. See `update-one` and `update-many` for usage."
  [{:keys [update-options upsert? bypass-document-validation?]}]
  (let [^UpdateOptions opts (or update-options (UpdateOptions.))]
    (cond-> opts
      (some? upsert?) (.upsert upsert?)
      (some? bypass-document-validation?) (.bypassDocumentValidation bypass-document-validation?))))

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
   (if-let [^ClientSession session (:session opts)]
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
   (if-let [^ClientSession session (:session opts)]
     (.updateMany (collection db coll opts) session (document q) (document update) (->UpdateOptions opts))
     (.updateMany (collection db coll opts) (document q) (document update) (->UpdateOptions opts)))))

;;; Admin functions

(defn ^CreateCollectionOptions ->CreateCollectionOptions
  "Coerce options map into CreateCollectionOptions. See `create` usage."
  [{:keys [create-collection-options capped? max-documents max-size-bytes]}]
  (let [^CreateCollectionOptions opts (or create-collection-options (CreateCollectionOptions.))]
    (cond-> opts
      (some? capped?) (.capped capped?)
      max-documents (.maxDocuments max-documents)
      max-size-bytes (.sizeInBytes max-size-bytes))))

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

(defn ^RenameCollectionOptions ->RenameCollectionOptions
  "Coerce options map into RenameCollectionOptions. See `rename` usage."
  [{:keys [rename-collection-options drop-target?]}]
  (let [^RenameCollectionOptions opts (or rename-collection-options (RenameCollectionOptions.))]
    (cond-> opts
      (some? drop-target?) (.dropTarget drop-target?))))

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

(defn ^IndexOptions ->IndexOptions
  "Coerces an options map into an IndexOptions.

  See `create-index` for usage"
  [{:keys [index-options name sparse? unique?]}]
  (let [^IndexOptions opts (or index-options (IndexOptions.))]
    (cond-> opts
      name (.name name)
      (some? sparse?) (.sparse sparse?)
      (some? unique?) (.unique unique?))))

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