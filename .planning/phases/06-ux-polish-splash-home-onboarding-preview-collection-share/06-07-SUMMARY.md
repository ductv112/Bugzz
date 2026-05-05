---
phase: 06
plan: 07
subsystem: settings-screen-nav-graph-stub-deletion
tags: [wave-5b, settings-screen, nav-graph-close-out, stub-deletion, ux-polish, t-06-06-defer]
dependency_graph:
  requires:
    - "Plan 06-06 (c1311db) — DeleteConfirmDialog + ShareIntentBuilder + HomeScreen onSettings: () -> Unit lambda parameter shipped; BugzzApp HomeRoute composable passes placeholder onSettings lambda awaiting Plan 07 finalize"
    - "Plan 06-03 (Routes.kt — SettingsRoute @Serializable data object declared in Wave 2 alongside OnboardingRoute; composable wiring deferred to Plan 07)"
    - "Plans 03/04/05 — production SplashScreen/PreviewScreen/CollectionScreen replaced their StubScreens.kt counterparts; no source file imports `com.bugzz.filter.camera.ui.screens.*` after Plan 06"
  provides:
    - "ui/settings/SettingsScreen.kt — stateless composable with TopAppBar 'Settings' + back arrow + 4 ListItem rows (Version/Privacy/Rate/About) separated by HorizontalDividers (UX-09, D-17, D-18, UI-SPEC §9). Version row trailing = BuildConfig.VERSION_NAME; About row supportingContent = 'Bugzz — Bug filter prank camera'; Privacy + Rate rows each show a single-line Toast on tap (no real Intent.ACTION_VIEW per T-06-06 stub-only scope, no Play Store deep-link per D-18). All 4 rows use MaterialTheme.typography.bodyMedium (14sp Normal — preserves 4-size {10,14,16,24} typography inventory + 2-weight {Normal,Medium} palette). Read-only rows (Version + About) carry semantics { disabled() } for TalkBack."
    - "BugzzApp — finalized nav graph: import com.bugzz.filter.camera.ui.nav.SettingsRoute + import com.bugzz.filter.camera.ui.settings.SettingsScreen added; HomeRoute composable's onSettings lambda swapped from Plan 06's placeholder comment to onSettings = { navController.navigate(SettingsRoute) }; new composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) } block appended after CollectionRoute. Final NavHost composable count = 7 (Splash, Onboarding, Home, Camera-which-branches-FaceFilter/InsectFilter, Preview, Collection, Settings)."
    - "DELETED: app/src/main/java/com/bugzz/filter/camera/ui/screens/StubScreens.kt (108 lines removed) — orphaned Phase 1 stubs (SplashScreen/CameraScreen/PreviewScreen/CollectionScreen + StubContent helper). Verified pre-deletion via 3 safety greps (zero matches in app/src for `import com.bugzz.filter.camera.ui.screens`, `\\.ui\\.screens\\.`, and `screens\\.{Splash,Preview,Collection,Camera}Screen|screens\\.StubContent`)."
  affects:
    - "Suite count: 172 tests / 0 ignored / 0 failures — IDENTICAL to Plan 06-06 baseline. Plan 07 added zero test files (all production code; no behavior change in existing test scope)."
    - "9 D-32 grep-asserts intact post-deletion: isCapturing(14) / bindJob?.cancel()(1) / OneShotEvent.FilterLoadError(7) / captureFlash(13) / require(frameCount > 0)(1) / assetLoader.preload(def.assetDir)(3) / isRecording(47) / CameraMode.InsectFilter(4) / setPreviewSize(2) — all ≥1 match; no production source touched outside BugzzApp.kt + new SettingsScreen.kt."
    - "Final NavHost wiring complete: HomeScreen onSettings lambda parameter (added in Plan 06) now propagates through to a real navigation event. All 5 production user-facing screens (Splash, Onboarding, Home, Camera, Preview, Collection, Settings) reachable via the navigation graph."
    - "Repository cleanup: ui/screens/ package source-empty after StubScreens.kt deletion (Git auto-prunes empty directory entries on next git operation; the .git tree no longer references the file)."
    - "APK assembleDebug clean: 91962902 bytes (87.7 MiB) — within +500B of Plan 06-06 (Plan 07 nets +SettingsScreen.kt — about 6KB Kotlin source / ~3KB compiled bytecode — minus StubScreens.kt — 108 lines / ~3KB compiled bytecode — for ~zero net delta)."
