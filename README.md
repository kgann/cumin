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
(-> base (paginate :page 2))
```

Paginate query and specify `:per-page`

```clojure
(-> base (paginate :page 10 :per-page 100))
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
(-> base (paginate :page 3 :meta? false))
```

## License

Copyright © 2015 Kyle Gann

Distributed under the Eclipse Public License, the same as Clojure.
