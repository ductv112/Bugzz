---
phase: 04-filter-catalog-picker-face-filter-mode
plan: "01"
subsystem: assets + build
tags: [sprites, extraction, lottie, gradle, coil, datastore, turbine]
dependency_graph:
  requires: []
  provides:
    - app/src/main/assets/sprites/sprite_spider/ (23 frames + manifest.json)
    - app/src/main/assets/sprites/sprite_bugA/ (7 frames + manifest.json)
    - app/src/main/assets/sprites/sprite_bugB/ (12 frames + manifest.json)
    - app/src/main/assets/sprites/sprite_bugC/ (16 frames + manifest.json)
    - libs.coil.compose alias (io.coil-kt:coil-compose:2.7.0)
    - libs.androidx.datastore.preferences alias (1.1.3)
    - libs.turbine alias (app.cash.turbine:turbine:1.2.0)
  affects:
    - Plan 04-04 (FilterCatalog references sprite_spider/bugA/bugB/bugC assetDirs)
    - Plan 04-05 (DataStore FilterPrefsRepository)
    - Plan 04-06 (Coil AsyncImage in filter picker)
    - Plan 04-02 (Turbine in Flow tests)
tech_stack:
  added:
    - io.coil-kt:coil-compose:2.7.0
    - androidx.datastore:datastore-preferences:1.1.3
    - androidx.datastore:datastore-preferences-core:1.1.3
    - app.cash.turbine:turbine:1.2.0
  patterns:
    - Node.js CommonJS Lottie-JSON base64-PNG frame extraction script
    - Gradle Version Catalog alias additions (libs.versions.toml)
key_files:
  created:
    - scripts/extract_sprites.cjs
    - app/src/main/assets/sprites/sprite_spider/ (23 PNGs + manifest.json)
    - app/src/main/assets/sprites/sprite_bugA/ (7 PNGs + manifest.json)
    - app/src/main/assets/sprites/sprite_bugB/ (12 PNGs + manifest.json)
    - app/src/main/assets/sprites/sprite_bugC/ (16 PNGs + manifest.json)
    - .planning/phases/03-first-filter-end-to-end-photo-capture/03-gaps-01-SUMMARY.md
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
decisions:
  - "D-05 density formula (buf/w*h*4 > 0.10) is wrong for PNG — replaced with MIN_BYTES=2000 absolute threshold"
  - "sprite_spider frames are outline-only silhouettes (confirmed Phase 3) — extracted as-is, Plan 04-04 assigns correct anchorType/behavior"
  - "home_lottie.json imgSeq ranges confirmed: bugA=0..6, bugB=14..25, bugC=38..53 (exactly as researched)"
metrics:
  duration: "~12 minutes"
  completed_date: "2026-04-21"
  tasks_completed: 2
  files_changed: 69
---

# Phase 4 Plan 01: Sprite Extraction + Dep Catalog — Summary

**One-liner:** 4 sprite groups (58 PNG frames) extracted from reference APK Lottie JSONs via Node.js script; Coil 2.7 + DataStore 1.1.3 + Turbine 1.2.0 wired into Gradle catalog; 03-gaps-01 explicitly superseded.

## Tasks Completed

| # | Name | Commit | Key outputs |
|---|------|--------|-------------|
| 1 | Extract 4 sprite groups + supersede 03-gaps-01 | bc26ab2 | scripts/extract_sprites.cjs; sprite_spider (23f), sprite_bugA (7f), sprite_bugB (12f), sprite_bugC (16f); 03-gaps-01-SUMMARY.md |
| 2 | Add Coil + DataStore + Turbine to Gradle catalog | 554c9f6 | gradle/libs.versions.toml (+3 versions, +4 aliases); app/build.gradle.kts (+4 dep lines); compileDebugKotlin GREEN |

## Extraction Results (Actual vs Research Estimate)

| Output dir | Source | Expected frames | Actual frames | Frame dims | Frame size range | Match |
|-----------|--------|-----------------|--------------|-----------|-----------------|-------|
| `sprite_spider` | spider_prankfilter.json | 23 | **23** | 1500×1500 | 6.2–8.3 KB | exact |
| `sprite_bugA` | home_lottie.json imgSeq_0..6 | 7 | **7** | 360×360 | ~12 KB | exact |
| `sprite_bugB` | home_lottie.json imgSeq_14..25 | 12 | **12** | 360×360 | ~16 KB | exact |
| `sprite_bugC` | home_lottie.json imgSeq_38..53 | 16 | **16** | 300×300 | 13–22 KB | exact |

Total: 58 frames extracted, 0 skipped.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] D-05 density formula incorrect for PNG compression**

- **Found during:** Task 1, first script run — all 58 frames rejected
- **Issue:** Research formula `buf.length / (w * h * 4) > 0.10` compares compressed PNG byte size against uncompressed RGBA byte budget. PNG compression is so efficient that even colorful sprites on transparent backgrounds produce density ratios of 0.0007–0.023, all far below 0.10. The formula was designed conceptually for uncompressed RGBA bitmaps, not PNG files.
- **Evidence:** Spider frames: 6–8KB for 1500×1500 → density 0.0007. BugA frames: 12KB for 360×360 → density 0.023. Both clearly contain real content but both fail the 0.10 threshold.
- **Fix:** Replaced `MIN_DENSITY = 0.10` with `MIN_BYTES = 2000` (absolute byte threshold). A fully-transparent all-alpha PNG of any supported size compresses to <500 bytes; any real sprite content (even thin outlines) produces ≥2KB. Also added PNG magic byte header check (89 50 4E 47) as additional validation.
- **Files modified:** `scripts/extract_sprites.cjs`
- **Commit:** bc26ab2

## Known Stubs

None — this plan produces only asset files and build catalog entries. No Kotlin code or UI wiring introduced.

## Threat Surface Scan

No new network endpoints, auth paths, file access patterns, or schema changes introduced. All extraction is offline (on-disk JSON → on-disk PNG). Maven Central artifacts (Coil, DataStore, Turbine) fetched over HTTPS by Gradle default. Consistent with T-04-04 (accept) disposition in plan threat register.

## Notes for Plan 04-04

- `sprite_spider` frames are thin outline silhouettes (Phase 3 device test confirmed ~1–2% non-alpha pixel coverage on screen). The reference APK's `spider_prankfilter.json` only contains the outline/stroke layer; no filled-color spider layer exists in the bundled Lottie assets. Plan 04-04 should account for this when setting `scaleFactor` for the spider filter — may need larger scale to make the outline visible.
- `sprite_bugA` frames (imgSeq_0..6) are identical to the `ant_on_nose_v1` first 7 frames (same source data, same sizes). The ant group previously extracted in Phase 3 used all 35 imgSeq assets combined; this plan splits them into 3 logical groups (bugA/bugB/bugC).
- Phase 3 dirs `ant_on_nose_v1`, `spider_on_forehead_v1`, `test_filter`, `bad_filter` are untouched per plan guard.
