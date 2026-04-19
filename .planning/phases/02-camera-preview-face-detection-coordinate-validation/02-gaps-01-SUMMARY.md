---
phase: 02-camera-preview-face-detection-coordinate-validation
plan: gaps-01
subsystem: detector
tags: [mlkit, face-detection, contour-mode, tracking-id, one-euro-filter, adr, research-correction]

# Dependency graph
requires:
  - phase: 02-camera-preview-face-detection-coordinate-validation
    provides: "FaceDetectorClient + FaceDetectorOptionsTest + CONTEXT D-15/D-22 + VALIDATION CAM-08 row — all landed by Plans 02-01..02-06 and surfaced by Plan 02-06 device runbook as GAP-02-A"
provides:
  - "FaceDetectorClient.buildOptions() without .enableTracking() — honors ML Kit contour/tracking mutual exclusivity"
  - "FaceDetectorOptionsTest pinned to trackingEnabled=false with GAP-02-A cross-reference"
  - "02-ADR-01 documenting the decision + Phase 3 BboxIouTracker follow-ups"
  - "02-CONTEXT.md D-15/D-22 amended with the runtime limitation callout"
  - "02-VALIDATION.md CAM-08 relaxed acceptance (boundingBox centroid continuity) + Wave 0 checklist correction"
  - ".planning/research/PITFALLS.md §3 amended to remove the misleading .enableTracking() recommendation + add MediaPipe-style bbox-IoU alternative + LANDMARK_MODE_ALL fallback"
affects: [phase-03, phase-04, phase-05, phase-07, bbox-iou-tracker, landmark-smoother, face-identity]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "ADR pattern for research-correctness amendments — documents Status/Context/Decision/Consequences/Follow-ups + Alternatives table so downstream planners see the rejection rationale before re-introducing the bug."
    - "Plan-time cross-linking: gap-closure plans amend the research that caused the gap (PITFALLS.md) so future phases using the same research inherit the correction."

key-files:
  created:
    - ".planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md"
    - ".planning/phases/02-camera-preview-face-detection-coordinate-validation/02-gaps-01-SUMMARY.md"
  modified:
    - "app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt"
    - "app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt"
    - ".planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md"
    - ".planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md"
    - ".planning/research/PITFALLS.md"

key-decisions:
  - "Remove .enableTracking() from FaceDetectorClient.buildOptions() permanently while CONTOUR_MODE_ALL is present — ML Kit silently ignores the call under contour mode (documented Google behavior, 459/459 null-trackingId evidence from Xiaomi 13T)."
  - "Keep CONTOUR_MODE_ALL — contour points are load-bearing for Phase 3/4 filter anchoring; dropping contour to regain tracking would rewrite CRAWL behavior."
  - "Defer full bbox-IoU face-identity tracking to Phase 3 via a new BboxIouTracker utility (~100 LOC Kotlin + unit tests). Phase 2 keys 1€ filter on the -1 sentinel; single-face runbook masks cross-face contamination."
  - "Relax CAM-08 acceptance: boundingBox centerX/centerY stability on still-head is the Phase 2 exit criterion; original trackingId-across-60-frames deferred to Phase 3 exit re-verification."
  - "Amend PITFALLS.md §3 at the root — the gap was caused by misleading research (line 110 recommended .enableTracking() without noting the mutual exclusivity); future research consumers inherit the corrected bullet + MediaPipe-style alternative + LANDMARK_MODE_ALL fallback."

patterns-established:
  - "Pattern: Research-correction ADR — when device-verification surfaces a research correctness issue, the closure plan amends BOTH the research file (so future phases inherit the fix) AND the phase CONTEXT (so immediate planners see it) AND writes an ADR (so the decision is discoverable in future phase-plan context-assembly scans)."
  - "Pattern: Diagnostic KDoc — in-code KDoc that documents 'do not re-add X' with evidence link (device model + frame count + date) is a high-signal defense against regression when the original bug was silent (ML Kit returning null instead of throwing)."

requirements-completed: [CAM-08]

# Metrics
duration: ~18 min
completed: 2026-04-19
---

# Phase 02 Plan gaps-01: GAP-02-A Closure — ML Kit Tracking + Contour Mutual Exclusivity

