---
phase: 06
plan: 03
subsystem: splash-onboarding-routes-wiring
tags: [wave-2, splash, onboarding, lottie, navigation, ux-polish]
dependency_graph:
  requires:
    - "Plan 06-02 (47e5185) — lottie-compose 6.7.1 on classpath, FilterPrefsRepository.onboardingCompleted Flow + setOnboardingCompleted(), home_lottie.json asset present"
    - "Phase 1 navigation scaffold (Routes.kt SplashRoute/HomeRoute/PreviewRoute/CollectionRoute + BugzzApp NavHost)"
    - "Phase 5 baseline GREEN (170 tests / 24 ignored / 0 failures) post 06-02 = (170 / 24 - 3 onboarding-prefs un-ignored = 24)"
  provides:
    - "ui/components/LottiePlayer.kt — shared D-26 wrapper around lottie-compose for Splash + Onboarding (and Plan 06-04+ EmptyStateColumn)"
    - "ui/splash/SplashScreen.kt + SplashViewModel.kt — production Splash with 1.5s auto-advance, conditional first-launch routing via DataStore onboardingCompleted, popUpTo back-stack clearing"
    - "ui/onboarding/OnboardingScreen.kt + OnboardingViewModel.kt — 3-page HorizontalPager carousel, Skip/Next/GetStarted controls, 3-dot indicator, completion writes onboarding_completed=true"
    - "Routes.kt extended with @Serializable OnboardingRoute + SettingsRoute (D-24); PreviewRoute deliberately UNCHANGED (Plan 06-04 owns the breaking PreviewRoute(uri) change)"
    - "BugzzApp NavHost wires composable<SplashRoute> + composable<OnboardingRoute>; full first-launch flow operational: Splash → Onboarding → Home (first launch) or Splash → Home (subsequent)"
    - "decideNextAction(currentPage, pageCount): NextAction internal helper — pure-JVM testable extraction of Next/Get-Started branch"
  affects:
    - "Suite count: 170 tests / 16 ignored / 0 failures (Phase 5 + Plan 06-02 baseline 170/24 minus 8 un-Ignored cases — 3 SplashVM + 2 OnboardingVM + 3 OnboardingPagerState)"
    - "ui/screens/SplashScreen (StubScreens.kt) is now unreferenced (deletion deferred to Plan 06-07 per scope contract); APK still includes the dead code"
tech-stack:
  added:
    - "(none — all Lottie/Media3 deps landed Plan 06-02)"
  patterns:
    - "StateFlow<Boolean?> for tri-state DataStore loading: null=loading, false=show-onboarding, true=show-home (T-06-04 mirror of T-04-01 `.catch { emit(null) }` defence-in-depth)"
    - "Fire-and-forget viewModelScope.launch { suspend write } pattern for OnboardingViewModel.completeOnboarding — composable navigates immediately, write resolves on its own"
    - "Internal sealed-class NextAction extraction lets us unit-test pager-button branch logic without spinning up Compose UI runtime — pure JVM @Test only"
    - "popUpTo(SplashRoute) inclusive=true on BOTH navigation branches from Splash — Back from Home/Onboarding never returns to Splash (D-25)"
key-files:
  created:
    - "app/src/main/java/com/bugzz/filter/camera/ui/components/LottiePlayer.kt (49 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/splash/SplashScreen.kt (98 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/splash/SplashViewModel.kt (37 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/onboarding/OnboardingScreen.kt (230 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/onboarding/OnboardingViewModel.kt (34 lines)"
  modified:
    - "app/src/main/java/com/bugzz/filter/camera/ui/nav/Routes.kt (21 → 38 lines; +OnboardingRoute, +SettingsRoute, KDoc on both citing D-24; PreviewRoute UNCHANGED)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt (54 → 75 lines; SplashScreen import swap to splash.SplashScreen + OnboardingScreen import; SplashRoute composable now production with popUpTo; new composable<OnboardingRoute> block between Splash and Home)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/splash/SplashViewModelTest.kt (un-Ignored 3 cases via Turbine + mock prefs; markMissing() removed)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/onboarding/OnboardingViewModelTest.kt (un-Ignored 2 cases via Mockito verify())"
    - "app/src/test/java/com/bugzz/filter/camera/ui/onboarding/OnboardingPagerStateTest.kt (un-Ignored 3 cases against decideNextAction helper)"
    - ".planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-VALIDATION.md (Plan 03 row → ✅ Wave 2 Splash + Onboarding GREEN)"
