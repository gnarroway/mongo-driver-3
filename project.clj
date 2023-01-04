(defproject mongo-driver-3 "0.7.0"
  :plugins [[lein-cljfmt "0.6.4"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.11.1"]
                                  [org.mongodb/mongodb-driver-sync "4.7.1"]]}}
  :repositories [["github" {:url "https://maven.pkg.github.com/getaroom/mongo-driver-3"
                            :username "getaroom"
                            :password :env/GITHUB_TOKEN
                            :sign-releases false}]])