**Removed the silently-ignored `.enableTracking()` call from `FaceDetectorClient`, flipped the unit-test assertion to reflect runtime reality (`trackingEnabled=false`), created ADR-01 documenting the decision with Phase 3 `BboxIouTracker` follow-ups, amended CONTEXT D-15/D-22 + VALIDATION CAM-08 + PITFALLS §3 at the research root — closing GAP-02-A from 02-VERIFICATION.md.**

## Performance

- **Duration:** ~18 min
- **Started:** 2026-04-19T20:11Z (approx)
- **Completed:** 2026-04-19T20:29Z (approx)
- **Tasks:** 3 (all `type="auto"`, one with `tdd="true"`)
- **Files modified:** 6 (1 created, 5 modified)
- **Commits:** 3 atomic task commits + 1 metadata commit (pending)

## Accomplishments

- **Code layer closed:** `FaceDetectorClient.buildOptions()` no longer calls `.enableTracking()` while `setContourMode(CONTOUR_MODE_ALL)` is present. In-code KDoc documents the mutual exclusivity with verified device evidence (Xiaomi 13T, 459/459 null trackingIds, 2026-04-19) and cross-references ADR-01 so Phase 3 planners cannot re-introduce the same bug by reading stale research.
- **Test layer closed:** `FaceDetectorOptionsTest` pinned to `trackingEnabled=false` via both per-field toString substring check AND composite `equals()` gate. Assertion messages reference GAP-02-A / ADR-01 for diagnostic clarity on any future regression.
- **ADR landed:** `02-ADR-01-no-ml-kit-tracking-with-contour.md` with Status/Context/Decision/Consequences/Follow-ups/Alternatives sections (569 words). Lists four explicit Phase 3 follow-up action items the Phase 3 planner must address.
- **CONTEXT amendments:** D-15 rewritten to document `.enableTracking()` intentionally NOT called + runtime trackingId == null consequence + ADR-01 + GAP-02-A cross-refs. D-22 rewritten to document Phase 2 falls back to `id=-1` sentinel; Phase 3 BboxIouTracker replaces it.
- **VALIDATION amendments:** Per-Task Verification Map row split — CAM-08 moved to its own `02-gaps-01` row with "relaxed" acceptance; Manual-Only Verifications CAM-08 row updated to `boundingBox centerX/centerY stable across 60+ frames` with trackingId=null expected; Wave 0 checklist flipped `isTrackingEnabled == true` → `== false`.
- **Research root patched:** `.planning/research/PITFALLS.md §3` line 110 bullet replaced with 3-bullet callout (do NOT enable + bbox-IoU alternative + LANDMARK_MODE_ALL fallback) + new Warning Signs bullet flagging the `trackingId always null` symptom.
- **No regressions:** 10/10 existing Phase 2 unit tests remain GREEN. APK still builds cleanly (82 MB).

## Task Commits

Each task was committed atomically:

1. **Task 1: Remove .enableTracking() from detector + flip unit test** — `98e032a` (fix, TDD red→green verified)
2. **Task 2: Write ADR-01 + amend CONTEXT D-15/D-22 + VALIDATION CAM-08** — `3aa2ed3` (docs)
3. **Task 3: Amend PITFALLS.md §3** — `cb54bc6` (docs)

**Plan metadata:** pending (this SUMMARY + STATE.md + ROADMAP.md)

## Files Created/Modified

### Created
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md` — ADR documenting ML Kit contour + tracking mutual exclusivity, Phase 3 `BboxIouTracker` follow-ups, alternatives table.

### Modified
- `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` — Removed `.enableTracking()` call from `buildOptions()`; added KDoc block with ADR-01 cross-ref + GAP-02-A evidence; updated `activeIds` inline comment to document null-trackingId + `-1` sentinel flow.
- `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` — Dropped `.enableTracking()` from `expected` builder; flipped `trackingEnabled=true` → `trackingEnabled=false`; updated assertion messages to reference GAP-02-A / ADR-01; added class KDoc amendment note.
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` — D-15 amended (no .enableTracking(), runtime trackingId == null, ADR-01 ref); D-22 amended (Phase 2 uses -1 sentinel; Phase 3 BboxIouTracker provides stable ID).
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md` — Per-Task row split CAM-08 off into its own `02-gaps-01` row with "relaxed" acceptance; Manual-Only CAM-08 row updated to boundingBox-centroid-stability acceptance; Wave 0 `FaceDetectorOptionsTest` checklist flipped to `isTrackingEnabled == false`.
- `.planning/research/PITFALLS.md` — §3 line ~110 bullet replaced with 3-bullet callout (do NOT enable tracking under CONTOUR_MODE_ALL + bbox-IoU tracker alternative + LANDMARK_MODE_ALL fallback for apps that don't need contour); §3 Warning Signs gained one bullet for the `trackingId always null` symptom.

## Grep Evidence (Verification)

```
app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt
  grep ".enableTracking()"        → 0 call sites remaining (2 KDoc mentions, both prefaced by "Do not re-add" / "silently ignores" — these are warnings, not calls)
  grep "GAP-02-A|ADR-01"          → 3 matches (KDoc evidence ref + ADR cross-ref + comment ref)

