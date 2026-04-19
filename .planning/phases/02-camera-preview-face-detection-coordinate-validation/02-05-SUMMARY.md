---
phase: 02-camera-preview-face-detection-coordinate-validation
plan: 05
subsystem: ui-compose-wiring
tags: [camerax-viewfinder, hilt-viewmodel, compose-permission-gate, orientation-listener, debug-only-test-record, implementationmode-external]

# Dependency graph
requires:
  - phase: 01-foundation-skeleton
    provides: Compose/Hilt/nav baseline (BugzzApp + CameraRoute + StubScreens.kt permission pattern)
  - plan: 02-02
    provides: CameraX 1.6.0 deps (camera-compose + camera-view + camera-effects) + Hilt CameraModule
  - plan: 02-03
    provides: CameraLens enum + CameraLensProvider.next() — consumed by CameraViewModel.onFlipLens()
  - plan: 02-04
    provides: CameraController public surface (surfaceRequest, bind, flipLens, setTargetRotation, startTestRecording, stopTestRecording)
provides:
  - CameraUiState — Phase 2-scoped 5-field data class (lens / permissionState / isDetectorReady / isRecording / lastErrorMessage); no Phase 3+ fields (D-14 grep-gate satisfied)
  - PermissionState — sealed interface (Unknown / Denied / Granted + isGranted helper) for runtime CAMERA gate
  - OneShotEvent — sealed interface for one-shot toasts (TestRecordSaved / TestRecordFailed / CameraError)
  - CameraViewModel — @HiltViewModel @Inject(CameraController) exposing uiState:StateFlow + surfaceRequest (reshared) + events:Flow; orientationListener quadrant-thresholded (D-08); onTestRecord delay(5_000L) auto-stop (D-04)
  - CameraScreen — CameraXViewfinder(ImplementationMode.EXTERNAL) + flip button (TopEnd) + debug TEST RECORD button (BottomCenter, BuildConfig.DEBUG-gated) + permission gate (reuses Phase 1 pattern)
  - BugzzApp CameraRoute now wired to ui/camera/CameraScreen (ui/screens/CameraScreen stub orphaned but file retained for Splash/Home/Preview/Collection routes)
  - CameraController internal-constructor split — primary takes providerFactory (test-visible); secondary @Inject hard-codes production factory so Hilt graph satisfies without Function2 binding
affects: [plan-02-06]

# Tech tracking
tech-stack:
  added:
    - "None — all dependencies already on the classpath from Plan 02-02 (androidx.camera.viewfinder.core, androidx.camera.compose, androidx.hilt.navigation.compose, androidx.lifecycle.compose)."
  patterns:
    - "**Compose CameraXViewfinder binding via SurfaceRequest flow:** CameraController holds `MutableStateFlow<SurfaceRequest?>`; Preview.setSurfaceProvider publishes each new request into the flow. ViewModel reshares it as-is (`val surfaceRequest = controller.surfaceRequest`). Composable collects via `collectAsStateWithLifecycle()` and passes the non-null value into `CameraXViewfinder(surfaceRequest = sr, implementationMode = ImplementationMode.EXTERNAL)`. This avoids the `AndroidView(PreviewView)` pre-1.5 hack and gets correct Compose z-ordering for overlays."
    - "**Hilt @Inject + Kotlin constructor defaults do NOT mix:** Kotlin's default parameter value lives in bytecode only as a synthetic `$default` method callable from Kotlin callers. Dagger's KSP-generated `Provider<T>` does NOT invoke `$default` — it calls the full-arity constructor and requires a binding for every parameter. Fix: when a test-seam default-valued parameter exists, SPLIT the constructor — primary `internal constructor(...)` with the seam (test-visible), secondary `@Inject constructor(...)` WITHOUT the seam that delegates to the primary with the production factory literal. Hilt graph stays unambiguous; tests still have the seam. This is the first codebase-wide application of the pattern — document as the convention for any future CameraController-style @Singleton with a test seam."
    - "**Quadrant-thresholded OrientationEventListener:** Device rotation raw degrees arrive from `OrientationEventListener.onOrientationChanged(degrees)` at sensor resolution (1-2°). Translating every update to a `setTargetRotation()` call thrashes CameraX internals. Solution: quantize degrees into four 90° quadrants via `when (degrees) { in 45..134 -> ROTATION_270; in 135..224 -> ROTATION_180; in 225..314 -> ROTATION_90; else -> ROTATION_0 }`, emit only on quadrant change (`if (rot != currentRotation)`). Mitigates the rotation-noise attack surface and keeps D-08 targetRotation updates discrete."
    - "**BuildConfig.DEBUG compose-branch gating:** `if (BuildConfig.DEBUG) { Button(...) }` — release builds compile-eliminate the branch (R8 const-folding `BuildConfig.DEBUG=false` removes the `Button(...)` construction entirely, including the `Modifier.align` allocation). T-02-03 (TEST RECORD visible in release) is mitigated by construction, not by runtime check. Same pattern applies to any debug-only UI in Phase 3+."
    - "**ImplementationMode.EXTERNAL vs PERFORMANCE rename verified by AAR inspection:** Research §Open Questions #1 listed both names with uncertainty. Inspected `viewfinder-core-1.6.0.aar` via `unzip classes.jar` + `javap -p`. The enum is `androidx.camera.viewfinder.core.ImplementationMode` with values `EXTERNAL` (SurfaceView) and `EMBEDDED` (TextureView). `PERFORMANCE` does not exist in 1.4+; the rename is complete. Canonical import: `import androidx.camera.viewfinder.core.ImplementationMode` (NOT `.surface.ImplementationMode` as 02-RESEARCH.md Sketch G suggested)."

