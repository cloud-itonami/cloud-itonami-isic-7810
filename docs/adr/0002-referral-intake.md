# ADR-0002: candidacies carry the referral record id they arrived with

## Status
Accepted. The receiving side of superproject ADR-2607131000 (the
human-carried handoff from cloud-itonami-isic-6399).

## Decision
`:candidacy/intake` patches may carry `:referral-id` — the 6399-side
application-referral record id (e.g. `JPN-REF-000000`) the human
operator carried over. It is an ordinary intake field (round-trips
both Store backends), NOT a live link: no store, governor or API is
shared with the posting side, and this actor's own governor re-checks
everything from intake onward exactly as if the candidacy had walked
in the door. Joining the two actors' records — the referral draft in
6399's history, the intake fact here — is deliberately how the
end-to-end story is reconstructed.

## Consequences
- 40 tests / 180 assertions green (Mem ≡ Datomic round-trip added).
- The demo page ingests a referred candidacy through the real intake
  op and its card shows the 紹介経由 chip with the SAME record id the
  Meta Job Search demo shows leaving its ledger.
