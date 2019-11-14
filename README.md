# mongo-driver-3

A Mongo client for clojure, lightly wrapping 3.11+ versions of the [MongoDB Java Driver](https://mongodb.github.io/mongo-java-driver/)

In general, it will feel familiar to users of mongo clients like [monger](https://github.com/michaelklishin/monger).
Like our HTTP/2 client [hato](https://github.com/gnarroway/hato), the API is designed to be idiomatic and to make common 
tasks convenient, whilst still allowing the underlying client to be configured via native Java objects.

It was developed with the following goals:

- Up to date with the latest driver versions
- Minimal layer that doesn't block any functionality
- Consistent API across all functions
- Configuration over macros
- Simple


## Status

mongo-driver-3 is under active development but the existing API is unlikely to break.
Please try it out and raise any issues you may find.

## Usage

For Leinengen, add this to your project.clj:

```clojure
;; The underlying driver -- any newer version can also be used
[org.mongodb/mongodb-driver-sync "3.11.0"]

;; This wrapper library
[com.gnarroway/mongo-driver-3 "0.1.0-SNAPSHOT"]
```

## License

Released under the MIT License: http://www.opensource.org/licenses/mit-license.php