key-files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraUiState.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/OneShotEvent.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt
    - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-05-SUMMARY.md
  modified:
    - app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt (1-line import path rewire)
    - app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt (constructor split — Rule 3 Hilt fix)

key-decisions:
  - "**Split CameraController constructor to satisfy Hilt (Rule 3 — plan-fixup):** Plan spec wanted a single primary `@Inject constructor(..., providerFactory: ... = { default })`. Hilt KSP-codegen could not synthesize a binding for the `Function2<Context, Continuation, ProcessCameraProvider>` parameter, failing at `hiltJavaCompileDebug`. Applied the split-constructor pattern: primary `internal constructor(...)` takes providerFactory; secondary `@Inject constructor(...)` without the param delegates to the primary with the literal production factory. Test behavior unchanged (test calls internal primary with 5 named args); production Hilt graph sees a 4-arg injectable. Duration impact: ~2 minutes."
  - "**Flip button: Text fallback, not Material Icons extended (plan-fallback):** Plan proposed `Icons.Default.Cameraswitch` from `material-icons-extended`. Classpath inspection showed only `material-icons-core` is transitively present (via Compose BOM). Pulling `material-icons-extended` adds a ~10 MB AAR with thousands of icons for one button. Per plan's explicit 'fallback to text if icon module not added' + CLAUDE.md D-24 ('icon polish deferred to Phase 6'), used `OutlinedButton { Text(\"Flip\") }`. Zero new deps; Phase 6 can swap to an icon or custom drawable."
  - "**ImplementationMode package path canonicalized (plan-fixup):** 02-RESEARCH.md Sketch G imports `androidx.camera.viewfinder.surface.ImplementationMode`; that package does not exist in viewfinder-core 1.6.0. AAR class dump confirmed the correct import is `androidx.camera.viewfinder.core.ImplementationMode`. Same enum values (EXTERNAL / EMBEDDED); only the package path differs."
  - "**MutableCoordinateTransformer import dropped:** Plan 02-05 action section mentioned the import but noted 'remove it — it's a placeholder for potential coord-transform helpers but not used in the current body.' Dropped cleanly; zero call-site usage."
  - "**Orientation listener lives in ViewModel, enable/disable in Composable DisposableEffect:** Plan could have put the listener fully in the Activity. Putting the `OrientationEventListener` constructor in the ViewModel (so the ViewModel owns the rotation state `currentRotation: Int`) while the Composable's `DisposableEffect(lifecycleOwner)` owns enable/disable keeps the rotation logic unit-testable (no Activity dependency) AND still respects the Compose lifecycle (listener disables when the screen leaves composition). Matches research §Concrete Code Sketches H."

