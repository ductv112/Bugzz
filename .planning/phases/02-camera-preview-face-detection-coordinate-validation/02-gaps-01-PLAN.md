---
phase: 02-camera-preview-face-detection-coordinate-validation
plan: gaps-01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt
  - app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt
  - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md
  - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md
  - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md
  - .planning/research/PITFALLS.md
autonomous: true
gap_closure: true
requirements: [CAM-08]
must_haves:
  truths:
    - "FaceDetectorClient.buildOptions() no longer calls .enableTracking() — the CONTOUR_MODE_ALL + .enableTracking() mutual-exclusivity is honored"
    - "FaceDetectorOptionsTest asserts options.isTrackingEnabled == false (reflects ML Kit runtime reality, not plan intent)"
    - "./gradlew :app:testDebugUnitTest --tests \"*FaceDetectorOptionsTest*\" exits 0"
    - "./gradlew :app:assembleDebug exits 0 — APK rebuilds cleanly with detector change"
    - "02-CONTEXT.md D-15 amended to document the mutual-exclusivity limitation; D-22 amended to document trackingId-as-null consequence on 1€ filter state keying (LandmarkSmoother keys on id=-1 sentinel)"
    - "02-VALIDATION.md Per-Task Verification Map + Manual-Only Verifications CAM-08 row relaxed to reflect bbox-IoU-bridging-deferred-to-Phase-3 acceptance"
    - "Wave 0 FaceDetectorOptionsTest.kt checklist item updated: `isTrackingEnabled == false` (was `== true`)"
    - ".planning/research/PITFALLS.md §3 line ~110 amended to remove the `.enableTracking()` recommendation and add a mutual-exclusivity callout"
    - "An ADR file `02-ADR-01-no-ml-kit-tracking-with-contour.md` documents the decision so Phase 3 planners see it early"
    - "New CAM-08 acceptance: face identity persists via bbox-centroid continuity heuristic (Phase 3 implements full bbox-IoU); Phase 2 acceptance is trackingId-may-be-null + FaceTracker logs show stable boundingBox centerX/centerY across frames"
  artifacts:
    - path: "app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt"
      provides: "FaceDetectorOptions without .enableTracking(); documented in-code comment explaining the contour-mode exclusivity"
      contains: "setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)"
    - path: "app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt"
      provides: "Test pinned to isTrackingEnabled == false + documentation comment linking to ADR-01"
      contains: "trackingEnabled=false"
    - path: ".planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md"
      provides: "ADR documenting the ML Kit contour + tracking mutual-exclusivity and Phase 3 bbox-IoU bridge plan"
      min_lines: 30
    - path: ".planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md"
      provides: "D-15 + D-22 amended with the runtime limitation callout"
      contains: "trackingId"
    - path: ".planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md"
      provides: "CAM-08 acceptance row relaxed + Wave 0 checklist assertion corrected"
      contains: "CAM-08"
    - path: ".planning/research/PITFALLS.md"
      provides: "§3 amended to remove the misleading .enableTracking() recommendation"
      contains: "contour"
  key_links:
    - from: "FaceDetectorClient.buildOptions()"
      to: "FaceDetectorOptionsTest.options_configured_per_D15()"
      via: "equals() + toString() substring assertions"
      pattern: "trackingEnabled=false"
    - from: "02-CONTEXT.md D-15/D-22"
      to: "02-ADR-01-no-ml-kit-tracking-with-contour.md"
      via: "cross-reference"
      pattern: "ADR-01"
    - from: "Phase 3 planner"
      to: "02-ADR-01 + PITFALLS §3 amendment"
      via: "canonical_refs inheritance"
      pattern: "bbox-IoU"
---

<objective>
Close GAP-02-A (CAM-08 trackingId always null) by honoring the Google ML Kit documented runtime exclusivity between `setContourMode(CONTOUR_MODE_ALL)` and `.enableTracking()`. Remove the misleading call, flip the unit-test assertion to reality, document the decision in an ADR + context amendments + research PITFALLS correction, and relax the CAM-08 acceptance to boundingBox continuity (Phase 2 exit) with full bbox-IoU tracking deferred to Phase 3.

Purpose: The research (`PITFALLS.md §3 line 110`) was incorrect — it recommended `.enableTracking()` without noting the contour-mode-all mutual exclusivity. This is a research correctness issue that surfaced at device verification (459/459 `FaceTracker` log frames showed `id=null`). The fix has three layers: (1) code — stop calling a silently-ignored API; (2) tests — pin reality; (3) docs — prevent Phase 3 planners from re-introducing the same bug by reading the same broken research.

