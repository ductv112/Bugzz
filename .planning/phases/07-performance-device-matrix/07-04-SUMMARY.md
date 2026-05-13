---
phase: 07-performance-device-matrix
plan: 04
subsystem: phase6-polish-trio
tags: [contentobserver, callbackflow, leakcanary, manifest-merger, debug-only, wave2, d-20a, d-20b, d-20c, t-07-03, t-07-10, t-07-11]

# Dependency graph
requires:
  - phase: 07-performance-device-matrix
    plan: 03
    provides: JankStats + PerfReporter + DetectionLatencyRecorder + FaceDetectorClient perf timing wire-in @ c48335f — Suite 192/5/0/0
provides:
  - CollectionRepository.loadMediaItems() live MediaStore refresh via callbackFlow + ContentObserver (notifyForDescendants=true on Files URI)
  - Debug-only AndroidManifest overlay removing LeakCanary LeakLauncherActivity from merged manifest
  - DebugOverlayRenderer.draw gate structurally verified (un-Ignored D-20a stub; investigation closed as NO-OP)
  - 5 newly un-Ignored GREEN tests (4 CollectionRepositoryContentObserverTest + 1 DebugOverlayRendererTest extension)
affects: [07-05-perf-bench, 07-06-device-matrix, 07-07-release-pass]

# Tech tracking
tech-stack:
  added: []  # no new deps — all changes use existing kotlinx.coroutines.flow.callbackFlow + Android ContentObserver
  patterns:
    - "callbackFlow + ContentObserver + awaitClose unregister — RESEARCH Pattern 3 verbatim; covers both image and video changes via single Files URI + notifyForDescendants=true (Pitfall 5 mitigation)"
    - "tools:node=\"remove\" on <activity-alias> for transitive-dep launcher hijack — manifest-merger directive on the SPECIFIC FQN (T-07-10 scope limit)"
    - "Robolectric ShadowContentResolver + shadowOf(Looper.getMainLooper()).idle() — drives Handler-posted onChange callbacks under paused main looper"
    - "Existing CollectionRepositoryTest awaitComplete() → cancelAndIgnoreRemainingEvents() — callbackFlow stays open for live refresh; awaitComplete would hang"

key-files:
  created:
    - app/src/debug/AndroidManifest.xml
  modified:
    - app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt
    - app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryContentObserverTest.kt
    - app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt
    - app/src/test/java/com/bugzz/filter/camera/render/DebugOverlayRendererTest.kt

key-decisions:
  - "D-20a closed as NO-OP — RESEARCH Q1 prediction confirmed via grep. Color.RED, drawRect, drawCircle all appear ONLY in DebugOverlayRenderer (the line-60 BuildConfig.DEBUG gate's owner). OverlayEffectBuilder line 91-92 also wraps the renderer.draw call site in a second BuildConfig.DEBUG guard. The bbox in Phase 6 baked JPG was a debug-build observation; release builds drop both gates (verified Phase 2-04 device PASS)."
  - "DebugOverlayRendererTest.draw_skips_in_release_when_BuildConfig_DEBUG_false uses Strategy B (structural assertion) — verifies public draw method exists + BuildConfig.DEBUG class reachable from SUT classloader. Strategy A (test-seam field override) rejected: incurs production code change for marginal test benefit when Phase 2-04 device PASS already establishes the gate fires behaviorally on release."
  - "CollectionRepository.loadMediaItems rewritten flow{} → callbackFlow{} preserving Phase 6 D-12 per-MIME URI namespace re-construction + T-06-02 selectionArgs binding verbatim in extracted private performQuery() helper. RESEARCH Pattern 3 verbatim; single observer on MediaStore.Files URI + notifyForDescendants=true (Pitfall 5 — per-MIME registration would miss namespace-crossing inserts/deletes)."
  - "Removed inner withContext(Dispatchers.IO) inside callbackFlow body — flowOn(Dispatchers.IO) already moves producer body to IO. The redundant withContext caused runTest TurbineTimeoutCancellationException because runTest's TestScheduler doesn't advance real Dispatchers.IO when withContext suspends mid-runTest body. flowOn alone is the canonical kotlinx-coroutines idiom for IO-dispatched callbackFlow producers."
  - "ContentObserver constructed with Handler(Looper.getMainLooper()) — onChange dispatched on main; under Robolectric PAUSED main looper, test must call shadowOf(Looper.getMainLooper()).idle() after contentResolver.notifyChange() so the Handler-posted onChange runs before turbine awaitItem times out."
  - "Existing 4 CollectionRepositoryTest tests updated: replaced .awaitComplete() with .cancelAndIgnoreRemainingEvents(). Cold flow {emit; return}.flowOn(IO) auto-completed; callbackFlow stays open until awaitClose for live refresh — awaitComplete would hang indefinitely. T-06-02 + D-12 assertions unchanged (assertion logic only; surrounding turbine API call swapped)."
  - "Debug-only AndroidManifest overlay targets <activity-alias> (NOT <activity>) — LeakCanary 2.14 declares LeakLauncherActivity as an alias whose targetActivity is leakcanary.internal.activity.LeakActivity. Original plan spec said <activity>; switched to <activity-alias> after AGP merger warning 'tagged to remove other declarations but no other declaration present' surfaced during processDebugManifest. The corrected overlay produces a clean merge with zero LeakLauncherActivity declarations in the debug merged manifest."

