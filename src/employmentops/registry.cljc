(ns employmentops.registry
  "Pure-function candidate-matching + candidate-placement record
  construction -- an append-only employment-agency book-of-record
  draft.

  Like every sibling actor's registry, there is no single international
  reference-number standard for a matching or placement record --
  every agency/jurisdiction assigns its own reference format. This
  namespace does NOT invent one; it builds a jurisdiction-scoped
  sequence number and validates the record's required fields, the
  same honest, non-fabricating discipline `employmentops.facts` uses.

  `placement-fee-matches-claim?` is an HONEST reapplication of the
  SAME ground-truth-recompute DISCIPLINE `practiceops.registry`'s own
  `fee-total-matches-claim?`, `hospitalityops.registry`'s own `folio-
  total-matches-claim?`, `agronomyops.registry`'s own `dose-matches-
  claim?` and `quarryops.registry`'s own `royalty-matches-claim?`
  establish (verify a claimed monetary total against the entity's own
  recorded quantity x unit fields), reapplied to a candidacy's
  placement-fee line rather than a professional-fee, folio, dose or
  royalty line -- not claimed as new code, though no literal code is
  shared (different domain).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real applicant-tracking system. It builds the RECORD an
  operator would keep, not the act of matching or placing a candidate
  itself (that is `employmentops.operation`'s `:candidacy/match`/
  `:candidacy/place`, always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the employment-agency operator's act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn compute-placement-fee
  "The ground-truth placement fee for `candidacy`'s own `:annual-
  salary` and `:fee-rate` -- a single flat salary x rate calculation,
  not a full commission/tiered-fee engine."
  [{:keys [annual-salary fee-rate]}]
  (* (double annual-salary) (double fee-rate)))

(defn placement-fee-matches-claim?
  "Does `candidacy`'s own `:claimed-fee` equal the independently
  recomputed `compute-placement-fee`? A pure ground-truth check
  against the candidacy's own permanent fields -- see ns docstring
  for why this is an honest reapplication of the SAME discipline
  every sibling actor's own cost/total-matching check establishes,
  not a new concept."
  [{:keys [claimed-fee] :as candidacy}]
  (== (double claimed-fee) (compute-placement-fee candidacy)))

(defn register-match
  "Validate + construct the CANDIDATE-MATCHING registration DRAFT --
  the employment-agency operator's own act of matching a real
  candidate to a real job opportunity. Pure function -- does not
  touch any real applicant-tracking system; it builds the RECORD an
  operator would keep. `employmentops.governor` independently
  re-verifies the candidacy's own matching-criteria ground truth, and
  blocks a double-match of the same record, before this is ever
  allowed to commit."
  [candidacy-id jurisdiction sequence]
  (when-not (and candidacy-id (not= candidacy-id ""))
    (throw (ex-info "match: candidacy_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "match: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "match: sequence must be >= 0" {})))
  (let [match-number (str (str/upper-case jurisdiction) "-MTC-" (zero-pad sequence 6))
        record {"record_id" match-number
                "kind" "match-draft"
                "candidacy_id" candidacy-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "match_number" match-number
     "certificate" (unsigned-certificate "CandidateMatch" match-number match-number)}))

(defn register-placement
  "Validate + construct the CANDIDATE-PLACEMENT registration DRAFT --
  the employment-agency operator's own act of placing a real
  candidate into a real job (triggering placement-fee accrual). Pure
  function -- does not touch any real applicant-tracking system; it
  builds the RECORD an operator would keep. `employmentops.governor`
  independently re-verifies the candidacy's own fee/consent/work-
  authorization ground truth, and blocks a double-placement of the
  same record, before this is ever allowed to commit."
  [candidacy-id jurisdiction sequence]
  (when-not (and candidacy-id (not= candidacy-id ""))
    (throw (ex-info "placement: candidacy_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "placement: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "placement: sequence must be >= 0" {})))
  (let [placement-number (str (str/upper-case jurisdiction) "-PLC-" (zero-pad sequence 6))
        record {"record_id" placement-number
                "kind" "placement-draft"
                "candidacy_id" candidacy-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "placement_number" placement-number
     "certificate" (unsigned-certificate "CandidatePlacement" placement-number placement-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
