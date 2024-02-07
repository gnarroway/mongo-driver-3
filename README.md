# mongo-driver-3

[![Clojars Project](https://img.shields.io/clojars/v/mongo-driver-3.svg)](https://clojars.org/mongo-driver-3)

[![cljdoc badge](https://cljdoc.org/badge/mongo-driver-3/mongo-driver-3)](https://cljdoc.org/d/mongo-driver-3/mongo-driver-3/CURRENT)


A Mongo client for clojure, lightly wrapping 3.11/4.0+ versions of the [MongoDB Java Driver](https://mongodb.github.io/mongo-java-driver/)

In general, it will feel familiar to users of mongo clients like [monger](https://github.com/michaelklishin/monger).
Like our HTTP/2 client [hato](https://github.com/gnarroway/hato), the API is designed to be idiomatic and to make common 
tasks convenient, whilst still allowing the underlying client to be configured via native Java objects.

It was developed with the following goals:

- Simple
- Up to date with the latest driver versions
- Minimal layer that does not prevent access to the underlying driver
- Consistent API across all functions
- Configuration over macros



## Status

mongo-driver-3 is used in production, and the existing public API will be maintained.
Please try it out and raise any issues you may find.

## Usage

For Leinengen, add this to your project.clj:

```clojure
;; The underlying driver -- any newer version can also be used
[org.mongodb/mongodb-driver-sync "4.11.1"]

;; This wrapper library
[mongo-driver-3 "0.8.0"]
```

## Getting started

```clojure
(ns my.app
  (:require [mongo-driver-3.client :as mcl]))
```

We usually start by creating a client and connecting to a database with a connection string.
`connect-to-db` is a convenience function that allows you to do this directly.

```clojure
(mcl/connect-to-db "mongodb://localhost:27017/my-db")
; =>
; {
;  :client - a MongoClient instance
;  :db - a Database that you can pass to all the collection functions
; } 
```

You can also create a client and get a DB separately:

```clojure 
;; Calling create without an arg will try and connect to the default host/port.
(def client (mcl/create "mongodb://localhost:27017")) 

;; Create a db that you can pass around.
(def db (mcl/get-db client "my-db"))
```

### Collection functions

All the collection functions closely mirror the naming in the corresponding java driver 
[module](https://mongodb.github.io/mongo-java-driver/3.11/javadoc/com/mongodb/client/MongoCollection.html).

They always take a db as the first argument, collection name as the second,
and an optional map of options as the last. Full documentation of options can be found on 
[cljdoc](https://cljdoc.org/d/mongo-driver-3/mongo-driver-3/CURRENT/api/mongo-driver-3.collection).

As an example:

```clojure 
(ns my.app
  (:require [mongo-driver-3.collection :as mc]))
  
;; Insert some documents
(mc/insert-many db "test" [{:v "hello"} {:v "world"}])

;; Count all documents
(mc/count-documents db "test")
; => 2

;; Count with a query
(mc/count-documents db "test" {:v "hello"})
; => 1

;; Find the documents, returning a seq
(mc/find db "test" {} {:limit 1 :projection {:_id 0}})
; => ({:v "hello"})

;; Find the documents, returning the raw FindIterable response
(mc/find db "test" {} {:raw? true})
; => a MongoIterable

;; Find a single document or return nil
(mc/find-one db "test" {:v "world"} {:keywordize? false})
; => {"v" "world"}

;; Avoid laziness in queries 
(mc/find db "test" {} {:realise-fn (partial into [])}
; => [...]
```

While most options are supported directly, sometimes you may need to some extra control.
In such cases, you can pass in a configured java options object. Any other
options will be applied on top of this object.

```clojure 
;; These are equivalent
(mc/rename db "test" "new-test" {:drop-target? true})

(mc/rename db "test" "new-test" {:rename-collection-options (.dropTarget (RenameCollectionOptions.) true)})
```

Again, read the [docs](https://cljdoc.org/d/mongo-driver-3/mongo-driver-3/CURRENT/api/mongo-driver-3.collection)
for full API documentation.

### Using operators

Many mongo queries take operators like `$eq` and `$gt`. These are exposed in the `mongo-driver-3.operator` namespace.

```clojure
(ns my.app
  (:require [mongo-driver-3.collection :as mc]
            [mongo-driver-3.operator :refer [$gt]))
  
(mc/find db "test" {:a {$gt 3}})

;; This is equivalent to, but with less chance of error than:
(mc/find db "test" {:a {"$gt" 3}})
```

### Bulk operations

The bulk API is similar to the [mongo shell](https://docs.mongodb.com/manual/reference/method/db.collection.bulkWrite/),
except each operation is defined as a 2-tuple rather than a map.

```clojure
;; Execute a mix of operations in one go
(bulk-write [[:insert-one {:document {:a 1}}]
             [:delete-one {:filter {:a 1}}]
             [:delete-many {:filter {:a 1}}]
             [:update-one {:filter {:a 1} :update {:$set {:a 2}}}]
             [:update-many {:filter {:a 1} :update {:$set {:a 2}}}]
             [:replace-one {:filter {:a 1} :replacement {:a 2}}]])
; => a BulkWriteResult
             
;; Each operation can take the same options as their respective functions
(bulk-write [[:update-one {:filter {:a 1} :update {:$set {:a 2}} :upsert? true}]
             [:update-many {:filter {:a 1} :update {:$set {:a 2}} :upsert? true}]
             [:replace-one {:filter {:a 1} :replacement {:a 2} :upsert? true}]])
```

### Using transactions

You can create a session to perform multi-document transactions, where all operations either
succeed or none are persisted. 

It is important to
use `with-open` so the session is closed after both successful and failed transactions.

```clojure
;; Inserts 2 documents into a collection
(with-open [s (mg/start-session client)]
  (mg/with-transaction s
    (fn []
      (mc/insert-one my-db "coll" {:name "hello"} {:session s})
      (mc/insert-one my-db "coll" {:name "world"} {:session s}))))

;; There is also a helper method to make this easier,
;; where it is not necessary to manually open or pass a session:
(mg/with-implicit-transaction
  {:client client}
  (fn []
    (mc/insert-one my-db "coll" {:name "hello"}) 
    (mc/insert-one my-db "coll" {:name "world"})))
```

## Development

1. Run mongo (e.g. via docker):
    - `docker run -it --rm -p 27017:27017 mongo`
2. Run tests
    - `lein test`

## License

Released under the MIT License: http://www.opensource.org/licenses/mit-license.php