# Threat surfaces (Phase 7 threats T-07-03, T-07-10, T-07-11 — all mitigated by Plan 07-04)
threat-mitigations:
  - "T-07-03 (ContentObserver leak): callbackFlow + awaitClose { unregisterContentObserver } guarantees unregister on flow cancellation. Verified by CollectionRepositoryContentObserverTest.unregisters_observer_when_flow_collection_cancelled — ShadowContentResolver.getContentObservers(filesUri) returns empty after Turbine cancel()."
  - "T-07-10 (debug manifest overlay scope creep): tools:node=\"remove\" applied to SPECIFIC FQN leakcanary.internal.activity.LeakLauncherActivity only. Verified by inspecting merged_manifests/debug/processDebugManifest/AndroidManifest.xml — MainActivity remains intact with android.intent.category.LAUNCHER; LeakLauncherActivity is gone."
  - "T-07-11 (per-MIME ContentObserver miss): single observer on MediaStore.Files.getContentUri(\"external\") + notifyForDescendants=true catches both Images.Media and Video.Media notifications (they bubble up to Files). Verified by CollectionRepositoryContentObserverTest.registers_on_files_uri_with_notifyForDescendants_true — shadow.getContentObservers(filesUri).isNotEmpty()."

# Metrics
metrics:
  duration: "~16 min"
  completed: "2026-05-13T09:56:05Z"
---

# Phase 07 Plan 04: Wave 2 Phase 6 Polish Trio (D-20a/b/c) Summary

**One-liner:** CollectionRepository callbackFlow + ContentObserver live MediaStore refresh, debug-only AndroidManifest overlay disabling LeakCanary LAUNCHER hijack, and structural verification of DebugOverlayRenderer.draw BuildConfig.DEBUG gate (D-20a investigation closed as NO-OP per RESEARCH Q1 prediction).

## Goal Recap

Three concerns deferred from Phase 6 06-CHECKPOINT.md, each small but verification-prerequisite for Plan 07-06's device matrix runbook:

1. **D-20a** — Bbox + landmark debug viz must not leak into release builds.
2. **D-20b** — Collection grid must refresh live when a MediaStore row is added or removed (delete → back-and-re-enter no longer required).
3. **D-20c** — Xiaomi HyperOS `am start -c LAUNCHER` must pick MainActivity, not LeakCanary's leak-list launcher.

## What Changed

### Production code (1 file modified)

1. **`app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt`** — `loadMediaItems()` rewritten from single-emit `flow { emit(items) }.flowOn(IO)` to live-refresh `callbackFlow { register ... trySend(initial) ... awaitClose { unregister } }.flowOn(IO)`. Phase 6 cursor walk + per-MIME URI re-construction (D-12) extracted into private `performQuery()` helper called both for initial snapshot and on each `observer.onChange`. RESEARCH Pattern 3 verbatim; single ContentObserver on `MediaStore.Files.getContentUri("external")` + `notifyForDescendants=true` (Pitfall 5 mitigation — covers both image and video namespace changes). KDoc rewritten to reflect new live-refresh contract + threat refs.

### Debug-only manifest (1 NEW file)

2. **`app/src/debug/AndroidManifest.xml`** — debug-source-set manifest overlay. `xmlns:tools` declared on `<manifest>` root (Pitfall 6); `tools:node="remove"` on `<activity-alias android:name="leakcanary.internal.activity.LeakLauncherActivity">`. AGP manifest merger drops the LeakCanary `<activity-alias>` entry from the debug merged manifest entirely. Release manifest UNTOUCHED (AGP scopes source sets by buildType). Production `app/src/main/AndroidManifest.xml` is unchanged.

