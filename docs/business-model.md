# Business Model: Community Employment Agency

## Classification
- Repository: `cloud-itonami-7810`
- ISIC Rev.5: `7810` — employment agency — candidate intake, matching, placement and follow-up
- Social impact: workforce-access fair-placement worker-protection

## Customer
- community employment agencies, workforce programs and cooperatives leaving closed ATS SaaS

## Offer
- candidate intake and matching, placement workflows, follow-up records, compliance and audit

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

## Trust Controls
- placement outside consent is blocked; matching decisions are explainable; candidate data stays outside Git
- a robot action the governor refuses is never dispatched to hardware
- every dispatch, hold, approval and disclosure path is auditable
- sensitive operating and personal data stays outside Git