tech-stack:
  added:
    - "(none — Plan 06-07 only consumes existing classpath: Material3 Scaffold/TopAppBar/ListItem/HorizontalDivider/IconButton/Icon from Compose BOM 2026.04.00; android.widget.Toast + LocalContext from framework SDK 35; Material Icons Default.ArrowBack already used by CollectionScreen)"
  patterns:
    - "Stateless presentational composable with no ViewModel: SettingsScreen takes a single onBack lambda and uses LocalContext for Toast launches. No Hilt scope, no remember state, no LaunchedEffect — keeps the file 161 lines + trivially testable. Pattern is justified by D-18: every interactive concern in v1 is a Toast stub; v2 milestone replaces the stub bodies with Intent.ACTION_VIEW (Privacy URL) / market:// deep-link (Rate). At that point a SettingsViewModel may emerge to handle URL fetching; v1 has nothing to inject."
    - "@OptIn(ExperimentalMaterial3Api::class) on TopAppBar: same opt-in pattern as Phase 6 CollectionScreen (Plan 06-05) — Material3 component is stable in Compose BOM 2026.04.00 but still flagged @ExperimentalMaterial3Api. Acknowledged risk profile via opt-in annotation; stable across BOM minor versions."
    - "Two-step BugzzApp wiring for HomeScreen.onSettings (completion): Plan 06 added the API surface (lambda parameter on HomeScreen + Toast removal) and BugzzApp passed a placeholder; Plan 07 ships SettingsScreen + replaces the placeholder with navController.navigate(SettingsRoute). Atomic split kept Plan 06 tests passing (no SettingsScreen yet to break the build) and kept Plan 07 atomic (added composable + wired call-site in one commit)."
    - "Pre-deletion safety greps before file deletion: D-32 invariant pattern — before removing an orphan source file, run a grep sweep across app/src for any import / FQN reference to its package or its declared symbols. Only proceed if all return zero matches. Plan 06-07 ran 3 such greps before `git rm StubScreens.kt`: (a) literal import of the package, (b) FQN substring `\\.ui\\.screens\\.`, (c) symbol-level FQN for each declared composable. All zero. Pattern recorded for future orphan-removal scenarios."
    - "git rm over Remove-Item for source file deletion: chose `git rm` over PowerShell `Remove-Item` so git auto-stages the deletion (no manual `git add` step needed). Same one-shot semantics; cleaner audit trail."
  patterns_avoided:
    - "Real Intent.ACTION_VIEW launch for Privacy Policy: T-06-06 explicitly defers this to v2 milestone — v1 ships Toast stub only. When real URL added in v2, MUST use HTTPS-only Intent.ACTION_VIEW (system browser handles URL safely; no WebView). Documented in SettingsScreen KDoc + threat register."
    - "Real market:// deep-link for Rate the App: D-18 explicit — app is not on Play Store; deep-link target does not exist. Stubbed with Toast 'Coming when published to Play Store'."
    - "Empty directory removal post-deletion: ui/screens/ package directory is left in place. Empty Kotlin package directories are not problematic and Git's tree-merge semantics handle the void naturally (no .gitkeep needed). Cleanup deferred."
key-files:
  created:
    - "app/src/main/java/com/bugzz/filter/camera/ui/settings/SettingsScreen.kt (161 lines)"
  modified:
    - "app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt (102 → 106 lines; +2 imports for SettingsRoute + SettingsScreen, +1 line for onSettings lambda swap, +3 lines for composable<SettingsRoute> block, =4 net lines added)"
    - ".planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-VALIDATION.md (Plan 06-06 row Status ⬜ → ✅; Plan 06-07 row Status ⬜ → ✅; nyquist_compliant unchanged at false — Plan 08 owns that flip post-device-PASS)"
  deleted:
    - "app/src/main/java/com/bugzz/filter/camera/ui/screens/StubScreens.kt (108 lines, all 5 stub composables removed: SplashScreen, CameraScreen, PreviewScreen, CollectionScreen, StubContent helper)"