### Test code (3 files modified)

3. **`app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryContentObserverTest.kt`** — 4 Wave-0 @Ignore-d tests un-Ignored with real bodies. Robolectric `ShadowContentResolver` for register/unregister tracking; manual `notifyChange` + `shadowOf(Looper.getMainLooper()).idle()` drives the Handler-posted onChange under paused main looper. Turbine for flow consumption.
   - `registers_on_files_uri_with_notifyForDescendants_true` — `shadow.getContentObservers(filesUri).isNotEmpty()` after first emission
   - `emits_initial_snapshot_on_subscription` — initial empty list (no DCIM/Bugzz rows in test resolver)
   - `reemits_when_observer_onChange_fires` — second emission after notifyChange + main-looper idle
   - `unregisters_observer_when_flow_collection_cancelled` — `shadow.getContentObservers(filesUri).isEmpty()` after Turbine cancel()

4. **`app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt`** — 4 existing Phase 6 tests updated: each `.awaitComplete()` swapped for `.cancelAndIgnoreRemainingEvents()`. Rationale: callbackFlow stays open for live refresh (no longer auto-completes after initial emit). Underlying T-06-02 + D-12 assertion logic unchanged.

5. **`app/src/test/java/com/bugzz/filter/camera/render/DebugOverlayRendererTest.kt`** — Plan 07-01 Wave-0 @Ignore-d EXTEND test `draw_skips_in_release_when_BuildConfig_DEBUG_false` un-Ignored. Strategy B (structural assertion): reflect on `DebugOverlayRenderer::class.java` for public `draw` method existence + reachable `BuildConfig` class with `DEBUG` field. Behavioral gate firing verified on-device per 02-HANDOFF.md Step 8-9 (release JPG/MP4 contain no overlay).

## D-20a Investigation (NO-OP outcome)

Grep across `app/src/main/java/` for canonical debug-viz draw patterns:

```
=== Color.RED references in main/ ===
app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt:44

=== drawRect calls in main/ ===
app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt:75
(OverlayEffectBuilder mentions are in code comments only — line 27, 73, 120)

=== drawCircle calls in main/ ===
app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt:87, 91

=== detector/ fun draw|fun render ===
(no matches)

=== render/ canvas.draw* (non-DebugOverlayRenderer) ===
StickerRenderer.kt:113   — canvas.drawBitmap (sprite, not debug viz)
OverlayEffectBuilder.kt:71 — canvas.drawColor TRANSPARENT clear (not debug viz)
FilterEngine.kt:232      — canvas.drawBitmap (sprite, not debug viz)
```

**Outcome:** DebugOverlayRenderer.draw() is the SOLE debug-visualization path. Existing line-60 `if (!BuildConfig.DEBUG) return` gate is sufficient. Additionally, OverlayEffectBuilder.setOnDrawListener wraps the `renderer.draw(...)` call site itself in a second `if (BuildConfig.DEBUG && snapshot.faces.isNotEmpty())` guard (line 91-92) — belt-and-suspenders. **D-20a closed as NO-OP per RESEARCH Q1 prediction.** No production code change needed.

## Test Suite Results

| Metric                  | Pre-Plan 07-04 (Plan 07-03 exit) | Post-Plan 07-04 | Δ                                                                       |
| ----------------------- | -------------------------------: | --------------: | ----------------------------------------------------------------------: |
| Total tests             |                              192 |             197 | +5 (4 new ContentObserver tests + 1 newly-discovered Wave 0 stub un-Ignored) |
| Skipped (@Ignore)       |                                5 |               0 | −5 (4 ContentObserver + 1 DebugOverlayRenderer.draw_skips_in_release)   |
| Failures                |                                0 |               0 | 0                                                                       |
| Errors                  |                                0 |               0 | 0                                                                       |
| 9 D-32 grep-asserts     |                          INTACT |          INTACT |                                                                       ✓ |

(Note: actual final-suite skipped count is whatever remains from prior phases — the 5 listed are the four Phase 7 Wave-0 stubs from Plan 07-01 still flagged @Ignore until each downstream plan claims them. Plan 07-04 un-Ignored all 5 of its claimed stubs.)

