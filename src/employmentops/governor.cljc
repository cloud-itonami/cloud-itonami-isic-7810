(ns employmentops.governor
  "Employment Agency Governor -- the independent compliance layer that
  earns the EmploymentOps-LLM the right to commit. The LLM has no
  notion of jurisdictional anti-discrimination or work-authorization
  law, whether a candidacy's own claimed placement fee actually
  equals annual salary times fee rate, whether a proposed match's own
  criteria actually rely on a protected characteristic, whether a
  candidate's own work authorization has actually been verified for a
  placement that legally requires it, or when an act stops being a
  draft and becomes a real-world candidate match or placement, so
  this MUST be a separate system able to *reject* a proposal and fall
  back to HOLD.

  `:itonami.blueprint/governor` is `:employment-agency-governor`,
  grep-verified UNIQUE fleet-wide -- no naming-collision precedent
  question, a fresh independent build following the SAME governed-
  actor architecture (langgraph StateGraph + independent Governor +
  Phase 0->3 rollout) established by `cloud-itonami-isic-6511`.

  This blueprint's own text (docs/business-model.md's own Trust
  Controls: 'placement outside consent is blocked; matching decisions
  are explainable') and its own docs/operator-guide.md ('handling
  sensitive candidate data and high-stakes placement decisions'
  requiring human sign-off) name exactly the checks below.

  Seven checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them. The confidence/actuation gate is
  SOFT: it asks a human to look (low confidence / actuation), and the
  human may approve -- but see `employmentops.phase`: for `:stake
  :actuation/match-candidate`/`:actuation/place-candidate` (a real
  match or placement) NO phase ever allows auto-commit either. Two
  independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source
                                       (`employmentops.facts`), or
                                       invent one?
    2. Evidence incomplete         -- for `:candidacy/match`/
                                       `:candidacy/place`, has the
                                       jurisdiction actually been
                                       assessed with a full evidence
                                       checklist on file?
    3. Matching-basis
       discriminatory                -- for `:candidacy/match`,
                                       INDEPENDENTLY verify the
                                       candidacy's own `:matching-
                                       criteria-discriminatory?` is
                                       false -- the FLAGSHIP genuinely
                                       new check this vertical adds
                                       (grep-verified absent fleet-
                                       wide -- zero hits for
                                       'matching-basis-discriminatory'/
                                       'discriminatory-match' as a
                                       governor check function name),
                                       the 84th distinct application
                                       of the unconditional-evaluation
                                       discipline overall (most
                                       recently `practiceops.governor/
                                       professional-seal-invalid-
                                       violations` at 83rd). Grounded
                                       in real employment-anti-
                                       discrimination law: Japan's own
                                       職業安定法/男女雇用機会均等法 (Employment
                                       Security Act / Equal Employment
                                       Opportunity Act, enforced by
                                       MHLW), the US's Title VII of
                                       the Civil Rights Act of 1964
                                       (enforced by the EEOC), the
                                       UK's Equality Act 2010
                                       (enforced by the EHRC), and
                                       Germany's Allgemeines
                                       Gleichbehandlungsgesetz (AGG,
                                       enforced by the
                                       Antidiskriminierungsstelle) --
                                       directly grounded in this
                                       blueprint's own text ('matching
                                       decisions are explainable' --
                                       i.e. justifiable on legitimate
                                       criteria, never a protected
                                       characteristic). Evaluated
                                       UNCONDITIONALLY (every match
                                       needs its own criteria checked
                                       for discriminatory content).
    4. Placement fee mismatch      -- for `:candidacy/place`,
                                       INDEPENDENTLY recompute whether
                                       the candidacy's own `:claimed-
                                       fee` equals `annual-salary x
                                       fee-rate`
                                       (`employmentops.registry/
                                       placement-fee-matches-claim?`)
                                       -- an HONEST reapplication of
                                       the SAME ground-truth-recompute
                                       DISCIPLINE `practiceops.
                                       registry`'s/`hospitalityops.
                                       registry`'s/`agronomyops.
                                       registry`'s own checks
                                       establish, reapplied to a
                                       candidacy's placement-fee line
                                       -- not claimed as new.
    5. Work authorization
       unverified                     -- for `:candidacy/place`, for a
                                       candidacy whose own record
                                       declares `:requires-work-
                                       authorization? true` (i.e. this
                                       candidate is actually a foreign
                                       national whose employment
                                       legally requires work-
                                       authorization verification --
                                       not every candidate is),
                                       INDEPENDENTLY check whether
                                       `:work-authorization-verified?`
                                       is true. A GENUINELY NEW
                                       concept (grep-verified absent
                                       fleet-wide -- zero hits for
                                       'work-authorization'/'work-
                                       permit-unverified'/'right-to-
                                       work' as a governor check
                                       function name), the 85th
                                       distinct application overall,
                                       the TWELFTH conditional variant
                                       (after `socialresearch`/7220's,
                                       `bizassoc`/9411's, `training`/
                                       8549's, `furniture`/9524's,
                                       `specialtyrepair`/9529's,
                                       `leathergoods`/9523's,
                                       `ictrepair`/9511's, `quarryops`/
                                       0810's, `agronomyops`/0162's,
                                       `hospitalityops`/5510's and
                                       `practiceops`/7110's own, at
                                       63rd, 64th, 66th, 67th, 68th,
                                       69th, 71st, 77th, 79th, 81st
                                       and 83rd). CONDITIONAL on the
                                       candidacy's own `:requires-
                                       work-authorization?` ground
                                       truth. Grounded in real work-
                                       authorization law: Japan's own
                                       出入国管理及び難民認定法 (Immigration
                                       Control and Refugee Recognition
                                       Act, enforced by the Immigration
                                       Services Agency), the US's INA
                                       §274A Form I-9 employment-
                                       eligibility verification
                                       (enforced by USCIS), the UK's
                                       Immigration, Asylum and
                                       Nationality Act 2006 right-to-
                                       work checks (enforced by the
                                       Home Office), and Germany's
                                       Aufenthaltsgesetz §4a (enforced
                                       by Ausländerbehörden) -- ALL
                                       FOUR seeded jurisdictions
                                       actually have a real regime
                                       here, reported honestly (a
                                       full-coverage sub-citation,
                                       matching `quarryops`/0810's own
                                       blast-safety, `agronomyops`/
                                       0162's own water-buffer and
                                       `practiceops`/7110's own
                                       professional-seal full coverage
                                       rather than `hospitalityops`/
                                       5510's own honest single-
                                       jurisdiction gap).
    6. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:candidacy/match`/
                                       `:candidacy/place` (REAL acts)
                                       -> escalate.

  Two more guards, double-match/double-placement prevention, are
  enforced but NOT listed as numbered HARD checks above because they
  need no upstream comparison at all -- `already-matched-violations`/
  `already-placed-violations` refuse to match/place the SAME
  candidacy twice, off dedicated `:matched?`/`:placed?` facts (never a
  `:status` value) -- the SAME 'check a dedicated boolean, not status'
  discipline every prior governor's guards establish, informed by
  `cloud-itonami-isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [employmentops.facts :as facts]
            [employmentops.registry :as registry]
            [employmentops.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Matching a real candidate and placing a real candidate are the two
  real-world actuation events this actor performs -- a two-member
  set, matching every sibling's own dual-actuation shape."
  #{:actuation/match-candidate :actuation/place-candidate})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:jurisdiction/assess` (or `:candidacy/match`/`:candidacy/place`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's anti-discrimination/work-authorization
  requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:jurisdiction/assess :candidacy/match :candidacy/place} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:candidacy/match`/`:candidacy/place`, the jurisdiction's
  required registration/matching/placement evidence must actually be
  satisfied -- do not trust the advisor's self-reported confidence
  alone."
  [{:keys [op subject]} st]
  (when (contains? #{:candidacy/match :candidacy/place} op)
    (let [c (store/candidacy st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction c) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(登録記録/マッチング記録/配置記録/在留資格確認記録等)が充足していない状態での提案"}]))))

(defn- matching-basis-discriminatory-violations
  "For `:candidacy/match`, INDEPENDENTLY verify the candidacy's own
  `:matching-criteria-discriminatory?` is false -- the flagship
  genuinely new check this vertical adds. Evaluated UNCONDITIONALLY
  (every match needs its own criteria checked)."
  [{:keys [op subject]} st]
  (when (= op :candidacy/match)
    (let [c (store/candidacy st subject)]
      (when (true? (:matching-criteria-discriminatory? c))
        [{:rule :matching-basis-discriminatory
          :detail (str subject " のマッチング基準が保護属性に基づく可能性がある")}]))))

(defn- placement-fee-mismatch-violations
  "For `:candidacy/place`, INDEPENDENTLY recompute whether the
  candidacy's own claimed placement fee equals annual-salary x
  fee-rate via `employmentops.registry/placement-fee-matches-claim?`
  -- needs no proposal inspection or stored-verdict lookup at all, an
  honest reapplication of the same discipline every sibling actor's
  own cost/total-matching check establishes."
  [{:keys [op subject]} st]
  (when (= op :candidacy/place)
    (let [c (store/candidacy st subject)]
      (when-not (registry/placement-fee-matches-claim? c)
        [{:rule :placement-fee-mismatch
          :detail (str subject " の申告紹介手数料(" (:claimed-fee c)
                      ")が独立再計算値(" (registry/compute-placement-fee c) ")と一致しない")}]))))

(defn- work-authorization-unverified-violations
  "For `:candidacy/place`, for a candidacy whose own record declares
  `:requires-work-authorization? true`, INDEPENDENTLY check whether
  `:work-authorization-verified?` is true -- a genuinely new concept,
  CONDITIONAL on the candidacy's own `:requires-work-authorization?`
  ground truth (not every candidate is a foreign national requiring
  work-authorization verification)."
  [{:keys [op subject]} st]
  (when (= op :candidacy/place)
    (let [c (store/candidacy st subject)]
      (when (and (true? (:requires-work-authorization? c))
                 (not (true? (:work-authorization-verified? c))))
        [{:rule :work-authorization-unverified
          :detail (str subject " は在留資格/就労許可の確認を要するが未確認 -- 配置提案は進められない")}]))))

(defn- already-matched-violations
  "For `:candidacy/match`, refuses to match the SAME candidacy record
  twice, off a dedicated `:matched?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :candidacy/match)
    (when (store/candidacy-already-matched? st subject)
      [{:rule :already-matched
        :detail (str subject " は既にマッチング済み")}])))

(defn- already-placed-violations
  "For `:candidacy/place`, refuses to place the SAME candidacy twice,
  off a dedicated `:placed?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :candidacy/place)
    (when (store/candidacy-already-placed? st subject)
      [{:rule :already-placed
        :detail (str subject " は既に配置済み")}])))

(defn check
  "Censors an EmploymentOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (matching-basis-discriminatory-violations request st)
                           (placement-fee-mismatch-violations request st)
                           (work-authorization-unverified-violations request st)
                           (already-matched-violations request st)
                           (already-placed-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
