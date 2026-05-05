---
phase: 06
plan: 01
subsystem: testing-scaffolds
tags: [wave-0, nyquist, red-tests, ux-polish]
dependency_graph:
  requires:
    - "Phase 5 unit suite GREEN baseline (143 tests)"
    - "FilterPrefsRepository internal-ctor test seam (Phase 4 STATE #30)"
    - "Robolectric @Config(sdk=[34]) pattern (Phase 5 VideoRecorderTest)"
  provides:
    - "8 NEW RED test files at canonical Phase 6 paths"
    - "3 EXT @Ignored cases in FilterPrefsRepositoryTest for onboardingCompleted"
    - "Per-task verification command targets for Plans 06-02..06-06"
  affects:
    - "06-VALIDATION.md (Plan 01 row marked complete; wave_0_complete: true)"
tech-stack:
  added: []   # No new test-time deps; Lottie / Media3 / lottie-compose are production deps landing in Plan 06-02
  patterns:
    - "@Ignore('Plan 06-NN — un-ignore when {Class} lands') + KDoc forward-pointer (Phase 5 Wave 0 precedent)"
    - "Pure JVM scaffolds use a private markMissing() no-op helper so test bodies compile cleanly while @Ignored at runtime"
    - "Robolectric @RunWith for tests touching Context/ContentResolver/Intent/Uri — pure JVM otherwise"
key-files:
  created:
    - "app/src/test/java/com/bugzz/filter/camera/ui/splash/SplashViewModelTest.kt (64 lines, 3 @Ignored)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/onboarding/OnboardingViewModelTest.kt (53 lines, 2 @Ignored)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/onboarding/OnboardingPagerStateTest.kt (62 lines, 3 @Ignored)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/preview/PreviewViewModelTest.kt (72 lines, 4 @Ignored)"
    - "app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt (86 lines, 4 @Ignored, Robolectric)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/collection/CollectionViewModelTest.kt (49 lines, 2 @Ignored)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/components/DeleteConfirmDialogTest.kt (49 lines, 2 @Ignored)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilderTest.kt (82 lines, 4 @Ignored, Robolectric)"
  modified:
    - "app/src/test/java/com/bugzz/filter/camera/data/FilterPrefsRepositoryTest.kt (137 → 199 lines; +3 @Ignored cases for onboardingCompleted)"
    - ".planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-VALIDATION.md (frontmatter wave_0_complete: false → true; Plan 01 row status; checklist [ ] → [x])"
decisions:
  - "Stub helper pattern: every new test class uses a private markMissing() no-op so test bodies compile cleanly while remaining @Ignored. Avoids dragging Plan 06-02..07 production class imports into Wave 0 test sources (would force production stubs to ship now to keep tests compiling — same anti-pattern as Phase 2 Plan 02-03 placeholder stubs that had to be deleted in 02-04). Implementer in landing plan replaces markMissing() with real construction."
  - "Production sources untouched in Wave 0. All 9 D-32 grep-asserts re-verified post-task — no regression to Phase 3/4/5 fixes."
metrics:
  duration: ~600s
  completed: 2026-05-05
---

# Phase 6 Plan 01: Wave 0 RED Test Scaffolds Summary

**One-liner:** 8 RED test scaffolds + 1 extension to FilterPrefsRepositoryTest landed for Phase 6 UX Polish — Splash/Onboarding/Preview/Collection/DeleteDialog/Share + onboardingCompleted prefs path; suite GREEN at 170 tests / 27 ignored / 0 failures; per-task verification command targets for Plans 06-02..06-06 are pre-wired.

---

## What Landed

### 8 New Test Files (24 @Ignored cases total)

| File | Path | Tests | Runner | Lands in |
|------|------|-------|--------|----------|
| SplashViewModelTest | `ui/splash/` | 3 | pure JVM | Plan 06-03 |
| OnboardingViewModelTest | `ui/onboarding/` | 2 | pure JVM | Plan 06-03 |
| OnboardingPagerStateTest | `ui/onboarding/` | 3 | pure JVM | Plan 06-03 |
| PreviewViewModelTest | `ui/preview/` | 4 | pure JVM | Plan 06-04 |
| CollectionRepositoryTest | `data/` | 4 | Robolectric (sdk=34) | Plan 06-05 |
| CollectionViewModelTest | `ui/collection/` | 2 | pure JVM | Plan 06-05 |
| DeleteConfirmDialogTest | `ui/components/` | 2 | pure JVM | Plan 06-06 |
| ShareIntentBuilderTest | `ui/share/` | 4 | Robolectric (sdk=34) | Plan 06-06 |

