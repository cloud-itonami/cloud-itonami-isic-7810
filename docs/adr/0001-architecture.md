# ADR-0001: EmploymentOps-LLM ⊣ Employment Agency Governor architecture

## Status

Accepted. `cloud-itonami-isic-7810` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-7810` publishes an OSS business blueprint for
community employment agency operations (candidate intake, matching,
placement and follow-up). Like every prior actor in this fleet, the
blueprint alone is not an implementation: this ADR records the
governed-actor architecture that promotes it to real, tested code,
following the same langgraph StateGraph + independent Governor + Phase
0→3 rollout pattern established by `cloud-itonami-isic-6511` (life
insurance) and applied across 92 prior siblings, most recently
`cloud-itonami-isic-7110` (community architectural and engineering
practice).

A `kotoba-lang` org search for employment/staffing/recruiting/ats/hr/
talent/candidate/matching-named repos returned zero hits.
`kotoba-lang/occupation` was investigated: it is the ISCO-08 sole-
proprietor occupation registry, the GENERIC occupation-classification
counterpart to `kotoba.industry` (ISIC-coded businesses) -- generic
technology-capability-resolution infrastructure, not a bespoke domain
capability library for employment-agency matching/placement business
records. This build returns to self-contained domain logic, the same
pattern the majority of this fleet's actors use.

This blueprint's own `:itonami.blueprint/governor` keyword,
`:employment-agency-governor`, is grep-verified UNIQUE fleet-wide --
no naming-collision precedent question, a fresh independent build.

## Decision

### Decision 1: fresh governor identity, no reuse precedent needed

`:employment-agency-governor` is grep-verified unique across every
blueprint.edn in this fleet. This build follows the SAME governed-
actor architecture as every prior actor, but with its own distinct
governor identity.

### Decision 2: dual-actuation shape, SEQUENTIAL on the SAME `candidacy` entity

This blueprint's own operating states ("intake : register : match :
place : follow-up : audit") name two real-world acts: matching a
candidate and placing a candidate. These apply SEQUENTIALLY to the
SAME `candidacy` entity -- match first, place later -- matching
`practiceops`/7110's, `hospitalityops`/5510's, `freightops`/4920's,
`quarryops`/0810's and `agronomyops`/0162's own sequential shape
rather than `retailops`/4711's own alternative-kind shape.
`high-stakes` is `#{:actuation/match-candidate :actuation/
place-candidate}`.

### Decision 3: `placement-fee-matches-claim?` -- an honest reapplication of the ground-truth-recompute discipline

`employmentops.registry/placement-fee-matches-claim?` (candidacy's own
claimed placement fee vs. annual-salary x fee-rate) applies the SAME
discipline `practiceops.registry`'s own `fee-total-matches-claim?`,
`hospitalityops.registry`'s own `folio-total-matches-claim?`,
`agronomyops.registry`'s own `dose-matches-claim?` and `quarryops.
registry`'s own `royalty-matches-claim?` establish -- verify a
claimed monetary total against the entity's own recorded fields,
independent of proposal inspection. No literal code is shared
(different domain), but the discipline is the same, documented as
such rather than claimed as a novel invention.

### Decision 4: entity and op shape

The primary entity is a `candidacy`. Four ops: `:candidacy/intake`
(directory upsert, no candidate-facing risk), `:jurisdiction/assess`
(per-jurisdiction anti-discrimination/work-authorization evidence
checklist, never auto), `:candidacy/match` (POSITIVE, high-stakes),
and `:candidacy/place` (POSITIVE, high-stakes).

### Decision 5: `matching-basis-discriminatory?` -- the 84th unconditional-evaluation grounding, the FLAGSHIP genuinely new check

Grep-verified absent fleet-wide (zero hits for `matching-basis-
discriminatory`, `discriminatory-match` as a governor check name).
Grounded in real employment-anti-discrimination law: Japan's own
職業安定法/男女雇用機会均等法 (Employment Security Act / Equal Employment
Opportunity Act, enforced by MHLW), the US's Title VII of the Civil
Rights Act of 1964 (enforced by the EEOC), the UK's Equality Act 2010
(enforced by the EHRC), and Germany's Allgemeines
Gleichbehandlungsgesetz (AGG, enforced by the
Antidiskriminierungsstelle) -- directly grounded in this blueprint's
own text ("matching decisions are explainable" -- i.e. justifiable on
legitimate criteria, never a protected characteristic). Evaluated
UNCONDITIONALLY on every `:candidacy/match` (every match needs its own
criteria checked for discriminatory content).

### Decision 6: `work-authorization-unverified?` -- the 85th unconditional-evaluation grounding, the TWELFTH conditional variant

Before writing this check, every prior sibling's governor namespace
was grepped for any check function named `work-authorization`,
`work-permit-unverified` or `right-to-work` -- zero hits, confirming
this is a genuinely new concept. This is the TWELFTH conditional
variant (after `socialresearch`/7220's, `bizassoc`/9411's, `training`/
8549's, `furniture`/9524's, `specialtyrepair`/9529's, `leathergoods`/
9523's, `ictrepair`/9511's, `quarryops`/0810's, `agronomyops`/0162's,
`hospitalityops`/5510's and `practiceops`/7110's own, at 63rd, 64th,
66th, 67th, 68th, 69th, 71st, 77th, 79th, 81st and 83rd) -- CONDITIONAL
on the candidacy's own `:requires-work-authorization?` ground truth:
not every candidate is a foreign national whose employment requires
work-authorization verification. Grounded in real work-authorization
law: Japan's own 出入国管理及び難民認定法 (Immigration Control and Refugee
Recognition Act, enforced by the Immigration Services Agency), the
US's INA §274A Form I-9 employment-eligibility verification (enforced
by USCIS), the UK's Immigration, Asylum and Nationality Act 2006
right-to-work checks (enforced by the Home Office), and Germany's
Aufenthaltsgesetz §4a (enforced by Ausländerbehörden). ALL FOUR seeded
jurisdictions actually have a real regime here, reported honestly -- a
full-coverage sub-citation, matching `quarryops`/0810's own blast-
safety, `agronomyops`/0162's own water-buffer and `practiceops`/7110's
own professional-seal full coverage rather than `hospitalityops`/
5510's own honest single-jurisdiction gap.

### Decision 7: dedicated double-actuation-guard booleans

`:matched?`/`:placed?` are dedicated booleans on the `candidacy`
record, never a single `:status` value -- the same discipline every
prior governor's guards establish, informed by
`cloud-itonami-isic-6492`'s real status-lifecycle bug
(ADR-2607071320).

### Decision 8: Store protocol, MemStore + DatomicStore parity

`employmentops.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in
`test/employmentops/store_contract_test.clj`.

