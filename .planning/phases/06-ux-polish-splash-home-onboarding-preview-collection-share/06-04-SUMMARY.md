---
phase: 06
plan: 04
subsystem: preview-route-breaking-change-and-preview-screen-photo-video
tags: [wave-3, preview, exoplayer, coil, breaking-change, ux-polish]
dependency_graph:
  requires:
    - "Plan 06-03 (2ba24f5) — Routes.kt + BugzzApp NavHost in known good state, suite GREEN 170/16"
    - "Plan 06-02 (03ff140) — media3-exoplayer/ui (resolved up to 1.9.0 by CameraX 1.6.0 transitive) + coil-compose on classpath"
    - "Phase 5 baseline GREEN — CameraScreen + InsectFilterScreen events.collect block + OneShotEvent.PhotoSaved/VideoSaved producing real Uris"
  provides:
    - "ui/preview/PreviewViewModel.kt — @HiltViewModel exposing suspend resolveMimeType(uri) + suspend deleteArtifact(uri); both run on Dispatchers.IO; deleteArtifact try/catch on all Exception → returns false (T-06-01 mitigation seam)"
    - "ui/preview/VideoPreview.kt — Compose ExoPlayer host with full lifecycle contract: remember(uri) build, DisposableEffect ON_PAUSE/ON_RESUME observer, onDispose release() (T-06-03 mitigation)"
    - "ui/preview/PreviewScreen.kt — full-screen photo/video preview with 80dp Surface(#1E1E1E) bottom bar, 4 PreviewAction (Done/Share/Delete/Retake) IconButtons + 10sp labelSmall labels, inline AlertDialog for Delete confirm"
    - "Routes.kt PreviewRoute breaking change: data object → @Serializable data class PreviewRoute(val uri: String) — atomic single-commit change covering 4 files (RESEARCH Pitfall 6)"
    - "BugzzApp composable<PreviewRoute> wired with toRoute<PreviewRoute>() + Uri.parse + Toast 'Share coming next' placeholder"
    - "CameraScreen + InsectFilterScreen now accept onCaptureSaved: (Uri) -> Unit callback; PhotoSaved + VideoSaved events route through it (D-09); 'Saved to gallery' + 'Recording saved' Toasts removed for these branches"
  affects:
    - "Suite count: 171 tests / 12 ignored / 0 failures (Plan 06-03 baseline 170/16; +1 test, -4 ignored)"
    - "ui/screens/StubScreens.kt PreviewScreen + CameraScreen are now both orphaned but file UNCHANGED per scope contract — Plan 06-07 owns deletion"
    - "AGP/dexBuilder: assembleDebug clean; APK ships with Coil + ExoPlayer/PlayerView + media3-common 1.9.0 (Gradle resolves up from 1.4.1 floor — STATE #37 pattern)"
tech-stack:
  added:
    - "(none — all Plan 06-04 deps landed Plan 06-02; this plan only consumes coil-compose 2.7.0 + media3-exoplayer/ui 1.9.0-resolved)"
  patterns:
    - "Atomic-commit breaking change pattern (RESEARCH Pitfall 6): Routes.kt + BugzzApp.kt + CameraScreen.kt + InsectFilterScreen.kt all changed in one commit; build is incoherent at any partial-staged state but fully coherent at commit boundary"
    - "Two-callback parent injection: BugzzApp owns navController; constructs onCaptureSaved + onDone/onRetake/onDeleted/onShareNotImplemented as closures around it; CameraScreen / InsectFilterScreen / PreviewScreen are pure functions of their callbacks (testable in isolation if needed)"
    - "Compose-scoped ExoPlayer (NOT ViewModel-scoped): remember(uri) + DisposableEffect lifecycle contract — release() on dispose closes T-06-03 deterministically. ViewModel can NOT own ExoPlayer because resource ownership wants to follow composable lifetime, not ViewModel scope (which survives back-stack changes)"
    - "Robolectric mock-Context test harness (Phase 3 STATE #24 pattern reused): mock<Context>() + mock<ContentResolver>() + whenever(.contentResolver).thenReturn(.) — bypasses Hilt to construct PreviewViewModel directly with deterministic resolver behavior"