Output: 6 files modified, 1 ADR created, APK rebuilds cleanly, `FaceDetectorOptionsTest` turns GREEN on the new assertion, Phase 3 planner sees the ADR on day one.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VERIFICATION.md
@.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md
@.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-06-SUMMARY.md
@.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-HANDOFF.md
@.planning/research/PITFALLS.md
@app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt
@app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt

<interfaces>
<!-- Key contracts the executor needs. Extracted from current codebase. -->

From app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt (current — line numbers as of 2026-04-19):

```kotlin
companion object {
    // ...SMOOTHED_CONTOUR_TYPES omitted...

    fun buildOptions(): FaceDetectorOptions =
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)  // line 119
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)           // line 120  <-- KEEP
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)        // line 121
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)  // line 122
            .enableTracking()                                               // line 123  <-- REMOVE
            .setMinFaceSize(0.15f)                                          // line 124
            .build()
}
```

Other sites in FaceDetectorClient.kt that reference the now-null trackingId:

- Line 50: `val activeIds = faces.mapNotNull { it.trackingId }.toSet()` — always returns emptySet; `smoother.retainActive(emptySet())` clears all smoother state every frame. KEEP call but update comment to document that `activeIds` will always be empty in contour mode (smoother falls back to the `id = -1` sentinel path in `smoothFace`).
- Line 62: `f.trackingId?.toString() ?: "null"` — already null-safe; no change.
- Line 78-79: `val id = face.trackingId ?: -1` — already null-safe sentinel; `LandmarkSmoother` keys on `-1:c$type` for all faces in contour mode. Multi-face pipelines will cross-contaminate until Phase 3 bbox-IoU lands; Phase 2 expects one face for its device-runbook, so acceptable.

From app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt (current):

```kotlin
val expected = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
    .enableTracking()                // line 40  <-- REMOVE
    .setMinFaceSize(0.15f)
    .build()

// ...
assertTrue(
    "tracking must be enabled (D-15 / CAM-08 — trackingId stability); toString=$actualStr",
    actualStr.contains("trackingEnabled=true"),  // line 70  <-- FLIP to trackingEnabled=false
)
```
</interfaces>

