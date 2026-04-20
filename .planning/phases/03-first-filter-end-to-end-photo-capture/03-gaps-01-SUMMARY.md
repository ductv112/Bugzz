---
phase: 03-first-filter-end-to-end-photo-capture
plan: gaps-01
status: superseded
superseded_by: 04-01-PLAN.md Task 1
superseded_date: 2026-04-21
---

# Plan 03-gaps-01 — SUPERSEDED

## Outcome: superseded — not executed

## Why

Phase 3 handoff surfaced a spider-sprite extraction issue (wrong asset source in `spider_prankfilter.json` traversal — Phase 3 used the ant-script pattern which didn't match spider's flat asset layout). Plan `03-gaps-01-PLAN.md` was filed as a standalone re-extraction of the spider group.

Phase 4 planning (04-CONTEXT D-03 / D-04 / D-30) superseded this gap plan: rather than fix spider alone, Phase 4 executes the **full 4-group extraction** from both `spider_prankfilter.json` and `home_lottie.json` in a single Wave 0 pass. The 4 resulting sprite groups (sprite_spider, sprite_bugA, sprite_bugB, sprite_bugC) power all 15 Phase 4 FilterDefinitions per the shared-sprite D-30 model.

## Closed by

- `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-01-PLAN.md` Task 1 (this supersedence marker is itself a Task 1 output)
- Commit: `feat(04-01-01): extract 4 sprite groups from reference APK + supersede 03-gaps-01`

## Extraction results (actual, from Task 1 execution)

| Output dir | Source | Frames extracted | Frame size | Notes |
|-----------|--------|-----------------|-----------|-------|
| `sprite_spider` | `spider_prankfilter.json` all assets | 23 | 1500×1500 | image_0..image_22; 6–8KB/frame; outline-only silhouette (known from Phase 3 device test) |
| `sprite_bugA` | `home_lottie.json` imgSeq_0..6 | 7 | 360×360 | ~12KB/frame; same data as Phase 3 ant_on_nose_v1 group A |
| `sprite_bugB` | `home_lottie.json` imgSeq_14..25 | 12 | 360×360 | ~16KB/frame |
| `sprite_bugC` | `home_lottie.json` imgSeq_38..53 | 16 | 300×300 | 13–22KB/frame |

## D-05 validation deviation

The research-specified D-05 density formula (`buf.length / (w * h * 4) > 0.10`) was found to be incorrect for PNG files. PNG compression makes even content-rich sprites appear well below 0.10 (e.g., a 1500x1500 spider frame at 6–8KB gives density ~0.0007). A corrected absolute byte-size threshold (MIN_BYTES = 2000) was used instead — a fully-transparent PNG of any size compresses to <500 bytes, so 2KB+ reliably indicates real content. All 58 frames passed.

## Status at close

- Phase 3 ROADMAP entry remains complete — spider filter was marked in `03-05-SUMMARY.md` as a "soft gap — SUPERSEDED by Phase 4 D-03 extraction pass"
- No production code in Phase 3 depended on the re-extracted spider (the existing `spider_on_forehead_v1` dir from Phase 3 is retained and still referenced by Phase 3 FilterCatalog entry until Plan 04-04 retires it)
- The Phase 4 extraction produces fresh `sprite_spider` co-located with the existing `spider_on_forehead_v1` — both directories exist after Wave 0

## See also

- 04-CONTEXT.md D-03 (4 sprite groups, not 15)
- 04-CONTEXT.md D-30 (shared assetDir per sprite group)
- 04-RESEARCH.md §Critical Finding: Reference APK Sprite Availability
- 04-RESEARCH.md §Lottie Extraction Script
