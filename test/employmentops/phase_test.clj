(ns employmentops.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:candidacy/match`/`:candidacy/place` must NEVER be a
  member of any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [employmentops.phase :as phase]))

(deftest candidacy-match-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real candidate match"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :candidacy/match))
          (str "phase " n " must not auto-commit :candidacy/match")))))

(deftest candidacy-place-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real candidate placement"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :candidacy/place))
          (str "phase " n " must not auto-commit :candidacy/place")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-candidate-facing-risk-ops
  (testing ":candidacy/intake carries no direct candidate-facing risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:candidacy/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :candidacy/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :candidacy/match} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :candidacy/place} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :candidacy/intake} :commit)))))
