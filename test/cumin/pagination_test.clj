(ns cumin.pagination-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as sql]
            [cumin.pagination :refer :all]
            [cumin.test-helper :refer :all]
            [korma [core :refer :all]
                   [db :refer :all]]))

(use-transactional-fixtures!)

(defentity person)
(defentity person-with-per-page-default
  (per-page 100))

(deftest entity-per-page
  (testing "per-page"
    (is (= 100 (:cumin.pagination/per-page person-with-per-page-default)))
    (is (nil? (:cumin.pagination/per-page person)))
    (is (thrown? AssertionError (per-page person -1)))
    (is (thrown? AssertionError (per-page person "invalid")))))

(deftest pagination
  (testing "paginated?"
    (is (paginated? (-> (select* person) (paginate :page 1))))
    (is (not (paginated? (select* person)))))

  (testing "paginate"
    (testing "page"
      (is (= 100 (get-in (paginate {} :page 100)
                         [:cumin.pagination/pagination :page])))
      (is (thrown? AssertionError (paginate {} :page -1)))
      (is (thrown? AssertionError (paginate {}))))

    (testing "per-page"
      (is (= 100 (get-in (paginate {} :page 1 :per-page 100)
                         [:cumin.pagination/pagination :per-page])))
      (is (= per-page-default (get-in (paginate {} :page 1)
                                      [:cumin.pagination/pagination :per-page]))))

    (testing "meta?"
      (is (true? (get-in (paginate {} :page 1)
                         [:cumin.pagination/pagination :meta?])))
      (is (false? (get-in (paginate {} :page 1 :meta? false)
                          [:cumin.pagination/pagination :meta?]))))

    (testing "limit"
      (is (= 100 (-> (select* person) (paginate :page 1 :per-page 100) (:limit)))))

    (testing "offset"
      (is (= 0 (-> (select* person) (paginate :page 1 :per-page 100) (:offset))))
      (is (= 100 (-> (select* person) (paginate :page 2 :per-page 100) (:offset))))
      (is (= 200 (-> (select* person) (paginate :page 3 :per-page 100) (:offset)))))))

(deftest integration
  (create-fixtures person
                   {:id 1}
                   {:id 2}
                   {:id 3}
                   {:id 4}
                   {:id 5})
  (testing "metadata"
    (let [r1 (select person (paginate :page 2 :per-page 2))
          r2 (select person (paginate :page 1 :per-page 5))
          r3 (select person (paginate :page 2 :per-page 2 :meta? false))]
      (is (= (page-info r1) {:total 5
                             :per 2
                             :curr 2
                             :prev 1
                             :next 3
                             :last 3}))

      (is (= (page-info r2) {:total 5
                             :per 5
                             :curr 1
                             :prev nil
                             :next nil
                             :last 1}))

      (is (nil? (page-info r3)))))

  (testing "metadata query removes post-queries, limit, offset, and order"
    (is (= (with-out-str
             (dry-run (select person
                              (order :id)
                              (fields :id :age)
                              (post-query (fn [rows] (select person) rows))
                              (paginate :page 1 :per-page 2))))
           (str "dry run :: SELECT `person`.`id`, `person`.`age` FROM `person` ORDER BY `person`.`id` ASC LIMIT 2 OFFSET 0 :: []\n"
                "dry run :: SELECT `person`.* FROM `person` :: []\n"
                "dry run :: SELECT COUNT(`person`.`id`) AS `count` FROM `person` :: []\n")))))
