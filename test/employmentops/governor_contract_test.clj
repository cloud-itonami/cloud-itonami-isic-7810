(ns employmentops.governor-contract-test
  "The governor contract as executable tests -- this vertical's own
  Trust Controls ('placement outside consent is blocked; matching
  decisions are explainable') implemented faithfully. The single
  invariant under test:

    EmploymentOps-LLM never matches or places a candidate the
    Employment Agency Governor would reject, `:candidacy/match`/
    `:candidacy/place` NEVER auto-commit at any phase, `:candidacy/
    intake` (no direct candidate-facing risk) MAY auto-commit when
    clean, and every decision (commit OR hold) leaves exactly one
    ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [employmentops.store :as store]
            [employmentops.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :agency-operator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  "Walks `subject` through assess -> approve, leaving an assessment on
  file. Uses distinct thread-ids per call site by suffixing
  `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-assess") {:op :jurisdiction/assess :subject subject} operator)
  (approve! actor (str tid-prefix "-assess")))

(defn- match!
  "Walks `subject` through match -> approve, leaving :matched? true.
  Assumes `assess!` already ran for this subject."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-match") {:op :candidacy/match :subject subject} operator)
  (approve! actor (str tid-prefix "-match")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :candidacy/intake :subject "candidacy-1"
                   :patch {:id "candidacy-1" :candidate "Kita Taro"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Kita Taro" (:candidate (store/candidacy db "candidacy-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest jurisdiction-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :jurisdiction/assess :subject "candidacy-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "candidacy-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a jurisdiction/assess proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :jurisdiction/assess :subject "candidacy-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "candidacy-1")) "no assessment written"))))

(deftest match-without-assessment-is-held
  (testing "candidacy/match before any jurisdiction assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :candidacy/match :subject "candidacy-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest matching-basis-discriminatory-is-held-and-unoverridable
  (testing "a discriminatory matching basis -> HOLD, and never reaches request-approval -- the FLAGSHIP genuinely new check this vertical adds, the 84th unconditional-evaluation-discipline grounding overall, grounded in Japan's own 職業安定法/男女雇用機会均等法, the US's Title VII (EEOC), the UK's Equality Act 2010 (EHRC) and Germany's AGG"
    (let [[db actor] (fresh)
          _ (assess! actor "t5pre" "candidacy-4")
          res (exec-op actor "t5" {:op :candidacy/match :subject "candidacy-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:matching-basis-discriminatory} (-> (store/ledger db) last :basis)))
      (is (empty? (store/match-history db))))))

(deftest placement-fee-mismatch-is-held
  (testing "a claimed placement fee that doesn't equal annual-salary x fee-rate -> HOLD (the ground-truth-recompute discipline every sibling's cost/total-matching check establishes)"
    (let [[db actor] (fresh)
          _ (assess! actor "t6pre" "candidacy-3")
          _ (match! actor "t6pre" "candidacy-3")
          res (exec-op actor "t6" {:op :candidacy/place :subject "candidacy-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:placement-fee-mismatch} (-> (store/ledger db) last :basis)))
      (is (empty? (store/placement-history db))))))

(deftest work-authorization-unverified-is-held-and-unoverridable
  (testing "an unverified work authorization on a work-authorization-required candidacy -> HOLD, and never reaches request-approval -- a genuinely new check, the 85th unconditional-evaluation-discipline grounding overall, the TWELFTH conditional variant (see this actor's governor ns docstring / the full accumulated ADR-0001 chain: parksafety's ADR-2607071922 Decision 5 through leathergoods's, ictrepair's, retailops's, freightops's, quarryops's, agronomyops's, hospitalityops's and practiceops's own)"
    (let [[db actor] (fresh)
          _ (assess! actor "t7pre" "candidacy-5")
          _ (match! actor "t7pre" "candidacy-5")
          res (exec-op actor "t7" {:op :candidacy/place :subject "candidacy-5"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:work-authorization-unverified} (-> (store/ledger db) last :basis)))
      (is (empty? (store/placement-history db))))))

(deftest place-is-a-noop-when-no-work-authorization-required
  (testing "the work-authorization check is CONDITIONAL: a candidacy with no work-authorization requirement has no such requirement at all"
    (let [[_db actor] (fresh)
          _ (assess! actor "t7bpre" "candidacy-1")
          _ (match! actor "t7bpre" "candidacy-1")
          res (exec-op actor "t7b" {:op :candidacy/place :subject "candidacy-1"} operator)]
      (is (= :interrupted (:status res)) "clean placement still escalates for human sign-off, but is NOT a HARD hold"))))

(deftest place-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, matching-fee, no-authorization-required placement still ALWAYS interrupts for human approval -- actuation/place-candidate is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t8pre" "candidacy-1")
          _ (match! actor "t8pre" "candidacy-1")
          r1 (exec-op actor "t8" {:op :candidacy/place :subject "candidacy-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, placement record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:placed? (store/candidacy db "candidacy-1"))))
          (is (= 1 (count (store/placement-history db))) "one draft placement record"))))))

(deftest match-always-escalates-then-human-decides
  (testing "a clean, fully-assessed match still ALWAYS interrupts for human approval -- actuation/match-candidate is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "candidacy-1")
          r1 (exec-op actor "t9" {:op :candidacy/match :subject "candidacy-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, match record drafted"
        (let [r2 (approve! actor "t9")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:matched? (store/candidacy db "candidacy-1"))))
          (is (= 1 (count (store/match-history db))) "one draft match record"))))))

(deftest candidacy-double-match-is-held
  (testing "matching the same candidacy record twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "candidacy-1")
          _ (match! actor "t10pre" "candidacy-1")
          res (exec-op actor "t10" {:op :candidacy/match :subject "candidacy-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-matched} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/match-history db))) "still only the one earlier match"))))

(deftest candidacy-double-placement-is-held
  (testing "placing the same candidacy twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t11pre" "candidacy-1")
          _ (match! actor "t11pre" "candidacy-1")
          _ (exec-op actor "t11a" {:op :candidacy/place :subject "candidacy-1"} operator)
          _ (approve! actor "t11a")
          res (exec-op actor "t11" {:op :candidacy/place :subject "candidacy-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-placed} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/placement-history db))) "still only the one earlier placement"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :candidacy/intake :subject "candidacy-1"
                          :patch {:id "candidacy-1" :candidate "Kita Taro"}} operator)
      (exec-op actor "b" {:op :jurisdiction/assess :subject "candidacy-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
