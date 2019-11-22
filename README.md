# mongo-driver-3

[![Clojars Project](https://img.shields.io/clojars/v/mongo-driver-3.svg)](https://clojars.org/mongo-driver-3)

[![cljdoc badge](https://cljdoc.org/badge/mongo-driver-3/mongo-driver-3)](https://cljdoc.org/d/mongo-driver-3/mongo-driver-3/CURRENT)


A Mongo client for clojure, lightly wrapping 3.11+ versions of the [MongoDB Java Driver](https://mongodb.github.io/mongo-java-driver/)

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

mongo-driver-3 is under active development and the API may change.
Please try it out and raise any issues you may find.

## Usage

For Leinengen, add this to your project.clj:

```clojure
;; The underlying driver -- any newer version can also be used
[org.mongodb/mongodb-driver-sync "3.11.2"]

;; This wrapper library
[mongo-driver-3 "0.4.0"]
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

``` clojure
(ns my.app
  (:require [mongo-driver-3.collection :as mc]
            [mongo-driver-3.operator :refer [$gt]))
  
(mc/find db "test" {:a {$gt 3}})

;; This is equivalent to, but with less chance of error than:
(mc/find db "test" {:a {:$gt 3}})
```

## License

Released under the MIT License: http://www.opensource.org/licenses/mit-license.php
