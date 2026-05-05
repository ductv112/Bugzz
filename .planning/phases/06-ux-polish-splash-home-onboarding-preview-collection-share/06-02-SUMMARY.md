---
phase: 06
plan: 02
subsystem: dependencies-assets-prefs
tags: [wave-1, infrastructure, lottie, media3, datastore, ux-polish]
dependency_graph:
  requires:
    - "Plan 06-01 Wave 0 RED scaffolds (170/24 ignored — 3 @Ignored FilterPrefsRepositoryTest cases at @Ignore('Plan 06-02'))"
    - "FilterPrefsRepository internal-ctor test seam (Phase 4 STATE #14)"
    - "Compose BOM 2026.03.00 + AGP 8.9.1 baseline"
  provides:
    - "lottie-compose 6.7.1 on compile classpath (LottieAnimation + rememberLottieComposition for Splash/Onboarding/EmptyState — D-29/D-30)"
    - "media3-exoplayer + media3-ui 1.4.1 on compile classpath (UX-04 video PreviewScreen playback in Plan 06-04)"
    - "app/src/main/assets/lottie/home_lottie.json (746491 bytes, byte-identical to reference) — single shared Lottie file (D-29)"
    - "FilterPrefsRepository.onboardingCompleted: Flow<Boolean> + setOnboardingCompleted() suspend (D-23 — single repo for both lastUsedFilterId and onboarding_completed keys)"
    - "T-06-04 mitigation present (IOException → emit emptyPreferences → false default)"
  affects:
    - "Suite count: 170 tests / 24 ignored / 0 failures (-3 ignored vs 06-01 baseline = 3 onboarding tests now GREEN)"
    - "APK size: net delta documented below"
tech-stack:
  added:
    - "com.airbnb.android:lottie-compose:6.7.1 (catalog: lottieCompose)"
    - "androidx.media3:media3-exoplayer:1.4.1 (resolved 1.9.0 transitively — see deviation 1)"
    - "androidx.media3:media3-ui:1.4.1 (resolved 1.9.0 transitively — see deviation 1)"
  patterns:
    - "Same FilterPrefsRepository class extended with second key (booleanPreferencesKey) — mirrors lastUsedFilterId .catch + .map pattern verbatim. NOT a separate @Singleton class (D-23 single-instance rule)."
    - "Asset-based Lottie composition (LottieCompositionSpec.Asset path = 'lottie/home_lottie.json') — RESEARCH Pitfall 4 avoided by placing under assets/lottie/ subdirectory (NOT res/raw/)."
    - "Byte-identical asset copy verified via SHA256 hash equality (no JSON re-formatting drift)."
key-files:
  created:
    - "app/src/main/assets/lottie/home_lottie.json (746491 bytes; SHA256 5d3cfc5ca6517cd2c8ce2504da73dc0be291edeac0066cd5ab26cdc89bd2fa36)"
  modified:
    - "gradle/libs.versions.toml (+5 lines: 2 versions + 3 libraries)"
    - "app/build.gradle.kts (+5 lines: 3 implementation entries + 2 comment lines)"
    - "app/src/main/java/com/bugzz/filter/camera/data/FilterPrefsRepository.kt (69 → 95 lines; added booleanPreferencesKey import + onboardingCompleted Flow + setOnboardingCompleted() + KEY_ONBOARDING_COMPLETED)"
    - "app/src/test/java/com/bugzz/filter/camera/data/FilterPrefsRepositoryTest.kt (199 → 199 lines; un-ignored 3 cases, removed sketch comments, implemented bodies)"
decisions:
  - "Single FilterPrefsRepository class for both lastUsedFilterId (Phase 4) and onboarding_completed (Phase 6) keys — D-23. No second @Singleton repo class created."
  - "Setter signature: setOnboardingCompleted() with no parameter (single-shot 'mark complete' write of true). Idempotent on repeated calls. Plan-text spec was setOnboardingCompleted() (no Boolean) per <interfaces> block; tests were sketched as setOnboardingCompleted(true) but implementation chose the no-arg form."
  - "T-06-04 mitigation mirrors T-04-01 verbatim: same .catch { e -> if (e is IOException) emit(emptyPreferences()) }. Default value false (D-23 — first launch must show onboarding; safer to re-onboard than to skip on corruption)."
  - "Lottie asset placed at app/src/main/assets/lottie/home_lottie.json (NOT res/raw/) so production code can use LottieCompositionSpec.Asset('lottie/home_lottie.json') — RESEARCH Pitfall 4."