patterns-established:
  - "**Hilt-compatible test-seam pattern for default-valued constructor parameters:** When a `@Singleton @Inject` class needs a test-substitutable factory/strategy parameter, do NOT use `@Inject constructor(..., seam: T = defaultValue)`. Hilt cannot satisfy the T binding. Instead: (1) declare a primary `internal constructor(..., seam: T)` WITHOUT @Inject — no default value — this is the test entry point. (2) Declare a secondary `@Inject constructor(...)` WITHOUT the seam parameter, delegating `: this(..., seam = defaultLiteral)` to the primary. Production Hilt graph injects the secondary (no seam binding needed); tests call the primary with a mock seam. Future: any @Singleton @Inject class following the 'test seam' pattern from Plan 02-01 MUST use this split — including future FilterEngine, MediaRepository, any SDK wrapper."
  - "**Split-constructor internal visibility — test-module reachability:** Primary constructor visibility is `internal` (not `private`), so same-module `src/test/` and `src/androidTest/` can call it. Production code and other modules cannot bypass the @Inject constructor accidentally."
  - "**CameraController reused across ViewModel lifecycles:** ViewModel constructor-injects `CameraController` (`@Singleton`). Navigation away from /camera and back creates a NEW `CameraViewModel` (default scope), but the same `CameraController` — so the `StateFlow<SurfaceRequest?>` retains the latest value, `surfaceRequest` flow immediately re-emits to the new composable. This is why the controller is `@Singleton` and why all state flows belong there, not in the ViewModel."
  - "**Compose permission flow delegates state to ViewModel, not to remember():** Plan 1 StubScreens kept `hasCameraPermission` in `remember { mutableStateOf(...) }`. Phase 2 moves it into `CameraUiState.permissionState` (ViewModel-owned). Rationale: ViewModel needs to know permission state for `LaunchedEffect(uiState.permissionState, ...)` to trigger `controller.bind()`. Having two sources of truth (Compose `remember` + ViewModel) creates drift on recomposition. Single source in ViewModel via `vm.onPermissionResult(granted)`."

requirements-completed: [CAM-01, CAM-02]
# Note:
#   CAM-01 — Live CameraXViewfinder preview — source-level complete; device verification in Plan 06
#   CAM-02 — Lens flip via button — source-level complete; device verification in Plan 06 (10-tap stability)

# Metrics
duration: 5m 39s
completed: 2026-04-19
---

# Phase 2 Plan 5: CameraViewModel + CameraScreen Compose UI Summary

**Wired the Wave 2-3 pipeline (CameraController + FaceDetectorClient + OverlayEffectBuilder) into a live Compose UI surface. Four new files in `ui/camera/` plus a one-line import rewire in `ui/BugzzApp.kt`. One Rule 3 auto-fix for Hilt ↔ Kotlin default-parameter incompatibility resolved by splitting `CameraController` into a secondary `@Inject` constructor + internal primary constructor. All three tasks committed atomically; APK builds; 10 unit tests remain GREEN (9 Phase 2 Nyquist + 1 placeholder).**

## Performance

- **Duration:** 5 min 39 s (wall clock)
- **Started:** 2026-04-19T09:37:34Z
- **Completed:** 2026-04-19T09:43:13Z
- **Tasks:** 3 primary (per 02-05-PLAN.md)
- **Files created:** 4 (CameraUiState.kt / OneShotEvent.kt / CameraViewModel.kt / CameraScreen.kt) + 02-05-SUMMARY.md
- **Files modified:** 2 (BugzzApp.kt 1-line rewire + CameraController.kt Rule 3 constructor split)
- **LOC added (net):** ~325 (26 CameraUiState + 13 OneShotEvent + 124 CameraViewModel + 161 CameraScreen; +1 import change in BugzzApp; ~+20 in CameraController for the constructor split)

## Accomplishments

- **Task 1 — CameraUiState.kt (26 LOC) + OneShotEvent.kt (13 LOC):**
  - `CameraUiState(lens, permissionState, isDetectorReady, isRecording, lastErrorMessage)` — exactly D-14 + `isRecording` addition per plan; grep of `selectedFilter | captureResult | filterId | photoUri` in `ui/camera/` returns zero code hits (one docstring mention in a negative-affirmation comment).
  - `PermissionState` sealed interface: `Unknown`, `Denied`, `Granted` + computed `isGranted: Boolean`.
  - `OneShotEvent` sealed interface: `TestRecordSaved(Uri)`, `TestRecordFailed(String)`, `CameraError(String)`.
  - Compiles via `./gradlew :app:compileDebugKotlin`.

- **Task 2 — CameraViewModel.kt (124 LOC):** `@HiltViewModel class CameraViewModel @Inject constructor(private val controller: CameraController) : ViewModel()`. Exposes:
  - `uiState: StateFlow<CameraUiState>` via `MutableStateFlow` backing.
  - `surfaceRequest: StateFlow<SurfaceRequest?>` — direct reshare of `controller.surfaceRequest`, no new flow (D-13).
  - `events: Flow<OneShotEvent>` via `Channel<OneShotEvent>(Channel.BUFFERED).receiveAsFlow()`.
  - `bind(owner)` — `viewModelScope.launch { runCatching { controller.bind(...) }.onFailure { _events.send(CameraError) } }` — resilient to HAL failures.
  - `onFlipLens()` — `CameraLensProvider.next(...)` + uiState update (bind re-fires via LaunchedEffect key on `uiState.lens`).
  - `onTestRecord()` — `controller.startTestRecording(onFinalized)` + `viewModelScope.launch { delay(5_000L); controller.stopTestRecording() }` (D-04 5-second auto-stop); no audio code path (D-05).
  - `onPermissionResult(Boolean)` — state-only update for permission gate.
  - `orientationListener(context)` — returns anonymous `OrientationEventListener` with quadrant-thresholded `when (degrees)` block emitting only on quadrant change (D-08).
  - `onCleared()` — defensive `controller.stopTestRecording()`.
  - Hilt KSP processes `@HiltViewModel` + `@Inject constructor(CameraController)`; compiles GREEN.

