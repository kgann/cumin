(defproject cumin "0.2.0-SNAPSHOT"
  :description "Cumin - some spice for your Korma SQL (pagination, eager loading, query helpers)"
  :url "https://github.com/kgann/cumin"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [robert/hooke "1.3.0"]]

  :profiles {:dev {:plugins [[codox "0.8.10"]]
                   :dependencies [[korma "0.4.0"]
                                  [com.h2database/h2 "1.3.164"]]
                   :codox {:src-dir-uri "http://github.com/kgann/cumin/tree/master/"
                           :src-linenum-anchor-prefix "L"
                           :defaults {:doc/format :markdown}}}})