### 1 EXTENSION (3 new @Ignored cases)

`app/src/test/java/com/bugzz/filter/camera/data/FilterPrefsRepositoryTest.kt`:
- 4 existing GREEN tests (writeThenRead, readBeforeWrite, writeAgain, corruptedDataStore) **untouched**
- +3 new @Ignored("Plan 06-02") cases for the onboardingCompleted prefs path:
  - `writeOnboardingCompleted_thenRead_returnsTrue` (UX-02 round-trip)
  - `readOnboardingCompleted_beforeWrite_returnsFalseDefault` (UX-01 first-launch default)
  - `corruptedDataStore_onboardingCompleted_emitsFalseDefault` (T-06-04 mirror of T-04-01)

### Coverage Crosswalk (PLAN frontmatter `requirements`)

| Req ID | Test File | Status |
|--------|-----------|--------|
| UX-01 | SplashViewModelTest + FilterPrefsRepositoryTest (ext) | RED scaffold landed |
| UX-02 | OnboardingViewModelTest + OnboardingPagerStateTest + FilterPrefsRepositoryTest (ext) | RED scaffold landed |
| UX-04 | PreviewViewModelTest | RED scaffold landed |
| UX-05 | CollectionRepositoryTest | RED scaffold landed |
| UX-07 | CollectionViewModelTest | RED scaffold landed |
| UX-08 | DeleteConfirmDialogTest | RED scaffold landed |
| SHR-01 | ShareIntentBuilderTest (innerIntentExtraStream_equalsPassedUri) | RED scaffold landed |
| SHR-02 | ShareIntentBuilderTest (innerIntentType_matchesPassedMimeType) | RED scaffold landed |
| SHR-03 | ShareIntentBuilderTest (buildShareIntent_resultActionIsActionChooser) | RED scaffold landed |

---

## Verification Results

### Suite GREEN (`./gradlew :app:testDebugUnitTest -x lintDebug`)

| Metric | Baseline (pre-Plan 01) | After Task 1 | After Task 2 (final) |
|--------|------------------------|--------------|----------------------|
| Total tests | 155 | 155 | **170** |
| Failures | 0 | 0 | **0** |
| Ignored | 0 | 12 | **27** |
| Successful | 100% | 100% | **100%** |
| Duration | ~12s | ~12s | **~11s** |

Note: "Baseline 155 / 0 ignored" reflects the snapshot at execution start. Phase 5 SUMMARY recorded 143 GREEN; the delta (155 - 143 = 12) is consistent with Phase 5 closure landing additional tests committed after the 05-07 SUMMARY metric capture.

Final state: **170 tests / 27 IGNORED / 0 failures.**

### IGNORED Distribution (from build/reports/tests/testDebugUnitTest/index.html)

- **24 @Ignored across 8 NEW Wave 0 files:** 3 + 2 + 3 + 4 + 4 + 2 + 2 + 4 = 24 ✓
- **3 @Ignored in FilterPrefsRepositoryTest extension:** ✓
- **27 total ignored** = 24 + 3 = matches plan's 11+ requirement (well above the floor)

### D-32 Grep-Assert Re-verification (production source untouched)

All 9 patterns verified post-Task-2 (pre-existing fixes from Phase 3/4/5 still in place; Wave 0 modified ZERO production files):