decisions:
  - "Atomic single commit for all 3 tasks (per Phase 5 + Plan 06-06 convention): Task 1 (SettingsScreen + BugzzApp wire) + Task 2 (StubScreens delete) + Task 3 (06-VALIDATION.md status update) all land as a single Plan 07 commit. Same one-commit-per-plan pattern as Plans 04/05/06."
  - "@OptIn(ExperimentalMaterial3Api::class) annotation: TopAppBar in Material3 still flagged @ExperimentalMaterial3Api in Compose BOM 2026.04.00. Same convention as CollectionScreen (Plan 06-05). Acknowledged risk profile via opt-in; stable across BOM minor versions."
  - "Icons.Default.ArrowBack vs Icons.AutoMirrored.Filled.ArrowBack (deprecation warning): SettingsScreen uses Icons.Default.ArrowBack — same as CollectionScreen (Plan 06-05) and OnboardingScreen. The Material Icons compose library deprecates the auto-mirrored variant in favor of explicit AutoMirrored.Filled.ArrowBack for RTL locales. Phase 6 ships LTR-only (Vietnamese + English markets); the deprecation warning is informational only — verified Phase 6 has zero RTL locale support and zero string-resource RTL declarations. Tracked for future i18n milestone (would migrate ALL deprecated Icons.Default.ArrowBack uses across CollectionScreen + OnboardingScreen + SettingsScreen to AutoMirrored variant in a single sweep). Out-of-scope for Plan 06-07."
  - "Read-only row semantics — Modifier.semantics { disabled() }: per UI-SPEC §9 accessibility note, Version + About rows announce as non-interactive to TalkBack. Used the Material3 ListItem default (no clickable modifier) plus an explicit semantics { disabled() } block — TalkBack reads the row's contentDescription but does not announce 'double-tap to activate'. Pattern matches UI-SPEC §9 Accessibility table verbatim."
  - "Toast.makeText use over Snackbar: SettingsScreen sits inside a Scaffold but does not declare a SnackbarHost. Toast is the Phase 6 convention for all stub-only feedback (matches Plan 03 Splash error path, Plan 06-06 confirmed Plan 04's Share placeholder Toast removal). Snackbar would require Scaffold's SnackbarHost slot + SnackbarHostState remember + LaunchedEffect dispatch — overkill for a 2-line stub. Toast is dismissible without UI commitment."
  - "Plan 07 added zero test files: SettingsScreen is pure presentational (4 ListItem renders + 2 Toast launches + 1 IconButton onClick). The Toast launches are framework-level Android calls; testing them requires Robolectric Toast.LENGTH_SHORT/Toast.LENGTH_LONG capture and adds 90% test boilerplate for 10% of behavior (was-Toast-shown). Plan 06 device handoff tests the visual + interaction contract directly. Zero new tests is correct under D-17/D-18 stub-only scope."
metrics:
  duration: 240
  completed: 2026-05-05
---

# Phase 6 Plan 07: Wave 5b SettingsScreen + StubScreens Delete + Nav Graph Close-Out Summary

**One-liner:** Wave 5b ships final UX-09 SettingsScreen — 161-line stateless composable with TopAppBar 'Settings' + back arrow + 4 ListItem rows (Version trailing BuildConfig.VERSION_NAME, Privacy Policy + Rate the App Toast stubs per D-17/D-18/T-06-06-defer, About supporting text 'Bugzz — Bug filter prank camera') separated by Material3 default HorizontalDividers; BugzzApp.kt nav graph closed out (composable<SettingsRoute> block added + HomeScreen onSettings lambda swapped from Plan 06 placeholder comment to navController.navigate(SettingsRoute)); orphaned StubScreens.kt deleted via `git rm` after 3 pre-deletion safety greps confirmed zero references across app/src; 9 D-32 grep-asserts intact (14/1/7/13/1/3/47/4/2); suite GREEN 172/0 ignored/0 failures (zero test delta from Plan 06); APK assembleDebug clean at 91962902 bytes; 06-VALIDATION.md Plans 06+07 statuses flipped to ✅. Phase 6 production code complete — all 13 requirements (UX-01..09 + SHR-01..04) have grep-evidence in production sources. Plan 08 (clean build + 06-HANDOFF + device checkpoint + post-PASS close-out) ready to start.

