(ns cumin.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [cumin.core :refer :all]
            [cumin.test-helper :refer :all]
            [korma.core :refer :all]))

(use-transactional-fixtures!)

(defn- map-keys [f m]
  (reduce-kv
    (fn [acc k v] (assoc acc (f k) v))
    {}
    m))

(defn- snake-case-keys [m]
  (map-keys #(keyword (string/replace (name %) "-" "_"))
            m))

(defn- kebab-case-keys [m]
  (map-keys #(keyword (string/replace (name %) "_" "-"))
            m))

(defentity person)
(defentity email)
(defentity address
  (prepare snake-case-keys)
  (transform kebab-case-keys))

(defentity valid-email
  (table "email")
  (scope (where {:valid true})
         (order :id :desc)))

(defscoped [deleted-email valid-email]
  (scope (where {:deleted true})))

(defentity email-body
  (table "email_body"))

(deftest default-scope
  (testing "scoped?"
    (is (true? (scoped? valid-email))))
  (testing "defscoped"
    (is (= (with-out-str (dry-run (select deleted-email)))
           "dry run :: SELECT `email`.* FROM `email` WHERE (`email`.`valid` = ?) AND (`email`.`deleted` = ?) ORDER BY `email`.`id` DESC :: [true true]\n")))
  (testing "scope"
    (is (= (with-out-str (dry-run (select valid-email)))
           "dry run :: SELECT `email`.* FROM `email` WHERE (`email`.`valid` = ?) ORDER BY `email`.`id` DESC :: [true]\n"))
    (is (= (sql-only (select valid-email))
           "SELECT `email`.* FROM `email` WHERE (`email`.`valid` = ?) ORDER BY `email`.`id` DESC"))))

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

(deftest eager-loading
  (testing "query generation"
    (is (= (with-out-str (dry-run (select person (where {:age [> 30]})
                                          (include email {:id :person_id}
                                            (fields :valid)
                                            (where {:valid true})))))
           (str "dry run :: SELECT `person`.* FROM `person` WHERE (`person`.`age` > ?) :: [30]\n"
                "dry run :: SELECT `email`.`valid`, `email`.`person_id` FROM `email` WHERE (`email`.`valid` = ?) AND (`email`.`person_id` IN (?)) :: [true 1]\n")))))

(deftest eager-loading-integration
  (create-fixtures person
                   {:id 1 :age 30}
                   {:id 2 :age 40})
  (create-fixtures email
                   {:id 1 :person_id 1 :valid true}
                   {:id 2 :person_id 1 :valid false}
                   {:id 3 :person_id 2 :valid true}
                   {:id 4 :person_id 2 :valid false})
  (create-fixtures email-body
                   {:id 1 :email_id 1 :body "email 1"}
                   {:id 2 :email_id 2 :body "email 2"})
  (create-fixtures address
                   {:id 1 :person_id 1 :line_1 "123 Test Lane"}
                   {:id 2 :person_id 1 :line_1 "234 Test Lane"}
                   {:id 3 :person_id 2 :line_1 "987 Test Lane"}
                   {:id 4 :person_id 2 :line_1 "876 Test Lane"})

  (testing "include"
    (is (= (select person (include email {:id :person_id}))
           [{:id 1 :age 30 :email [{:id 1 :person_id 1 :valid true}
                                   {:id 2 :person_id 1 :valid false}]}
            {:id 2 :age 40 :email [{:id 3 :person_id 2 :valid true}
                                   {:id 4 :person_id 2 :valid false}]}])))

  (testing "include with transforms/prepares"
    (is (= (select person (include address {:id :person_id}))
           [{:id 1 :age 30 :address [{:id 1 :person-id 1 :line-1 "123 Test Lane"}
                                     {:id 2 :person-id 1 :line-1 "234 Test Lane"}]}
            {:id 2 :age 40 :address [{:id 3 :person-id 2 :line-1 "987 Test Lane"}
                                     {:id 4 :person-id 2 :line-1 "876 Test Lane"}]}])))

  (testing "include :as"
    (is (= (select person (include email {:id :person_id :as :person_email}))
           [{:id 1 :age 30 :person_email [{:id 1 :person_id 1 :valid true}
                                          {:id 2 :person_id 1 :valid false}]}
            {:id 2 :age 40 :person_email [{:id 3 :person_id 2 :valid true}
                                          {:id 4 :person_id 2 :valid false}]}])))

  (testing "include query modifications"
    (is (= (select person (include email {:id :person_id :as :valid_emails}
                            (where {:valid true})))
           [{:id 1 :age 30 :valid_emails [{:id 1 :person_id 1 :valid true}]}
            {:id 2 :age 40 :valid_emails [{:id 3 :person_id 2 :valid true}]}])))

  (testing "include using join data"
    (is (= (select person
             (fields :id [:email.id :email_id_alias])
             (join :inner email (= :id :email.person_id))
             (where {:email.valid true})
             (include email-body {:email_id_alias :email_id}))
           [{:id 1 :email_id_alias 1 :email_body [{:id 1 :email_id 1 :body "email 1"}]}
            {:id 2 :email_id_alias 3}]))))