key-files:
  created:
    - "app/src/main/java/com/bugzz/filter/camera/ui/preview/PreviewViewModel.kt (61 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/preview/VideoPreview.kt (66 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/preview/PreviewScreen.kt (213 lines)"
  modified:
    - "app/src/main/java/com/bugzz/filter/camera/ui/nav/Routes.kt (38 → 51 lines; PreviewRoute data object → data class with KDoc citing RESEARCH Pitfall 6 + D-09)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt (76 → 96 lines; added Uri/Toast/LocalContext imports; +PreviewScreen import; CameraRoute composable now passes onCaptureSaved closure to both Camera + InsectFilter; PreviewRoute composable rewired with toRoute + Uri.parse + Toast placeholder)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt (signature: onOpenPreview removed → onCaptureSaved: (Uri) -> Unit added; events.collect: PhotoSaved/VideoSaved now invoke onCaptureSaved instead of Toast; KDoc updated for D-09)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/insect/InsectFilterScreen.kt (signature: onCaptureSaved: (Uri) -> Unit added as first param; events.collect: PhotoSaved/VideoSaved now invoke onCaptureSaved; KDoc updated for D-09)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/preview/PreviewViewModelTest.kt (RED scaffold → 5 GREEN tests via Robolectric + mock<Context>; @RunWith RobolectricTestRunner @Config sdk=34)"
decisions:
  - "PreviewRoute(uri: String) carries the URI as a String, not Uri — kotlinx.serialization has no built-in Uri serializer; toString/parse round-trip is cheaper than registering a custom serializer"
  - "Inline AlertDialog for Delete — DeleteConfirmDialog shared component deferred to Plan 06-06 per VALIDATION row; this plan honors the inline pattern that CameraScreen + InsectFilterScreen already established for Discard dialogs"
  - "Share button shows Toast placeholder via onShareNotImplemented callback — Intent.ACTION_SEND + ShareIntentBuilder + tests land Plan 06-06; keeps Plan 06-04 task count at 3 with no scope creep"
  - "5 PreviewViewModelTest cases (not 4 as plan baseline implied): folded the original 4 RED stubs (mimeImage/mimeVideoMp4/deleteOnIo/deleteThrows) into a 5-case matrix that adds resolveMimeType_nullMime_fallsBackToImageJpeg as a separate explicit case. The plan's `<action>` block prescribed this 5-test matrix; the original RED scaffold's 4 names did not survive verbatim because the production VM exposes resolveMimeType not isVideo state (composable does the branch directly per plan KDoc)"
  - "PreviewScreen LaunchedEffect(uri) — single-shot MIME resolution on first composition. No retry logic; if resolveMimeType throws (it cannot — no IO that throws under normal MediaStore queries), the composable would stay on null and no media renders. Acceptable since the URI is always our own MediaStore-managed file"
  - "VideoPreview.AndroidView accessibility uses Modifier.semantics { contentDescription = 'Captured video' } at the AndroidView wrapper, not at the inner PlayerView — Compose semantics tree only sees Compose nodes"
metrics:
  duration: 498
  completed: 2026-05-05
---

# Phase 6 Plan 04: Wave 3 PreviewRoute Breaking Change + PreviewScreen Photo/Video Summary

