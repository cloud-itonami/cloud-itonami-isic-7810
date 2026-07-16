# cloud-itonami-isic-7810

Open Business Blueprint for **ISIC Rev.5 7810**: employment agency --
candidate intake, matching, placement and follow-up.

This repository publishes a community-employment-agency actor --
candidacy intake, per-jurisdiction anti-discrimination/work-
authorization regulatory assessment, candidate matching and candidate
placement -- as an OSS business that any qualified operator can fork,
deploy, run, improve and sell, so a community employment agency or
workforce program never surrenders candidate and placement data to a
closed ATS SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet (93 prior actors) -- here it is
**EmploymentOps-LLM ⊣ Employment Agency Governor**. This blueprint's
own `:itonami.blueprint/governor` keyword,
`:employment-agency-governor`, is a UNIQUE keyword fleet-wide
(grep-verified: no other blueprint declares it) -- a fresh,
independent build.

> **Why an actor layer at all?** An LLM is great at drafting a
> candidacy summary, normalizing records, and checking whether a
> claimed placement fee actually equals a candidacy's own recorded
> annual salary times fee rate -- but it has **no notion of which
> jurisdiction's anti-discrimination/work-authorization law is
> official, no license to match a real candidate or place a real
> candidate, and no way to know on its own whether a proposed match's
> own criteria actually rely on a protected characteristic or whether
> a candidate's own work authorization has actually been verified for
> a placement that legally requires it**. Letting it match or place a
> candidate directly invites fabricated regulatory citations, a
> placement-fee mismatch being charged to a client, a discriminatory
> matching decision being executed, and a candidate being placed
> without verified work authorization -- exposing the agency to real
> regulatory and civil-rights liability. This project seals the
> EmploymentOps-LLM into a single node and wraps it with an
> independent **Employment Agency Governor**, a human **approval
> workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers candidacy intake through anti-discrimination/work-
authorization regulatory assessment, candidate matching and candidate
placement. It does **not**, by itself, hold any operating license
required to run an employment agency in a given jurisdiction, and it
does not claim to. It also does not perform the actual candidate
sourcing/screening/interview work itself, or judge candidate fit --
`employmentops.registry/placement-fee-matches-claim?` is a pure
ground-truth recompute against the candidacy's own recorded fields,
not a fit judgment. Whoever deploys and operates a live instance (a
qualified agency operator/placement coordinator) supplies any
jurisdiction-specific license, the real candidate-sourcing/applicant-
tracking integration and the real work-authorization-verification
integrations, and bears that jurisdiction's liability -- the software
supplies the governed, spec-cited, audited execution scaffold so that
operator does not have to build the compliance layer from scratch.

### Actuation

