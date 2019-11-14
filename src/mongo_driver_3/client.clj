(ns mongo-driver-3.client
  (:refer-clojure :exclude [find])
  (:import (com.mongodb.client MongoClients MongoClient)
           (com.mongodb ConnectionString)))

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