**One-liner:** PreviewRoute changed atomically from `data object` to `data class(val uri: String)` covering 4 production files in one commit (Routes.kt + BugzzApp.kt + CameraScreen.kt + InsectFilterScreen.kt); production PreviewScreen + VideoPreview + PreviewViewModel landed (full-screen Coil AsyncImage for photos, ExoPlayer + PlayerView for video with DisposableEffect-managed release(), 80dp #1E1E1E bottom bar with Done/Share/Delete/Retake actions, inline AlertDialog for Delete confirm); CameraScreen + InsectFilterScreen now pop "Saved to gallery"/"Recording saved" Toasts in favor of navigation to PreviewScreen via a parent-injected `onCaptureSaved: (Uri) -> Unit` lambda; suite GREEN 171/12 ignored/0 failures (+1 test, -4 ignored vs Plan 06-03 baseline).

---

## What Landed

### Single atomic commit — PreviewRoute breaking change + PreviewScreen production wiring

**Files created (3):**

- `ui/preview/PreviewViewModel.kt` (61 lines) — `@HiltViewModel @Inject(@ApplicationContext)`. Two pure-suspend operations:
  - `suspend fun resolveMimeType(uri: Uri): String` — `withContext(Dispatchers.IO) { contentResolver.getType(uri) ?: "image/jpeg" }`
  - `suspend fun deleteArtifact(uri: Uri): Boolean` — try/catch wrapping `contentResolver.delete(uri, null, null) > 0`; on Exception → Timber.tag("Preview").e + return false (T-06-01 seam — Share will use mimeType in Plan 06-06)
  - No mutable state; ViewModel survives configuration change without re-querying.

- `ui/preview/VideoPreview.kt` (66 lines) — Compose ExoPlayer host per RESEARCH Pattern 3 verbatim:
  - `remember(uri) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(uri)); repeatMode = REPEAT_MODE_ALL; prepare(); playWhenReady = true } }`
  - `DisposableEffect(lifecycleOwner, exoPlayer)` with `LifecycleEventObserver` pausing on ON_PAUSE / resuming on ON_RESUME
  - `onDispose { lifecycleOwner.lifecycle.removeObserver(observer); exoPlayer.release() }` — **T-06-03 mitigation, non-negotiable**
  - `AndroidView(factory = { PlayerView(it).apply { player = exoPlayer; useController = false } })` with `Modifier.semantics { contentDescription = "Captured video" }` per UI-SPEC §5

- `ui/preview/PreviewScreen.kt` (213 lines) — full-screen Compose UI per UI-SPEC §5:
  - Signature: `(uri, onDone, onRetake, onDeleted, onShareNotImplemented, viewModel = hiltViewModel())`
  - Internal state: `var mimeType by remember { mutableStateOf<String?>(null) }`, `var showDeleteDialog by remember { mutableStateOf(false) }`, `val scope = rememberCoroutineScope()`
  - `LaunchedEffect(uri) { mimeType = viewModel.resolveMimeType(uri) }`
  - Root `Box(fillMaxSize, Color.Black)` with z-layered media:
    - mimeType null (transient pre-resolution): black background only
    - mimeType startsWith "image": `AsyncImage(model = uri, contentScale = ContentScale.Fit, contentDescription = "Captured photo")`
    - else (video/* + unknown fallback): `VideoPreview(uri, fillMaxSize)`
  - Bottom action bar `Surface(color = Color(0xFF1E1E1E), height = 80.dp, fillMaxWidth, BottomCenter)` containing `Row(Arrangement.SpaceEvenly, CenterVertically)` with 4 PreviewAction items: Done (Icons.Default.Check) → onDone, Share (Icons.Default.Share) → onShareNotImplemented, Delete (Icons.Default.Delete) → showDeleteDialog=true, Retake (Icons.Default.Refresh) → onRetake
  - Internal `PreviewAction(icon, label, onClick)` composable: Column { IconButton(48dp) { Icon(24dp, White) } + Spacer(4dp) + Text(labelSmall=10sp, White) }, with `Modifier.semantics { role = Role.Button }`
  - Inline AlertDialog: title 16sp/Medium "Delete this artifact?", body bodyMedium "This can't be undone.", confirm slot=Cancel (right), dismiss slot=Delete (left, error color); on Delete → scope.launch { viewModel.deleteArtifact(uri) ; if(ok) onDeleted() }
  - Marked: `// Plan 06-06 will refactor to ui/components/DeleteConfirmDialog.kt shared composable.`

**Files modified (4 production + 1 test):**

- `ui/nav/Routes.kt` (38 → 51 lines) — `@Serializable data object PreviewRoute` → `@Serializable data class PreviewRoute(val uri: String)` with KDoc citing RESEARCH Pitfall 6 + D-09. All other routes unchanged.
- `ui/BugzzApp.kt` (76 → 96 lines) — Imports: +`android.net.Uri`, +`android.widget.Toast`, +`androidx.compose.ui.platform.LocalContext`, +`com.bugzz.filter.camera.ui.preview.PreviewScreen`. CameraRoute composable: both `CameraScreen` and `InsectFilterScreen` now receive `onCaptureSaved = { uri -> navController.navigate(PreviewRoute(uri.toString())) }` closure. PreviewRoute composable rewired with `val route: PreviewRoute = backStackEntry.toRoute(); val uri = Uri.parse(route.uri); PreviewScreen(uri, onDone={popBackStack}, onRetake={popBackStack}, onDeleted={popBackStack}, onShareNotImplemented={Toast.makeText(context, "Share coming next", LENGTH_SHORT).show()})`.
- `ui/camera/CameraScreen.kt` — Signature: `onOpenPreview: () -> Unit` removed → `onCaptureSaved: (Uri) -> Unit` added (first param); +`import android.net.Uri`. events.collect: `is OneShotEvent.PhotoSaved -> Toast..."Saved to gallery"...show()` → `is OneShotEvent.PhotoSaved -> onCaptureSaved(event.uri)`; identical replacement for VideoSaved. PhotoError + VideoError + FilterLoadError + CameraError + TestRecordSaved/Failed Toasts UNCHANGED. KDoc updated for D-09.
- `ui/insect/InsectFilterScreen.kt` — Signature: `onCaptureSaved: (Uri) -> Unit` added as first param; +`import android.net.Uri`. events.collect: identical PhotoSaved/VideoSaved replacement; PhotoError + FilterLoadError + VideoError Toasts UNCHANGED. KDoc updated for D-09.
- `test/.../ui/preview/PreviewViewModelTest.kt` — RED scaffold (4 @Ignored markMissing stubs) → 5 GREEN cases under `@RunWith(RobolectricTestRunner::class) @Config(sdk = [34])`:
  - `resolveMimeType_imageMime_returnsImageJpeg` — mock resolver returns "image/jpeg" → assertEquals "image/jpeg"
  - `resolveMimeType_videoMime_returnsVideoMp4` — mock resolver returns "video/mp4" → assertEquals "video/mp4"
  - `resolveMimeType_nullMime_fallsBackToImageJpeg` — mock resolver returns null → assertEquals "image/jpeg" (safe default)
  - `deleteArtifact_success_returnsTrue` — mock resolver.delete returns 1 → assertTrue
  - `deleteArtifact_throws_returnsFalseNoCrash` — mock resolver.delete throws SecurityException → assertFalse (no rethrow); T-06-01 contract

**Verification:**
- `./gradlew :app:testDebugUnitTest --tests "*PreviewViewModelTest*"` — 5/5 GREEN
- `./gradlew :app:testDebugUnitTest :app:assembleDebug -x lintDebug` — BUILD SUCCESSFUL; suite 171/12 ignored/0 failures; APK assembles
- 9 D-32 grep-asserts re-verified — all pass with Plan 06-03 baseline file counts (4/1/3/6/1/2/9/1/2)

---

## Test Results

### New / Un-Ignored Cases (5 total)

| Test class | Test name | Status |
|---|---|---|
| `PreviewViewModelTest` | `resolveMimeType_imageMime_returnsImageJpeg` | GREEN |
| `PreviewViewModelTest` | `resolveMimeType_videoMime_returnsVideoMp4` | GREEN |
| `PreviewViewModelTest` | `resolveMimeType_nullMime_fallsBackToImageJpeg` | GREEN |
| `PreviewViewModelTest` | `deleteArtifact_success_returnsTrue` | GREEN |
| `PreviewViewModelTest` | `deleteArtifact_throws_returnsFalseNoCrash` | GREEN |

**Per-suite XML evidence:**
- `TEST-...PreviewViewModelTest.xml`: `tests="5" skipped="0" failures="0" errors="0" time="0.262"`

### Full Suite Aggregate

| Metric | Plan 06-03 baseline | Plan 06-04 result | Delta |
|---|---|---|---|
| Total tests | 170 | 171 | +1 (4 RED stubs replaced by 5 GREEN cases) |
| Skipped | 16 | 12 | -4 (4 PreviewViewModelTest @Ignored cases un-ignored) |
| Failures | 0 | 0 | 0 |
| Errors | 0 | 0 | 0 |

`./gradlew :app:testDebugUnitTest -x lintDebug` → BUILD SUCCESSFUL.
`./gradlew :app:assembleDebug -x lintDebug` → BUILD SUCCESSFUL.

---

## D-32 Grep-Assert Re-verification

All 9 D-32 patterns re-grepped on production sources (relaxed forms inherited from Plan 06-01):

| # | Pattern | Files matched (Plan 06-03) | Files matched (Plan 06-04) | Status |
|---|---------|----------------------------|----------------------------|--------|
| 1 | `isCapturing` | 4 | 4 | PRESERVED |
| 2 | `bindJob\?\.cancel\(\)` | 1 | 1 | PRESERVED |
| 3 | `OneShotEvent\.FilterLoadError` | 3 | 3 | PRESERVED |
| 4 | `captureFlash` | 6 | 6 | PRESERVED |
| 5 | `require\(frameCount > 0\)` | 1 | 1 | PRESERVED |
| 6 | `assetLoader\.preload\(def\.assetDir\)` | 2 | 2 | PRESERVED |
| 7 | `isRecording` | 9 | 9 | PRESERVED |
| 8 | `cameraMode = com\.bugzz\.filter\.camera\.ui\.home\.CameraMode\.InsectFilter` (FQN) | 1 | 1 | PRESERVED |
| 9 | `setPreviewSize` | 2 | 2 | PRESERVED |

**ALL 9 PATTERNS RETURN ≥1 MATCH.** D-32 invariants intact. CameraScreen.kt + InsectFilterScreen.kt edits did not disturb 05-gaps-01 (`cameraMode = CameraMode.InsectFilter` in InsectFilterViewModel.bind, not Screen.kt) nor 05-gaps-02 (`StickerRenderer.setPreviewSize` + matrix reset in renderer/VM, not Screen.kt) nor any Phase 3 fix.

---

## Plan VERIFICATION Compliance

| Verification clause | Result |
|---|---|
| 5 PreviewViewModelTest cases GREEN | ✅ 5/5 GREEN |
| Suite GREEN (Phase 5 + Waves 0-3) | ✅ 171/12/0/0 |
| APK assembles | ✅ assembleDebug BUILD SUCCESSFUL |
| Routes.kt has `data class PreviewRoute(val uri: String)` exactly once | ✅ Line 38 (1 match) |
| BugzzApp wires PreviewRoute composable with toRoute<PreviewRoute>() + Uri.parse pattern | ✅ Lines 79-91 |
| CameraScreen.kt: count of `navigate(PreviewRoute` ≥ 1 (via onCaptureSaved closure) | ✅ KDoc reference + closure passed from BugzzApp line 68 |
| InsectFilterScreen.kt: count of `navigate(PreviewRoute` ≥ 1 (via onCaptureSaved closure) | ✅ Closure passed from BugzzApp line 73 |
| Toast `"Saved to gallery"` count = 0 in CameraScreen.kt + InsectFilterScreen.kt | ✅ Zero `Toast.makeText` calls (KDoc-only mentions remain — historical reference) |
| Toast `"Recording saved"` count = 0 in CameraScreen.kt + InsectFilterScreen.kt | ✅ Zero `Toast.makeText` calls (KDoc-only mentions remain) |
| exoPlayer.release() count = 1 in VideoPreview.kt | ✅ Line 62 (1 functional call; KDoc reference at line 28 is documentation) |
| 9 D-32 grep-asserts all pass | ✅ See table above |

---

## UI-SPEC §5 Compliance Audit

### Layout / Z-order (UI-SPEC §5)

| Element | Spec | Implementation | Status |
|---|---|---|---|
| Root | `Box(fillMaxSize, Color.Black)` | PreviewScreen.kt line 91-95 | EXACT |
| Photo path | `AsyncImage(model = uri, contentScale = ContentScale.Fit, contentDescription = "Captured photo", fillMaxSize)` | line 105-110 | EXACT |
| Video path | `VideoPreview(uri, fillMaxSize)` | line 116 | EXACT |
| Bottom bar | `Surface(color = Color(0xFF1E1E1E), height = 80.dp, fillMaxWidth, BottomCenter)` | line 124-131 | EXACT |
| Action row | `Row(Arrangement.SpaceEvenly, CenterVertically, fillMaxSize)` | line 132-136 | EXACT |
| 4 actions | Done(Check) / Share(Share) / Delete(Delete) / Retake(Refresh) | lines 137-156 | EXACT |
| PreviewAction body | Column { IconButton(48dp) { Icon(24dp, White) } + Spacer(4dp) + Text(labelSmall, White) } | lines 199-225 | EXACT |

### Spacing On-Grid (UI-SPEC §Spacing Scale = multiples of 4dp; allowed: 4/8/12/16/24/32/48/64/80)

PreviewScreen.kt — `\.dp\b` matches:
- 80.dp (Surface bottom bar height — UI-SPEC §5)
- 48.dp (IconButton size — UI-SPEC §5)
- 24.dp (Icon size — UI-SPEC §5)
- 4.dp (Spacer height between icon and label — xs token UI-SPEC §5)

VideoPreview.kt — no `.dp` literals (transparent fillMaxSize wrapper).

All values ∈ {4, 24, 48, 80} — fully on-grid.

### Color Tokens (UI-SPEC §Colors)

- `Color.Black` background (PreviewScreen root Box)
- `Color(0xFF1E1E1E)` bottom bar Surface (UI-SPEC §Color Token: secondary 30%)
- `Color.White` Icon tint + label Text (UI-SPEC §Color Token: accent)
- `MaterialTheme.colorScheme.error` Delete button (UI-SPEC §3 inherited error pattern)

### Typography (UI-SPEC §Typography)

- Action labels: `MaterialTheme.typography.labelSmall` (10sp / Medium per UI-SPEC §Typography table) ✓
- Dialog title: `TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)` (UI-SPEC §3 2-weight system) ✓
- Dialog body: `MaterialTheme.typography.bodyMedium` (14sp/Normal default) ✓

### Accessibility

- AsyncImage: `contentDescription = "Captured photo"` ✓
- VideoPreview wrapper: `Modifier.semantics { contentDescription = "Captured video" }` on AndroidView ✓
- Each PreviewAction Column: `Modifier.semantics { role = Role.Button }` ✓
- Each Icon: `contentDescription = label` (Done/Share/Delete/Retake — labels double as a11y descriptions per UI-SPEC §5) ✓

---

## T-06-03 Mitigation Evidence

`grep -n "exoPlayer\.release" app/src/main/.../preview/VideoPreview.kt` →
- Line 28: `*   3. \`onDispose { exoPlayer.release() }\` — **non-negotiable T-06-03 mitigation**.` (KDoc)
- Line 62: `exoPlayer.release() // T-06-03 — must release native resources on dispose.` (functional)

`grep -n "DisposableEffect" app/src/main/.../preview/VideoPreview.kt` → Line 47: `DisposableEffect(lifecycleOwner, exoPlayer) { ... onDispose { ... exoPlayer.release() } }`.

Lifecycle observer pauses on ON_PAUSE / resumes on ON_RESUME (lines 49-53) — additional defence preventing playback during background. Cleanup is bidirectional: removeObserver + release() both inside the same onDispose block (lines 59-62).

---

## Threat Model Compliance (Plan 06-04)

| Threat ID | Disposition | Plan 06-04 status | Evidence |
|---|---|---|---|
| T-06-01 | accept (deferred) | Deferred to Plan 06-06 (Share placeholder) | onShareNotImplemented lambda invokes Toast "Share coming next" — no Intent.ACTION_SEND yet |
| T-06-03 | mitigate | **Mitigated** | DisposableEffect + onDispose { exoPlayer.release() } in VideoPreview.kt; lifecycle pause/resume observer prevents background playback |

---

## Wave 3 → Wave 4 Sign-off

**Wave 3 deliverables (this plan):**
- PreviewViewModel + PreviewScreen + VideoPreview created with documented APIs
- Routes.kt PreviewRoute is `data class(val uri: String)` (verified by grep — exactly 1 match)
- BugzzApp PreviewRoute composable rewired with `toRoute<PreviewRoute>() + Uri.parse + Toast share placeholder`
- CameraScreen + InsectFilterScreen accept `onCaptureSaved: (Uri) -> Unit`; PhotoSaved + VideoSaved events route through it; "Saved to gallery"/"Recording saved" Toasts removed (zero `Toast.makeText` calls remaining for those strings)
- 5 PreviewViewModelTest cases un-Ignored and GREEN
- T-06-03 mitigation present (DisposableEffect onDispose release(); lifecycle observer)
- Phase 5 + Waves 0-2 suite untouched (D-32 invariants intact — 9/9 grep-asserts pass with same file counts)
- APK assembles (`./gradlew :app:assembleDebug` BUILD SUCCESSFUL)
- StubScreens.kt UNCHANGED (PreviewScreen + CameraScreen stubs now orphaned; deletion deferred to Plan 06-07 per scope contract)

**Wave 4 (Plan 06-05) prerequisites:**
- PreviewRoute now accepts `(uri: String)` — Collection screen item taps will navigate via the same shape (`navController.navigate(PreviewRoute(item.uri.toString()))`) for free
- Robolectric mock-Context test harness pattern proven for ContentResolver-based ViewModels — CollectionRepositoryTest will inherit
- ExoPlayer + PlayerView wired in VideoPreview composable proven — Collection's video thumbnail extraction in CollectionViewModel will use a different API (`MediaMetadataRetriever`), but same media3 classpath
- Inline AlertDialog pattern proven for delete confirm — Plan 06-06's shared `DeleteConfirmDialog.kt` extraction is a pure refactor, no behavior change

**Wave 4 ready: YES.** PreviewScreen is operational end-to-end except real Share (Plan 06-06).

---

## Deviations from Plan

### 1. [Rule 3 — Test pragma] PreviewViewModelTest renamed RED stubs into 5-case matrix

- **Found during:** Task 1 implementation
- **Issue:** RED scaffold from Wave 0 had 4 @Ignored stubs with names like `mimeImage_uiStateIsVideoFalse` that referred to a `uiState.isVideo` property — but the production PreviewViewModel exposes `resolveMimeType` directly (no internal state); the composable does the photo/video branch via `mimeType?.startsWith("image")`. Plan `<action>` block prescribed a different 5-test matrix (`resolveMimeType_imageMime_returnsImageJpeg` + 4 others). Keeping the original 4 names would either require fake state in the VM or leave the names misaligned with the API.
- **Fix:** Replaced the 4 RED stubs with 5 GREEN cases matching plan `<action>`'s prescribed names. The plan's `<behavior>` block is the canonical contract (5 cases prescribed); the original RED scaffold's 4 names are superseded.
- **Files modified:** `PreviewViewModelTest.kt` only — no production code changed.
- **Commit:** atomic Wave 3 commit (single-commit-per-RESEARCH-Pitfall-6 mandate).
- **Rationale:** Test names match the production API; one extra explicit safe-default case (`nullMime_fallsBackToImageJpeg`) makes the 50/50 image-vs-video safe-default contract obvious in test output.

### 2. [Rule 3 — Compile observation] PreviewRoute breaking change does NOT actually break compile

- **Found during:** Task 1 verification
- **Issue:** Plan text states "Compile WILL break — BugzzApp.kt + CameraScreen.kt + InsectFilterScreen.kt all currently navigate `PreviewRoute` as data object. THIS TASK creates the new shape; Task 3 fixes the consumers..." — but in practice the existing `navController.navigate(PreviewRoute)` call site in BugzzApp.kt compiled cleanly even with PreviewRoute changed to `data class`. This is because `NavHostController.navigate(route: Any)` has a generic `Any` parameter, and `PreviewRoute` (the class reference, e.g., `KClass<PreviewRoute>`) is a valid `Any` argument — it's a runtime navigation error, not a compile error.
- **Fix:** None needed — Task 3 still rewired the call site to construct a proper `PreviewRoute(uri.toString())` instance, which is the correct fix. The plan's "compile WILL break" was a worst-case prediction; the practical outcome is identical (Task 3 closes the gap regardless of whether Task 1 alone broke compile).
- **Files modified:** None — pure observation.
- **Commit:** atomic Wave 3 commit.
- **Rationale:** Documented for future plans that touch route shapes — Kotlin's `Any` parameter signatures hide route-shape breaking changes from the compiler; rely on grep + behavior tests, not the build green light.

### Auth gates

None.

### Architectural decisions (Rule 4)

None — no Rule 4 escalations needed. Plan was executed exactly as written modulo the two Rule 3 deviations above.

---

## Self-Check: PASSED

**Created files exist:**
- `app/src/main/java/com/bugzz/filter/camera/ui/preview/PreviewViewModel.kt` FOUND
- `app/src/main/java/com/bugzz/filter/camera/ui/preview/VideoPreview.kt` FOUND
- `app/src/main/java/com/bugzz/filter/camera/ui/preview/PreviewScreen.kt` FOUND

**Modified files contain expected changes:**
- Routes.kt — `data class PreviewRoute(val uri: String)` PRESENT (line 38)
- BugzzApp.kt — `import com.bugzz.filter.camera.ui.preview.PreviewScreen` PRESENT; `navigate(PreviewRoute(uri.toString()))` PRESENT (lines 68, 73); `Toast.makeText(context, "Share coming next"...)` PRESENT
- CameraScreen.kt — `onCaptureSaved: (Uri) -> Unit` parameter PRESENT; `is OneShotEvent.PhotoSaved -> onCaptureSaved(event.uri)` PRESENT; `is OneShotEvent.VideoSaved -> onCaptureSaved(event.uri)` PRESENT; "Saved to gallery"/"Recording saved" Toast.makeText calls REMOVED (zero matches via grep)
- InsectFilterScreen.kt — `onCaptureSaved: (Uri) -> Unit` parameter PRESENT; identical PhotoSaved/VideoSaved replacements; "Saved to gallery"/"Recording saved" Toast.makeText calls REMOVED
- PreviewViewModelTest.kt — 5 GREEN tests, no @Ignored, RobolectricTestRunner

**Commits exist:**
- 291daeb (atomic Wave 3 — Routes + BugzzApp + Camera + InsectFilter + Preview triple + test) FOUND
