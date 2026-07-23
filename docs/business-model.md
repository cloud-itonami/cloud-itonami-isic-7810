# Business Model: Community Employment Agency

## Classification
- Repository: `cloud-itonami-7810`
- ISIC Rev.5: `7810` — employment agency — candidate intake, matching, placement and follow-up
- Social impact: workforce-access fair-placement worker-protection
- Actor: EmploymentOps-LLM ⊣ **Employment Agency Governor**
  (`:itonami.blueprint/governor :employment-agency-governor` — a unique
  governor keyword fleet-wide, grep-verified)

## Customer

Primary customers (an operator's customers, and the operator itself):

- community employment agencies and workforce programs (地方自治体の就労支援,
  cooperatives, NPOs) leaving a closed ATS that holds their candidate and
  placement data hostage
- labor-management organizations and sector staffing desks (建設・介護・製造など)
  that place workers at member employers and need a defensible compliance record
- private employment agencies (有料職業紹介事業者) that want their own governed
  execution scaffold instead of renting a per-seat agency CRM
- public-sector placement programs that must prove non-discriminatory matching
  and verified work authorization for every placement they make

## Problem

Closed agency CRMs (Bullhorn, Crelate, JobAdder, Zoho Recruit) rent
recruiters a per-seat pipeline and keep the candidate + placement data in
their store. For a 3–5-staff community agency the per-seat bill is already
on the order of this actor's managed tier, and the data never comes home.
Worse, none of them enforce the two liabilities that actually sink an
employment agency:

- **placement-fee mismatch** — a claimed placement fee that does not equal
  the candidacy's own recorded annual salary × fee rate, silently charged
  to the client
- **a discriminatory or unauthorized placement** — a match advanced on a
  basis the jurisdiction's anti-discrimination law forbids, or a placement
  made without the work-authorization evidence the jurisdiction requires
  (入管法 / I-9 / right-to-work / AufenthG §4a)

Today an agency catches these with a manual review queue, if it catches
them at all. The agency's operating licence (有料職業紹介事業許可 etc.) is
its own legal duty; this actor is the governed execution scaffold that
makes compliance the default, not the afterthought.

## Offer

- candidacy intake with an evidence checklist, HARD-gated on an official
  spec-basis citation (`:candidacy/intake` / `:jurisdiction/assess`)
- per-jurisdiction anti-discrimination AND work-authorization assessment
  with official citations (JPN/USA/GBR/DEU seeded in
  `employmentops.facts/catalog`; all four carry a work-authorization
  sub-citation — extensible by one map entry citing a real source, never
  fabricated)
- governed matching and placement: the **Employment Agency Governor**
  HARD-blocks a match on a discriminatory basis, a placement-fee mismatch
  (`employmentops.registry/placement-fee-matches-claim?` — the fee is
  recomputed from the candidacy's own `:annual-salary` × `:fee-rate`), and
  an unverified work authorization on a candidacy that requires one — plus
  a double-match / double-placement guard. A human approves every match
  and every placement; nothing is autonomous, at any rollout phase.
- follow-up records and an append-only audit ledger — the evidence trail
  for every intake, assessment, match and placement decision, and the
  agency's defence if a placement is later disputed

The core promise: **no placement the governor would reject ever ships, and
the agency can prove it** — that is the differentiation against closed
per-seat agency CRMs.

## Funnel (demo → fork → certified operator)

1. **Demo** — the live GitHub Pages demo
   (<https://cloud-itonami.github.io/cloud-itonami-isic-7810/>) runs the
   actor on synthetic candidacies and surfaces every HARD-hold kind
   (discriminatory basis, fee mismatch, unverified work authorization,
   double placement); like the Talent Board sibling, real candidacy data
   never lives on public Pages.
2. **Fork / self-host** — AGPL; run the actor privately on your own store
   (`employmentops.store/->MemStore` for dev, `DatomicStore` for
   production, contract parity proven by `store_contract_test`).
3. **License / registration** — the operator obtains its own jurisdiction
   agency licence (有料職業紹介事業許可 etc.); the actor's evidence
   checklists and ledger are the supporting record, not the licence.
4. **itonami.cloud certification** — listed operators get leads and may
   run managed tenants (same trust ladder as every cloud-itonami venture).
5. **Referral intake (inbound)** — a candidacy on the posting side
   (`cloud-itonami-isic-6399`) becomes a placement-desk candidacy via a
   HUMAN-CARRIED referral draft, never a cross-actor call (superproject
   ADR-2607131000: separate governance domains, no PII in the public
   actor's store).

## Revenue
- setup fee per agency, monthly operations subscription, placement and compliance services

Example pricing (market-anchored 2026-07-16 against 4 real competitor ATS/agency-CRM products —
`90-docs/pricing-intelligence/pricing-intelligence-ledger.edn`, `run-id "pricing-intel-20260716-03"`):

| Package | Customer | Price shape (example) |
|---|---|---|
| Self-host starter | community agency IT/volunteer lead | setup ¥200k–500k + optional support |
| Managed agency | one agency, unlimited seats | ¥60k–150k/月 |
| Compliance package | visa/work-authorization audit export, retention | +¥20k–50k/月 |

Crelate ($119/user/mo, 5-seat min), JobAdder (~$99–160/user/mo, estimated), Zoho Recruit Staffing
edition ($25–75/recruiter/mo) and Bullhorn (~$99–315/user/mo, estimated) all price **per recruiter
seat** — a 3–5-staff community agency's real spend on those tools already lands ~¥22k–90k/月, before
Bullhorn/Crelate's higher per-seat tiers push past ¥90k/月. Pricing this flat and unlimited-seat
undercuts the per-seat total once an agency has more than ~4–5 staff, and none of the 4 competitors
enforce work-authorization/visa-consent (入管法/I-9/right-to-work/AufenthG §4a) as a hard governor
block — that compliance layer is the basis for the top of the band. This also lands in the same
¥50k–150k/月 range as the sibling flagships (`cloud-itonami-isic-6399`, `cloud-itonami-isic-6310`),
giving the portfolio one consistent managed-tenant price point across all three.

**Subscribe (2026-07-16, ADR-2607161745)**: a live Stripe Payment Link for the ¥80,000/月 flat
Managed Starter tier is available now — [**subscribe to Managed Placement Desk — Starter**](https://buy.stripe.com/3cIcN474ncW48yA0VNbMQ0d).
This is a no-code Stripe-hosted checkout; nothing in this repo's code changed. After subscribing,
contact gftdcojp via an [operator-interest issue](https://github.com/cloud-itonami/cloud-itonami-isic-7810/issues/new?template=operator-interest.yml)
to arrange managed-tenant setup (manual fulfillment today, no automated onboarding yet).

## Unit Economics (worked example, illustrative)

One managed agency tenant (unlimited seats, JPN only):

- infrastructure: actor runtime + store ≈ ¥5k–15k/月 (the actor runs at
  intake / assess / match / place time, not per candidate lookup)
- LLM cost: proposals only at intake / assess / match / place —
  candidacies/month × a few yen; bounded because search and lookup never
  call a model
- human approval labor: the real cost driver — every match and every
  placement is a human sign-off; at ~2–3 min/placement, 50 placements/月
  ≈ 2–3 h/月 of operator time, plus intake review
- support + incident: budget ~5 h/月 until jurisdiction catalogs and feeds
  stabilize

At ¥80k–100k/月 the flat unlimited-seat pricing undercuts the per-seat
total of Bullhorn/Crelate once an agency has more than ~4–5 staff, and
gross margin stays healthy because the business scales with number of
agencies (and placements), not seats. The compliance package
(visa/work-authorization audit export, retention) is the expansion lever
— it is the layer none of the four competitor CRMs enforce as a hard
block.

Track per operator: intake hours, approvals/placement, % placements
HARD-held (data-quality and feed signal), fee-mismatch catch rate,
work-authorization verification lead time, churn. **These are not yet
measured at fleet scale — the figures above are an illustrative shape,
not a reported metric.**

## Open Participation

Anyone may fork, run the demo, self-host, submit patches, publish
jurisdiction catalog entries (with official citations — never fabricated),
and build a local agency business. itonami.cloud certification is required
before an operator is listed, receives leads, or runs managed tenants
under the platform brand.

## Operator Trust Levels

| Level | Capability |
|---|---|
| Contributor | patches, docs, jurisdiction catalog entries, examples |
| Self-host operator | runs their own placement desk, no platform endorsement |
| Certified operator | listed on itonami.cloud after review |
| Managed operator | may receive leads and operate customer agencies |
| Core maintainer | can approve changes to governor, security and governance |

## Trust Controls
- placement outside consent is blocked; matching decisions are explainable; candidate data stays outside Git
- a robot action the governor refuses is never dispatched to hardware
- every dispatch, hold, approval and disclosure path is auditable
- sensitive operating and personal data stays outside Git

## Non-Negotiables

- Do not commit real candidate, employer or placement data.
- Do not bypass the Employment Agency Governor for production matches or
  placements.
- Do not advance a match on a basis the jurisdiction's anti-discrimination
  law forbids, or a placement without the work-authorization evidence the
  jurisdiction requires.
- Do not market an uncertified deployment as an itonami.cloud certified
  operator.
- The agency operating licence (有料職業紹介事業許可 etc.) is the
  operator's own legal duty; the software is the governed execution
  scaffold, not the licence.
