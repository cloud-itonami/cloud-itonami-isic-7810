(ns employmentops.registry-test
  (:require [clojure.test :refer [deftest is]]
            [employmentops.registry :as r]))

;; ----------------------------- placement-fee-matches-claim? -----------------------------

(deftest matches-when-claim-equals-recompute
  (is (r/placement-fee-matches-claim?
       {:annual-salary 3000000 :fee-rate 0.2 :claimed-fee 600000.0})))

(deftest mismatches-when-claim-differs-from-recompute
  (is (not (r/placement-fee-matches-claim?
            {:annual-salary 3500000 :fee-rate 0.2 :claimed-fee 800000.0}))))

(deftest compute-placement-fee-is-a-flat-salary-times-rate
  (is (= 600000.0 (r/compute-placement-fee {:annual-salary 3000000 :fee-rate 0.2}))))

;; ----------------------------- register-match -----------------------------

(deftest match-is-a-draft-not-a-real-match
  (let [result (r/register-match "candidacy-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest match-assigns-match-number
  (let [result (r/register-match "candidacy-1" "JPN" 7)]
    (is (= (get result "match_number") "JPN-MTC-000007"))
    (is (= (get-in result ["record" "candidacy_id"]) "candidacy-1"))
    (is (= (get-in result ["record" "kind"]) "match-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest match-validation-rules
  (is (thrown? Exception (r/register-match "" "JPN" 0)))
  (is (thrown? Exception (r/register-match "candidacy-1" "" 0)))
  (is (thrown? Exception (r/register-match "candidacy-1" "JPN" -1))))

;; ----------------------------- register-placement -----------------------------

(deftest placement-is-a-draft-not-a-real-placement
  (let [result (r/register-placement "candidacy-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest placement-assigns-placement-number
  (let [result (r/register-placement "candidacy-1" "JPN" 7)]
    (is (= (get result "placement_number") "JPN-PLC-000007"))
    (is (= (get-in result ["record" "candidacy_id"]) "candidacy-1"))
    (is (= (get-in result ["record" "kind"]) "placement-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest placement-validation-rules
  (is (thrown? Exception (r/register-placement "" "JPN" 0)))
  (is (thrown? Exception (r/register-placement "candidacy-1" "" 0)))
  (is (thrown? Exception (r/register-placement "candidacy-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-match "candidacy-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-match "candidacy-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-MTC-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-MTC-000001" (get-in hist2 [1 "record_id"])))))