<read_first>
1. Read `02-VERIFICATION.md` §GAP-02-A in full — it is the contract this plan closes.
2. Read `FaceDetectorClient.kt` lines 29-127 — full class. Note line 123 `.enableTracking()` + line 50 `activeIds` site + line 78 `val id = face.trackingId ?: -1` sentinel.
3. Read `FaceDetectorOptionsTest.kt` in full. Note line 40 (`.enableTracking()` in expected) + line 70 (`trackingEnabled=true` substring).
4. Read `PITFALLS.md` lines 90-120 — find the exact line that recommends `.enableTracking()` (documented as line 110 in VERIFICATION.md).
5. Read `02-CONTEXT.md` §Implementation Decisions looking for D-15 (ML Kit configuration) and D-22 (1€ filter state keyed on trackingId).
6. Read `02-VALIDATION.md` full — two rows change: Per-Task Verification Map CAM-08 row + Manual-Only Verifications CAM-08 row + Wave 0 `FaceDetectorOptionsTest` checklist assertion (`isTrackingEnabled == true` → `isTrackingEnabled == false`).
</read_first>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Remove .enableTracking() from detector, flip unit test, document in-code</name>
  <files>
    app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt,
    app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt
  </files>
  <behavior>
    - After change: `FaceDetectorClient.buildOptions()` returns a FaceDetectorOptions where `toString()` contains `trackingEnabled=false` and DOES NOT contain `trackingEnabled=true`.
    - After change: `FaceDetectorOptionsTest.options_configured_per_D15` asserts `trackingEnabled=false` and calls `expected` via a builder that does NOT call `.enableTracking()` — composite `equals()` passes.
    - `./gradlew :app:testDebugUnitTest --tests "*FaceDetectorOptionsTest*"` exits 0.
    - `./gradlew :app:testDebugUnitTest --tests "com.bugzz.filter.camera.detector.*"` exits 0 (no regression on OneEuroFilterTest + any other detector tests).
  </behavior>
  <action>
    In `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt`:

    1. Delete line 123 (the `.enableTracking()` call) from `buildOptions()`. Leave all other lines untouched — preserves D-15 field ordering for toString stability.

    2. Add a KDoc block above the `buildOptions` companion method explaining the omission (use this verbatim — it will be referenced by ADR-01):
       ```kotlin
       /**
        * Exposed for FaceDetectorOptionsTest (Plan 01) — asserts D-15 values exactly. Do NOT
        * inline into `options` field initializer; the test calls this static method directly.
        *
        * **Tracking NOT enabled.** Google ML Kit silently ignores `.enableTracking()` at runtime
        * when `CONTOUR_MODE_ALL` is active — `face.trackingId` is always null. This was verified
        * on Xiaomi 13T / HyperOS (GAP-02-A, 459/459 frames, 2026-04-19). Face identity across
        * frames is deferred to Phase 3 via a bbox-IoU centroid-overlap heuristic — see
        * `02-ADR-01-no-ml-kit-tracking-with-contour.md`. Do not re-add `.enableTracking()` here
        * without also switching away from `CONTOUR_MODE_ALL`; the two are mutually exclusive.
        */
       fun buildOptions(): FaceDetectorOptions = ...
       ```

    3. Update the in-line comment at line 50 (currently reads `// 1€ smoothing: retain state only for currently-tracked faces (D-22)`) to:
       ```kotlin
       // 1€ smoothing: `activeIds` is always empty in contour mode (face.trackingId == null —
       // see buildOptions KDoc / ADR-01). `LandmarkSmoother` keys on the -1 sentinel in
       // smoothFace() — so retainActive(emptySet()) clears all smoother state every frame.
       // This is acceptable for Phase 2's single-face runbook; Phase 3 replaces this with
       // a bbox-IoU-keyed smoother.
       ```

    4. In `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt`:
       - Delete line 40: `.enableTracking()` inside the `expected` builder.
       - Flip line 70 substring from `trackingEnabled=true` to `trackingEnabled=false`.
       - Flip line 69 assertion message from `"tracking must be enabled (D-15 / CAM-08 — trackingId stability); ..."` to `"tracking must be DISABLED — ML Kit silently ignores .enableTracking() under CONTOUR_MODE_ALL (GAP-02-A / ADR-01); toString=$actualStr"`.
       - Update the class KDoc block (lines 8-29) to add a final sentence: `"Updated 2026-04-19 per GAP-02-A: assertion flipped from trackingEnabled=true to trackingEnabled=false — see 02-ADR-01-no-ml-kit-tracking-with-contour.md."`

    Do NOT modify `LandmarkSmoother.kt`, `OneEuroFilter.kt`, `FaceSnapshot.kt`, or any other detector file — the `id = -1` sentinel path in `smoothFace()` already accepts null trackingId gracefully.

    Keep threading / Hilt / executor wiring exactly as-is (D-18).

    After edits, run:
    - `./gradlew :app:testDebugUnitTest --tests "*FaceDetectorOptionsTest*"` — expect 1/1 GREEN.
    - `./gradlew :app:testDebugUnitTest --tests "com.bugzz.filter.camera.detector.*"` — expect all GREEN (no regression).
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "*FaceDetectorOptionsTest*" --tests "*OneEuroFilterTest*"</automated>
    Also run:
    - `grep -c "enableTracking" app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` MUST return `0`.
    - `grep -c "enableTracking" app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` MUST return `0`.
    - `grep -c "trackingEnabled=false" app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` MUST return `>= 1`.
    - `grep -c "trackingEnabled=true" app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` MUST return `0`.
    - `grep -c "GAP-02-A\|ADR-01" app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` MUST return `>= 1`.
  </verify>
  <done>
    `FaceDetectorOptionsTest` passes with `trackingEnabled=false` assertion. `FaceDetectorClient` no longer calls `.enableTracking()`. In-code KDoc references ADR-01 for Phase 3 planners. No other tests regress.
  </done>
</task>