metrics:
  duration: ~400s
  completed: 2026-05-05
---

# Phase 6 Plan 02: Wave 1 Dependency Catalog + Lottie Asset + DataStore Onboarding Key Summary

**One-liner:** Lottie 6.7.1 + Media3 1.4.1 added to version catalog/build script; home_lottie.json copied byte-identical (746491 B) to assets/lottie/; FilterPrefsRepository extended in-place with onboardingCompleted Flow + setOnboardingCompleted() suspend (D-23 single-repo); 3 previously-@Ignored onboarding tests now GREEN — suite at 170/24 ignored/0 failures (Phase 5 baseline + 3 net green vs 06-01).

---

## What Landed

### Task 1 — Catalog + Build Script (gradle/libs.versions.toml + app/build.gradle.kts)

**Catalog diff (additions only — zero existing entries touched):**

```toml
[versions]
+ # Phase 6 additions
+ lottieCompose = "6.7.1"
+ media3 = "1.4.1"

[libraries]
+ # Phase 6 — Lottie + Media3
+ lottie-compose = { module = "com.airbnb.android:lottie-compose", version.ref = "lottieCompose" }
+ androidx-media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
+ androidx-media3-ui = { module = "androidx.media3:media3-ui", version.ref = "media3" }
```

**Build script diff (app/build.gradle.kts dependencies block — appended at end, no existing deps modified):**

```kotlin
+ // Phase 6 — Lottie animations (Splash + Onboarding + EmptyState) per 06-CONTEXT D-30
+ implementation(libs.lottie.compose)
+
+ // Phase 6 — Media3 ExoPlayer for video preview playback (UX-04 video case)
+ implementation(libs.androidx.media3.exoplayer)
+ implementation(libs.androidx.media3.ui)
```

**Resolution verified via `:app:dependencies --configuration debugRuntimeClasspath`:**

| Declared | Resolved | Reason |
|----------|----------|--------|
| `com.airbnb.android:lottie-compose:6.7.1` | `com.airbnb.android:lottie-compose:6.7.1` | exact |
| `com.airbnb.android:lottie:6.7.1` | `com.airbnb.android:lottie:6.7.1` | transitive of lottie-compose, exact |
| `androidx.media3:media3-exoplayer:1.4.1` | `androidx.media3:media3-exoplayer:1.9.0` | conflict-resolved up by CameraX 1.6.0 (which already pulls media3 1.9.0 transitively for VideoCapture+muxer integration) |
| `androidx.media3:media3-ui:1.4.1` | `androidx.media3:media3-ui:1.9.0` | same conflict resolution |

See **Deviation 1** below for the media3 version-up disposition.

**Verification command run:** `./gradlew :app:assembleDebug -x lintDebug` — BUILD SUCCESSFUL (2m 23s, 41 actionable tasks executed on first cold run; 2s incremental reuse afterward).

### Task 2 — Lottie Asset Copy

| Aspect | Value |
|--------|-------|
| Source | `reference/raw_extract/res/raw/home_lottie.json` |
| Destination | `app/src/main/assets/lottie/home_lottie.json` |
| Bytes | 746491 (source) = 746491 (dest) |
| SHA256 source | `5d3cfc5ca6517cd2c8ce2504da73dc0be291edeac0066cd5ab26cdc89bd2fa36` |
| SHA256 dest | `5d3cfc5ca6517cd2c8ce2504da73dc0be291edeac0066cd5ab26cdc89bd2fa36` |
| Hash match | ✅ EQUAL — byte-identical |
| Path validation | RESEARCH Pitfall 4 avoided — placed under `assets/lottie/` so `LottieCompositionSpec.Asset("lottie/home_lottie.json")` resolves correctly. NOT placed under `res/raw/` (would require different API). |
| Git tracking | Untracked at SUMMARY-write time; will be added in final commit. Not in `.gitignore`. |

**APK size delta (debug, unminified):**