- **Task 3 — CameraScreen.kt (161 LOC) + BugzzApp.kt (1-line rewire) + CameraController.kt (constructor split):**
  - **CameraScreen**: Top-level `@Composable fun CameraScreen(onOpenPreview, vm: CameraViewModel = hiltViewModel())`. Collects `uiState` + `surfaceRequest` via `collectAsStateWithLifecycle()`. Permission `rememberLauncherForActivityResult` with `ActivityResultContracts.RequestPermission()` — CAMERA only (D-26/27). `LaunchedEffect(Unit)` auto-probes `ContextCompat.checkSelfPermission` on first composition. `LaunchedEffect(uiState.permissionState, uiState.lens, uiState.isDetectorReady)` triggers `vm.bind(lifecycleOwner)` after permission grant + on lens change. `DisposableEffect(lifecycleOwner)` enables/disables the ViewModel-owned `OrientationEventListener`. `LaunchedEffect(Unit)` collects `vm.events` and emits `Toast.makeText(...).show()` per event type. Root `Box(fillMaxSize + background(Color.Black))` with `when` on `uiState.permissionState`:
    - `Granted` → `CameraXViewfinder(surfaceRequest = sr, implementationMode = ImplementationMode.EXTERNAL)` fullscreen + `OutlinedButton { Text("Flip") }` aligned `TopEnd` + `if (BuildConfig.DEBUG) Button { Text(if isRecording "REC..." else "TEST RECORD 5s") }` aligned `BottomCenter`.
    - `Denied` → rationale text + "Grant permission" + "Open Settings" (launches `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` intent — matches Phase 1 StubScreens verbatim, D-27).
    - `Unknown` → transient blank.
  - **BugzzApp.kt**: One-line import change — `com.bugzz.filter.camera.ui.screens.CameraScreen` → `com.bugzz.filter.camera.ui.camera.CameraScreen`. The `composable<CameraRoute> { CameraScreen(onOpenPreview = ...) }` signature unchanged (onOpenPreview parameter preserved for API compatibility even though Phase 2 doesn't wire to the button). `ui/screens/StubScreens.kt` retained unchanged for Splash/Home/Preview/Collection.
  - **CameraController.kt constructor split** (Rule 3 — see Deviations §1): Primary constructor now `internal constructor(...)` with the `providerFactory` seam (test-visible within module); secondary `@Inject constructor(...)` omits the seam and delegates `: this(..., providerFactory = { ctx -> ProcessCameraProvider.getInstance(ctx).await() })` to the primary. Behavior identical; Hilt graph now builds.
  - `./gradlew :app:assembleDebug` exits 0, produces `app-debug.apk` (79 MB).
  - `./gradlew :app:testDebugUnitTest` exits 0 — 10 tests / 0 failures / 0 skipped.

## Task Commits

- **Task 1** — `f227c22` — `feat(02-05-01): add CameraUiState + OneShotEvent data models`
- **Task 2** — `13f9119` — `feat(02-05-02): add @HiltViewModel CameraViewModel wrapping CameraController`
- **Task 3** — `4023f31` — `feat(02-05-03): wire CameraScreen + rewire BugzzApp CameraRoute` (includes Rule 3 CameraController constructor split)

## Quoted Diffs

### BugzzApp.kt — 1-line import rewire

```diff
-import com.bugzz.filter.camera.ui.screens.CameraScreen
+import com.bugzz.filter.camera.ui.camera.CameraScreen
```

The `composable<CameraRoute> { CameraScreen(onOpenPreview = ...) }` line is UNCHANGED. Phase 1 stub `CameraScreen` in `ui/screens/StubScreens.kt` is now orphaned (still compiles — `Splash/Home/Preview/Collection` stubs still wired — but no nav route consumes it). Acceptable per plan ("Phase 1 stub can be removed or left alone — planner decides; prefer remove, but preserve StubScreens.kt").

### CameraController.kt — constructor split (Rule 3 Hilt fix)

```diff
 @Singleton
-class CameraController @Inject constructor(
+class CameraController internal constructor(
     @ApplicationContext private val appContext: Context,
     @Named("cameraExecutor") private val cameraExecutor: Executor,
     private val faceDetector: FaceDetectorClient,
     private val overlayEffectBuilder: OverlayEffectBuilder,
-    private val providerFactory: suspend (Context) -> ProcessCameraProvider =
-        { ctx -> ProcessCameraProvider.getInstance(ctx).await() },
+    private val providerFactory: suspend (Context) -> ProcessCameraProvider,
 ) {
+
+    /**
+     * Production constructor — Hilt-visible. Uses the static `ProcessCameraProvider.getInstance`
+     * ListenableFuture adapted via the `await()` extension at the bottom of this file.
+     */
+    @Inject
+    constructor(
+        @ApplicationContext appContext: Context,
+        @Named("cameraExecutor") cameraExecutor: Executor,
+        faceDetector: FaceDetectorClient,
+        overlayEffectBuilder: OverlayEffectBuilder,
+    ) : this(
+        appContext = appContext,
+        cameraExecutor = cameraExecutor,
+        faceDetector = faceDetector,
+        overlayEffectBuilder = overlayEffectBuilder,
+        providerFactory = { ctx -> ProcessCameraProvider.getInstance(ctx).await() },
+    )
```

Test compatibility: `CameraControllerTest.buildController` still calls `CameraController(appContext = ..., cameraExecutor = ..., faceDetector = ..., overlayEffectBuilder = ..., providerFactory = { mockProvider })` with 5 named args — resolves to the `internal` primary constructor (same-module visibility). Both `CameraControllerTest` methods GREEN after the split.

### CameraScreen.kt — CameraXViewfinder composition

```kotlin
Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    when {
        uiState.permissionState is PermissionState.Granted -> {
            surfaceRequest?.let { sr ->
                CameraXViewfinder(
                    surfaceRequest = sr,
                    modifier = Modifier.fillMaxSize(),
                    implementationMode = ImplementationMode.EXTERNAL,
                )
            }
            OutlinedButton(
                onClick = { vm.onFlipLens() },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            ) { Text("Flip") }
            if (BuildConfig.DEBUG) {
                Button(
                    onClick = { vm.onTestRecord() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                ) { Text(if (uiState.isRecording) "REC..." else "TEST RECORD 5s") }
            }
        }
        ...
```

### CameraViewModel.kt — quadrant-thresholded rotation listener (D-08)

```kotlin
fun orientationListener(context: Context): OrientationEventListener =
    object : OrientationEventListener(context) {
        override fun onOrientationChanged(degrees: Int) {
            if (degrees == ORIENTATION_UNKNOWN) return
            val rot = when (degrees) {
                in 45..134  -> Surface.ROTATION_270
                in 135..224 -> Surface.ROTATION_180
                in 225..314 -> Surface.ROTATION_90
                else        -> Surface.ROTATION_0
            }
            if (rot != currentRotation) {
                currentRotation = rot
                controller.setTargetRotation(rot)
            }
        }
    }
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Hilt cannot satisfy Kotlin `@Inject constructor(..., param: T = default)` — CameraController constructor split required**

- **Found during:** Task 3 verification (`./gradlew :app:assembleDebug` after writing CameraScreen + BugzzApp rewire). Error:
  ```
  Execution failed for task ':app:hiltJavaCompileDebug'.
    error: [Dagger/MissingBinding] kotlin.jvm.functions.Function2<? super android.content.Context,?
    super kotlin.coroutines.Continuation<? super ProcessCameraProvider>,?> cannot be provided
    without an @Provides-annotated method.
      CameraController(…, providerFactory) — requested at CameraViewModel(controller)
  ```
- **Issue:** Plan 02-04 shipped `CameraController @Inject constructor(..., providerFactory: suspend (Context) -> ProcessCameraProvider = { default })` with a Kotlin default value. Plan 02-04's test validates the seam (GREEN) but the production Hilt graph never actually needed to materialize a `CameraController` instance until Plan 02-05 — this is the first plan where `@HiltViewModel CameraViewModel @Inject constructor(private val controller: CameraController)` forces Hilt to synthesize a `CameraController` binding. Hilt's KSP codegen calls the full-arity constructor and requires a binding for every parameter including the Kotlin-default lambda; the default value in bytecode lives only in a synthetic `$default` method that only Kotlin callers invoke. Dagger does not invoke `$default`.
- **Root cause:** The "providerFactory AS a default parameter" pattern from Plan 02-01/02-04 KEY DECISIONS is compatible only if no Hilt consumer ever actually requests the `CameraController` binding at compile time — which was true through Plan 04 (test seam is the only consumer) but fails in Plan 05 (CameraViewModel consumes it through the Hilt graph).
- **Fix:** Split the constructor:
  1. Primary: `internal constructor(..., providerFactory: T)` — NO `@Inject`, NO default value. Test entry point.
  2. Secondary: `@Inject constructor(...)` — omits `providerFactory`, delegates `: this(..., providerFactory = { ctx -> ProcessCameraProvider.getInstance(ctx).await() })` with the literal production factory inlined.
  - Primary visibility is `internal` (not `private`) so same-module unit tests can still call it.
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt` (added secondary @Inject constructor; changed primary visibility; removed default on primary).
- **Commit:** `4023f31`
- **Verification:**
  - `./gradlew :app:assembleDebug` GREEN.
  - `./gradlew :app:testDebugUnitTest` GREEN — CameraControllerTest (2/2) still passes; `buildController` calls the internal primary with 5 named args identical to pre-fix.
- **Pattern established:** This is the canonical fix for Hilt + Kotlin default-parameter test seams. Documented in `patterns-established` + key-decisions above; apply to any future `@Singleton @Inject` class that needs a test seam.

**2. [Rule 3 - Blocking] ImplementationMode package path — research §Open Questions #1 resolution**

- **Found during:** Task 3 — writing `CameraScreen.kt` imports.
- **Issue:** 02-RESEARCH.md Sketch G imports `androidx.camera.viewfinder.surface.ImplementationMode`. Pre-execution AAR inspection of `viewfinder-core-1.6.0.aar` revealed the correct package is `androidx.camera.viewfinder.core.ImplementationMode`. The `.surface.` package does not exist in 1.6.0.
- **Root cause:** 02-RESEARCH.md documents this uncertainty (§Open Questions #1) — "verify exact enum at build time" — and the legacy package path appears in transcripts of earlier versions (camera-viewfinder-view 1.3). Resolution: single-word rename from `.surface.` to `.core.`.
- **Fix:** Imported `import androidx.camera.viewfinder.core.ImplementationMode` in CameraScreen.kt. Enum values `EXTERNAL` / `EMBEDDED` both exist on this class — the plan's primary-path `ImplementationMode.EXTERNAL` compiled successfully with no fallback needed.
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt`
- **Commit:** `4023f31`
- **Verification:** Compile GREEN; research §Open Questions #1 resolved to "EXTERNAL in androidx.camera.viewfinder.core."

**3. [Rule 3 - Blocking] Material Icons extended not on classpath — flip-button icon → text label fallback**

- **Found during:** Task 3 — Sketch G's `Icons.Default.Cameraswitch` import lookup.
- **Issue:** Plan's action block referenced `Icons.Default.Cameraswitch`, which lives in `androidx.compose.material:material-icons-extended`. Compose BOM only transitively pulls `material-icons-core` (the basic ~50-icon set); extended icons (thousands of Material Symbols, ~10 MB AAR) requires an explicit dep.
- **Root cause:** Plan's own action block foresaw this: "fallback to text if icon module not added". CLAUDE.md D-24 also marks icon polish as Phase 6 scope.
- **Fix:** Replaced `IconButton { Icon(Icons.Default.Cameraswitch, ...) }` with `OutlinedButton { Text("Flip") }`. Identical behavior (tap handler → `vm.onFlipLens()`), zero new deps. Phase 6 can swap to a custom drawable or add `material-icons-extended` if aesthetically warranted.
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt`
- **Commit:** `4023f31`
- **Verification:** Compile GREEN; flip button renders as text label top-right.

**4. [Rule 3 - Blocking] MutableCoordinateTransformer import unresolved / unused — dropped**

- **Found during:** Task 3 — Sketch G's import list included `androidx.camera.viewfinder.compose.MutableCoordinateTransformer` but the body did not reference the class.
- **Issue:** Unused import creates a stale warning + risks R8 not stripping.
- **Root cause:** Plan's action block explicitly mentioned this uncertainty: "If `MutableCoordinateTransformer` import is unresolved, remove it — it's a placeholder for potential coord-transform helpers but not used in the current body."
- **Fix:** Omitted the import. `CameraXViewfinder` has an optional `coordinateTransformer` parameter (verified via AAR class dump: `public static final void CameraXViewfinder(SurfaceRequest, Modifier, ImplementationMode, MutableCoordinateTransformer, Alignment, ContentScale, ...)`); Phase 3 can add the param when production sprites need view↔buffer coordinate translation.
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt`
- **Commit:** `4023f31`
- **Verification:** Compile GREEN; no unused-import warning.

**Total deviations:** 4 auto-fixed (all Rule 3 blocking / plan-fallback). No Rule 4 escalations. Three of the four are research / plan drift or explicit plan fallbacks; one (the Hilt split) is a latent architectural issue in Plan 02-04's providerFactory pattern that only surfaced under Plan 05's first real Hilt graph consumer of `CameraController` — pattern now fixed and documented as canonical.

## Authentication Gates

None — plan is pure-Kotlin Compose UI + ViewModel glue, no network, no device interaction, no credentials.

## Issues Encountered

- **Windows CRLF warnings:** Git continues to warn `LF will be replaced by CRLF the next time Git touches it` on every committed `.kt` file. Benign on Windows host; matches repo convention.
- **Configuration cache invalidated once:** Changing `libs.versions.toml` between sessions (Plan 04 → Plan 05) invalidated the Gradle configuration cache on first `compileDebugKotlin` — visible as "calculating task graph" message. ~5s extra cost; subsequent runs reused. Not blocking.

## User Setup Required

None. Plan 02-06 (next — device handoff runbook) consumes this APK for on-device verification on Xiaomi 13T.

## Known Stubs

None introduced by this plan.

Verified-not-stub check:
- `CameraUiState` fields are all read/written by real code paths (no placeholder `= null` fields).
- `OneShotEvent` subtypes are all emitted from `CameraViewModel` and consumed by `CameraScreen.LaunchedEffect`.
- `CameraViewModel.bind` calls real `controller.bind`; `onTestRecord` calls real `controller.startTestRecording/stopTestRecording`; `orientationListener` instantiates real `OrientationEventListener`.
- `CameraScreen.CameraXViewfinder` is wired to the live `surfaceRequest` flow (not a placeholder composable).

The Phase 1 stub `CameraScreen` in `ui/screens/StubScreens.kt` is now orphaned (no nav route wires to it). Retained intentionally per plan instruction ("do NOT delete StubScreens.kt — Splash/Home/Preview/Collection stubs still needed there"); will be cleaned up in Phase 6 when production Splash/Home/Preview/Collection screens land.

## Threat Flags

No new trust boundaries introduced. Mitigations for T-02-01 through T-02-06 verified at source level:
- **T-02-01 (bind without CAMERA)**: `LaunchedEffect(uiState.permissionState, ...)` only calls `vm.bind(lifecycleOwner)` inside `if (uiState.permissionState.isGranted && uiState.isDetectorReady)`. No path to bind without grant — grep-verified.
- **T-02-02 (biometric data in logcat)**: CameraScreen emits no Timber logs; Toast messages contain URI or error message only, never face data. ViewModel emits no logs.
- **T-02-03 (TEST RECORD visible in release)**: `if (BuildConfig.DEBUG) { Button(...) }` — R8 removes the whole `Button { Text("TEST RECORD 5s") }` allocation in release builds. Debug-only by construction.
- **T-02-05 (DisposableEffect camera-binding race)**: `DisposableEffect(lifecycleOwner)` owns ONLY the OrientationEventListener enable/disable; it does NOT call `controller.unbindAll()` or any lifecycle-level API — per plan + PITFALLS #9, camera lifecycle stays on Hilt/@Singleton CameraController.
- **T-02-06 (accidental RECORD_AUDIO request)**: Strict grep of `Manifest.permission.RECORD_AUDIO` in `ui/camera/` returns ONE match — and that match is in a CameraScreen.kt KDoc comment (line 47: `- No RECORD_AUDIO contract anywhere (D-05/D-26/T-02-06).`) — a negative-affirmation docstring, NOT a `launcher.launch(...)` call site. Strict grep of `\.launch\(Manifest\.permission\.RECORD_AUDIO\)` returns 0 matches. T-02-06 hardened at code level.

## Self-Check: PASSED

- [x] `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraUiState.kt` exists — `data class CameraUiState(...)` with 5 fields; `sealed interface PermissionState` with Unknown/Denied/Granted + isGranted
- [x] `app/src/main/java/com/bugzz/filter/camera/ui/camera/OneShotEvent.kt` exists — `sealed interface OneShotEvent` with 3 data class children
- [x] `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt` exists — `@HiltViewModel` present, `@Inject constructor(private val controller: CameraController)`, `delay(5_000L)` in `onTestRecord()`, quadrant `when` block in `orientationListener`, `CameraLensProvider.next(...)` in `onFlipLens()`, no `DisposableEffect` (belongs in Compose)
- [x] `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt` exists — `@Composable fun CameraScreen(onOpenPreview, vm: CameraViewModel = hiltViewModel())`; contains `CameraXViewfinder(`, `vm.onFlipLens()`, `vm.onTestRecord()` under `if (BuildConfig.DEBUG)`, `"TEST RECORD 5s"` + `"REC..."` string literals, `ActivityResultContracts.RequestPermission()`, `Manifest.permission.CAMERA`, `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, `vm.orientationListener(context).enable()` inside `DisposableEffect(lifecycleOwner)`, `Alignment.TopEnd` on flip, `Alignment.BottomCenter` on TEST RECORD
- [x] `ui/camera/` code files contain NO `Manifest.permission.RECORD_AUDIO` `.launch()` call site (only one docstring reference in CameraScreen.kt as a negative-affirmation comment)
- [x] `ui/camera/` contains NO `selectedFilter | captureResult | filterId | photoUri` field declarations (only one docstring mention in CameraUiState.kt as a negative-affirmation comment)
- [x] `app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt` imports `com.bugzz.filter.camera.ui.camera.CameraScreen` (new path — grep returns 1 match)
- [x] `app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt` does NOT import `com.bugzz.filter.camera.ui.screens.CameraScreen` (old path — grep returns 0)
- [x] `app/src/main/java/com/bugzz/filter/camera/ui/screens/StubScreens.kt` still exists — Splash/Home/Preview/Collection composables present
- [x] Commit `f227c22` (feat(02-05-01) CameraUiState + OneShotEvent) present in `git log`
- [x] Commit `13f9119` (feat(02-05-02) CameraViewModel) present in `git log`
- [x] Commit `4023f31` (feat(02-05-03) CameraScreen + BugzzApp + CameraController split) present in `git log`
- [x] `./gradlew :app:assembleDebug` exits 0 — `app-debug.apk` produced (79 MB)
- [x] `./gradlew :app:testDebugUnitTest` exits 0 — 10 tests / 0 failures / 0 skipped (all 9 Phase 2 Nyquist tests still GREEN: OneEuroFilterTest 4/4, FaceDetectorOptionsTest 1/1, OverlayEffectBuilderTest 2/2, CameraControllerTest 2/2 — plus ExampleUnitTest placeholder)
- [x] `ImplementationMode.EXTERNAL` resolved at compile time (primary path per plan) — no fallback to PERFORMANCE needed; research §Open Questions #1 resolved

## Next Plan Readiness

- **Plan 02-06 (device handoff runbook) unblocked:** Debug APK at `app/build/outputs/apk/debug/app-debug.apk` (79 MB) is ready for `adb install`. Runbook must verify on Xiaomi 13T:
  1. **CAM-01 live preview** — launch app → splash → home → Open Camera → grant CAMERA permission → CameraXViewfinder renders front-camera live preview with red-rect debug overlay drawn on face (proves Plan 04's OverlayEffect + Plan 03's FaceDetectorClient pipeline end-to-end; proves this plan's CameraXViewfinder binding).
  2. **CAM-02 lens flip** — tap top-right "Flip" button → preview swaps to back camera in <500ms → 10 consecutive flips without "Camera in use" error or preview freeze (Plan 04's `faceDetector.onLensFlipped()` before rebind prevents 1€ stale state).
  3. **D-08 rotation handling** — with device physically turned through 4 orientations (phone UI stays portrait-locked per D-07), red rect + landmark dots stay pixel-aligned to the face on front AND back camera across all 4 sensor rotations.
  4. **D-04 TEST RECORD** — tap bottom-center "TEST RECORD 5s" button → button shows "REC..." → 5 seconds later auto-stops → toast displays `Saved: content://media/...` → open Google Photos → play the MP4 → red rect + landmark dots are baked into every video frame (proves Plan 04's OverlayEffect VIDEO_CAPTURE target baking).
  5. **D-05 no-audio enforcement** — during TEST RECORD, system should NOT show a RECORD_AUDIO permission prompt. Play back MP4 — should be silent (no audio track).
  6. **FaceTracker trackingId logcat** — `adb logcat -s FaceTracker:V` while the face stays in view for 60+ frames — `t=*` trackingId should remain stable (same ID across frames), proving Plan 03's enableTracking + 1€ keyed-on-trackingId state preservation.

- **Phase 3 pre-readiness:** All load-bearing Phase 2 wiring is now executable. Phase 3's `FilterEngine` replaces `DebugOverlayRenderer.draw()` at a stable call site inside `OverlayEffectBuilder.setOnDrawListener { ... renderer.draw(...) }` — no CameraScreen or CameraViewModel changes required for Phase 3 Plan 1 (the ViewModel will grow `selectedFilter` + `captureResult` fields, but `CameraUiState` is a copy-safe data class and the Compose tree will add new buttons/modals as peers to the existing `Flip` + `TEST RECORD` buttons).

---
*Phase: 02-camera-preview-face-detection-coordinate-validation*
*Completed: 2026-04-19*