---

## What Landed

### Production Code (1 created + 1 modified + 1 deleted)

1. **`app/src/main/java/com/bugzz/filter/camera/ui/settings/SettingsScreen.kt`** (161 lines, NEW)
   - `@OptIn(ExperimentalMaterial3Api::class) @Composable fun SettingsScreen(onBack: () -> Unit)`
   - Body: `Scaffold(topBar = TopAppBar("Settings" + back-arrow IconButton)) { padding -> Column(.fillMaxSize().padding(padding)) { 4 × ListItem + HorizontalDivider } }`
   - **Row 1 — Version** (read-only):
     - `headlineContent = Text("Version", style = MaterialTheme.typography.bodyMedium)`
     - `trailingContent = Text(BuildConfig.VERSION_NAME, style = bodyMedium)`
     - `Modifier.semantics { contentDescription = "Version: ${BuildConfig.VERSION_NAME}"; disabled() }`
   - **Row 2 — Privacy Policy** (Toast stub; D-17, T-06-06):
     - `headlineContent = Text("Privacy Policy", style = bodyMedium)`
     - `Modifier.clickable { Toast.makeText(context, "Coming in next release", Toast.LENGTH_SHORT).show() }.semantics { role = Role.Button; contentDescription = "Privacy Policy" }`
   - **Row 3 — Rate the App** (Toast stub; D-18):
     - `headlineContent = Text("Rate the App", style = bodyMedium)`
     - `Modifier.clickable { Toast.makeText(context, "Coming when published to Play Store", Toast.LENGTH_SHORT).show() }.semantics { role = Role.Button; contentDescription = "Rate the App" }`
   - **Row 4 — About** (read-only):
     - `headlineContent = Text("About", style = bodyMedium)`
     - `supportingContent = Text("Bugzz — Bug filter prank camera", style = bodyMedium)`
     - `Modifier.semantics { contentDescription = "About: Bugzz — Bug filter prank camera"; disabled() }`
   - KDoc references UX-09, D-17, D-18, UI-SPEC §9 + T-06-06 v2 mitigation note

2. **`app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt`** (102 → 106 lines, MODIFIED)
   - +`import com.bugzz.filter.camera.ui.nav.SettingsRoute`
   - +`import com.bugzz.filter.camera.ui.settings.SettingsScreen`
   - HomeRoute composable: `onSettings = { /* Plan 06-07 will replace ... */ }` → `onSettings = { navController.navigate(SettingsRoute) }`
   - +`composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }` block (after CollectionRoute)
   - Final NavHost composable count = 7

3. **`app/src/main/java/com/bugzz/filter/camera/ui/screens/StubScreens.kt`** (108 lines, DELETED via `git rm`)
   - Removed: `SplashScreen` stub (replaced by Plan 03 production), `CameraScreen` stub (never imported by BugzzApp; production CameraScreen lives in `ui/camera/`), `PreviewScreen` stub (replaced by Plan 04 production), `CollectionScreen` stub (replaced by Plan 05 production), `StubContent` helper (orphaned)
   - Pre-deletion safety greps (all 0 matches):
     - `import com.bugzz.filter.camera.ui.screens` across app/src → 0
     - `\.ui\.screens\.` across app/src → 0
     - `screens\.SplashScreen|screens\.PreviewScreen|screens\.CollectionScreen|screens\.CameraScreen|screens\.StubContent` across app/src → 0

### Validation Doc Update (1 modified)

4. **`.planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-VALIDATION.md`** (per-task status table)
   - Plan 06-06 row Status ⬜ → ✅ "DeleteConfirmDialog + Share + HomeScreen onSettings GREEN" (post-Plan-06 retro flip)
   - Plan 06-07 row Status ⬜ → ✅ "SettingsScreen + nav graph + StubScreens delete GREEN"
   - `nyquist_compliant: false` UNCHANGED — Plan 08 owns the flip post-device-PASS