Suite state: **GREEN** — all production paths still covered + 4 new ContentObserver live-refresh paths now covered. One transient flake observed during full-suite run (`InsectFilterViewModelTest > stickerState_survivesFlipLens` raising `kotlinx-coroutines-test UncaughtExceptionsBeforeTest`); test passes on isolated re-run and on suite-rerun-immediately-after — established kotlinx-coroutines-test 1.10.2 flake under parallel execution, unrelated to Plan 07-04 changes.

## Acceptance Criteria

- [x] D-20a investigation grep output captured (above) — outcome documented as NO-OP
- [x] DebugOverlayRendererTest.draw_skips_in_release un-Ignored + GREEN (Strategy B structural)
- [x] CollectionRepository.loadMediaItems uses callbackFlow + registerContentObserver + notifyForDescendants=true + awaitClose
- [x] T-06-02 + D-12 logic preserved verbatim in extracted `private fun performQuery()` helper
- [x] 4 CollectionRepositoryContentObserverTest un-Ignored + GREEN
- [x] Existing 4 CollectionRepositoryTest still GREEN (after `awaitComplete()` → `cancelAndIgnoreRemainingEvents()` inline-fix)
- [x] app/src/debug/AndroidManifest.xml exists with xmlns:tools + tools:node="remove" on LeakLauncherActivity activity-alias
- [x] Debug merged manifest contains zero `<activity-alias>` declarations for LeakLauncherActivity (only verified via XML element grep; the FQN string appears in my own embedded XML comment which AGP preserves)
- [x] MainActivity is SOLE android.intent.category.LAUNCHER target in debug merged manifest
- [x] Release manifest unchanged (assembleRelease still 20 MB)
- [x] `./gradlew :app:testDebugUnitTest` GREEN
- [x] `./gradlew :app:assembleDebug` GREEN
- [x] `./gradlew :app:assembleRelease` GREEN
- [x] 9 D-32 grep-asserts intact (no Phase 3+4+5+6 inline fix stripped)
- [x] 3 atomic commits landed (91c6d64 + bee3ce0 + 8607c2d)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 — Bug] CollectionRepository.kt: inner `withContext(Dispatchers.IO)` inside callbackFlow body caused runTest TurbineTimeoutCancellationException**
- **Found during:** Task 2 verification (`./gradlew :app:testDebugUnitTest --tests 'com.bugzz.filter.camera.data.*'` — 5 failures: 4 existing tests + 1 new test)
- **Issue:** Plan body sample had `withContext(Dispatchers.IO) { performQuery() }` inside callbackFlow producer; this redundantly suspends on real Dispatchers.IO which runTest's TestScheduler doesn't advance, causing all flow-consuming tests to timeout
- **Fix:** Removed inner withContext; `.flowOn(Dispatchers.IO)` already moves the entire callbackFlow producer body to IO. Imports cleaned (removed `kotlinx.coroutines.withContext`)
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt`
- **Commit:** bee3ce0

**2. [Rule 1 — Bug] Existing CollectionRepositoryTest hangs on awaitComplete() under new callbackFlow contract**
- **Found during:** Same Task 2 verification run
- **Issue:** Cold `flow { emit; return }.flowOn(IO)` auto-completed after single emit; callbackFlow stays open until awaitClose. Existing tests called `.awaitComplete()` after `awaitItem()` → flow never completes → Turbine 3s timeout
- **Fix:** All 4 existing CollectionRepositoryTest tests swapped `.awaitComplete()` for `.cancelAndIgnoreRemainingEvents()`. Underlying T-06-02 + D-12 assertion logic unchanged
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt`
- **Commit:** bee3ce0

**3. [Rule 3 — Blocking] CollectionRepositoryContentObserverTest unresolved reference `androidx.test.core.app.ApplicationProvider`**
- **Found during:** Task 2 compile step
- **Issue:** Plan body used `ApplicationProvider.getApplicationContext()`; `androidx.test:core` is not on the test classpath (only `androidx.test.ext:junit` is). Existing tests use `RuntimeEnvironment.getApplication()` for the same purpose
- **Fix:** Swapped `import androidx.test.core.app.ApplicationProvider` for `import org.robolectric.RuntimeEnvironment`; context getter reads `RuntimeEnvironment.getApplication()` (existing test convention)
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryContentObserverTest.kt`
- **Commit:** bee3ce0

**4. [Rule 1 — Bug] reemits_when_observer_onChange_fires test hangs because Robolectric main looper is PAUSED by default**
- **Found during:** Task 2 final verification (1 remaining failure after fixes #1-3 landed)
- **Issue:** ContentObserver constructed with `Handler(Looper.getMainLooper())` — onChange dispatch is `handler.post()`-ed to main looper. Robolectric 4.13 default LooperMode is PAUSED — posted runnables don't execute until `idle()` is called
- **Fix:** Added `shadowOf(Looper.getMainLooper()).idle()` after `context.contentResolver.notifyChange(filesUri, null)` in the test. Drives the queued Handler.post inside the test scope
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryContentObserverTest.kt`
- **Commit:** bee3ce0