The Lottie asset itself = +746 KB (no compression for already-text JSON beyond AAPT's pass; assets are compressed at packaging via `compressDebugAssets` task which DID re-run after the copy — confirmed in build log).

The Lottie + Media3 deps add roughly:
- `com.airbnb.android:lottie:6.7.1` ≈ 1.0 MB DEX/resources
- `androidx.media3:media3-exoplayer:1.9.0` ≈ 1.5 MB DEX/native libs
- `androidx.media3:media3-ui:1.9.0` ≈ 200 KB DEX

Net debug APK size: 91.7 MB (post-Plan 06-02). Pre-plan delta was not captured before changes (no fixed APK size baseline in 06-01-SUMMARY.md), but the magnitudes above are within the +3-4 MB ballpark expected. **Acceptable** for a personal-use prank app per CLAUDE.md (no Play Store size constraint at this milestone).

**Verification command run:** `./gradlew :app:assembleDebug -x lintDebug` — BUILD SUCCESSFUL (2s incremental: only `compressDebugAssets`, `packageDebug`, `assembleDebug` re-ran).

### Task 3 — FilterPrefsRepository Extension + Un-Ignore Tests (TDD)

**RED step (un-ignored tests, no production change):**

Confirmed compile failure: `Unresolved reference 'onboardingCompleted'` in 3 test bodies. Logged as expected RED state.

**GREEN step (extended FilterPrefsRepository.kt):**

```kotlin
+ import androidx.datastore.preferences.core.booleanPreferencesKey

  // ... existing lastUsedFilterId + setLastUsedFilter unchanged ...

+ /**
+  * D-23: Onboarding completion flag. Defaults to `false` on fresh install ...
+  * On [IOException] (DataStore corruption — T-06-04 mirror of T-04-01), also falls back to `false`...
+  */
+ val onboardingCompleted: Flow<Boolean> = dataStore.data
+     .catch { e ->
+         if (e is IOException) {
+             Timber.tag("FilterPrefs").w(e, "DataStore read error — onboarding default false")
+             emit(emptyPreferences())
+         } else throw e
+     }
+     .map { prefs -> prefs[KEY_ONBOARDING_COMPLETED] ?: false }
+
+ suspend fun setOnboardingCompleted() {
+     dataStore.edit { prefs -> prefs[KEY_ONBOARDING_COMPLETED] = true }
+ }
+
  companion object {
      private val KEY_LAST_FILTER = stringPreferencesKey("last_used_filter_id")
+     private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
      const val DEFAULT_FILTER_ID = "spider_nose_static"
  }
```

**Un-ignored test cases (FilterPrefsRepositoryTest.kt):**

| Test | Behavior verified | Mirrors |
|------|-------------------|---------|
| `writeOnboardingCompleted_thenRead_returnsTrue` | setter idempotent → onboardingCompleted.first() == true | writeThenRead_returnsSameId |
| `readOnboardingCompleted_beforeWrite_returnsFalseDefault` | fresh DataStore (no write) → onboardingCompleted.first() == false | read_beforeWrite_returnsDefault |
| `corruptedDataStore_onboardingCompleted_emitsFalseDefault` | mock DataStore.data throws IOException → onboardingCompleted.first() == false (T-06-04) | corruptedDataStore_emitsDefault (T-04-01) |

**Test report (XML):**

```xml
<testsuite name="com.bugzz.filter.camera.data.FilterPrefsRepositoryTest"
           tests="7" skipped="0" failures="0" errors="0" time="1.228">
  ✅ corruptedDataStore_onboardingCompleted_emitsFalseDefault   (1.012s)
  ✅ corruptedDataStore_emitsDefault                            (0.005s)
  ✅ writeThenRead_returnsSameId                                (0.174s)
  ✅ readOnboardingCompleted_beforeWrite_returnsFalseDefault    (0.004s)
  ✅ read_beforeWrite_returnsDefault                            (0.004s)
  ✅ writeOnboardingCompleted_thenRead_returnsTrue              (0.009s)
  ✅ writeAgain_overwritesOldValue                              (0.020s)
```

**4 existing lastUsedFilterId tests untouched + 3 new onboarding tests GREEN = 7/7.**

---

## Suite Status Snapshot

| Snapshot | Total | Skipped | Failures | Errors | Passed |
|----------|-------|---------|----------|--------|--------|
| Phase 5 baseline (pre-06-01) | 143 | varies | 0 | 0 | varies |
| Plan 06-01 baseline (post-Wave-0) | 170 | 27 | 0 | 0 | 143 |
| **Plan 06-02 (current)** | **170** | **24** | **0** | **0** | **146** |
| Δ vs 06-01 | 0 | **-3** | 0 | 0 | **+3** |

**+3 net GREEN** = the 3 FilterPrefsRepositoryTest onboarding cases moved from `@Ignore("Plan 06-02 — ...")` to passing.

---

## D-32 Grep-Assert Re-verification

All 9 D-32 patterns re-grepped on production sources (relaxed for Plan 06-01 deviations: `bindJob?.cancel()` and `cameraMode = com.bugzz.filter.camera.ui.home.CameraMode.InsectFilter`):

| # | Pattern | Files matched | Status |
|---|---------|---------------|--------|
| 1 | `isCapturing` | 4 (CameraViewModel, CameraUiState, InsectFilterViewModel, InsectFilterUiState) | ✅ |
| 2 | `bindJob\?\.cancel\(\)` | 1 (CameraViewModel.kt) | ✅ |
| 3 | `OneShotEvent.FilterLoadError` | 3 (CameraScreen, CameraViewModel, InsectFilterScreen) | ✅ |
| 4 | `captureFlash` | 6 (Camera + Insect screens/VMs/states) | ✅ |
| 5 | `require(frameCount > 0)` | 1 (FilterDefinition.kt) | ✅ |
| 6 | `assetLoader\.preload\(def\.assetDir\)` | 2 (CameraViewModel, StickerRenderer) | ✅ |
| 7 | `isRecording` | 9 (Camera + Insect VMs/screens/states + RecordingIndicator/RecordButton/VideoRecorder) | ✅ |
| 8 | `cameraMode = com.bugzz.filter.camera.ui.home.CameraMode.InsectFilter` (FQN per Plan 01 deviation) | 1 (InsectFilterViewModel.kt) | ✅ |
| 9 | `setPreviewSize` | 2 (StickerRenderer.kt, InsectFilterViewModel.kt) | ✅ |

**ALL 9 PATTERNS RETURN ≥1 MATCH.** D-32 invariants preserved — no regression to Phase 3/4/5 fixes (dafc21e/9abbd0b/6ff00e0/4e94591/b7f74cf/514410c/D-26/gap-01 37b7a17/gap-02 de27c4e).

---

## Deviations from Plan

### 1. [Rule 3 — Dependency conflict] Media3 1.4.1 declared, resolved to 1.9.0 transitively

- **Found during:** Task 1 verification (`./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep media3`).
- **Issue:** Catalog declares `media3 = "1.4.1"` for both `media3-exoplayer` and `media3-ui`. Gradle's classpath conflict resolver upgrades both to **1.9.0** because CameraX 1.6.0 already pulls `androidx.media3:media3-common:1.9.0` (and friends) transitively for its VideoCapture/muxer integration. Mixing 1.4.1 and 1.9.0 transitive deps would crash at runtime due to ABI mismatches on shared `media3-common`.
- **Fix:** Accept Gradle's resolution to 1.9.0 (correct, safe — all media3 modules at the same major.minor.patch). The catalog version `1.4.1` is preserved as the *floor* declaration; resolver handles the harmonization. No change to catalog or build script.
- **Why Rule 3 (auto-fix blocking issue):** Pinning down to 1.4.1 would require excluding the CameraX-transitive 1.9.0 modules — invasive, fragile, and would not actually deliver 1.4.1 because CameraX would still need 1.9.0 internally. The *spirit* of the plan request was "media3-exoplayer + media3-ui on classpath for UX-04 PreviewScreen video playback" — that ship is on classpath at 1.9.0. ExoPlayer 1.9.0 API is a strict superset of 1.4.1 for the surface PreviewScreen will use (`ExoPlayer.Builder`, `Player.STATE_*`, `PlayerView`).
- **Impact on Plan 06-04 (PreviewScreen video):** ExoPlayer 1.9.0 used instead of 1.4.1. Both have identical public API for the basic `setMediaItem(Uri) → prepare() → play() → release()` flow. No code change needed in 06-04 plan.
- **Files modified:** None (resolution is automatic).
- **Commit:** Folded into the single Plan 06-02 commit.

### 2. [Rule 3 — Plan-text drift] Setter signature finalized as `setOnboardingCompleted()` (no Boolean param)

- **Found during:** Task 3 RED→GREEN authoring.
- **Issue:** Plan `<interfaces>` block specifies `suspend fun setOnboardingCompleted()` (no parameter — single-shot mark-complete). The `<behavior>` block sketches `setOnboardingCompleted()` (no arg). The `<action>` block test sketches use `repo.setOnboardingCompleted(true)` AND the IGNORE annotations in 06-01 mention `setOnboardingCompleted(value: Boolean)`. Three forms in the same plan.
- **Fix:** Implemented the **no-arg form** (`suspend fun setOnboardingCompleted()`) that `<interfaces>` and `<action>` body specify and that the OnboardingViewModel.completeOnboarding consumer (Plan 06-03) is documented to call. Rationale: D-23 says "single-shot 'mark complete'" — the boolean is always true at call site, so the parameter is dead weight. Idempotent on repeated calls.
- **Why Rule 3:** Plan-text drift, not a production bug. Picked the form that (a) matches the interfaces block (canonical), (b) matches the doc comment for the consumer in Plan 06-03 (compatibility), (c) is simpler.
- **Test bodies:** Sketches `repo.setOnboardingCompleted(true)` were rewritten to `repo.setOnboardingCompleted()` to match the final API. Behavior unchanged: the setter writes `true` unconditionally.
- **Files modified:** FilterPrefsRepository.kt (no-arg setter), FilterPrefsRepositoryTest.kt (test bodies).
- **Forward note for Plan 06-03:** OnboardingViewModel.completeOnboarding() should call `prefs.setOnboardingCompleted()` (no arg). The 06-01-PLAN @Ignore annotation comment that mentions `setOnboardingCompleted(value: Boolean)` is now superseded by this SUMMARY.

### Auto-fixed Issues

None besides the two deviations above.

### Auth gates

None — pure local build + test work.

---

## Wave 2 Readiness Sign-Off

Wave 2 (Plan 06-03 — Splash + Onboarding) is unblocked:

- ✅ `lottie-compose` on compile classpath → `LottieAnimation` + `rememberLottieComposition(LottieCompositionSpec.Asset(...))` available
- ✅ `app/src/main/assets/lottie/home_lottie.json` present byte-identical → composition spec `"lottie/home_lottie.json"` will resolve at runtime
- ✅ `FilterPrefsRepository.onboardingCompleted: Flow<Boolean>` available → SplashViewModel can `combine` with timer/animation completion
- ✅ `FilterPrefsRepository.setOnboardingCompleted()` available → OnboardingViewModel.completeOnboarding() can persist
- ✅ T-06-04 mitigation present → Splash will not crash on corrupted prefs file
- ✅ media3-exoplayer + media3-ui (1.9.0) on compile classpath → Plan 06-04 PreviewScreen video case ready (one wave ahead)
- ✅ Suite at 170/24 ignored/0 failures — 3 onboarding tests un-ignored. Ignored count 24 = 8 onboarding/splash/preview/collection/share scaffolds × multi-cases (will continue draining as Plans 03-07 land).
- ✅ D-32 invariants intact (9/9 grep-asserts pass).

---

## Self-Check: PASSED

**Created files exist:**
- ✅ `app/src/main/assets/lottie/home_lottie.json` — 746491 bytes, SHA256 5d3cfc5ca6517cd2c8ce2504da73dc0be291edeac0066cd5ab26cdc89bd2fa36

**Modified files have expected diffs:**
- ✅ `gradle/libs.versions.toml` — `lottieCompose = "6.7.1"` + `media3 = "1.4.1"` + 3 library entries appended
- ✅ `app/build.gradle.kts` — 3 implementation lines appended in dependencies block
- ✅ `app/src/main/java/com/bugzz/filter/camera/data/FilterPrefsRepository.kt` — booleanPreferencesKey import + onboardingCompleted Flow + setOnboardingCompleted() + KEY_ONBOARDING_COMPLETED
- ✅ `app/src/test/java/com/bugzz/filter/camera/data/FilterPrefsRepositoryTest.kt` — 3 @Ignore removed, test bodies implemented

**Verification commands all GREEN:**
- ✅ `./gradlew :app:assembleDebug -x lintDebug` (BUILD SUCCESSFUL)
- ✅ `./gradlew :app:testDebugUnitTest --tests "*FilterPrefsRepositoryTest*"` (7/7 GREEN)
- ✅ `./gradlew :app:testDebugUnitTest -x lintDebug` (170/24 ignored/0 failures)

**D-32 grep-asserts:**
- ✅ 9/9 patterns return ≥1 match

**Commit hash:** Recorded post-commit in execution output.
