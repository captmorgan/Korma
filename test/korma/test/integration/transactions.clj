(ns korma.test.integration.transactions
  (:use clojure.test
        korma.db
        korma.core))

(def mem-db (create-db (h2 {:db "mem:query_database"})))

(def mem-db-2 (create-db (h2 {:db "mem:query_database_2"})))

(defentity user
  (table :user)
  (database mem-db))

(defentity user-2
  (table :user)
  (database mem-db-2))

(def schema
  ["drop table if exists \"user\";"
   "create table \"user\" (\"id\" integer primary key,
                           \"name\" varchar(100));"])

(defn- setup-db []
  (with-db mem-db
    (dorun (map exec-raw schema))
    (insert user (values {:id 1 :name "Chris"})))
  (with-db mem-db-2
    (dorun (map exec-raw schema))
    (insert user-2 (values {:id 1 :name "Paul"}))))

(use-fixtures :once (fn [f]
                      (default-connection nil)
                      (setup-db)
                      (f)))

(defn delete-some-rows-from-db []
  (delete user))

; (deftest wacky-with-db
;   (with-db mem-db
;       (is (= [{:id 1 :name "Paul"}]  ; should this be Chris?
;          (select user-2)))))

(deftest transaction-works
  (with-db mem-db 
    (transaction (delete-some-rows-from-db)))

  (is (= []
         (select user)))
  (is (= [{:id 1 :name "Paul"}]
         (select user-2))))

(deftest transaction-rollback-works
  (with-db mem-db 
    (transaction (delete user-2) (rollback)))

  (is (= [{:id 1 :name "Chris"}]
         (select user)))
  (is (= [{:id 1 :name "Paul"}]
         (select user-2))))


; uncommenting this last test causes all tests to pass?

; (deftest transaction-rollback-for-other-db-works
;   (with-db mem-db (transaction 
;         (with-db mem-db-2 (transaction (delete user) (delete user-2) (rollback)))))

;   (is (= [{:id 1 :name "Chris"}]
;          (select user
;            (database mem-db))))
;   (is (= [{:id 1 :name "Paul"}]
;          (select user
;            (database mem-db-2)))))
