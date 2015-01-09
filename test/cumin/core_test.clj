(ns cumin.core-test
  (:require [clojure.test :refer :all]
            [cumin.core :refer :all]
            [cumin.test-helper :refer :all]
            [korma.core :refer :all]))

(defentity person)

(deftest re-ordering
  (testing "correct select statement"
    (is (= (with-out-str (dry-run (select person (order :name) (re-order :id))))
           "dry run :: SELECT `person`.* FROM `person` ORDER BY `person`.`id` ASC :: []\n"))))

(deftest post-ordering
  (testing "correct result set ordering"
    (let [post-query-fn (first (:post-queries (post-order {} :name [:c :b :a])))]
      (is (= (post-query-fn [{:name :a :id 2}
                             {:name :b}
                             {:name :c}
                             {:name :x}
                             {:name :a :id 1}
                             {:name :d}
                             {:name :e}])
             [{:name :c}
              {:name :b}
              {:name :a :id 2}
              {:name :a :id 1}
              {:name :x}
              {:name :d}
              {:name :e}])))))
