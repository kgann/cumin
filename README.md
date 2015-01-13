Cumin
=====

Some spice for your [Korma SQL](http://sqlkorma.com) - pagination, eager loading, query helpers

[Korma on Github](https://github.com/korma/Korma)

## ~~Installation~~

~~Add the following dependency to your `project.clj` file:~~

    [cumin "0.1.0"]

## Documentaion
* [API docs](http://kgann.github.io/cumin)

## Usage

```clojure
(ns hello-world
  (:require [korma.core :refer :all]
            [korma.db :refer :all]
            [cumin.core :refer [include post-order re-order]]
            [cumin.pagination :refer [paginate per-page page-info]]))

(defdb db ...)

(defentity person
  (per-page 25))

(defentity email)

(-> (select* person)

    ;; select page 5
    (paginate :page 5)

    ;; apply custom ordering after result set is fetched
    (post-order :name ["Kyle" "Chris"])

    ;; remove any existing order clause and apply new clause
    (re-order :name :asc)

    ;; load emails where `person`.`id` = `email`.`person_id`
    (include email {:id :person_id}
      (where {:valid true})
      (fields :domain :address)))
```

## Pagination

Paginate query using the default `per-page` set on the entity

```clojure
(select person (paginate :page 3))
;; SELECT `person`.* FROM `person` LIMIT 25 OFFSET 50
;; SELECT COUNT(`person`.`id`) AS `count` FROM `person`
```

Paginate query and specify `:per-page`

```clojure
(select person (paginate :page 10 :per-page 100))
;; SELECT `person`.* FROM `person` LIMIT 100 OFFSET 900
;; SELECT COUNT(`person`.`id`) AS `count` FROM `person`
```

Inspect pagination information

```clojure
(page-info (select person (paginate :page 4)))
=> { ... }

:total - total number of records for all pages
:per   - per page argument
:curr  - current page number
:prev  - previous page number, nil if no previous page
:next  - next page number, nil if no next page
:last  - last page number
 ```

*An additional query is required to gather this information*

Prevent additional query and just apply a `limit` and `offset` to query

```clojure
(select (paginate :page 3 :info? false))
;; SELECT `person`.* FROM `person` WHERE `person`.`age` > 30 LIMIT 25 OFFSET 50
;; NO ADDITIONAL QUERY - page-info returns nil
```

## Eager Loading

Eager load records without using Korma relationships. Specify primary and foreign keys inline.

A call to `include` must have an options map containing at least the primary key from the parent and the foreign key of the relationship.

Eagerly load all valid emails

```clojure
(select person
  (include email {:id :person_id}
    (where {:valid true})))
;; => [{:name Kyle
;;      :age 30
;;      :email [{:address "foo@bar.com" :valid true}]}
;;    ... ]
```

Eagerly load invalid emails as `:invalid_email`

```clojure
(select person
  (include email {:id :person_id :as :invalid_email}
    (where {:valid false})))
;; => [{:name Kyle
;;      :age 30
;;      :invalid_email [{:address "bad@invalid.com" :valid false}]}
;;    ... ]
```

Nest `include` calls arbitrarily

```clojure
(defentity email-body)

(select person
  (include email {:id :person_id :as :invalid_email}
    (where {:valid true})
    (include email-body {:id :email_id}
      (fields :body :id))))
;; => [{:name Kyle
;;      :age 30
;;      :email [{:address "foo@bar.com"
;;               :valid true
;;               :email_body [{:body "..." :id 1}]}]}
;;    ... ]
```

Join any information you want and use that for the eager loading

```clojure
(select person
  (fields :* [:emails.id :email_id])
  (join :inner email (= :id :emails.person_id))
  (include email-body {:email_id :email_id}))
;; => [{:name Kyle
;;      :age 30
;;      :email_id 10
;;      :email_body [{:body "..." :id 1 :email_id 10}]}
;;    ... ]
```

## Post Ordering

Order result set based on a `fn` and a collection of values.
Useful when gathering ID's from another resource (Elastic Search) and fetching records from SQL.

```clojure
(def ids [1 3 5 9 7])

(select person
  (where {:id [in ids]})
  (post-order :id ids)) ;; records with ID's other than 1, 3, 5, 9 or 7
                        ;; are appended and retain their original ordering
```

## Re Ordering

```clojure
(def base (-> (select* person) (order :id)))

(select (-> base (re-order :name)))
;; SELECT `person`.* FROM `person` ORDER BY `person`.`name` ASC

(select (-> base (re-order :name :desc)))
;; SELECT `person`.* FROM `person` ORDER BY `person`.`name` DESC
```

## License

Copyright © 2015 Kyle Gann

Distributed under the Eclipse Public License, the same as Clojure.
