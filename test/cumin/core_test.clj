(ns cumin.core-test
  (:require [clojure.test :refer :all]
            [cumin.core :refer :all]
            [korma.core :refer :all]))

(defentity person)

(deftest re-ordering
  (testing "correct select statement"
    (is (= (with-out-str (dry-run (select person (order :name) (re-order :id))))
           "dry run :: SELECT `person`.* FROM `person` ORDER BY `person`.`id` ASC :: []\n"))))

(deftest post-ordering
  (testing "correct result set ordering"
    (let [post-query-fn (first (:post-queries (post-order {} :id [3 2 1])))]
      (is (= (post-query-fn [{:id 1} {:id 2} {:id 3} {:id 4} {:id 5} {:id 9}])
           [{:id 3} {:id 2} {:id 1} {:id 4} {:id 5} {:id 9}])))))
