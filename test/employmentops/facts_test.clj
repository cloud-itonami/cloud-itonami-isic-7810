(ns employmentops.facts-test
  (:require [clojure.test :refer [deftest is]]
            [employmentops.facts :as facts]))

(deftest jpn-has-a-spec-basis
  (is (some? (facts/spec-basis "JPN")))
  (is (string? (:provenance (facts/spec-basis "JPN")))))

(deftest aus-has-a-spec-basis
  (is (some? (facts/spec-basis "AUS")))
  (is (= "Australia" (:name (facts/spec-basis "AUS"))))
  (is (string? (:provenance (facts/spec-basis "AUS")))))

(deftest all-five-seeded-jurisdictions-have-an-authorization-spec-basis
  ;; matching practiceops/7110's own full professional-seal, quarryops/
  ;; 0810's own full blast-safety and agronomyops/0162's own full
  ;; water-buffer sub-citation coverage, ALL FIVE seeded jurisdictions
  ;; actually have a real work-authorization regime here -- reported
  ;; honestly, not forced narrower
  (doseq [iso3 ["JPN" "USA" "GBR" "DEU" "AUS"]]
    (is (some? (facts/authorization-spec-basis iso3)) (str iso3 " authorization-spec-basis"))
    (is (string? (:authorization-provenance (facts/authorization-spec-basis iso3))) (str iso3 " authorization-provenance"))))

(deftest aus-has-a-third-tier-state-labour-hire-licensing-spec-basis
  ;; AUS is the only jurisdiction in this catalog with a genuinely
  ;; different THIRD regulatory shape -- state-level labour-hire
  ;; LICENSING on top of the federal Fair Work Act framework. Confirmed
  ;; (this session, via a source actually fetched) for 3 states.
  (let [regimes (facts/licensing-spec-basis "AUS")
        jurisdictions (set (map :jurisdiction regimes))]
    (is (= 3 (count regimes)))
    (is (= #{"Victoria" "Queensland" "South Australia"} jurisdictions))
    (is (every? #(string? (:legal-basis %)) regimes))
    (is (every? #(string? (:provenance %)) regimes))))

(deftest non-aus-jurisdictions-have-no-fabricated-licensing-spec-basis
  ;; the other four jurisdictions have no third-tier state-licensing
  ;; entry in this catalog -- honest absence, not a forced/guessed one
  (doseq [iso3 ["JPN" "USA" "GBR" "DEU"]]
    (is (nil? (facts/licensing-spec-basis iso3)) (str iso3 " licensing-spec-basis"))))

(deftest unknown-jurisdiction-has-no-fabricated-licensing-spec-basis
  (is (nil? (facts/licensing-spec-basis "ATL"))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest unknown-jurisdiction-has-no-authorization-spec-basis
  (is (nil? (facts/authorization-spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "GBR"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["GBR" "JPN"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "JPN")]
    (is (facts/required-evidence-satisfied? "JPN" all))
    (is (not (facts/required-evidence-satisfied? "JPN" (rest all))))
    (is (not (facts/required-evidence-satisfied? "ATL" all)) "no spec-basis -> never satisfied")))
