---
phase: 02-camera-preview-face-detection-coordinate-validation
plan: 01
subsystem: testing
tags: [junit4, mockito-kotlin, camerax-1.6.0, mlkit-face-detection-16.1.7, nyquist-tdd, 1-euro-filter]

# Dependency graph
requires:
  - phase: 01-foundation-skeleton
    provides: JUnit 4 test runner wiring, `ExampleUnitTest` scaffold, package `com.bugzz.filter.camera`, KSP + Hilt baseline
provides:
  - Four Nyquist unit-test files pinning CAM-03, CAM-04, CAM-05, CAM-06, CAM-09 contracts
  - Compile-time contract for `OneEuroFilter(minCutoff, beta, dCutoff)` + `filter(x, tNanos)` signature (Plan 02-03 implements)
  - Compile-time contract for `FaceDetectorClient.buildOptions()` companion producing D-15 FaceDetectorOptions (Plan 02-03 implements)
  - Compile-time contract for `OverlayEffectBuilder.TARGETS` + `OverlayEffectBuilder.QUEUE_DEPTH` companion constants (Plan 02-04 implements)
  - Compile-time contract for `CameraController(appContext, cameraExecutor, faceDetector, overlayEffectBuilder)` + `bind(owner, lens, rotation)` producing 4-usecase + 1-effect UseCaseGroup with KEEP_ONLY_LATEST (Plan 02-04 implements)
  - VALIDATION.md Wave 0 gate flipped (`nyquist_compliant: true`, `wave_0_complete: true`)
affects: [plan-02-02, plan-02-03, plan-02-04, phase-03, phase-04, phase-05]

# Tech tracking
tech-stack:
  added: []  # No new runtime deps this plan — pure test files; Plan 02-02 adds mockito-kotlin to testImplementation
  patterns:
    - Nyquist-TDD RED-before-GREEN (test files committed in separate wave before any feat commit)
    - Companion-constant testability seam (`OverlayEffectBuilder.TARGETS`/`QUEUE_DEPTH`) so Android-Handler-dependent classes stay unit-testable
    - Provider-factory seam requirement for `CameraController` (constructor-injected `providerFactory: suspend (Context) -> ProcessCameraProvider` default) — flagged as Plan 02-04 acceptance criterion

key-files:
  created:
    - app/src/test/java/com/bugzz/filter/camera/detector/OneEuroFilterTest.kt
    - app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt
    - app/src/test/java/com/bugzz/filter/camera/render/OverlayEffectBuilderTest.kt
    - app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt
  modified:
    - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md

key-decisions:
  - "1€ filter test harness fixes parameters to literature defaults (minCutoff=1.0, beta=0.007, dCutoff=1.0) per D-20 — tests assert the default constructor produces these values, preventing silent drift."
  - "FaceDetectorOptionsTest asserts each of 6 ML Kit FaceDetectorOptions fields individually by name rather than constructing an `expected == actual` options pair — catches drift on a per-field basis with targeted failure messages referencing D-15 directly."
  - "OverlayEffectBuilder exposes `TARGETS` and `QUEUE_DEPTH` as companion-object constants so the builder's architectural contract (three-stream mask + zero-queue-depth) is testable in a plain JVM harness. OverlayEffect itself needs Android Handler/HandlerThread and is not unit-testable without Robolectric."
  - "CameraControllerTest methods are `@Ignore`d with inline TODOs documenting the provider-factory seam Plan 02-04 must add. File + test-method signatures still pin the contract (4 usecases + 1 effect + KEEP_ONLY_LATEST); unIgnore happens in Plan 02-04."
  - "Intentional Nyquist RED for all 4 files: compilation will fail with `Unresolved reference` until Plan 02-02 (adds mockito-kotlin testImplementation) and Plans 02-03/02-04 (land SUT classes) close the gap. This is the gate that blocks any premature `feat(02-...)` commit."

patterns-established:
  - "Nyquist-TDD RED gate: every phase that introduces architecturally load-bearing pure-logic units (math, options builders, usecase group builders) ships failing tests in Wave 0 before any `feat(...)` commit touches the SUT. Commit message prefix `test(phase-plan-NN): ...`; acceptance criterion includes expected `Unresolved reference` compile error."
  - "Testability seam via companion-object constants: Android SDK classes that require Handler/HandlerThread/Context at construction are wrapped by a builder whose configuration surface is exposed as companion constants (`TARGETS`, `QUEUE_DEPTH`). The builder's `build()` returns the live SDK object; tests pin the constants without paying the Android-runtime cost."
  - "Provider-factory seam: classes that call static `SomeProvider.awaitInstance(ctx)` must accept `providerFactory: suspend (Context) -> SomeProvider = { SomeProvider.awaitInstance(it) }` as a constructor default parameter, so unit tests can inject a mock provider without Robolectric."

requirements-completed: [CAM-03, CAM-04, CAM-05, CAM-06, CAM-09]
# Note: "completed" here means "failing test ships for the requirement"; the requirements
# graduate to GREEN (implementation done) when Plans 02-02/02-03/02-04 land. Nyquist gate
# satisfied — requirements are pinned to their contract.