<task type="auto">
  <name>Task 2: Write ADR + amend 02-CONTEXT.md D-15/D-22 + amend 02-VALIDATION.md CAM-08 rows</name>
  <files>
    .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md,
    .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md,
    .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md
  </files>
  <action>
    **Step 1 — Create `02-ADR-01-no-ml-kit-tracking-with-contour.md`** (new file, in phase directory).

    Structure the ADR in the standard format (Status / Context / Decision / Consequences / Follow-ups):

    ```markdown
    # ADR 02-01: ML Kit Tracking + Contour Mode are Mutually Exclusive

    **Status:** Accepted (2026-04-19)
    **Phase:** 02 (Camera Preview + Face Detection)
    **Gap reference:** GAP-02-A (02-VERIFICATION.md)
    **Authors:** gsd-planner (gap closure wave 1), Claude (gsd-verifier evidence)

    ## Context

    Phase 2 plan 02-03 wired `FaceDetectorClient.buildOptions()` per CONTEXT.md D-15 with
    both `.setContourMode(CONTOUR_MODE_ALL)` and `.enableTracking()`. Research
    `.planning/research/PITFALLS.md §3 (line ~110)` recommended `.enableTracking()` for
    stable per-face identity without noting that Google ML Kit silently ignores the call
    when contour mode is active.

    Device verification on Xiaomi 13T Pro / HyperOS (2026-04-19) produced **459/459
    FaceTracker log frames with `face.trackingId == null`** over a 20-second continuous
    face hold. `FaceDetectorOptions.isTrackingEnabled` still reflects `true` (which is why
    the unit test passed pre-gap-closure), but the detector runtime emits faces with null
    trackingId.

    This is documented Google ML Kit behavior — contour-mode detection and per-face
    tracking use disjoint runtime paths.

    ## Decision

    1. **Remove `.enableTracking()` from `FaceDetectorClient.buildOptions()`.** Do not
       re-add it as long as `setContourMode(CONTOUR_MODE_ALL)` is present.
    2. **Keep `CONTOUR_MODE_ALL`** — contour points are load-bearing for Phase 3+
       (filter anchoring to nose bridge, cheeks, jawline, lips). Dropping contour to
       regain tracking would rewrite Phase 4 CRAWL behavior.
    3. **Defer full bbox-IoU face-identity tracking to Phase 3.** Phase 2 accepts
       `face.trackingId == null` and keys the 1€ filter on the `-1` sentinel (all faces
       share one smoother bucket). Phase 2's device runbook uses a single-face scenario,
       so cross-face state contamination is not observable in this phase.
    4. **Phase 3 prerequisite:** implement a `BboxIouTracker` utility that assigns
       stable integer IDs based on boundingBox centroid-IoU frame-over-frame. Feed the
       assigned ID into `LandmarkSmoother` as the state key, replacing the `-1` sentinel
       path.

    ## Consequences

    - `LandmarkSmoother` state in Phase 2 is recomputed every frame (keyed on `-1`);
      1€ smoothing degrades to per-frame initialization. On a still head in Phase 2's
      runbook this still reduces jitter versus raw landmarks, because 1€ internally
      maintains per-"key" state and reinitialization on the first call is cheap.
    - `CAM-08` acceptance relaxes to "boundingBox centerX/centerY persists across
      consecutive frames when face is held still"; the original "trackingId remains
      stable across 60+ frames" is downgraded to "trackingId may be null — documented
      ML Kit limitation in contour mode."
    - Multi-face scenarios cross-contaminate smoother state in Phase 2. Phase 3 must
      land `BboxIouTracker` before any production filter ships, else two faces confuse
      the 1€ smoother's cached per-landmark state.

    ## Follow-ups (Phase 3 planner must action)

    - [ ] Implement `BboxIouTracker` (spatial-proximity tracker, cost: ~100 LOC +
      unit tests with contrived frame sequences).
    - [ ] Re-key `LandmarkSmoother` on `BboxIouTracker` ID, not on `face.trackingId`.
    - [ ] Update `FaceDetectorClient.createAnalyzer()` to pass the tracker handle into
      the MlKitAnalyzer callback.
    - [ ] Update `02-VERIFICATION.md` CAM-08 row on next re-verification (Phase 3 exit)
      to reference the new tracker.

    ## Alternatives Considered

    | Alternative | Rejected because |
    |-------------|------------------|
    | Switch to `LANDMARK_MODE_ALL + .enableTracking()` | Loses contour points; Phase 4 CRAWL needs them. Research SUMMARY.md Resolution #2 already locked this. |
    | Run two detectors (contour + landmark+tracking, merged by bbox) | 2× CPU cost; PITFALLS #4 says we're already at the backpressure edge on mid-tier devices. Explicit non-goal. |
    | Wait for ML Kit fix | No timeline. Google documentation explicitly says the two are exclusive; not a bug. |
    | Implement bbox-IoU tracker in Phase 2 | Out of scope — would bloat Phase 2 past the risk-front-loaded boundary. Gap-closure charter is to close the current gap, not introduce net-new production code. |
    ```

    **Step 2 — Amend `02-CONTEXT.md` D-15 and D-22** (do NOT rewrite decisions — append clarifying clauses):

    In section `### ML Kit Configuration (locked from research; recorded for planner)`, find the D-15 bullet. It currently reads (verbatim from the file):

    > **D-15:** `FaceDetectorOptions.Builder()` = `setPerformanceMode(PERFORMANCE_MODE_FAST)` + `setContourMode(CONTOUR_MODE_ALL)` + `enableTracking()` + `setMinFaceSize(0.15f)`. Landmarks + classification are NOT enabled (PITFALLS #3: contour + landmarks + classification together degrades accuracy). Bugs anchor off contour points (Phase 3); bounding-box center is fallback.

    Replace the text with (keep the same bullet marker and label):

    > **D-15 (amended 2026-04-19 post-GAP-02-A):** `FaceDetectorOptions.Builder()` = `setPerformanceMode(PERFORMANCE_MODE_FAST)` + `setContourMode(CONTOUR_MODE_ALL)` + `setLandmarkMode(LANDMARK_MODE_NONE)` + `setClassificationMode(CLASSIFICATION_MODE_NONE)` + `setMinFaceSize(0.15f)`. **`.enableTracking()` is intentionally NOT called** — Google ML Kit silently ignores it under `CONTOUR_MODE_ALL` and the detector emits faces with `trackingId == null`. Verified on Xiaomi 13T / HyperOS (GAP-02-A, 459/459 null trackingIds over 20s). Face identity across frames is deferred to Phase 3 via a `BboxIouTracker` (see `02-ADR-01-no-ml-kit-tracking-with-contour.md`). Bugs anchor off contour points (Phase 3); bounding-box center is fallback. Landmarks + classification are NOT enabled (PITFALLS #3: contour + landmarks + classification together degrades accuracy).

    Find the D-22 bullet. It currently reads:

    > **D-22:** 1€ filter state is keyed on `face.trackingId`. When a face loses tracking (trackingId disappears), the corresponding 1€ filter state is cleared; when the same trackingId reappears, re-initialize (don't carry over stale state).

    Replace with:

    > **D-22 (amended 2026-04-19 post-GAP-02-A):** 1€ filter state is keyed on a frame-stable face identity. In Phase 2 this falls back to the `id = -1` sentinel because `face.trackingId` is always null under `CONTOUR_MODE_ALL` (see D-15 + ADR-01) — all faces share one smoother bucket, acceptable for the single-face device runbook. In Phase 3, a `BboxIouTracker` provides the stable ID; when a face loses tracking, the corresponding 1€ filter state is cleared; when the same ID reappears, re-initialize (don't carry over stale state).

    **Step 3 — Amend `02-VALIDATION.md`:**

    1. In §"Per-Task Verification Map" find the row starting `| 02-XX-YY | — | later | CAM-01, CAM-02, CAM-07, CAM-08 |`. Split CAM-08 off into its own row with a relaxed acceptance note. Replace the original row with:

       ```
       | 02-XX-YY | — | later | CAM-01, CAM-02, CAM-07 | — | N/A | manual-only | device runbook 02-HANDOFF.md on Xiaomi 13T | ❌ manual | ⬜ pending |
       | 02-gap-01 | 02-gaps-01 | 1 | CAM-08 (relaxed) | — | N/A | manual-only | adb logcat grep `FaceTracker` — trackingId may be null under contour mode (ML Kit limitation, ADR-01); acceptance = boundingBox centerX/centerY stable across consecutive frames on still head | ❌ manual | ⬜ pending |
       ```

    2. In §"Manual-Only Verifications" table, find the row for CAM-08. It currently reads:

       > `| trackingId stability 60+ consecutive frames | CAM-08 | Runtime logcat inspection | 02-HANDOFF.md step 4 — grep Timber `FaceTracker` output for 60s of session, verify trackingId value remains constant while one face present |`

       Replace with:

       > `| boundingBox centerX/centerY stable on still head 60+ consecutive frames (trackingId expected null — ADR-01) | CAM-08 (relaxed post-GAP-02-A) | Runtime logcat inspection + ML Kit contour-mode limitation | 02-HANDOFF.md step 10 (re-verify) — adb logcat grep `FaceTracker`, confirm `id=null` (expected), confirm `bb=X,Y` centerX/Y vary <10px across 60 consecutive frames while head held still. Full bbox-IoU tracking deferred to Phase 3 per ADR-01. |`

    3. In §"Wave 0 Requirements" find the bullet for `FaceDetectorOptionsTest.kt`. Change:

       > `- isTrackingEnabled == true`

       To:

       > `- isTrackingEnabled == false` (updated 2026-04-19 per GAP-02-A / ADR-01 — ML Kit silently ignores .enableTracking() under CONTOUR_MODE_ALL)

    Do NOT modify any other validation rows — leave CAM-01..CAM-07 and CAM-09 exactly as they are.
  </action>
  <verify>
    <automated>test -f .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md && grep -c "BboxIouTracker" .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md</automated>

    Additional grep checks:
    - `grep -c "GAP-02-A" .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` MUST return `>= 2` (D-15 and D-22 both amended).
    - `grep -c "ADR-01" .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` MUST return `>= 2`.
    - `grep -c "amended 2026-04-19" .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` MUST return `>= 2`.
    - `grep -c "isTrackingEnabled == false" .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md` MUST return `>= 1`.
    - `grep -c "CAM-08 (relaxed" .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md` MUST return `>= 1`.
    - ADR file word count: `wc -w .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md` MUST return `>= 300`.
  </verify>
  <done>
    ADR-01 exists with Status/Context/Decision/Consequences/Follow-ups sections. CONTEXT D-15 + D-22 both amended with GAP-02-A + ADR-01 cross-refs. VALIDATION Map + Manual-Only rows reflect the relaxed CAM-08 acceptance. Wave 0 checklist corrected.
  </done>
</task>

<task type="auto">
  <name>Task 3: Amend PITFALLS.md §3 to remove misleading .enableTracking() recommendation</name>
  <files>
    .planning/research/PITFALLS.md
  </files>
  <action>
    Open `.planning/research/PITFALLS.md`. Find §3 (the "landmark jitter / 1€ filter" pitfall, currently at line 110 in the "How to avoid" bullet list).

    The offending bullet currently reads (verbatim):

    > `- Enable **face tracking** (`.enableTracking()`) so the same face keeps a stable `trackingId` across frames — your filter state (position, velocity, sprite animation phase) is keyed off `trackingId`, not "first face in list."`

    Replace it with the following two-paragraph callout:

    > `- **Do NOT enable tracking (`.enableTracking()`) when `CONTOUR_MODE_ALL` is active** — Google ML Kit silently ignores the call at runtime; `face.trackingId` is always null, and `FaceDetectorOptions.isTrackingEnabled` reflective-reports `true` so unit tests miss the drift. Verified on Xiaomi 13T / HyperOS (Bugzz project GAP-02-A, 459/459 null trackingIds in 20s, 2026-04-19).`
    >
    > `- **If you need stable per-face identity with contour detection:** implement a boundingBox-IoU tracker (spatial centroid-overlap between consecutive frames assigns a monotonic local ID). This is what MediaPipe does internally. Budget ~100 LOC Kotlin + unit tests. Bugzz defers this to Phase 3 per `02-ADR-01-no-ml-kit-tracking-with-contour.md`.`
    >
    > `- **If your app does not need contour points** (you can anchor off bounding-box + coarse landmarks only): use `LANDMARK_MODE_ALL` + `.enableTracking()` instead. This path preserves ML Kit trackingId but loses the 100+ contour points needed for Phase 4 CRAWL behavior along face edges.`

    Also update §3 "Warning signs" sub-section to add one bullet at the end of its list:

    > `- trackingId always `null` in logcat despite `.enableTracking()` being wired — you hit the contour/tracking exclusivity; see above fix.`

    Do NOT edit §1, §2, §4, §5, §6, §7, §8, §9, §10, §11, §12, or §13. Do NOT rename §3. Do NOT delete any other recommendation in §3.

    After edits, verify the file still parses as markdown (no broken bullet indentation) and that no other pitfall was accidentally modified.
  </action>
  <verify>
    <automated>grep -c "silently ignores" .planning/research/PITFALLS.md && grep -c "GAP-02-A" .planning/research/PITFALLS.md && grep -c "02-ADR-01" .planning/research/PITFALLS.md</automated>

    Additional grep:
    - `grep -B1 -A1 "Enable \*\*face tracking\*\*" .planning/research/PITFALLS.md` MUST return no output (the old recommendation is gone).
    - `grep -c "CONTOUR_MODE_ALL" .planning/research/PITFALLS.md` MUST return `>= 1` (the amendment introduces the mode name).
    - `grep -c "MediaPipe" .planning/research/PITFALLS.md` MUST return `>= 1` (IoU tracker rationale).
  </verify>
  <done>
    PITFALLS §3 no longer recommends `.enableTracking()` with contour mode. The amendment cites GAP-02-A evidence + Bugzz ADR-01 + the MediaPipe-style bbox-IoU alternative. Other pitfalls untouched.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Biometric sensor → in-process | ML Kit face detection in the analysis pipeline; face landmarks stay in-process |
| In-process → logcat | `Timber` FaceTracker log lines — contain boundingBox centroid + trackingId (now always null) + contour count |

## STRIDE Threat Register (inherited from Phase 2; relevant carry-forwards)

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-02-02 | I (Information disclosure) | `FaceDetectorClient` Timber `FaceTracker` logs | mitigate | No raw landmark point coordinate lists written to log. Only aggregate: `t=%d id=%s bb=%d,%d contours=%d`. This plan does NOT add new log sites; the existing log line at FaceDetectorClient.kt:60 already meets the contract. No change needed. |
| T-02-06 | I (Information disclosure) | `FaceDetectorClient` state in release builds | accept | `Timber.tag("FaceTracker").v(...)` is verbose-level; in release builds, no DebugTree is planted → Timber no-ops. Verified in Phase 1. No change in this plan. |
| T-02-NEW-A | R (Repudiation) | ADR-01 | mitigate | ADR documented with date + authorship + evidence ref so Phase 3 planner cannot accidentally re-introduce `.enableTracking()` without seeing the rejection rationale. |
</threat_model>

<verification>
- `./gradlew :app:testDebugUnitTest --tests "*FaceDetectorOptionsTest*" --tests "*OneEuroFilterTest*" --tests "*OverlayEffectBuilderTest*" --tests "*CameraControllerTest*"` exits 0 — all 10 existing Phase 2 Nyquist unit tests remain GREEN.
- `./gradlew :app:assembleDebug` exits 0 — APK still builds (~82 MB).
- `grep -c "enableTracking" app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` returns `0`.
- `grep -c "enableTracking" app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` returns `0`.
- `grep -rc "GAP-02-A\|ADR-01" .planning/phases/02-camera-preview-face-detection-coordinate-validation/ .planning/research/PITFALLS.md` returns `>= 6` (ADR file itself counts, plus 2 CONTEXT amendments, plus PITFALLS amendment, plus CAM-08 VALIDATION row, plus FaceDetectorClient KDoc ref).
- ADR-01 file exists and contains `## Status`, `## Context`, `## Decision`, `## Consequences`, `## Follow-ups` sections.
</verification>

<success_criteria>
GAP-02-A closed when:
- `.enableTracking()` removed from `FaceDetectorClient.buildOptions()` and `FaceDetectorOptionsTest` expected options.
- `FaceDetectorOptionsTest` passes asserting `trackingEnabled=false`.
- ADR-01 exists documenting the decision with Phase 3 follow-ups.
- `02-CONTEXT.md` D-15 + D-22 amended with GAP-02-A + ADR-01 cross-refs.
- `02-VALIDATION.md` CAM-08 row relaxed + Wave 0 `FaceDetectorOptionsTest` checklist assertion corrected.
- `.planning/research/PITFALLS.md §3` amendment replaces the misleading `.enableTracking()` recommendation with the contour-mode exclusivity callout + bbox-IoU alternative + LANDMARK_MODE_ALL alternative.
- All Phase 2 existing unit tests remain GREEN (10/10 pre- and post-change).
- APK builds cleanly.
</success_criteria>

<output>
After completion, create `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-gaps-01-SUMMARY.md` covering:
- Files modified (6 total)
- Grep evidence outputs showing the assertions above passed
- Unit test run output confirming 10/10 GREEN
- APK build exit code
- Cross-reference: this plan closes GAP-02-A from `02-VERIFICATION.md`. GAP-02-B + GAP-02-C remain open, see `02-gaps-02-PLAN.md` and `02-gaps-03-PLAN.md`.
</output>
</content>
</invoke>