**Matching a real candidate and placing a real candidate are never
autonomous, at any phase, by construction.** Two independent layers
enforce this (`employmentops.governor`'s `:actuation/match-candidate`/
`:actuation/place-candidate` high-stakes gate and `employmentops.
phase`'s phase table, which never puts either op in any phase's
`:auto` set) -- see `employmentops.phase`'s docstring and
`test/employmentops/phase_test.clj`'s `candidacy-match-never-auto-at-
any-phase`/`candidacy-place-never-auto-at-any-phase`. The actor may
draft, check and recommend; a human agency operator is always the one
who actually matches or places a candidate. Grounded directly in this
blueprint's own `docs/business-model.md` Trust Controls text
("placement outside consent is blocked; matching decisions are
explainable") -- a genuine DUAL-actuation shape, applied SEQUENTIALLY
to the SAME candidacy record (match first, place later), matching
`practiceops`/7110's, `hospitalityops`/5510's, `freightops`/4920's,
`quarryops`/0810's and `agronomyops`/0162's own sequential shape
rather than `retailops`/4711's own alternative-kind shape.

## The core contract

```
candidacy intake + jurisdiction facts (employmentops.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ EmploymentOps-LLM     │ ─────────────▶ │ Employment Agency Governor    │  (independent system)
   │ (sealed)              │  + citations    │ spec-basis · evidence-       │
   └───────────────────────┘                 │ incomplete · matching-        │
          │                 commit ◀┼ basis-discriminatory (FLAGSHIP NEW)   │
          │                         │ · placement-fee-mismatch (ground-     │
    record + ledger        escalate ┼ truth) · work-authorization-              │
          │              (ALWAYS for│ unverified (conditional, NEW) ·           │
          │       :actuation/match- │ already-matched · already-placed          │
          │       candidate/        │                                            │
          │       :actuation/place- │                                            │
          │       candidate}         │                                            │
          ▼                          └───────────────────────┘
      human approval
```

**The EmploymentOps-LLM never matches or places a candidate the
Employment Agency Governor would reject, and never does so without a
human sign-off.** Hard violations (fabricated regulatory
requirements; unsupported evidence; a discriminatory matching basis;
a placement-fee mismatch; an unverified work authorization on a
work-authorization-required candidacy; a double match/placement)
force **hold** and *cannot* be approved past; a clean match/placement
proposal still always routes to a human.

## Live demo (GitHub Pages)

**<https://cloud-itonami.github.io/cloud-itonami-isic-7810/>** -- a
static demo. Want to run this as a local placement desk?
[**Register operator interest**](https://github.com/cloud-itonami/cloud-itonami-isic-7810/issues/new?template=operator-interest.yml)
-- see [`docs/business-model.md`](docs/business-model.md) for the
revenue model and [`docs/operator-quickstart.md`](docs/operator-quickstart.md)
for the fork-to-published walkthrough.

The demo above is a
static, zero-build Placement Desk demo (synthetic data). NOTHING on it
is hand-typed (the fleet demo-page rule, superproject
ADR-2607122300): `web/generate.cljs` (nbb) runs the FULL
OperationActor StateGraph at build time -- two clean match+place
lifecycles with approval interrupts, every HARD-hold kind and both
double-actuation guards -- then renders the post-run candidacy board
(real match/placement numbers), the real refusal verdicts and the
append-only audit ledger. In-browser search is `web/search.cljs` run
by scittle; `web/verify_search.cljs` is the headless nbb harness.

```bash
cd web && ../../../../node_modules/.bin/nbb \
  --classpath "../src:../../../kotoba-lang/html/src:../../../kotoba-lang/css/src:../../../kotoba-lang/langchain/src:../../../kotoba-lang/langgraph/src" \
  generate.cljs          # regenerate docs/index.html + docs/search.cljs
../../../../node_modules/.bin/nbb verify_search.cljs   # headless UI logic check
```

## Run

```bash
clojure -M:dev:run     # walk two clean match+place lifecycles (no work authorization required, work authorization required-and-verified), plus four HARD-hold cases, through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a screening and logistics
robot performs resume intake, matching and placement logistics, under
the actor, gated by the independent **Employment Agency Governor**.
The governor never dispatches hardware itself; `:high`/`:safety-
critical` actions (such as handling sensitive candidate data and
high-stakes placement decisions) require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Employment Agency Governor, match/placement draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/operator-quickstart.md`](docs/operator-quickstart.md) to go
from fork to a private production placement desk (seed candidacies,
jurisdiction catalog, approvals, phase rollout),
[`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`7810`). This vertical's candidacy/match records are practice-specific
rather than a shared cross-operator data contract, so
`employmentops.*` runs on the generic robotics/identity/forms/dmn/
bpmn/audit-ledger stack only -- no bespoke domain capability lib to
reference at all (unlike `retailops`/4711's own `kotoba-lang/retail`
and `freightops`/4920's own `kotoba-lang/logistics` integrations;
`kotoba-lang/occupation` is the generic ISCO-08 occupation-
classification registry -- the occupation-classification analog of
`kotoba.industry` -- not a bespoke domain capability library, matching
`quarryops`/0810's, `agronomyops`/0162's, `hospitalityops`/5510's and
`practiceops`/7110's own investigated-and-ruled-out precedent).

## Layout

| File | Role |
|---|---|
| `src/employmentops/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + match AND placement history (dual history). The double-actuation guard checks dedicated `:matched?`/`:placed?` booleans rather than a `:status` value |
| `src/employmentops/registry.cljc` | Match/placement draft records, plus `placement-fee-matches-claim?` -- an honest reapplication of the SAME ground-truth-recompute discipline every sibling actor's own cost/total-matching check establishes |
| `src/employmentops/facts.cljc` | Per-jurisdiction anti-discrimination AND work-authorization catalog with an official spec-basis citation per entry, honest coverage reporting -- ALL FOUR seeded jurisdictions have a work-authorization sub-citation here |
| `src/employmentops/employmentopsllm.cljc` | **EmploymentOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/jurisdiction-assessment/match/placement proposals |
| `src/employmentops/governor.cljc` | **Employment Agency Governor** -- 5 HARD checks (spec-basis · evidence-incomplete · matching-basis-discriminatory, FLAGSHIP NEW, the 84th unconditional-evaluation-discipline grounding · placement-fee-mismatch · work-authorization-unverified, CONDITIONAL, the 85th grounding) + 2 double-actuation guards + 1 soft (confidence/actuation gate) |
| `src/employmentops/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted assess → supervised (match/place always human; candidacy intake is the ONLY auto-eligible op, no direct candidate-facing risk) |
| `src/employmentops/operation.cljc` | **OperationActor** -- langgraph StateGraph |
| `src/employmentops/sim.cljc` | demo driver |
| `test/employmentops/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers candidacy intake through anti-discrimination/work-
authorization regulatory assessment, candidate matching and candidate
placement -- the core governed lifecycle this blueprint's own `docs/
business-model.md` names in its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Candidacy intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:candidacy/intake`/`:jurisdiction/assess`) | Real candidate-sourcing/applicant-tracking integration, real candidate-fit judgment (see `employmentops.facts`'s docstring) |
| Candidate matching, HARD-gated on full evidence and a non-discriminatory matching basis, plus a double-match guard (`:actuation/match-candidate`) | |
| Candidate placement, HARD-gated on full evidence, a matching fee claim and (when applicable) a verified work authorization, plus a double-placement guard (`:actuation/place-candidate`) | |
| Immutable audit ledger for every intake/assessment/match/placement decision | |

Extending coverage is additive: add the next gate (e.g. a follow-up-
completion-verification check) as its own governed op with its own
HARD checks and tests, following the SAME "an independent governor
re-verifies against the actor's own records before any real-world
act" pattern this repo's flagship ops already establish.

## Jurisdiction coverage (honest)

`employmentops.facts/coverage` reports how many requested
jurisdictions actually have an official spec-basis in
`employmentops.facts/catalog` -- currently 4 seeded (JPN, USA, GBR,
DEU) out of ~194 jurisdictions worldwide. This is a starting catalog
to prove the governor contract end-to-end, not a claim of global
coverage. Adding a jurisdiction is additive: one map entry in
`employmentops.facts/catalog`, citing a real official source -- never
fabricate a jurisdiction's requirements to make coverage look bigger.
Note that the work-authorization sub-citation is FULL coverage rather
than a gap: ALL FOUR seeded jurisdictions (JPN, USA, GBR, DEU)
actually have a real work-authorization enforcement regime, reported
honestly.

## Maturity

`:implemented` -- `EmploymentOps-LLM` + `Employment Agency Governor`
run as real, tested code (see `Run` above), promoted from the
originally-published `:blueprint`-tier scaffold, following the SAME
governed-actor architecture as the 93 other prior actors across this
fleet, with its own distinct, independently-named governor. See
`docs/adr/0001-architecture.md` for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