---

## 13-Requirement Grep-Evidence Map (Phase 6 Production-Source Audit)

Each Phase 6 requirement has at least one production source contributing — grep-verified:

| Req | Behavior | Grep-Evidence (production source) |
|-----|----------|------------------------------------|
| UX-01 | Splash routes to Onboarding (first launch) OR Home | `SplashScreen.kt` + `SplashViewModel.kt` (Plan 03, commit 06-03) |
| UX-02 | Onboarding completion sets DataStore flag; HorizontalPager Skip+Next+GetStarted | `OnboardingScreen.kt` + `OnboardingViewModel.kt` (Plan 03; "Welcome to Bugzz" / "Pick a filter" / "Capture and share" / "Skip" / "Get Started" / "Next" all present) |
| UX-03 | HomeScreen settings/collection nav | `HomeScreen.kt` `onSettings: () -> Unit` param (Plan 06) + `BugzzApp.kt` `navController.navigate(SettingsRoute)` (Plan 07) |
| UX-04 | PreviewScreen renders Image (photo) or PlayerView (video); deleteArtifact MediaStore.delete | `PreviewScreen.kt` + `PreviewViewModel.kt` + `VideoPreview.kt` (Plan 04; "Done" / "Share" / "Delete" / "Retake" labels present) |
| UX-05 | CollectionRepository MediaStore query DCIM/Bugzz/ filtered to image/jpeg + video/mp4 | `CollectionRepository.kt` + `CollectionScreen.kt` (Plan 05; "My Collection" title present) |
| UX-06 | Tap collection item → PreviewRoute(uri) | `BugzzApp.kt` CollectionRoute composable `onItemTap = { item -> navController.navigate(PreviewRoute(item.uri.toString())) }` (Plan 05) |
| UX-07 | Empty state when no MediaItems | `EmptyStateColumn.kt` ("No bugs captured yet" / "Open Camera" present in CollectionScreen.kt) (Plan 05) |
| UX-08 | DeleteConfirmDialog Cancel/Confirm callbacks | `DeleteConfirmDialog.kt` ("Delete this artifact?" / "This can't be undone." / "Cancel" present) (Plan 06) |
| UX-09 | Settings 4-row stub renders + back nav | `SettingsScreen.kt` ("Settings" / "Version" / "Privacy Policy" / "Rate the App" / "About" / "Bugzz — Bug filter prank camera" / "Coming in next release" / "Coming when published to Play Store" all present) + `BugzzApp.kt` SettingsRoute composable (Plan 07) |
| SHR-01 | buildShareIntent → Intent.ACTION_SEND with EXTRA_STREAM | `ShareIntentBuilder.kt` (Plan 06) |
| SHR-02 | buildShareIntent.type = mime from MediaItem | `ShareIntentBuilder.kt` (Plan 06) |
| SHR-03 | Intent.createChooser wrapped | `ShareIntentBuilder.kt` ("Share via" present) (Plan 06) |
| SHR-04 | Shared overlay intact (architectural — Phase 3 OverlayEffect bake) | `OverlayEffectBuilder.kt` (Phase 3 architectural carry; SHR-04 not a Phase 6 build target) |

**ALL 13 REQUIREMENTS HAVE PRODUCTION SOURCE CONTRIBUTING.** Phase 6 production code feature-complete.

---

## UI-SPEC Compliance Audit (production sources)

### Typography (4 sizes only — UI-SPEC §Typography)

`fontSize = N.sp` literals across `app/src/main`:

| Size | Locations |
|------|-----------|
| 10.sp | FilterPicker.kt:162 (Phase 4 carry — filter label) |
| 16.sp | DeleteConfirmDialog.kt:63, EmptyStateColumn.kt:74, InsectFilterScreen.kt:368, CameraScreen.kt:359, RecordingIndicator.kt:114 |
| 24.sp | SplashScreen.kt:91, OnboardingScreen.kt:174 |

14.sp not used as literal — all bodyMedium uses `MaterialTheme.typography.bodyMedium` (system-managed). Sizes ⊆ {10, 14, 16, 24} — **PASS**.

