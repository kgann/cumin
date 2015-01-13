(ns cumin.pagination
  (:require [korma.core :refer :all]
            [robert.hooke :as hooke]))

(def per-page-default 50)

(defn- page-offset [page per-page]
  (* (dec page) per-page))

(defn- total-pages [total per-page]
  (-> (if (zero? total) 1 total)
      (/ per-page)
      (Math/ceil)
      (int)))

(defn- next-page [total-pages curr-page]
  (if (< curr-page total-pages)
    (inc curr-page)))

(defn- prev-page [curr-page]
  (if (> curr-page 1)
    (dec curr-page)))

(defn- select-total [query]
  (-> query
      (dissoc ::pagination :post-queries :limit :offset :order)
      (assoc :fields [])
      (aggregate (count :id) :count)
      (select)
      (ffirst)
      (last)))

(defn- with-page-meta [query coll]
  (let [{:keys [page per-page]} (::pagination query)
        total (select-total query)
        page-count (total-pages total per-page)]
    (vary-meta coll assoc ::pagination {:total total
                                        :per per-page
                                        :curr page
                                        :prev (prev-page page)
                                        :next (next-page page-count page)
                                        :last page-count})))

(defn ^:no-doc exec-paginated
  [f query & args]
  (let [results (apply f query args)]
    (if (and (get-in query [::pagination :info?]) (sequential? results))
      (with-page-meta query results)
      results)))

(hooke/remove-hook #'korma.core/exec ::pagination)
(hooke/add-hook #'korma.core/exec ::pagination #'exec-paginated)

(defn per-page
  "Assoc `per-page` default into entity map

   ```
   (defentity person
     (table :people)
     (per-page 15))
  ```"
  [ent i]
  {:pre [(number? i) (pos? i)]}
  (assoc ent ::per-page i))

(defn paginated?
  "Return true if Korma query map has been paginated"
  [query]
  (contains? query ::pagination))

(defn page-info
  "Return pagination map from result set:

   * `:total` - total number of records for all pages
   * `:per`   - per page argument
   * `:curr`  - current page number
   * `:prev`  - previous page number, nil if no previous page
   * `:next`  - next page number, nil if no next page
   * `:last`  - last page number"
  [coll]
  (::pagination (meta coll)))

(defn paginate
  "Paginate a Korma query

   Options:

   * `:page` - page of query results
   * `:per-page` - number of records to select
   * `:info?` - false to prevent post-query from firing to gather and calculate page info

  ```
  (select person
    (where {:age [> 30]})
    (paginate :page 3 :per-page 25))
  ```"
  [query & {:keys [page per-page info?]
            :or {info? true
                 per-page (get-in query [:ent ::per-page] per-page-default)}}]
  {:pre [(every? number? [page per-page]) (pos? page) (pos? per-page)]}
  (-> query
      (limit per-page)
      (offset (page-offset page per-page))
      (assoc ::pagination {:page page :per-page per-page :info? info?})))