app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt
  grep "^\s+\.enableTracking()"   → 0 builder chain call sites
  grep "trackingEnabled=false"    → 2 matches (toString assertion substring + composite-equals message)
  grep "trackingEnabled=true"     → 1 match (class KDoc amendment note explaining the flip)

.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md
  grep "GAP-02-A"                 → 2 (D-15 + D-22 amendments)
  grep "ADR-01"                   → 2 (D-15 + D-22 amendments)
  grep "amended 2026-04-19"       → 2 (D-15 + D-22 headers)

.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md
  grep "isTrackingEnabled == false" → 1 (Wave 0 checklist)
  grep "CAM-08 (relaxed"           → 2 (Per-Task Map + Manual-Only rows)

.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md
  wc -w                            → 569 words (≥ 300 plan minimum)
  grep "BboxIouTracker"            → 4 matches (Decision + Consequences + Follow-ups + Alternatives)

.planning/research/PITFALLS.md
  grep "silently ignores"          → 1 (§3 amendment callout)
  grep "GAP-02-A"                  → 1 (§3 evidence ref)
  grep "02-ADR-01"                 → 1 (§3 cross-ref)
  grep "Enable \*\*face tracking\*\*" → 0 (old misleading bullet gone)
  grep "CONTOUR_MODE_ALL"          → 1 (§3 amendment)
  grep "MediaPipe"                 → 2 (pre-existing source #144 + new bbox-IoU alternative)

Cross-file:
  grep -rc "GAP-02-A|ADR-01" .planning/phases/02-.../ .planning/research/PITFALLS.md
    → 79 total hits across 10 files (≥ 6 plan minimum)
```

## Unit Test Evidence

```
$ ./gradlew :app:testDebugUnitTest
> Task :app:testDebugUnitTest UP-TO-DATE
BUILD SUCCESSFUL in 5s
31 actionable tasks: 31 up-to-date

Test report: app/build/reports/tests/testDebugUnitTest/index.html
  total    = 10
  failures = 0
  ignored  = 0
  duration = 21.493s
  result   = successful

Per-test-class breakdown:
  OneEuroFilterTest          4/4 GREEN
  FaceDetectorOptionsTest    1/1 GREEN (now asserts trackingEnabled=false)
  OverlayEffectBuilderTest   2/2 GREEN
  CameraControllerTest       2/2 GREEN (Robolectric-backed)
  PlaceholderTest            1/1 GREEN (Phase 1 smoke)
```

## APK Build Evidence

```
$ ./gradlew :app:assembleDebug
BUILD SUCCESSFUL in 21s
41 actionable tasks: 3 executed, 38 up-to-date

Output:  app/build/outputs/apk/debug/app-debug.apk
Size:    82,124,007 bytes (~78.3 MiB) — matches Plan 02-06 baseline exactly
```

## Decisions Made

All decisions are captured in `02-ADR-01-no-ml-kit-tracking-with-contour.md`. Summary:

1. **Remove `.enableTracking()` under `CONTOUR_MODE_ALL` permanently.** Documented ML Kit behavior, not a bug. Re-adding without also dropping contour is forbidden (KDoc + ADR-01 both warn future Phase 3+ planners).
2. **Keep `CONTOUR_MODE_ALL`** — contour points are load-bearing for Phase 4 CRAWL. Dropping contour to regain tracking would rewrite downstream filter anchoring.
3. **Defer bbox-IoU face-identity tracking to Phase 3.** Implementing it in Phase 2 would bloat the gap-closure past its charter and push the risk-front-loaded boundary.
4. **Relax CAM-08 to boundingBox-centroid continuity.** Original truth (`trackingId stable across 60+ frames`) re-verified at Phase 3 exit under the new `BboxIouTracker` ID.

## Deviations from Plan

None required by the deviation rules. Plan executed exactly as written — three tasks, three commits, zero auto-fixes needed. One minor observation:

- Plan §verify for Task 1 specifies `grep -c "enableTracking" ...` MUST return 0 on both `FaceDetectorClient.kt` and `FaceDetectorOptionsTest.kt`. The literal grep returns 2 in the main file (both inside the new KDoc block, explicitly warning future planners not to re-add the call — per the plan's own `<action>` block which prescribed verbatim KDoc text containing `.enableTracking()` by name). Interpreted the verification intent as "no actual builder-chain call site remaining"; verified with targeted regex (`grep -c "\.enableTracking\(\)" limited to code, not KDoc`) returning 0 call sites. Documented in the Grep Evidence section above.

## Issues Encountered

- **JAVA_HOME missing from shell env:** `./gradlew` bootstrap requires `java` on PATH, which it is not by default on this Windows environment — gradle.properties `org.gradle.java.home` is honored by gradle itself but the bash wrapper needs JAVA_HOME/PATH set first. Workaround: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && export PATH="$JAVA_HOME/bin:$PATH"` before any `./gradlew` call. Known environment limitation from memory.env_paths.md; not a project issue.

## User Setup Required

None — no external services, no credentials, no environment changes. All changes land in code + docs.

## Next Phase Readiness

- **Phase 2 exit:** GAP-02-A is closed at the code + test + docs + research layers. Device re-verification of the relaxed CAM-08 acceptance (boundingBox-centroid stability on still head, 60+ frames) remains pending but can be performed alongside GAP-02-B + GAP-02-C re-verification after plans 02-gaps-02 + 02-gaps-03 land.
- **Phase 3 prerequisites surfaced:** ADR-01's Follow-ups section lists four concrete action items for the Phase 3 planner: (1) implement `BboxIouTracker` utility, (2) re-key `LandmarkSmoother` on the tracker ID, (3) pass tracker handle through `FaceDetectorClient.createAnalyzer()`, (4) update `02-VERIFICATION.md` CAM-08 row to reflect Phase 3 re-verification. Phase 3 context-assembly will automatically surface ADR-01 via phase-local read + the PITFALLS.md §3 amendment.
- **Remaining gaps:** GAP-02-B (CAM-07 DebugOverlayRenderer over-draw) and GAP-02-C (CAM-06 MP4 overlay visual confirmation) remain open — see `02-gaps-02-PLAN.md` and `02-gaps-03-PLAN.md`. GAP-02-C depends on GAP-02-B resolution.

## Self-Check

- [x] `.enableTracking()` call site removed from `FaceDetectorClient.buildOptions()` — verified (FOUND: 0 builder-chain calls remain; 2 KDoc warning mentions are intentional per plan)
- [x] `FaceDetectorOptionsTest` asserts `trackingEnabled=false` — verified (FOUND: both substring + equals-message reference it)
- [x] `./gradlew :app:testDebugUnitTest` exits 0 — verified (10/10 GREEN, 0 failures, 0 ignored)
- [x] `./gradlew :app:assembleDebug` exits 0 — verified (APK 82 MB)
- [x] `02-ADR-01-no-ml-kit-tracking-with-contour.md` exists — verified (FOUND: 569 words, all required sections)
- [x] `02-CONTEXT.md` D-15/D-22 reference ADR-01 — verified (FOUND: ADR-01 grep = 2)
- [x] `02-VALIDATION.md` Wave 0 FaceDetectorOptionsTest line corrected — verified (FOUND: `isTrackingEnabled == false` grep = 1)
- [x] `.planning/research/PITFALLS.md §3` amended — verified (FOUND: old bullet gone, new callout present, Warning Signs bullet added)
- [x] Commits exist: 98e032a, 3aa2ed3, cb54bc6 — verified via `git log --oneline`

## Self-Check: PASSED

---
*Phase: 02-camera-preview-face-detection-coordinate-validation*
*Plan: gaps-01*
*Completed: 2026-04-19*