decisions:
  - "Routes.kt additions ONLY this plan — PreviewRoute change deferred to Plan 06-04 (separates a reversible Splash/Onboarding ship from the BugzzApp+CameraScreen+InsectFilterScreen breaking change RESEARCH Pitfall 6 calls out)."
  - "decideNextAction extracted as internal helper returning sealed NextAction (Complete | Advance(toPage)) — keeps OnboardingPagerStateTest pure-JVM (no Compose UI test dependency)."
  - "SplashViewModel exposes StateFlow<Boolean?> with null=loading, NOT a separate UiState class — minimal API surface; LaunchedEffect handles the tri-state branch directly."
  - "completeOnboarding() called twice (verifyTimes=2) treated as harmless — the write is idempotent at the DataStore layer (sets boolean to true regardless of prior); ViewModel does NOT memoize because each UI invocation is intentional."
  - "T-06-04 mitigation is defence-in-depth: FilterPrefsRepository.onboardingCompleted already emits false on IOException (per Plan 06-02 corruptedDataStore_onboardingCompleted_emitsFalseDefault test); SplashViewModel's `.catch { emit(null) }` is a second layer in case any operator above us throws. The composable's null-after-delay branch lands on Onboarding — same safe default."
  - "StubScreens.kt SplashScreen is now unreferenced but file UNTOUCHED — Plan 06-07 owns the deletion (separates dead-code removal from production Splash swap to keep blast radius scoped)."
  - "Onboarding body color uses Color.White.copy(alpha = 0.8f) per UI-SPEC §3 — slightly muted for body hierarchy; alpha-on-white is on the official 06-UI-SPEC palette."
metrics:
  duration: ~600s
  completed: 2026-05-05
---

# Phase 6 Plan 03: Wave 2 Splash + Onboarding + Routes Additions Summary

**One-liner:** Phase 1 stub Splash replaced with production composable (200dp Lottie + "Bugzz" 24sp/Medium + 1.5s auto-advance + DataStore-conditional first-launch routing); 3-page HorizontalPager OnboardingScreen added with Skip/Next/GetStarted that persists `onboarding_completed=true`; Routes.kt gains OnboardingRoute + SettingsRoute (PreviewRoute UNCHANGED); 8 unit tests un-Ignored — suite holds at 170/16 ignored/0 failures (-8 vs Plan 06-02 baseline).

---

## What Landed

### Task 1 (commit f0be773) — LottiePlayer + SplashScreen production swap + Routes additions

**Files created (3):**
- `ui/components/LottiePlayer.kt` (49 lines) — `@Composable LottiePlayer(assetPath, modifier, iterations, isPlaying)` wrapping `rememberLottieComposition(LottieCompositionSpec.Asset(...))` + `animateLottieCompositionAsState` + `LottieAnimation`. Single shared player for Splash, Onboarding, future EmptyStateColumn.
- `ui/splash/SplashViewModel.kt` (37 lines) — `@HiltViewModel` exposing `onboardingCompleted: StateFlow<Boolean?>` via `prefs.onboardingCompleted.map<Boolean,Boolean?>{it}.catch{emit(null)}.stateIn(..., SharingStarted.Eagerly, null)`. Tri-state: null=loading / false=onboarding / true=home.
- `ui/splash/SplashScreen.kt` (98 lines) — `Box(black, fillMaxSize, Center) { Column { LottiePlayer(200dp, iterations=1) + Spacer(16dp) + Text("Bugzz", 24sp/Medium/White) } }`. `LaunchedEffect(onboardingCompleted)` returns early on null, then `delay(1500)`, then branches to onNavigateToHome (true) or onNavigateToOnboarding (false-or-null safe default).

