(ns cumin.core
  (:require [korma.core :refer :all]
            [robert.hooke :as hooke]))

(defn- stitch [c1 c2 k [pk fk]]
  (let [res (group-by fk c2)]
    (mapv (fn [row]
            (if-let [rs (get res (pk row))]
              (assoc row k rs)
              row))
          c1)))

(defn ^:no-doc include* [ent mapping body-fn rows]
  (let [[pk fk] (first (dissoc mapping :as))
        pks (distinct (remove nil? (map pk rows)))
        sub-q (-> (select* ent) (body-fn))]
    (if (seq pks)
      (stitch rows
              (-> sub-q
                  (cond-> (not-any? #{:* :korma.core/* fk} (:fields sub-q))
                          (update-in [:fields] conj fk))
                  (where {fk [in pks]})
                  (select))
              (get mapping :as (keyword (:table ent)))
              [pk fk])
      rows)))

(defmacro include
  "Eager load records with custom relationship mapping

  ```
  (select person
    (include email {:id :email_id :as person_emails}
      (where {:valid true})))
  ```'"
  [query sub-ent m & body]
  `(post-query ~query
               (partial include* ~sub-ent ~m (fn [q#]
                                               (-> q# ~@body)))))

(defn scoped?
  "Return true if Korma entity has a default scope applied"
  [ent]
  (contains? ent ::scope))

(defmacro scope
  "Apply a default query body for all queries executed for entity `ent`

  ```
  (defentity teenager
    (table :person)
    (scope
      (where {:age [> 12] :age [< 20]})
      (order :name :desc)))
  ```"
  [ent & body]
  `(assoc ~ent ::scope (fn [q#] (-> q# ~@body))))

(defn ^:no-doc select-scoped* [f & [ent :as args]]
  (let [query (apply f args)]
    (if (scoped? ent)
      ((::scope ent) query)
      query)))

(hooke/remove-hook #'korma.core/select* ::scope)
(hooke/add-hook #'korma.core/select* ::scope #'select-scoped*)

(defn re-order
  "Remove any `order` clause from a query map and replace with `[field dir]`

  ```
  (-> (select* person)
      (order :name)
      (re-order :id))
  ```"
  [query field & [dir]]
  (-> query (assoc :order []) (order field dir)))


(defn post-order
  "Setup post-query to order results based on `f` and `coll`
   any record where (f record) is not in `coll` will be appended
   in their original SQL ordering.

  ```
  (select person
    (post-order :id [5 4 3 2 1]))
  ```"
  [query f coll]
  (post-query query (fn [rows]
                      (apply conj
                             (vec (mapcat (group-by f rows) coll))
                             (remove #(some #{(f %)} coll) rows)))))
