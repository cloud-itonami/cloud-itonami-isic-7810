(ns employmentops.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [employmentops.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "JPN" (:jurisdiction (store/candidacy s "candidacy-1"))))
      (is (= 600000.0 (:claimed-fee (store/candidacy s "candidacy-1"))))
      (is (false? (:matching-criteria-discriminatory? (store/candidacy s "candidacy-1"))))
      (is (false? (:requires-work-authorization? (store/candidacy s "candidacy-1"))))
      (is (= 800000.0 (:claimed-fee (store/candidacy s "candidacy-3"))))
      (is (true? (:matching-criteria-discriminatory? (store/candidacy s "candidacy-4"))))
      (is (true? (:requires-work-authorization? (store/candidacy s "candidacy-5"))))
      (is (false? (:work-authorization-verified? (store/candidacy s "candidacy-5"))))
      (is (true? (:work-authorization-verified? (store/candidacy s "candidacy-6"))))
      (is (false? (:matched? (store/candidacy s "candidacy-1"))))
      (is (false? (:placed? (store/candidacy s "candidacy-1"))))
      (is (= ["candidacy-1" "candidacy-2" "candidacy-3" "candidacy-4" "candidacy-5" "candidacy-6"]
             (mapv :id (store/all-candidacies s))))
      (is (nil? (store/assessment-of s "candidacy-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/match-history s)))
      (is (= [] (store/placement-history s)))
      (is (zero? (store/next-match-sequence s "JPN")))
      (is (zero? (store/next-placement-sequence s "JPN")))
      (is (false? (store/candidacy-already-matched? s "candidacy-1")))
      (is (false? (store/candidacy-already-placed? s "candidacy-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :candidacy/upsert
                                 :value {:id "candidacy-1" :candidate "Kita Taro"}})
        (is (= "Kita Taro" (:candidate (store/candidacy s "candidacy-1"))))
        (is (= 600000.0 (:claimed-fee (store/candidacy s "candidacy-1"))) "unrelated field preserved"))
      (testing "assessment payloads commit and read back"
        (store/commit-record! s {:effect :assessment/set :path ["candidacy-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/assessment-of s "candidacy-1"))))
      (testing "match drafts a record and advances the match sequence"
        (store/commit-record! s {:effect :candidacy/mark-matched :path ["candidacy-1"]})
        (is (= "JPN-MTC-000000" (get (first (store/match-history s)) "record_id")))
        (is (= "match-draft" (get (first (store/match-history s)) "kind")))
        (is (true? (:matched? (store/candidacy s "candidacy-1"))))
        (is (= 1 (count (store/match-history s))))
        (is (= 1 (store/next-match-sequence s "JPN")))
        (is (true? (store/candidacy-already-matched? s "candidacy-1"))))
      (testing "placement drafts a record and advances the placement sequence"
        (store/commit-record! s {:effect :candidacy/mark-placed :path ["candidacy-1"]})
        (is (= "JPN-PLC-000000" (get (first (store/placement-history s)) "record_id")))
        (is (= "placement-draft" (get (first (store/placement-history s)) "kind")))
        (is (true? (:placed? (store/candidacy s "candidacy-1"))))
        (is (= 1 (count (store/placement-history s))))
        (is (= 1 (store/next-placement-sequence s "JPN")))
        (is (true? (store/candidacy-already-placed? s "candidacy-1"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/candidacy s "nope")))
    (is (= [] (store/all-candidacies s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/match-history s)))
    (is (= [] (store/placement-history s)))
    (is (zero? (store/next-match-sequence s "JPN")))
    (is (zero? (store/next-placement-sequence s "JPN")))
    (store/with-candidacies s {"x" {:id "x" :candidate "c" :job-title "j"
                                    :annual-salary 1 :fee-rate 1.0 :claimed-fee 1.0
                                    :matching-criteria-discriminatory? false
                                    :requires-work-authorization? false :work-authorization-verified? false
                                    :matched? false :placed? false
                                    :jurisdiction "JPN" :status :intake}})
    (is (= "c" (:candidate (store/candidacy s "x"))))))