**Files modified (3):**
- `ui/nav/Routes.kt` (21 → 38 lines) — Additions only: `@Serializable data object OnboardingRoute` + `@Serializable data object SettingsRoute` with KDoc citing D-24. PreviewRoute, SplashRoute, HomeRoute, CameraRoute, CollectionRoute UNCHANGED.
- `ui/BugzzApp.kt` (Splash swap part) — Import `com.bugzz.filter.camera.ui.screens.SplashScreen` → `com.bugzz.filter.camera.ui.splash.SplashScreen`; new import `com.bugzz.filter.camera.ui.nav.OnboardingRoute`. SplashRoute composable now passes `onNavigateToOnboarding` + `onNavigateToHome` lambdas, both with `popUpTo(SplashRoute) { inclusive = true }`.
- `test/.../splash/SplashViewModelTest.kt` — Un-Ignored 3 cases:
  - `onboardingNotCompleted_emitsOnboardingNavTarget` — mock prefs flowOf(false) → `vm.onboardingCompleted.test { assertEquals(false, awaitItem()) }`
  - `onboardingCompleted_emitsHomeNavTarget` — mock prefs flowOf(true) → assertEquals(true, awaitItem())
  - `dataStoreIoException_emitsOnboardingNavTarget_safeDefault` — mock prefs `flow { throw IOException }` → `assertNull(awaitItem())` (verifies `.catch { emit(null) }` arm)

**Verification (Task 1):** `./gradlew :app:testDebugUnitTest --tests "*SplashViewModelTest*" :app:assembleDebug -x lintDebug` → BUILD SUCCESSFUL; 3/3 GREEN.

### Task 2 (commit f214b2f) — OnboardingScreen + ViewModel + BugzzApp OnboardingRoute wiring