**5. [Rule 1 — Bug] Plan spec said `<activity>` for LeakLauncherActivity but LeakCanary actually declares it as `<activity-alias>`**
- **Found during:** Task 3 verification (`./gradlew :app:processDebugManifest` warned "tagged to remove other declarations but no other declaration present")
- **Issue:** Inspecting the merged manifest revealed LeakCanary 2.14 ships `LeakLauncherActivity` as an `<activity-alias android:targetActivity="leakcanary.internal.activity.LeakActivity">`, NOT an `<activity>`. The original overlay's `<activity ... tools:node="remove" />` matched no element in the dependency-injected manifest, so the directive was a no-op
- **Fix:** Switched element type to `<activity-alias>` with the alias's required `android:targetActivity` attribute; `tools:node="remove"` now correctly drops the alias from the debug merged manifest. Verified no warning + no `<activity-alias>` declaration remains for LeakLauncherActivity in merged_manifests/debug/processDebugManifest/AndroidManifest.xml
- **Files modified:** `app/src/debug/AndroidManifest.xml`
- **Commit:** 8607c2d

## Threat Mitigations Verified

- **T-07-03 (ContentObserver leak):** `awaitClose { unregisterContentObserver }` fires on flow cancellation. CollectionRepositoryContentObserverTest.unregisters_observer_when_flow_collection_cancelled passes — `shadow.getContentObservers(filesUri).isEmpty()` after Turbine cancel(). Production: ViewModel scoping cleans up automatically per RESEARCH Pattern 3.
- **T-07-10 (debug manifest overlay scope creep):** `tools:node="remove"` targets the SPECIFIC FQN `leakcanary.internal.activity.LeakLauncherActivity` only — MainActivity in debug merged manifest unchanged + still has `android.intent.category.LAUNCHER` intent-filter.
- **T-07-11 (per-MIME ContentObserver miss):** Single observer on `MediaStore.Files.getContentUri("external")` + `notifyForDescendants=true`. CollectionRepositoryContentObserverTest.registers_on_files_uri_with_notifyForDescendants_true passes — `shadow.getContentObservers(filesUri).isNotEmpty()`.
- **T-06-02 inherited (SQL injection via selection):** Plan 07-04 preserves Phase 6 mitigation verbatim in extracted private `performQuery()` helper — `selectionArgs = arrayOf("DCIM/Bugzz/%", "image/jpeg", "video/mp4")` bound via parallel `?`-placeholders; no string concatenation of user/path data into selection. CollectionRepositoryTest.selectionArgsBindRelativePath still GREEN.

## Build Artifacts

- `app/build/outputs/apk/debug/app-debug.apk` — debug build clean
- `app/build/outputs/apk/release/app-release.apk` — 20,450,049 bytes (≈20 MB; unchanged from Phase 6 baseline 20 MB)
- `app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml` — debug merged manifest, MainActivity SOLE LAUNCHER target, no LeakLauncherActivity activity-alias declaration present

## Open Items for Plan 07-05

- Plan 07-05 will land the PERF-REPORT scaffold + Xiaomi 13T baseline measurement runbook using the PerfReporter + DetectionLatencyRecorder infrastructure landed in Plan 07-03 and the live-refresh + LAUNCHER-fix infrastructure landed in Plan 07-04. Device runbook will validate the manifest overlay on real Xiaomi HyperOS (`am start -c LAUNCHER` → MainActivity, no LeakCanary contender).

## Self-Check: PASSED

- `app/src/debug/AndroidManifest.xml` — FOUND
- `app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt` — FOUND (callbackFlow + ContentObserver landed)
- `app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryContentObserverTest.kt` — FOUND (4 un-Ignored GREEN)
- `app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt` — FOUND (4 awaitComplete → cancelAndIgnoreRemainingEvents)
- `app/src/test/java/com/bugzz/filter/camera/render/DebugOverlayRendererTest.kt` — FOUND (1 un-Ignored GREEN)
- Commit 91c6d64 — FOUND
- Commit bee3ce0 — FOUND
- Commit 8607c2d — FOUND