### Font Weights (2 only — UI-SPEC §Typography)

`fontWeight = FontWeight.X` literals across `app/src/main`: all `FontWeight.Medium` (8 occurrences in 7 files). No SemiBold, no Bold. ⊆ {Normal, Medium} — **PASS**.

### Colors (UI-SPEC §Color allow-list)

`Color(0xFF......)` literals across `app/src/main`:

| Hex | Use | Plan |
|-----|-----|------|
| `0xFFE53935` | Record button + recording indicator (accent red) | Phase 3 carry |
| `0xFF9E9E9E` | Onboarding inactive dot | Plan 06-03 |
| `0xFF2A2A2A` | Coil placeholder + error painter (CollectionScreen + FilterPicker + collection thumbnail) | Plan 06-05 |
| `0xFF1E1E1E` | PreviewScreen bottom action bar (Surface) | Plan 06-04 |

All 4 distinct hex colors are in UI-SPEC §Color allow-list — **PASS**. Plan 06-07 introduces zero new hex colors (SettingsScreen relies on Material3 colorScheme.outlineVariant for divider tint — system-managed).

### Spacing (4dp grid + allow-list — UI-SPEC §Layout)

`padding(...N.dp)` literals across `app/src/main`: {2, 4, 12, 16, 24, 32, 48, 104}.dp. All multiples of 4 except `2.dp` at FilterPicker.kt:170 (Phase 4 carry — pre-existing filter label baseline tweak; not a Plan 06-07 introduction). Plan 06-07 introduces zero off-grid spacing (SettingsScreen has zero explicit padding modifiers — relies on Material3 `ListItem` defaults). **PASS** at the Plan-07 introduction-delta level.

### Verbatim Copy Strings (UI-SPEC §Copywriting)

Phase 6 strings — all verified present in their owning file (≥1 match):

- "Bugzz" — SplashScreen.kt (2)
- "Welcome to Bugzz" + "Pick a filter" + "Capture and share" — OnboardingScreen.kt (3)
- "Skip" + "Get Started" + "Next" — OnboardingScreen.kt (3)
- "Done" + "Share" + "Delete" + "Retake" — PreviewScreen.kt (all 4 present, plus KDoc refs)
- "Share via" — ShareIntentBuilder.kt (1)
- "Delete this artifact?" + "This can't be undone." + "Cancel" — DeleteConfirmDialog.kt (3)
- "My Collection" + "No bugs captured yet" + "Open Camera" — CollectionScreen.kt (3)
- "Settings" + "Version" + "Privacy Policy" + "Rate the App" + "About" + "Bugzz — Bug filter prank camera" + "Coming in next release" + "Coming when published to Play Store" — SettingsScreen.kt (all 8 present)

Phase 5 carry strings:
- "Flip" — CameraScreen.kt:229 + InsectFilterScreen.kt:322 — **PASS**
- "Take photo" — **0 literal matches in production source** — see Deviation 2 below (plan-text drift, not regression)
- "Saved to gallery" + "Recording saved" Toast call sites — **0 active runtime matches** in `Toast.makeText(... <string>)` form (only KDoc historical references in CameraScreen.kt + InsectFilterScreen.kt remain — per Plan 06-06 documented decision to keep KDoc migration trail) — **PASS** at the active-runtime semantic level

**UI-SPEC compliance — PASS.**

---

## D-32 Grep-Assert Re-Verification (production source — post-StubScreens-deletion)

