(ns cumin.test-helper
  (:require [korma [core :refer :all]
                   [db :refer :all]]
            [clojure.java.jdbc :as sql]))

(defdb mem-db
  (h2 {:db "mem:test"
       :make-pool? true
       :delimiters "`"
       :naming {:keys clojure.string/lower-case}}))

(def person-ddl
  (sql/create-table-ddl "person"
    [:id "IDENTITY" "NOT NULL" "PRIMARY KEY"]
    [:age "INTEGER"]))

(def email-ddl
  (sql/create-table-ddl "email"
    [:id "IDENTITY" "NOT NULL" "PRIMARY KEY"]
    [:person_id "INTEGER" "NOT NULL"]
    [:valid "BOOLEAN" "NOT NULL" "DEFAULT" "true"]))

(def email-body-ddl
  (sql/create-table-ddl "email_body"
    [:id "IDENTITY" "NOT NULL" "PRIMARY KEY"]
    [:email_id "INTEGER" "NOT NULL"]
    [:body "VARCHAR"]))

(def schema
  ["DROP TABLE IF EXISTS person;"
   "DROP TABLE IF EXISTS email;"
   "DROP TABLE IF EXISTS email_body;"
   person-ddl
   email-ddl
   email-body-ddl])

(defn create-fixtures [entity & data]
  (insert entity (values data)))

(defmacro use-transactional-fixtures! []
  `(do
     (clojure.test/use-fixtures :once
       (fn [f#]
         (dorun (map exec-raw schema))
         (f#)))

     (clojure.test/use-fixtures :each
       (fn [f#]
         (transaction (try (default-connection mem-db) (f#) (finally (rollback))))))))
