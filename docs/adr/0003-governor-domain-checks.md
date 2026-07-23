# ADR-0003: the Employment Agency Governor's three domain checks

## Status

Accepted. Records the architectural decisions behind the three
domain-specific HARD checks `employmentops.governor` adds on top of
the shared spec-basis / evidence / actuation discipline every fleet
governor carries — decisions that until this ADR lived only in the
governor namespace's docstring.

## Context

The Employment Agency Governor earns EmploymentOps-LLM the right to
commit candidacy matches and placements. An LLM is good at drafting a
candidacy summary and normalizing records, but it has no notion of:

- whether a proposed match's own criteria actually rely on a protected
  characteristic,
- whether a candidacy's own claimed placement fee actually equals its
  recorded annual salary times fee rate, or
- whether a candidate whose employment legally requires work
  authorization has actually had that authorization verified.

Those three are the liabilities that actually sink an employment
agency, and the governor must be a separate system able to *reject* a
proposal and fall back to HOLD — a human approver cannot override a
HOLD on them. The question this ADR answers is not *whether* to check
them (ADR-0001 already commits to the independent-Governor invariant)
but *how each is structured* — and one of the three is deliberately
**conditional** where the other two are **unconditional**, a
distinction worth recording.

## Decision (as built)

Three domain HARD checks, in priority order, plus two double-actuation
guards. All HARD violations settle to HOLD immediately and never reach
request-approval.

1. **`matching-basis-discriminatory` — UNCONDITIONAL.** For every
   `:candidacy/match`, the governor independently reads the candidacy's
   own `:matching-criteria-discriminatory?` boolean and holds if true.
   Evaluated on every match, with no ground-truth precondition: every
   match needs its own criteria checked for discriminatory content,
   unconditionally. This is the genuinely-new check this vertical adds
   (the 84th distinct application of the unconditional-evaluation
   discipline overall), grounded in Japan's 職業安定法 / 男女雇用機会均等法
   (MHLW), the US's Title VII of the Civil Rights Act of 1964 (EEOC),
   the UK's Equality Act 2010 (EHRC) and Germany's AGG
   (Antidiskriminierungsstelle) — directly grounding this blueprint's
   own "matching decisions are explainable" trust control (i.e.
   justifiable on legitimate criteria, never a protected
   characteristic).

2. **`placement-fee-mismatch` — ground-truth recompute.** For
   `:candidacy/place`, the governor independently recomputes whether
   the candidacy's own `:claimed-fee` equals `annual-salary × fee-rate`
   via `employmentops.registry/placement-fee-matches-claim?`, with no
   proposal inspection or stored-verdict lookup at all. This is an
   honest *reapplication* of the SAME ground-truth-recompute discipline
   every sibling actor's own cost / total-matching check establishes
   (`practiceops`, `hospitalityops`, `agronomyops`) — reapplied to a
   candidacy's placement-fee line, not claimed as new.

3. **`work-authorization-unverified` — CONDITIONAL.** For
   `:candidacy/place`, **only** for a candidacy whose own record
   declares `:requires-work-authorization? true`, the governor checks
   whether `:work-authorization-verified?` is true. This is the
   deliberately conditional one, and the conditionality is the decision:
   not every candidate is a foreign national whose employment legally
   requires work-authorization verification, so checking it
   unconditionally would itself be wrong — the check must be gated on
   the candidacy's own `:requires-work-authorization?` ground truth.
   Grounded in Japan's 出入国管理及び難民認定法 (Immigration Services
   Agency), the US's INA §274A Form I-9 (USCIS), the UK's Immigration,
   Asylum and Nationality Act 2006 right-to-work checks (Home Office)
   and Germany's Aufenthaltsgesetz §4a (Ausländerbehörden) — all four
   seeded jurisdictions actually have a real regime here, reported
   honestly as full-coverage sub-citations in `employmentops.facts`
   (matching `practiceops`/7110's professional-seal full coverage),
   not a single-jurisdiction gap.

Two more guards — `already-matched` / `already-placed` — refuse to
match or place the SAME candidacy twice, off dedicated `:matched?` /
`:placed?` booleans (never a `:status` value), the same "check a
dedicated boolean, not status" discipline every prior governor's guards
establish, informed by `cloud-itonami-isic-6492`'s status-lifecycle bug
(superproject ADR-2607071320).

## Why one is conditional and two are not

`matching-basis-discriminatory` is unconditional because *every* match
has criteria that could be discriminatory — there is no candidacy for
which "are these matching criteria discriminatory?" is a non-question.
`work-authorization-unverified` is conditional precisely because there
*is* such a candidacy: one whose `:requires-work-authorization?` is
false. For that candidacy the work-authorization question does not
exist, and a governor that held an unconditional work-auth check
against it would be enforcing a requirement the law does not impose.
The conditionality is therefore not a relaxation — it is correctness.
(The `place-is-a-noop-when-no-work-authorization-required` test exists
to pin exactly this: a non-required candidacy is NOT held on this
check.)

`placement-fee-mismatch` is unconditional-in-applicability but narrow
in scope: every placement has exactly one fee line to recompute, so the
check applies to every `:candidacy/place` with no precondition beyond
the op itself.

## Consequences

- The governor contract test (`employmentops.governor-contract-test`)
  pins all three plus the conditional exemption: `matching-basis-
  discriminatory-is-held-and-unoverridable`, `placement-fee-mismatch-
  is-held`, `work-authorization-unverified-is-held-and-unoverridable`,
  and `place-is-a-noop-when-no-work-authorization-required` (the
  conditionality guard). 15 governor-contract tests, all green.
- The `:matched?` / `:placed?` dedicated-boolean discipline keeps the
  double-actuation guards free of the status-lifecycle class of bug.
- `employmentops.facts` carries the four real work-authorization
  sub-citations the conditional check depends on — extending the
  jurisdiction catalog is one map entry citing a real official source,
  never fabricated (a proposal without a spec-basis is held by the
  shared `spec-basis` check, by design).