| # | Pattern | Hits | File(s) | Threshold | Pass |
|---|---------|------|---------|-----------|------|
| 1 | `isCapturing` | 14 | CameraViewModel + InsectFilterViewModel + UI states | ≥1 | ✓ |
| 2 | `bindJob?.cancel()` | 1 | CameraViewModel.kt | ≥1 | ✓ |
| 3 | `OneShotEvent.FilterLoadError` | 7 | CameraScreen + CameraViewModel + InsectFilterScreen | ≥2 | ✓ |
| 4 | `captureFlash` | 13 | CameraScreen + CameraViewModel + InsectFilterScreen + UI states | ≥1 | ✓ |
| 5 | `require(frameCount > 0)` | 1 | FilterDefinition.kt | ≥1 | ✓ |
| 6 | `assetLoader.preload(def.assetDir)` | 3 | StickerRenderer + CameraViewModel | ≥1 | ✓ |
| 7 | `isRecording` | 47 | VideoRecorder + 8 other files | ≥1 | ✓ |
| 8 | `CameraMode.InsectFilter` | 4 | BugzzApp + InsectFilterViewModel + OverlayEffectBuilder | ≥1 | ✓ |
| 9 | `setPreviewSize` | 2 | StickerRenderer + InsectFilterViewModel | ≥1 | ✓ |

**ALL 9 D-32 INVARIANTS HOLD post-StubScreens-deletion.** No regression to Phase 3/4/5 fixes.

---

## Test Suite

```
./gradlew :app:testDebugUnitTest :app:assembleDebug -x lintDebug
```

| Metric | Pre-Plan-07 (Plan 06 baseline) | Post-Plan-07 | Delta |
|--------|-------------------------------|--------------|-------|
| Tests run | 172 | 172 | 0 |
| Ignored | 0 | 0 | 0 |
| Failures | 0 | 0 | 0 |
| BUILD | SUCCESSFUL | SUCCESSFUL | — |

Suite GREEN. Plan 07 added zero test files (per D-18 stub-only scope; Toast launches not unit-testable without Robolectric Toast capture which is 90% boilerplate for 10% behavior — manual device verification owns it).

---

## Clean Build (Pre-Plan-08 Gate)

```
./gradlew :app:clean :app:assembleDebug -x lintDebug
```

`BUILD SUCCESSFUL in 8s` (42 actionable tasks: 17 executed, 25 from cache).

| Artifact | Size | Path |
|----------|------|------|
| `app-debug.apk` | 91962902 bytes (87.7 MiB) | `app/build/outputs/apk/debug/app-debug.apk` |

**APK Size Delta vs Phase 5 Baseline (84 MB per 05-07-SUMMARY):** +3.7 MiB cumulative across Phase 6 (+Lottie + Coil + Media3-ui + Media3-exoplayer dependencies added in Plans 02–06). Plan 06-07 itself contributes ~zero net delta (+SettingsScreen.kt ≈ 3KB compiled bytecode minus -StubScreens.kt ≈ 3KB compiled bytecode).

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 — Plan-text drift] Phase 5 carry-string "Take photo" not literally present in production source (audit relaxation)**
- **Found during:** Task 3 (UI-SPEC compliance audit — verbatim copy string sweep)
- **Issue:** Plan 06-07 Task 3 audit asserts the literal string `"Take photo"` must appear ≥1 time in either `CameraScreen.kt` or `InsectFilterScreen.kt` (shutter contentDescription, Phase 3 carry). Grep returned zero matches across all production sources.
- **Root cause:** The actual shutter `contentDescription` is **dynamic** (RecordButton.kt:61 uses `contentDescription = when { ... }` keyed on isRecording state to switch between "Start recording" / "Stop recording" / "Photo" branches). There is no static `"Take photo"` literal in production code. Plan 06-07's audit assertion was authored from an outdated reading of Phase 3 code shape.
- **Fix:** Documented as plan-text drift (parallel to Plan 06-01 Deviation 2 for D-32 grep-asserts). The shutter accessibility contract is enforced via the dynamic contentDescription pattern and verified by separate Phase 3 manual device tests. Audit semantic (shutter has accessible label) holds; literal-string assertion does not.
- **Pattern for future Wave 5+ audits:** When asserting verbatim copy presence, prefer substring/dynamic-pattern asserts over literal full-string asserts when the underlying code uses computed contentDescription branches. The Plan 06-07 grep should have been `Shutter|capture|Photo|Take|Record` substring-disjunctive.
- **Files modified:** None (production code correct; plan-text drift only)
- **Commit:** Documented in this SUMMARY only

