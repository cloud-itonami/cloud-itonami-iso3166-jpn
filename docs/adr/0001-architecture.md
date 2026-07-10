# ADR-0001: Architecture — Japan market-entry compliance actor (`marketentry`)

**Status**: accepted
**Date**: 2026-07-10

## Context

`cloud-itonami-iso3166-jpn` was published as a `:blueprint` (docs +
`blueprint.edn` only) in the iso3166 family creation wave
(ADR-2607032330). The family maturity ladder is
`:spec` → `:blueprint` → `:implemented`. No iso3166 entry had reached
`:implemented` before this ADR — the second-sweep closing summary
(ADR-2607042900) listed that promotion as explicit future work.

Japan is the deepest jurisdiction in the family (country coordinator +
19/19 agency-level blueprints), so it is the correct first
`:implemented` pilot.

## Decision

Build the full governed-actor architecture for `marketentry`, following
the same template as the ISIC fleet (`cloud-itonami-isic-7810` and
siblings):

- **Store**: `marketentry.store`, MemStore + DatomicStore, proven parity
  via contract test.
- **Registry**: `marketentry.registry`, pure DRAFT-certificate
  construction via `unsigned-certificate`, jurisdiction-scoped
  sequence numbering (`JPN-DFT-000000`, `JPN-SUB-000000`).
- **Governor**: `:market-entry-compliance-governor` (family keyword from
  `blueprint.edn`; first *running* implementation).
- **Entity shape**: `engagement`, sequential draft → submit on the same
  record. `high-stakes` =
  `#{:actuation/draft-filing :actuation/submit-filing}`.
- **Phase**: 0→3; `:filing/draft` and `:filing/submit` NEVER auto-
  commit at any phase (matches the blueprint Trust Control
  `filing-submit-never-auto-at-any-phase`).

### HARD checks (all unoverridable)

1. **spec-basis** — never invent a jurisdiction's market-entry
   requirements (`marketentry.facts` G2 catalog: GEPS / 全省庁統一資格 /
   法人番号 for JPN).
2. **evidence-incomplete** — draft/submit require a full assessment
   checklist on file.
3. **japan-resident-rep-missing** (FLAGSHIP) — when
   `:requires-japan-resident-rep?` is true, independently verify
   `:has-japan-resident-rep?`. Grounded in 全省庁統一資格 domestic
   office / agent requirements.
4. **engagement-fee-mismatch** — recompute `base-fee + monthly-rate ×
   monitoring-months` (ground-truth-recompute discipline).
5. **corporate-number-unverified** — conditional on
   `:requires-corporate-number?` (法人番号 regime).
6. **already-drafted / already-submitted** — dedicated booleans, never
   a `:status` value.

## Consequences

- Family maturity: first `:implemented` entry on the iso3166 axis.
- `kotoba-lang/iso3166` flips JPN `:maturity` to `:implemented` and
  documents the ladder as validated end-to-end.
- Sibling country blueprints (USA/CHN/RUS/…) can promote by forking
  this actor and swapping `marketentry.facts` + demo data.
