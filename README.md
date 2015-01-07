cumin
=====

Some spice for your Korma SQL

## Installation

Add the following dependency to your `project.clj` file:

    [cumin "0.1.0"]

## Pagination

```clojure
(use 'cumin.pagination)

(defentity person
  (per-page 25))

(def base
  (-> (select* person)
      (where {:age [> 30]})))
```

Paginate query using the default `per-page` set on the entity

```clojure
(-> base (paginate :page 3) (select))
;; SELECT `person`.* FROM `person` WHERE `person`.`age` > 30 LIMIT 25 OFFSET 50
;; SELECT COUNT(`person`.`id`) AS `count` FROM `person`
```

Paginate query and specify `:per-page`

```clojure
(-> base (paginate :page 10 :per-page 100) (select))
;; SELECT `person`.* FROM `person` WHERE `person`.`age` > 30 LIMIT 100 OFFSET 900
;; SELECT COUNT(`person`.`id`) AS `count` FROM `person`
```

Result sets contain metadata with key `:page` containing:

```clojure
{:total - total number of records for all pages
 :per   - per page argument
 :curr  - current page number
 :prev  - previous page number, nil if no previous page
 :next  - next page number, nil if no next page
 :last  - last page number}
 ```

*An additional query is required to gather this information*

Prevent additional query and just apply a `limit` and `offset` to query

```clojure
(-> base (paginate :page 3 :meta? false) (select))
;; SELECT `person`.* FROM `person` WHERE `person`.`age` > 30 LIMIT 25 OFFSET 50
;; NO ADDITIONAL QUERY
```

## Eager Loading

Eager load records without using Korma relationships. Specify primary and foreign keys inline.

A call to `include` must have an options map containing at least the primary key from the parent and the foreign key of the relationship.

```clojure
(use 'cumin.core)

(defentity person)
(defentity email)
(defentity email-body)
```

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

Eagerly load invalid emails and store in `:invalid_email` key

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
(use 'cumin.core)

(defentity person)

(select person
  (post-order :id [1 3 5])) ;; records with ID's other than 1,3 or 5
                            ;; are appended and retain their original ordering
```

## Re Ordering

```clojure
(use 'cumin.core)

(defentity person)

(def base (-> (select* person) (order :id)))

(select (-> base (re-order :name)))
;; SELECT `person`.* FROM `person` ORDER BY `person`.`name` ASC

(select (-> base (re-order :name :desc)))
;; SELECT `person`.* FROM `person` ORDER BY `person`.`name` DESC
```

## License

Copyright © 2015 Kyle Gann

Distributed under the Eclipse Public License, the same as Clojure.
