(ns mongo-driver-3.model
  (:import (com.mongodb.client.model CountOptions DeleteOptions ReturnDocument FindOneAndUpdateOptions InsertOneOptions ReplaceOptions UpdateOptions CreateCollectionOptions RenameCollectionOptions InsertManyOptions FindOneAndReplaceOptions IndexOptions BulkWriteOptions DeleteManyModel DeleteOneModel InsertOneModel ReplaceOneModel UpdateManyModel UpdateOneModel)
           (org.bson Document)
           (java.util.concurrent TimeUnit)
           (com.mongodb WriteConcern ReadPreference ReadConcern)
           (clojure.lang Ratio Keyword Named IPersistentMap)
           (java.util Collection List Date)
           (org.bson.types Decimal128)))

(set! *warn-on-reflection* true)

;;; Conversions

(defprotocol ConvertToDocument
  (^Document document [input] "Convert from clojure to Mongo Document"))

(defn read-dates-as-instants! []
  (extend-protocol ConvertToDocument
    Date 
    (from-document [input _]
      (.toInstant ^Date input))))

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

;;; Config

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

(defn ^BulkWriteOptions ->BulkWriteOptions
  "Coerce options map into BulkWriteOptions. See `bulk-write` for usage."
  [{:keys [bulk-write-options bypass-document-validation? ordered?]}]
  (let [^BulkWriteOptions opts (or bulk-write-options (BulkWriteOptions.))]
    (cond-> opts
      (some? bypass-document-validation?) (.bypassDocumentValidation bypass-document-validation?)
      (some? ordered?) (.ordered ordered?))))

(defn ^CountOptions ->CountOptions
  "Coerce options map into CountOptions. See `count-documents` for usage."
  [{:keys [count-options hint limit max-time-ms skip]}]
  (let [^CountOptions opts (or count-options (CountOptions.))]
    (cond-> opts
      hint (.hint (document hint))
      limit (.limit limit)
      max-time-ms (.maxTime max-time-ms (TimeUnit/MILLISECONDS))
      skip (.skip skip))))

(defn ^DeleteOptions ->DeleteOptions
  "Coerce options map into DeleteOptions. See `delete-one` and `delete-many` for usage."
  [{:keys [delete-options]}]
  (let [^DeleteOptions opts (or delete-options (DeleteOptions.))]
    opts))

(defn ^FindOneAndReplaceOptions ->FindOneAndReplaceOptions
  "Coerce options map into FindOneAndReplaceOptions. See `find-one-and-replace` for usage."
  [{:keys [find-one-and-replace-options upsert? return-new? sort projection]}]
  (let [^FindOneAndReplaceOptions opts (or find-one-and-replace-options (FindOneAndReplaceOptions.))]
    (cond-> opts
      (some? upsert?) (.upsert upsert?)
      return-new? (.returnDocument (ReturnDocument/AFTER))
      sort (.sort (document sort))
      projection (.projection (document projection)))))

(defn ^FindOneAndUpdateOptions ->FindOneAndUpdateOptions
  "Coerce options map into FindOneAndUpdateOptions. See `find-one-and-update` for usage."
  [{:keys [find-one-and-update-options upsert? return-new? sort projection]}]
  (let [^FindOneAndUpdateOptions opts (or find-one-and-update-options (FindOneAndUpdateOptions.))]
    (cond-> opts
      (some? upsert?) (.upsert upsert?)
      return-new? (.returnDocument (ReturnDocument/AFTER))
      sort (.sort (document sort))
      projection (.projection (document projection)))))

(defn ^IndexOptions ->IndexOptions
  "Coerces an options map into an IndexOptions.

  See `create-index` for usage"
  [{:keys [index-options name sparse? unique?]}]
  (let [^IndexOptions opts (or index-options (IndexOptions.))]
    (cond-> opts
      name (.name name)
      (some? sparse?) (.sparse sparse?)
      (some? unique?) (.unique unique?))))

(defn ^InsertManyOptions ->InsertManyOptions
  "Coerce options map into InsertManyOptions. See `insert-many` for usage."
  [{:keys [insert-many-options bypass-document-validation? ordered?]}]
  (let [^InsertManyOptions opts (or insert-many-options (InsertManyOptions.))]
    (cond-> opts
      (some? bypass-document-validation?) (.bypassDocumentValidation bypass-document-validation?)
      (some? ordered?) (.ordered ordered?))))

(defn ^InsertOneOptions ->InsertOneOptions
  "Coerce options map into InsertOneOptions. See `insert-one` for usage."
  [{:keys [insert-one-options bypass-document-validation?]}]
  (let [^InsertOneOptions opts (or insert-one-options (InsertOneOptions.))]
    (cond-> opts
      (some? bypass-document-validation?) (.bypassDocumentValidation bypass-document-validation?))))

(defn ^ReplaceOptions ->ReplaceOptions
  "Coerce options map into ReplaceOptions. See `replace-one` and `replace-many` for usage."
  [{:keys [replace-options upsert? bypass-document-validation?]}]
  (let [^ReplaceOptions opts (or replace-options (ReplaceOptions.))]
    (cond-> opts
      (some? upsert?) (.upsert upsert?)
      (some? bypass-document-validation?) (.bypassDocumentValidation bypass-document-validation?))))

(defn ^UpdateOptions ->UpdateOptions
  "Coerce options map into UpdateOptions. See `update-one` and `update-many` for usage."
  [{:keys [update-options upsert? bypass-document-validation?]}]
  (let [^UpdateOptions opts (or update-options (UpdateOptions.))]
    (cond-> opts
      (some? upsert?) (.upsert upsert?)
      (some? bypass-document-validation?) (.bypassDocumentValidation bypass-document-validation?))))

(defn ^CreateCollectionOptions ->CreateCollectionOptions
  "Coerce options map into CreateCollectionOptions. See `create` usage."
  [{:keys [create-collection-options capped? max-documents max-size-bytes]}]
  (let [^CreateCollectionOptions opts (or create-collection-options (CreateCollectionOptions.))]
    (cond-> opts
      (some? capped?) (.capped capped?)
      max-documents (.maxDocuments max-documents)
      max-size-bytes (.sizeInBytes max-size-bytes))))

(defn ^RenameCollectionOptions ->RenameCollectionOptions
  "Coerce options map into RenameCollectionOptions. See `rename` usage."
  [{:keys [rename-collection-options drop-target?]}]
  (let [^RenameCollectionOptions opts (or rename-collection-options (RenameCollectionOptions.))]
    (cond-> opts
      (some? drop-target?) (.dropTarget drop-target?))))

(defmulti write-model
  (fn [[type _]] type))

(defmethod write-model :delete-many
  [[_ opts]]
  (DeleteManyModel. (document (:filter opts)) (->DeleteOptions opts)))

(defmethod write-model :delete-one
  [[_ opts]]
  (DeleteOneModel. (document (:filter opts)) (->DeleteOptions opts)))

(defmethod write-model :insert-one
  [[_ opts]]
  (InsertOneModel. (document (:document opts))))

(defmethod write-model :replace-one
  [[_ opts]]
  (ReplaceOneModel. (document (:filter opts)) (document (:replacement opts)) (->ReplaceOptions opts)))

(defmethod write-model :update-many
  [[_ opts]]
  (UpdateManyModel. (document (:filter opts)) (document (:update opts)) (->UpdateOptions opts)))

(defmethod write-model :update-one
  [[_ opts]]
  (UpdateOneModel. (document (:filter opts)) (document (:update opts)) (->UpdateOptions opts)))