# Metrics
duration: 3m 17s
completed: 2026-04-19
---

# Phase 2 Plan 1: Nyquist Unit-Test Gate Summary

**Four Kotlin unit-test files pinning CAM-03/04/05/06/09 contracts (1€ filter math, ML Kit FaceDetectorOptions, OverlayEffect target mask, CameraController UseCaseGroup+backpressure) — intentionally RED until Plans 02-02/02-03/02-04 turn them GREEN.**

## Performance

- **Duration:** 3 min 17 s
- **Started:** 2026-04-19T08:32:00Z
- **Completed:** 2026-04-19T08:35:17Z
- **Tasks:** 3 (one per `<task>` block in 02-01-PLAN.md)
- **Files modified:** 5 (4 test files created + 1 VALIDATION.md updated)

## Accomplishments

- Nyquist-TDD gate established for Phase 2: four failing unit-test files exist under `app/src/test/java/com/bugzz/filter/camera/{detector,render,camera}/` before any `feat(02-...)` commit, pinning the D-15 / D-20 / D-25 contracts from 02-CONTEXT.md.
- `OneEuroFilterTest` exercises all four canonical 1€ filter properties (passthrough, step smoothing, sine attenuation, divide-by-zero safety) with tight epsilons — Plan 02-03 cannot ship a sloppy implementation without tripping these.
- `FaceDetectorOptionsTest` asserts six individual ML Kit fields (performanceMode, contourMode, tracking, minFaceSize, landmarkMode, classificationMode) with D-15 references in every failure message — guards against silent drift on the contour+landmark+classification compatibility pitfall (PITFALLS #3).
- `OverlayEffectBuilderTest` pins the three-stream compositing contract via companion constants, establishing a reusable testability pattern for Android-Handler-dependent SDK wrappers.
- `CameraControllerTest` pins the 4-usecase + 1-effect + KEEP_ONLY_LATEST contract in two `@Ignore`d tests with inline TODOs specifying the provider-factory seam Plan 02-04 must add.
- VALIDATION.md frontmatter flipped `nyquist_compliant: true` + `wave_0_complete: true`; Per-Task Verification Map populated with Task IDs 02-01-01/02-01-02/02-01-03.

## Task Commits

Each task was committed atomically:

1. **Task 1: OneEuroFilterTest** — `98b3348` (test)
2. **Task 2: FaceDetectorOptionsTest + OverlayEffectBuilderTest (parallel files)** — `97a5378` (test)
3. **Task 3: CameraControllerTest with @Ignore seams** — `693d986` (test)

**Plan metadata:** `6829e28` (docs: flip `nyquist_compliant=true` + fill Per-Task Verification Map)

## Files Created/Modified

- `app/src/test/java/com/bugzz/filter/camera/detector/OneEuroFilterTest.kt` — 4 `@Test` methods pinning 1€ filter math (CAM-09). Asserts constant passthrough within ε=1e-6, step convergence within 5% of target after 10 samples, sine RMS attenuation, and divide-by-zero safety on `dt=1ns`.
- `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` — 1 `@Test` method asserting 6 D-15 field values on `FaceDetectorClient.buildOptions()` output (CAM-04).
- `app/src/test/java/com/bugzz/filter/camera/render/OverlayEffectBuilderTest.kt` — 2 `@Test` methods asserting `OverlayEffectBuilder.TARGETS == PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE` and `OverlayEffectBuilder.QUEUE_DEPTH == 0` (CAM-06).
- `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt` — 2 `@Ignore`d `@Test` methods pinning UseCaseGroup size (4 usecases + 1 effect) and `STRATEGY_KEEP_ONLY_LATEST` backpressure (CAM-03, CAM-05). Inline `TODO (Plan 04 seam)` comments document the `providerFactory` constructor-parameter requirement.
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md` — frontmatter flags flipped; Per-Task Verification Map rows now reference task IDs 02-01-01/02-01-02/02-01-03; Wave 0 Requirements checkboxes marked `[x]`.

## Decisions Made

- **`@Ignore` vs. skip on CameraControllerTest:** Chose `@Ignore` with inline activation instructions over simply deleting the `@Test` annotations. Rationale: the test-method bodies document the exact assertion shape Plan 02-04 must enable, so they double as a spec. Removing `@Test` would lose that machine-readable contract.
- **Mockito approach in CameraControllerTest:** Used the `mockito-kotlin` DSL (`mock()`, `whenever(...).thenReturn(...)`) consistently throughout. The plan's inline `doReturn` free-function redefinition was dropped in favour of `whenever(...).thenReturn(...)` to avoid conflicting with `mockito-kotlin`'s own `doReturn` import (Plan 02-02 adds `mockito-kotlin` to `testImplementation`). The two tests are `@Ignore`d, so the concrete DSL choice does not affect the RED state.
- **Epsilon tuning for `sine_jitter_attenuated_vs_rms`:** Used a 6 Hz sine (period = 5 samples at 30 fps) riding on a stationary base — well above the default `minCutoff=1.0` Hz so attenuation is guaranteed to be measurable. Raw RMS ≈ √2 ≈ 1.414; filtered RMS should drop below that. Kept the assertion as a simple `<` rather than a specific ratio to avoid implementation-specific brittleness.

## Deviations from Plan

None — plan executed exactly as written for Tasks 1 and 2.

**Task 3 (CameraControllerTest) minor adaptation** (not a deviation — plan expected this):

The plan's inline `doReturn` DSL sketch used `mock { on { ... } doReturn mock<T>() }` infixed with a hand-rolled `doReturn` extension appended at the file bottom. Because `mockito-kotlin` exports its own `doReturn` symbol that collides with the hand-rolled one (and the plan's snippet was marked "Executor fills in assertion details"), the final file uses the more conventional `whenever(mock.method()).thenReturn(value)` pattern. Both tests remain `@Ignore`d and the contract they pin (4 usecases + 1 effect + KEEP_ONLY_LATEST) is unchanged — the plan's success criteria and acceptance_criteria tokens are all present.

This is a documentation / mechanical adaptation to avoid a compiler-confusing import collision, not a semantic deviation. Call it out here for transparency.

**Total deviations:** 0 auto-fixed. Plan's inline mockito DSL sketch was resolved to an equivalent mockito-kotlin idiom; no Rule-1/2/3 auto-fixes triggered.

## Issues Encountered

- **Pre-existing unrelated unstaged changes:** At the start of this plan, `.planning/STATE.md` and `.planning/config.json` had pre-existing orchestrator modifications in the working tree (from the execute-phase init before this executor started). These were left untouched during per-task commits (staged only the Task's specific test files) and are expected to be absorbed by the state-update step's `record-session` + `advance-plan` invocations below.
- **Windows CRLF warnings:** Git warned `LF will be replaced by CRLF the next time Git touches it` on every committed `.kt` file. This is benign on Windows hosts and matches existing repo state; no `.gitattributes` override needed for Phase 2.

## User Setup Required

None — no external service configuration required. Phase 2 Plan 1 ships only JVM unit tests; no gradle-deps, no SDK keys, no device action.

## Known Stubs

None in the Phase 2 runtime surface. The four test files reference SUT classes that do not yet exist — this is the **intentional Nyquist RED** declared in 02-VALIDATION.md §Wave 0 Requirements and 02-CONTEXT.md deep-work rule. Plans 02-02, 02-03, 02-04 close the gap:

- Plan 02-02 (dependencies) adds `mockito-kotlin` to `testImplementation` — unblocks `CameraControllerTest` compilation of mockito DSL imports.
- Plan 02-03 (detector) creates `OneEuroFilter.kt` and `FaceDetectorClient.kt` — turns `OneEuroFilterTest` + `FaceDetectorOptionsTest` GREEN.
- Plan 02-04 (render + camera) creates `OverlayEffectBuilder.kt` (with `TARGETS`/`QUEUE_DEPTH` companion constants) and `CameraController.kt` (with `providerFactory` seam per Task 3 TODO) — turns `OverlayEffectBuilderTest` GREEN and allows Plan 02-04 to un-`@Ignore` the two `CameraControllerTest` methods.

Downstream dependency note, per plan `<output>` contract:

> **Plan 02-04 must add a `providerFactory: suspend (Context) -> ProcessCameraProvider = { ProcessCameraProvider.awaitInstance(it) }` constructor-injected default parameter on `CameraController`. Without it, `CameraControllerTest` cannot un-`@Ignore` without Robolectric, which Phase 1 D-21 explicitly defers.**

## Next Phase Readiness

- Plan 02-02 (dependencies + version-catalog wiring) can now proceed: Nyquist gate satisfied, `nyquist_compliant=true` in VALIDATION.md frontmatter.
- Plans 02-03 and 02-04 have machine-readable contracts they must honour (the 4 test files); silent drift from D-15 / D-20 / D-25 / CAM-03/04/05/06/09 is now caught at the test runner.
- No blockers.

## Self-Check: PASSED

- [x] `app/src/test/java/com/bugzz/filter/camera/detector/OneEuroFilterTest.kt` exists
- [x] `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` exists
- [x] `app/src/test/java/com/bugzz/filter/camera/render/OverlayEffectBuilderTest.kt` exists
- [x] `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt` exists
- [x] Commit `98b3348` (test(02-01-01)) present in `git log`
- [x] Commit `97a5378` (test(02-01-02)) present in `git log`
- [x] Commit `693d986` (test(02-01-03)) present in `git log`
- [x] Commit `6829e28` (docs(02-01)) present in `git log`
- [x] All 4 test files contain their required `class ...Test` declaration and `@Test` methods
- [x] VALIDATION.md frontmatter `nyquist_compliant: true` + `wave_0_complete: true`

---
*Phase: 02-camera-preview-face-detection-coordinate-validation*
*Completed: 2026-04-19*