### Decision 9: no bespoke domain capability lib, and no `blueprint.edn` field-sync fixes needed beyond `:optional-technologies`

Verified explicitly this session: no `kotoba-lang/employment`,
`kotoba-lang/staffing`, `kotoba-lang/recruiting`, `kotoba-lang/ats`,
`kotoba-lang/hr`, `kotoba-lang/talent`, `kotoba-lang/candidate` or
`kotoba-lang/matching`-style bespoke capability library exists;
`kotoba-lang/occupation` is the GENERIC ISCO-08 occupation-
classification registry (the occupation-classification analog of
`kotoba.industry`), not domain-specific to employment-agency
matching/placement. This repo's `blueprint.edn` had the correct
`:required-technologies` matching the `kotoba-lang/industry`
registry's own entry for `"7810"` exactly, but was MISSING
`:optional-technologies [:optimization]` entirely -- the same gap
pattern `agronomyops`/0162's, `hospitalityops`/5510's and
`practiceops`/7110's own builds found. Fixed cleanly in the same
commit as the `:maturity` flip.

### Decision 10: mock + LLM advisor pair

`employmentops.employmentopsllm` provides `mock-advisor`
(deterministic, default everywhere -- the actor graph and governor
contract run offline) and `llm-advisor` (backed by `langchain.model/
ChatModel`, with a defensive EDN-proposal parser so a malformed LLM
response degrades to a safe low-confidence noop rather than ever
auto-matching or auto-placing a candidate).

## Alternatives considered

- **An unconditional work-authorization check** (applying to every
  placement regardless of whether the candidate is actually a foreign
  national). Rejected: not every candidate requires work-authorization
  verification at all -- forcing the check onto every placement would
  fabricate a requirement.
- **Fabricating a jurisdiction gap** to match the pattern of
  `hospitalityops`/5510's own single-jurisdiction honesty gap.
  Rejected: the same honesty discipline that forbids fabricating
  coverage also forbids under-reporting it -- all four seeded
  jurisdictions genuinely have a real work-authorization regime here.
- **Treating `kotoba-lang/occupation` as this vertical's capability
  library.** Considered and explicitly ruled out: it is generic
  ISCO-08 occupation-classification infrastructure, not domain-
  specific business logic for employment-agency matching/placement.

## Consequences

- 94th actor in this fleet (93 implemented before this build).
- Establishes two genuinely NEW unconditional-evaluation-discipline
  checks: `matching-basis-discriminatory?` (FLAGSHIP, 84th distinct
  application overall) and `work-authorization-unverified?` (85th
  distinct application overall, the TWELFTH conditional variant).
- `MemStore` ‖ `DatomicStore` parity is proven by
  `test/employmentops/store_contract_test.clj`.
- 39 tests / 176 assertions pass; lint is clean; the demo
  (`clojure -M:dev:run`) walks two clean match+place lifecycles (no
  work authorization required, work authorization required-and-
  verified), plus four HARD-hold scenarios, end-to-end.
- `blueprint.edn` needed a genuine field-sync fix this time (a missing
  `:optional-technologies [:optimization]` key) in addition to the
  `:maturity` flip.

## References

- `cloud-itonami-isic-6511/docs/adr/0001-architecture.md` (origin of
  the general governed-actor architecture pattern)
- `cloud-itonami-isic-7110/docs/adr/0001-architecture.md` (most recent
  prior sibling, template for this ADR's structure)
- 職業安定法 (Employment Security Act); 男女雇用機会均等法 (Equal Employment
  Opportunity Act); 出入国管理及び難民認定法 (Immigration Control and Refugee
  Recognition Act) (Japan)
- Title VII of the Civil Rights Act of 1964, 42 U.S.C. §2000e; INA
  §274A (Form I-9) (US)
- Equality Act 2010; Immigration, Asylum and Nationality Act 2006 (UK)
- Allgemeines Gleichbehandlungsgesetz (AGG); Aufenthaltsgesetz (AufenthG) §4a (Germany)
