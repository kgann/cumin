(ns cumin.core)

(defn- stitch [c1 c2 k [pk fk]]
  (let [res (group-by fk c2)]
    (mapv (fn [row]
            (if-let [rs (get res (pk row))]
              (assoc-in row k rs)
              row))
          c1)))

(defn- includes* [ent mapping body-fn rows]
  (let [[pk fk] (first (dissoc mapping :as))
        as (get mapping :as (keyword (:table ent)))
        pks (distinct (remove nil? (map pk rows)))]
    (if (seq pks)
      (stitch rows
              (select ent
                (body-fn)
                (update-in [:fields] (comp distinct conj) fk)
                (where {fk [in pks]}))
              as
              [pk fk])
      rows)))

(defmacro includes
  "Eager load records with custom relationship mapping"
  [query sub-ent m & body]
  `(post-query ~query
               (partial includes* ~sub-ent ~m (fn [q#]
                                                (-> q# ~@body)))))

(defn force-index
  "Force the use of `index` for `query`"
  [query index]
  (update-in query
             [:from 0 :table]
             (partial format "%s FORCE INDEX (%s)")
             (name index)))

(defn re-order
  "Remove any `order` clause from a query map and replace with `[field dir]`"
  [query field & [dir]]
  (-> query (assoc :order []) (order field dir)))

(defn post-assoc
  "Setup post-query to associate key values for each row"
  [query & kvs]
  (post-query query (partial map #(apply assoc % kvs))))

(defn post-order
  "Setup post-query to order results based ok `f` and `coll`"
  [query f coll]
  (post-query query (fn [rows]
                      (let [grouped (into {} (map (juxt f identity) rows))]
                        (apply conj (mapv grouped coll)
                                    (remove #(some #{(f %)} coll) rows))))))
