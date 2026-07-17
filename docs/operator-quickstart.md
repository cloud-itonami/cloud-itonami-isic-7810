# Operator Quickstart — your own governed placement desk, fork to production

The shortest path from forking this repo to running a governed
employment-agency instance. This is the concrete version of
`docs/business-model.md`'s operator layer.

## Who this is for

You're an employment-agency operator, workforce program, or labor-management organization who:
- Runs placements today without systematic compliance scaffolding
- Faces civil-rights liability risk from discriminatory matches, fee mismatches, or unverified work authorization
- Wants to keep candidate data private (no ATS SaaS data lock-in)
- Can commit to hosting and supporting the infrastructure yourself
- Holds or can obtain the operating license required in your jurisdiction

**Like the Talent Board sibling (isic-6310), nothing here goes on
public Pages with real data.** Candidacies are personal data — the
live demo is synthetic; your instance runs privately. The public side
of the stack (posting aggregation/search) is
[`cloud-itonami-isic-6399`](https://github.com/cloud-itonami/cloud-itonami-isic-6399);
the two hand off via human-carried referrals, never by invoking each
other (superproject ADR-2607131000).

## 1. Fork and prove the actor green

```bash
git clone https://github.com/<you>/cloud-itonami-isic-7810 && cd cloud-itonami-isic-7810
clojure -M:dev:test    # 39 tests — governor contract, phases, store parity, registry, facts
clojure -M:dev:run     # two clean match+place lifecycles + every HARD-hold kind
```

(`deps.edn` resolves `kotoba-lang/langgraph`/`langchain` as sibling
checkouts via `:local/root`; standalone forks clone those two next to
the repo or override with git coordinates.)

## 2. Seed your candidacies

Record shapes are in `employmentops.store/demo-data`: a candidacy
carries the operational fields the governor independently re-verifies
— `:annual-salary`/`:fee-rate`/`:claimed-fee` (the placement-fee
recompute), `:matching-criteria-discriminatory?`,
`:requires-work-authorization?`/`:work-authorization-verified?`, and
the dedicated `:matched?`/`:placed?` booleans. Keep them accurate;
they are what the HARD checks defend. Build a store with
`employmentops.store/->MemStore` / `datomic-store` from your own data.

## 3. Your jurisdiction catalog

`employmentops.facts/catalog` ships JPN/USA/GBR/DEU with official
anti-discrimination AND work-authorization citations. Extending it is
one map entry citing a real official source — never fabricate a
jurisdiction's requirements (the governor holds any proposal without a
spec-basis, by design).

## 4. Wire approvals and the phase rollout

- Matching and placing are NEVER autonomous, at any phase, by
  construction (two independent layers). Operations pause on
  langgraph's `interrupt-before` and resume with
  `{:approval {:status :approved :by <who>}}` — bind that to your
  approval inbox.
- Roll out phases 0→3 (`employmentops.phase`): read-only → assisted
  intake → assisted assess → supervised (intake auto-commits when
  governor-clean; match/place stay human forever).

## 5. Production posture

- Swap `MemStore` for `DatomicStore` (contract parity proven by
  `store_contract_test`) pointed at your Datomic Local / kotoba-server.
- Swap `mock-advisor` for `llm-advisor` when you want real drafting —
  a malformed LLM response degrades to a safe noop.
- Export the audit ledger on a schedule: it is your evidence trail for
  every intake/assessment/match/placement decision, and your defence
  if a placement is later disputed.
- Your jurisdiction's agency licence (有料職業紹介事業許可 etc.) is
  YOUR obligation — the software is the governed execution scaffold,
  not the licence.

## 6. Where this goes next

Pricing shapes, the certification ladder (itonami.cloud) and the
referral handoff from the posting side are in
`docs/business-model.md` and superproject ADR-2607131000.