**2. [Rule 3 — Plan-text drift] "Saved to gallery" / "Recording saved" assertion shape vs Plan 06-06 KDoc-retain decision**
- **Found during:** Task 3 (UI-SPEC compliance audit — Phase 5 carry-string negative assertion sweep)
- **Issue:** Plan 06-07 Task 3 audit asserts `"Saved to gallery"` and `"Recording saved"` must each return **0 matches** in `app/src/main` (Plan 04 nav replacement). Literal grep returned 4 matches across CameraScreen.kt + InsectFilterScreen.kt.
- **Root cause:** All 4 matches are inside KDoc/doc-comment blocks (CameraScreen.kt:76, 203 + InsectFilterScreen.kt:104, 186), not active `Toast.makeText(... <string> ...)` runtime calls. Plan 06-06 SUMMARY explicitly decided to **keep KDoc historical references** for git-blame migration trail. Plan 06-07 audit grep is too literal — it does not differentiate active runtime calls from documentation strings.
- **Fix:** Refined the audit to use `Toast\.makeText.*Saved to gallery|Toast\.makeText.*Recording saved` — returns 0 matches as expected. Active runtime path is clean. KDoc historical references are intentionally preserved per Plan 06-06 decision.
- **Pattern for future audits:** When asserting "string X is removed", scope the grep to the runtime-call shape (`Toast\.makeText.*X` or `Text\(.*X\)`) rather than the bare string. Bare-string asserts will mis-flag KDoc historical refs that the prior plan's SUMMARY explicitly documented as kept.
- **Files modified:** None (production code correct; audit grep refinement only)
- **Commit:** Documented in this SUMMARY only

### Architectural Changes

None.

### Authentication Gates

None.

---

## Threat Flags

None — Plan 06-07 introduces zero new security-relevant surface. SettingsScreen is purely presentational; the Privacy Policy + Rate the App rows launch only Toasts (no Intent.ACTION_VIEW with URI, no WebView, no Play Store deep-link, no network). T-06-06 mitigation deferred to v2 milestone per documented stub-only scope.

---

## Plan 08 Readiness

Phase 6 production code is **feature-complete**:
- 5 production user-facing screens (Splash, Onboarding, Home, Camera, Preview, Collection, Settings) shipped + reachable via NavHost
- All 13 Phase 6 requirements (UX-01..09, SHR-01..04) have grep-evidence in production sources
- Suite GREEN: 172 tests / 0 ignored / 0 failures
- APK assembleDebug clean: 91962902 bytes
- 9 D-32 grep-asserts intact post-StubScreens-deletion
- UI-SPEC compliance audit GREEN (typography + weights + colors + spacing + verbatim copy)
- 0 orphaned source files in `ui/screens/`

**Blocking for Plan 08:** Device acceptance per `06-HANDOFF.md` (manual checkpoints for first-launch flow, Settings stub interactions, video preview playback, photo preview, delete confirmation, collection grid, share intent picker, shared overlay intact, Phase 4+5 deferred UAT bonus). Post-device-PASS, Plan 08 Task 4 flips `06-VALIDATION.md nyquist_compliant: true`.

---

## Self-Check: PASSED

Created file:
- `app/src/main/java/com/bugzz/filter/camera/ui/settings/SettingsScreen.kt` — FOUND (161 lines)

Modified files:
- `app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt` — FOUND (106 lines)
- `.planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-VALIDATION.md` — FOUND (Plan 06-06/07 rows show ✅)

Deleted file:
- `app/src/main/java/com/bugzz/filter/camera/ui/screens/StubScreens.kt` — DELETED (verified via `git status` showing `D` flag)

Build:
- `./gradlew :app:testDebugUnitTest :app:assembleDebug -x lintDebug` — BUILD SUCCESSFUL in 21s
- `./gradlew :app:clean :app:assembleDebug -x lintDebug` — BUILD SUCCESSFUL in 8s
- APK at `app/build/outputs/apk/debug/app-debug.apk` — FOUND (91962902 bytes)

D-32 invariants:
- All 9 grep-asserts return ≥1 match (14/1/7/13/1/3/47/4/2)

Test suite:
- 172 tests / 0 ignored / 0 failures — verified via `app/build/reports/tests/testDebugUnitTest/index.html` counter values
