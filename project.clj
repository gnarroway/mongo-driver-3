(defproject mongo-driver-3 "0.7.0"
  :description "A Clojure wrapper for the Java MongoDB driver 3.11/4.0+."
  :url "https://github.com/gnarroway/mongo-driver-3"
  :license {:name         "The MIT License"
            :url          "http://opensource.org/licenses/mit-license.php"
            :distribution :repo}
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo"
                                    :username      :env/clojars_user
                                    :password      :env/clojars_pass
                                    :sign-releases false}]]
  :plugins [[lein-cljfmt "0.6.4"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.11.1"]
                                  [org.mongodb/mongodb-driver-sync "4.2.3"]]}})