| # | Pattern (asserted) | Hits | File(s) |
|---|--------------------|------|---------|
| 1 | `isCapturing` | 14 | CameraViewModel + InsectFilterViewModel + UI states |
| 2 | `bindJob?.cancel()` *(see Deviation 1)* | 1 | CameraViewModel.kt:102 |
| 3 | `OneShotEvent.FilterLoadError` | 7 | CameraScreen + CameraViewModel + InsectFilterScreen |
| 4 | `captureFlash` | 13 | CameraScreen + CameraViewModel + InsectFilter |
| 5 | `require(frameCount > 0)` | 1 | FilterDefinition.kt |
| 6 | `assetLoader.preload(def.assetDir)` | 3 | StickerRenderer + CameraViewModel |
| 7 | `isRecording` | 47 | VideoRecorder + 8 other files |
| 8 | `CameraMode.InsectFilter` *(see Deviation 1)* | 4 | BugzzApp + InsectFilterViewModel + OverlayEffectBuilder |
| 9 | `setPreviewSize` | 2 | StickerRenderer + InsectFilterViewModel |

**ALL 9 PATTERNS RETURN ≥1 MATCH.** No regression to D-32 fixes (Phase 3 dafc21e/9abbd0b/6ff00e0/4e94591/b7f74cf, Phase 4 514410c, Phase 5 D-26/gap-01 37b7a17/gap-02 de27c4e).

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 — Blocking issue] Kotlin nested-comment treatment of `image/*` and `video/*` literals broke compile**
- **Found during:** Task 2 (`./gradlew :app:testDebugUnitTest` after creating CollectionRepositoryTest.kt)
- **Issue:** First Task 2 build failed with `Syntax error: Unclosed comment.` at CollectionRepositoryTest.kt:87:1. Root cause: Kotlin's KDoc lexer treats `/*` as a nested-block-comment opener even *inside* a `/** */` KDoc block. Lines 17 & 18 had `image/*` and `video/*` literally describing MIME-type prefix matching, which kotlinc misparsed as starting nested comments and then "consumed" the closing `*/` of the outer KDoc.
- **Fix:** Reworded the two bullets to use `image/` and `video/` (no asterisk) — semantically equivalent for the test intent description. Functional KDoc reads cleanly; no production behavior implication.
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt` (lines 17-18)
- **Commit:** `cc16307` (fix folded into the same Task 2 commit; recorded in commit message)
- **Pattern for future Wave 0 scaffolds:** in KDoc that describes MIME wildcards or glob patterns, write `image/<sub>` or `image/<asterisk>` instead of `image/*` to avoid the nested-comment trap. Same applies to any pattern containing `/*` inside a doc comment.

**2. [Rule 3 — Plan-text drift] Two D-32 grep-asserts use exact strings that don't match actual code shape**
- **Found during:** Task 3 (final D-32 verification gate — pre-Wave-0 sanity check)
- **Issue:** Plan Task 3 asks to grep production sources for these literal strings:
  - `bindJob.cancel()` → actual code uses `bindJob?.cancel()` (Kotlin safe-call) at CameraViewModel.kt:102.
  - `cameraMode = CameraMode.InsectFilter` → actual code uses fully-qualified name `cameraMode = com.bugzz.filter.camera.ui.home.CameraMode.InsectFilter` at InsectFilterViewModel.kt:100.
  Both literal patterns, as written in the plan, return 0 matches — but the underlying Phase 3 / Phase 5 fixes ARE present in code. The plan's grep strings were authored without the safe-call / FQN nuances.
- **Fix:** Relaxed two of the nine grep-asserts to forms that match actual code shape:
  - `bindJob.cancel()` → `bindJob?.cancel()` (keeps the intent: cancel call on the job before re-bind, preserving WR-03)
  - `cameraMode = CameraMode.InsectFilter` → `CameraMode.InsectFilter` (substring match — covers FQN form and would also match the un-qualified form if it ever returns)
- **Files modified:** None — the relaxation is documented here, not in source. Production code is correct as-is.
- **Why Rule 3:** D-32 is a regression-detection harness; the literal strings were the *intent*, not the *contract*. Claiming the harness "passes" is meaningless if the test pattern doesn't match the real fix shape. Per Rule 3, the blocking issue is a plan-text drift, not a production bug — fixed by documenting the relaxation here so Plans 02..07 use the same matching strings going forward.
- **Pattern for future Wave 0 D-32 sweeps:** when a fix lands as `?.method()` or with FQN, the grep-assert pattern in subsequent plans should use the actual surface form. Future plan authors: prefer substring patterns over full-line strings for fix-presence asserts.

### Manual Inspections (no auto-fix; documented for landing-plan implementers)

**Plan 06-04 implementer note (PreviewViewModelTest):** the scaffold tests assume PreviewViewModel will be constructed with a `SavedStateHandle` carrying `mime` + `uri` keys. If Plan 06-04 instead passes the URI via Hilt assisted injection or a different SavedStateKey shape, the implementer will need to adapt the test body — the @Ignore message intentionally points to "PreviewViewModel lands" without prescribing the exact ctor shape.

**Plan 06-05 implementer note (CollectionRepositoryTest selection test):** the fourth test (`selection_excludesRowsOutsideDcimBugzzRelativePath`) is asserted via cursor-stub composition, not by parsing the actual SQL selection string. Production code may either (a) rely on the cursor returning only in-scope rows because the SELECTION argument was correct, or (b) post-filter inside the repository. Both shapes pass the test; the implementer chooses.

---

## Authentication Gates

None encountered — Wave 0 is test-classpath only, no DataStore writes / no MediaStore queries / no network at runtime.

---

## Wave 1 Readiness Sign-Off

- [x] 8 new test files at exact paths specified in `files_modified` frontmatter
- [x] FilterPrefsRepositoryTest extended with 3 new @Ignored cases for onboardingCompleted (existing 4 GREEN tests untouched)
- [x] Full unit suite GREEN (170 / 0 / 27)
- [x] No production source files touched in this plan
- [x] All 9 D-32 grep-asserts return ≥1 match (2 patterns relaxed per Deviation 2 — fixes verified present)
- [x] 06-VALIDATION.md Plan 01 row marked `✅ Wave 0 RED scaffolds GREEN`; frontmatter `wave_0_complete: true`
- [x] Per-task verification command targets pre-wired for Plans 06-02..06-06 (`*FilterPrefsRepositoryTest*onboarding*`, `*SplashViewModelTest*`, `*OnboardingViewModelTest*`, `*OnboardingPagerStateTest*`, `*PreviewViewModelTest*`, `*CollectionRepositoryTest*`, `*CollectionViewModelTest*`, `*DeleteConfirmDialogTest*`, `*ShareIntentBuilderTest*`)

**Wave 1 (Plan 06-02) may proceed.** Plan 06-02 will:
1. Land production deps (lottie-compose 6.7.1, media3-ui 1.4.1, media3-exoplayer 1.4.1)
2. Add `onboardingCompleted: Flow<Boolean>` + `setOnboardingCompleted()` to FilterPrefsRepository
3. Un-ignore the 3 EXTENSION tests in FilterPrefsRepositoryTest

After Plan 06-02, the per-task verify command `./gradlew :app:testDebugUnitTest --tests "*FilterPrefsRepositoryTest*onboarding*"` will fire and run the 3 newly-un-ignored tests (instead of skipping them as IGNORED).

---

## Commits

| Task | Commit | Files |
|------|--------|-------|
| Task 1 | `151c92a` | 4 new ViewModel test scaffolds (Splash, Onboarding x2, Preview) |
| Task 2 | `cc16307` | 4 new tests (CollectionRepository, CollectionViewModel, DeleteConfirmDialog, ShareIntentBuilder) + FilterPrefsRepositoryTest extension + nested-comment fix |
| Task 3 | (no source change — verification gate only; subsumed by docs commit) | 06-VALIDATION.md status update |

---

## Self-Check: PASSED

- All 8 new test files exist at canonical paths ✓
- FilterPrefsRepositoryTest has 3 new @Ignored cases below corruptedDataStore_emitsDefault ✓
- Commit `151c92a` exists ✓
- Commit `cc16307` exists ✓
- 06-VALIDATION.md Plan 01 row contains `✅ Wave 0 RED scaffolds GREEN` ✓
- 06-VALIDATION.md frontmatter has `wave_0_complete: true` ✓
- All 9 D-32 grep-asserts return ≥1 match (2 relaxed per Deviation 2) ✓
- Suite GREEN: 170 tests / 0 failures / 27 ignored ✓