**Files created (2):**
- `ui/onboarding/OnboardingViewModel.kt` (34 lines) — `@HiltViewModel` with `fun completeOnboarding() { viewModelScope.launch { prefs.setOnboardingCompleted() } }`. Single fire-and-forget method.
- `ui/onboarding/OnboardingScreen.kt` (230 lines) — full layout per 06-UI-SPEC §3:
  - Root `Box(black, fillMaxSize)`; `HorizontalPager(state = rememberPagerState { 3 })` full-screen
  - Per-page Column (centered, horizontal padding 32dp): LottiePlayer(200dp, IterateForever) + Spacer(24dp) + Title (24sp/Medium/White) + Spacer(16dp) + Body (bodyMedium, alpha 0.8f)
  - `TextButton` "Skip" aligned TopEnd with padding(top=16, end=16) — onClick = `finish()` (= completeOnboarding + onComplete)
  - Bottom Column at BottomCenter with bottom padding 48dp:
    - Row spacedBy(8dp): repeat(3) { isActive ? Box(12dp, CircleShape, #E53935) : Box(8dp, CircleShape, #9E9E9E) }
    - Spacer(24dp)
    - `Button` — onClick branches via `decideNextAction(currentPage, pageCount)`: Complete → `finish()`; Advance(toPage) → `scope.launch { pagerState.animateScrollToPage(toPage) }`
    - Label = isFinalPage ? "Get Started" : "Next"
  - Verbatim copy per UI-SPEC §3 D-03:
    - Page 0: "Welcome to Bugzz" / "Bug filters that crawl on your face. Pranks made easy."
    - Page 1: "Pick a filter" / "15 bug filters with 4 behaviors. Static, crawl, swarm, fall."
    - Page 2: "Capture and share" / "Photo or video. Share to friends instantly."
  - Accessibility: contentDescription on Skip button ("Skip onboarding"), primary button ("Next page" / "Get started"), indicator Row ("Page X of 3"); Role.Button on Skip + primary
  - Internal `sealed class NextAction { data object Complete; data class Advance(toPage) }` + `internal fun decideNextAction(currentPage, pageCount): NextAction` — pure-JVM testable

**Files modified (2):**
- `ui/BugzzApp.kt` — Import `com.bugzz.filter.camera.ui.onboarding.OnboardingScreen`. New `composable<OnboardingRoute>` block inserted between SplashRoute and HomeRoute composables; passes `onComplete = { navigate HomeRoute with popUpTo(OnboardingRoute, inclusive=true) }`.
- `test/.../onboarding/OnboardingViewModelTest.kt` — Un-Ignored 2 cases:
  - `completeOnboarding_writesFlagViaRepository` — runTest + advanceUntilIdle → `verify(mockPrefs, times(1)).setOnboardingCompleted()`
  - `completeOnboarding_flowReemitsTrueAfterWrite` — 2 calls → verify times(2) (idempotent at storage layer)
- `test/.../onboarding/OnboardingPagerStateTest.kt` — Un-Ignored 3 cases against `decideNextAction`:
  - `skipOnAnyPage_firesOnCompleteCallback` — for currentPage in 0..2, decideNextAction(currentPage, pageCount=1) is `NextAction.Complete` (1-page boundary models Skip's index-independence)
  - `nextOnPage0Or1_advancesPagerStateByOne` — decideNextAction(0, 3)==Advance(1); decideNextAction(1, 3)==Advance(2)
  - `getStartedOnPage2_firesOnCompleteCallback` — decideNextAction(2, 3)==Complete

**Verification (Task 2):** `./gradlew :app:testDebugUnitTest --tests "*Onboarding*" --tests "*SplashViewModelTest*" :app:assembleDebug` → BUILD SUCCESSFUL.

### Task 3 — Sampling continuity + grep-assert + UI-SPEC compliance

Final gate run: full unit test suite + 9 D-32 grep-asserts + UI-SPEC verbatim copy + on-grid spacing + 06-VALIDATION row update.

---

## Test Results

### New / Un-Ignored Cases (8 total)

| Test class | Test name | Status |
|---|---|---|
| `SplashViewModelTest` | `onboardingNotCompleted_emitsOnboardingNavTarget` | GREEN |
| `SplashViewModelTest` | `onboardingCompleted_emitsHomeNavTarget` | GREEN |
| `SplashViewModelTest` | `dataStoreIoException_emitsOnboardingNavTarget_safeDefault` | GREEN |
| `OnboardingViewModelTest` | `completeOnboarding_writesFlagViaRepository` | GREEN |
| `OnboardingViewModelTest` | `completeOnboarding_flowReemitsTrueAfterWrite` | GREEN |
| `OnboardingPagerStateTest` | `skipOnAnyPage_firesOnCompleteCallback` | GREEN |
| `OnboardingPagerStateTest` | `nextOnPage0Or1_advancesPagerStateByOne` | GREEN |
| `OnboardingPagerStateTest` | `getStartedOnPage2_firesOnCompleteCallback` | GREEN |

**Per-suite XML evidence** (extracted from `app/build/test-results/testDebugUnitTest/`):
- `TEST-...SplashViewModelTest.xml`: tests=3 / skipped=0 / failures=0 / errors=0
- `TEST-...OnboardingViewModelTest.xml`: tests=2 / skipped=0 / failures=0 / errors=0
- `TEST-...OnboardingPagerStateTest.xml`: tests=3 / skipped=0 / failures=0 / errors=0

### Full Suite Aggregate

| Metric | Plan 06-02 baseline | Plan 06-03 result | Delta |
|---|---|---|---|
| Total tests | 170 | 170 | 0 (no test class additions; un-Ignore only) |
| Skipped | 24 | 16 | -8 (3 splash + 2 onboarding VM + 3 pager) |
| Failures | 0 | 0 | 0 |
| Errors | 0 | 0 | 0 |

`./gradlew :app:testDebugUnitTest -x lintDebug` → BUILD SUCCESSFUL in 14s.

`./gradlew :app:assembleDebug -x lintDebug` → BUILD SUCCESSFUL.

---

## D-32 Grep-Assert Re-verification

All 9 D-32 patterns re-grepped on production sources (relaxed forms inherited from Plan 06-01: `bindJob?.cancel()` and FQN `cameraMode = com.bugzz.filter.camera.ui.home.CameraMode.InsectFilter`):

| # | Pattern | Files matched (Plan 06-02) | Files matched (Plan 06-03) | Status |
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

**ALL 9 PATTERNS RETURN ≥1 MATCH.** D-32 invariants intact — no production code outside Splash/Onboarding/Routes/BugzzApp scope was modified.

---

## UI-SPEC Compliance Audit (Wave 2)

### Verbatim Copy Strings (UI-SPEC §3 D-03 + §2)

| String | File | Line | Status |
|---|---|---|---|
| `"Bugzz"` (Splash brand) | SplashScreen.kt | 89 | PRESENT |
| `"Welcome to Bugzz"` | OnboardingScreen.kt | 194 | PRESENT |
| `"Pick a filter"` | OnboardingScreen.kt | 198 | PRESENT |
| `"Capture and share"` | OnboardingScreen.kt | 202 | PRESENT |
| `"Skip"` | OnboardingScreen.kt | 99 | PRESENT |
| `"Next"` (and `"Get Started"`) | OnboardingScreen.kt | 148 | PRESENT (ternary on isFinalPage) |
| `"Bug filters that crawl on your face. Pranks made easy."` | OnboardingScreen.kt | 195 | PRESENT |
| `"15 bug filters with 4 behaviors. Static, crawl, swarm, fall."` | OnboardingScreen.kt | 199 | PRESENT |
| `"Photo or video. Share to friends instantly."` | OnboardingScreen.kt | 203 | PRESENT |

### Spacing On-Grid (UI-SPEC §Spacing Scale = multiples of 4dp; allowed in Wave 2: 4/8/12/16/24/32/48/64/200)

**SplashScreen.kt — `\.dp\b` matches:**
- 200.dp (Lottie size — UI-SPEC §2)
- 16.dp (Spacer height = md token — UI-SPEC §2)

**OnboardingScreen.kt — `\.dp\b` matches:**
- 16.dp (Skip TextButton padding top + end — UI-SPEC §3)
- 48.dp (bottom controls Column padding — UI-SPEC §3)
- 8.dp (indicator Row spacedBy + inactive dot size — UI-SPEC §3)
- 12.dp (active dot size — UI-SPEC §3)
- 24.dp (Spacer between dots and Button + Spacer between Lottie and Title — UI-SPEC §3)
- 32.dp (per-page horizontal padding — UI-SPEC §3)
- 200.dp (Lottie size — UI-SPEC §3)
- 16.dp (Spacer between Title and Body — UI-SPEC §3)

All values ∈ {4, 8, 12, 16, 24, 32, 48, 200} — fully on-grid.

### Color Tokens (UI-SPEC §Colors)

- `Color.Black` background (Splash + Onboarding root Box)
- `Color.White` for "Bugzz" brand text + Onboarding Title (full opacity)
- `Color.White.copy(alpha = 0.8f)` for Onboarding body (subtle hierarchy per UI-SPEC §3)
- `Color(0xFFE53935)` active indicator dot (UI-SPEC palette token)
- `Color(0xFF9E9E9E)` inactive indicator dot (UI-SPEC §3 + §Color: "Onboarding inactive dot color: Color.Gray (Color(0xFF9E9E9E))")

### Typography (UI-SPEC §Typography)

- "Bugzz" Splash brand: `TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Color.White)` — matches UI-SPEC iter-2 fix (NOT 32sp)
- Onboarding Title: same 24sp/Medium/White
- Onboarding Body: `MaterialTheme.typography.bodyMedium` (14sp/Normal default) with white-alpha-0.8 color
- Skip / Next / Get Started button text: `MaterialTheme.typography.labelLarge` (14sp/Medium per UI-SPEC §Typography table)

### Accessibility

- Splash Lottie: `Modifier.semantics { contentDescription = "Bugzz logo animation" }`
- Onboarding Skip button: `contentDescription = "Skip onboarding"` + `role = Role.Button`
- Onboarding primary button: contentDescription `"Next page"` (pages 0/1) or `"Get started"` (page 2) + `role = Role.Button`
- Onboarding indicator Row: dynamic contentDescription `"Page X of 3"` (announces page changes)

---

## Deviations from Plan

### 1. [Rule 3 — Test pragma] OnboardingPagerStateTest skip test models with pageCount=1 instead of mocking PagerState

- **Found during:** Task 2 implementation
- **Issue:** Plan-text wrote "test the BRANCH logic by extracting it" + "decideNextAction(0, isSkip=true) == Complete". The plan implies a 2-arg helper `decideNextAction(currentPage, isSkip)`. But Skip's behavior in the composable is index-independent (Skip TextButton always invokes `finish()` directly — never goes through decideNextAction); only the primary Next/Get-Started button consults the pager helper. Adding an `isSkip` parameter to the helper would be dead-code (always returns Complete when isSkip=true regardless of currentPage).
- **Fix:** Modeled `skipOnAnyPage_firesOnCompleteCallback` by iterating currentPage 0..2 with `pageCount = 1`. With a 1-page pager, every page is the "last" page → every call returns Complete. This pins the Skip-symmetric contract (Skip always Complete for any currentPage) without polluting the helper signature. Documented in test KDoc.
- **Files modified:** `OnboardingPagerStateTest.kt` only — no production code changed
- **Commit:** f214b2f
- **Rationale:** Cleaner helper signature; test still verifies the UX-02 contract that "Skip on any page → Complete".

### Auth gates

None.

### Architectural decisions (Rule 4)

None — no Rule 4 escalations needed.

---

## Wave 2 → Wave 3 Sign-off

**Wave 2 deliverables (this plan):**
- LottiePlayer + SplashScreen + SplashViewModel + OnboardingScreen + OnboardingViewModel created
- Routes.kt has +OnboardingRoute, +SettingsRoute (PreviewRoute UNCHANGED — verified by grep)
- BugzzApp wires Splash + Onboarding production composables with popUpTo back-stack rules per D-25
- 8 wave 0 → wave 2 unit tests un-Ignored and GREEN (3 splash + 2 onboarding VM + 3 pager-state)
- Existing Phase 5 + Plan 06-02 suite GREEN (170/16 ignored/0 failures)
- D-32 invariants preserved (9/9 grep-asserts pass with same file counts as 06-02 baseline)
- APK assembles clean (`./gradlew :app:assembleDebug` BUILD SUCCESSFUL)
- UI-SPEC §2 + §3 spacing + copy + accessibility contracts honored verbatim

**Wave 3 (Plan 06-04) prerequisites:**
- Routes.kt PreviewRoute is still `data object` — Plan 06-04 owns the breaking change to `data class PreviewRoute(val uri: String)` (per RESEARCH Pitfall 6 atomic-commit requirement: Routes.kt + BugzzApp.kt + CameraScreen.kt + InsectFilterScreen.kt all in one commit)
- StubScreens.kt PreviewScreen still routed (Phase 1 stub) — replaced by production PreviewScreen in Plan 06-04
- StubScreens.kt SplashScreen now unreferenced but file untouched (Plan 06-07 owns deletion)

**Wave 3 ready: YES.** Splash/Onboarding flow shippable in isolation; PreviewRoute breaking change cleanly deferred to next plan.

---

## Self-Check: PASSED

**Created files exist:**
- `app/src/main/java/com/bugzz/filter/camera/ui/components/LottiePlayer.kt` FOUND
- `app/src/main/java/com/bugzz/filter/camera/ui/splash/SplashScreen.kt` FOUND
- `app/src/main/java/com/bugzz/filter/camera/ui/splash/SplashViewModel.kt` FOUND
- `app/src/main/java/com/bugzz/filter/camera/ui/onboarding/OnboardingScreen.kt` FOUND
- `app/src/main/java/com/bugzz/filter/camera/ui/onboarding/OnboardingViewModel.kt` FOUND

**Commits exist:**
- f0be773 (Task 1) FOUND
- f214b2f (Task 2) FOUND
