---
phase: 02-camera-preview-face-detection-coordinate-validation
plan: 02
subsystem: build-infrastructure
tags: [camerax-1.6.0, mlkit-face-detection-16.1.7, timber-5.0.1, hilt-singleton, version-catalog, portrait-lock, compilesdk-36]

# Dependency graph
requires:
  - phase: 01-foundation-skeleton
    provides: Gradle 8.13 + AGP 8.9.1 + KSP 2.1.21-2.0.2 + Hilt 2.57 + Kotlin 2.1.21 baseline; empty AppModule placeholder; BugzzApplication with StrictMode in debug guard; MainActivity with exported=true LAUNCHER
  - plan: 02-01
    provides: Four Nyquist RED tests (OneEuroFilterTest, FaceDetectorOptionsTest, OverlayEffectBuilderTest, CameraControllerTest) expecting mockito-kotlin + CameraX + ML Kit on classpath — this plan closes that dependency contract so tests COMPILE-fail only on Unresolved SUT references (not deps)
provides:
  - Version catalog pinning CameraX 1.6.0 (8 artifacts) + ML Kit face-detection 16.1.7 + Timber 5.0.1 + Mockito 5.11.0 + mockito-kotlin 5.2.1
  - app/build.gradle.kts pulling all 11 new runtime deps as `implementation(...)` and 2 new test deps as `testImplementation(...)`
  - compileSdk bumped 35 -> 36 (required by CameraX 1.6.0 AAR metadata)
  - AndroidManifest portrait-lock (`android:screenOrientation="portrait"` on MainActivity + defensive `configChanges` for Xiaomi MIUI quirk)
  - Timber.DebugTree planted ONLY under `BuildConfig.DEBUG` guard (T-02-02 biometric-data-logging gate)
  - Hilt `CameraModule` providing `@Named("cameraExecutor")` + `@Named("renderExecutor")` as `@Singleton Executor` factories with named threads (D-18)
  - Phase 02-01 Nyquist RED state preserved: only SUT references (CameraController/OneEuroFilter/FaceDetectorClient/OverlayEffectBuilder) remain as `Unresolved reference`; all test-framework imports resolve
affects: [plan-02-03, plan-02-04, plan-02-05, plan-02-06, phase-03]

# Tech tracking
tech-stack:
  added:
    - "androidx.camera:camera-core:1.6.0"
    - "androidx.camera:camera-camera2:1.6.0"
    - "androidx.camera:camera-lifecycle:1.6.0"
    - "androidx.camera:camera-video:1.6.0"
    - "androidx.camera:camera-view:1.6.0"
    - "androidx.camera:camera-effects:1.6.0"
    - "androidx.camera:camera-mlkit-vision:1.6.0"
    - "androidx.camera:camera-compose:1.6.0"
    - "com.google.mlkit:face-detection:16.1.7"
    - "com.jakewharton.timber:timber:5.0.1"
    - "org.mockito:mockito-core:5.11.0 (testImplementation)"
    - "org.mockito.kotlin:mockito-kotlin:5.2.1 (testImplementation)"
  patterns:
    - "Named-singleton Executor provisioning: @Provides @Singleton @Named(\"xxxExecutor\") fun ... = Executors.newSingleThreadExecutor { r -> Thread(r, \"BugzzXxxExecutor\") }. Named threads surface in logcat/profiler. Single-thread for ordering without explicit locks."
    - "BuildConfig.DEBUG guard around biometric-relevant logging init (Timber.DebugTree). Release builds plant no tree, so downstream FaceTracker logging calls become no-ops — cheap but complete T-02-02 mitigation."
    - "configChanges defensive-attribute on portrait-locked activity for OEM-specific reverse-portrait recreation quirks (Xiaomi MIUI / PITFALLS #7)."

key-files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/di/CameraModule.kt
    - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-02-SUMMARY.md
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/bugzz/filter/camera/BugzzApplication.kt

key-decisions:
  - "Compose BOM 2026.04.00 prescribed by plan is NOT published on Google Maven as of 2026-04-19 — reverted to Phase 1's working 2026.03.00. No Phase 2 code depends on 2026.04.00-specific Compose APIs; revisit when BOM lands. Logged as Rule 1 auto-fix."
  - "compileSdk bumped 35 -> 36 to satisfy CameraX 1.6.0 AAR metadata requirement (artifacts demand compileSdk>=36). targetSdk stays 35 (CLAUDE.md Constraints locked to match reference app). The pre-armed android.suppressUnsupportedCompileSdk=36 gate from Phase 1 silences warning-as-error. Logged as Rule 3 auto-fix."
  - "CameraModule as Kotlin `object` (not class) — Hilt @Provides annotations on top-level functions inside a Kotlin object compile to static JVM members, matching Hilt best practice for stateless factories and enabling zero-allocation module instantiation."
  - "BugzzCameraExecutor / BugzzRenderExecutor thread names — surface clearly in logcat and Android Studio profiler for Xiaomi 13T debugging. Avoided the generic 'pool-N-thread-N' default."
  - "Timber.plant(DebugTree) placed INSIDE the existing `if (BuildConfig.DEBUG) { ... enableStrictMode() }` block rather than creating a new block — preserves Phase 1's single guard site and documents both StrictMode + Timber as debug-only together."

patterns-established:
  - "**Version-catalog-first dependency bumps:** any new runtime library requires 1) version pin under `[versions]`, 2) library alias under `[libraries]` referencing that version, 3) `implementation(libs.xxx)` in app/build.gradle.kts. Never hand-edit strings in build.gradle.kts. Enforced by Task 1 acceptance criteria."
  - "**Plan-deviation when upstream version not published:** if a prescribed version (Compose BOM, CameraX, etc.) is not yet on the target repository as of execution date, revert to the last known-good version AND log the deviation in SUMMARY.md + note follow-up when the prescribed version lands. Avoids 'plan says X, but X doesn't exist' stuck state."
  - "**compileSdk bumps as auto-fix when artifacts demand:** CameraX 1.6.0 (and future AndroidX library majors) require the consumer project's compileSdk>=N via AAR metadata check. Bumping compileSdk is Rule 3 (blocking) auto-fix, not an architectural decision — pre-arm `android.suppressUnsupportedCompileSdk=N` in gradle.properties to silence the cross-version gate."

requirements-completed: [CAM-01, CAM-02, CAM-03, CAM-04, CAM-05, CAM-06, CAM-07, CAM-08, CAM-09]
# Note: "completed" here means "infrastructure dependency available for the requirement on
# app classpath." The requirements themselves graduate to runtime-verified only after
# Plans 02-03 (detector) + 02-04 (camera/render) + 02-06 (device handoff).

# Metrics
duration: 10m 13s
completed: 2026-04-19
---

# Phase 2 Plan 2: CameraX + ML Kit + Timber + Mockito Dependency Wiring Summary

**Pinned CameraX 1.6.0 family + ML Kit 16.1.7 + Timber 5.0.1 + Mockito 5.11.0 / mockito-kotlin 5.2.1 into the version catalog + app/build.gradle.kts; portrait-locked MainActivity with defensive configChanges; planted Timber.DebugTree inside BuildConfig.DEBUG guard; created Hilt CameraModule providing two named single-thread Executors. Found and auto-fixed two deviations: Compose BOM 2026.04.00 not yet published (reverted to 2026.03.00) and CameraX 1.6.0 requires compileSdk>=36 (bumped 35->36).**

## Performance

- **Duration:** 10 min 13 s (wall clock; includes two gradle resolution probes + three assembleDebug runs)
- **Started:** 2026-04-19T08:40:31Z
- **Completed:** 2026-04-19T08:50:44Z
- **Tasks:** 3 primary (per 02-02-PLAN.md) + 1 auto-fix commit
- **Files modified:** 4 (catalog + gradle + manifest + Application) + 1 created (CameraModule)

## Accomplishments

- **Version catalog (gradle/libs.versions.toml):** Added `camerax = "1.6.0"`, `mlkitFace = "16.1.7"`, `timber = "5.0.1"`, `mockito = "5.11.0"`, `mockitoKotlin = "5.2.1"` to `[versions]`. Added 8 `androidx-camera-*` library aliases (core, camera2, lifecycle, video, view, effects, mlkit-vision, compose) + `mlkit-face-detection` + `timber` + `mockito-core` + `mockito-kotlin` to `[libraries]`. (Compose BOM intended bump to 2026.04.00 was auto-reverted — see Deviations below.)
- **app/build.gradle.kts:** Pulled all 8 CameraX + ML Kit + Timber as `implementation(libs.xxx)` (11 new runtime deps total); pulled Mockito + mockito-kotlin as `testImplementation(libs.xxx)` (2 new test deps). compileSdk bumped 35 -> 36 (Rule 3 auto-fix; see Deviations).
- **AndroidManifest.xml:** MainActivity node now declares `android:screenOrientation="portrait"` (D-07) and `android:configChanges="orientation|screenSize|keyboardHidden"` (D-08 defensive for Xiaomi MIUI reverse-portrait quirk per PITFALLS #7). Phase 1 `<uses-permission>` and `<uses-feature>` blocks preserved verbatim.
- **BugzzApplication.kt:** `Timber.plant(Timber.DebugTree())` added inside the existing `if (BuildConfig.DEBUG) { ... }` block, BEFORE the Phase 1 `enableStrictMode()` call. Release builds plant no tree — T-02-02 biometric-data-logging gate enforced before any detector code lands.
- **CameraModule.kt (new):** `@Module @InstallIn(SingletonComponent::class) object CameraModule` exposing two named single-thread Executor `@Singleton` factories — `@Named("cameraExecutor")` (thread name `BugzzCameraExecutor`) for MlKitAnalyzer/ImageCapture/VideoRecordEvent callbacks, and `@Named("renderExecutor")` (thread name `BugzzRenderExecutor`) for OverlayEffect.onDrawListener (D-18 threading model).
- **Nyquist RED preservation:** After adding mockito-kotlin to testImplementation, `compileDebugUnitTestKotlin` now fails ONLY with `Unresolved reference` errors pointing at the SUT classes (CameraController, OneEuroFilter, FaceDetectorClient, OverlayEffectBuilder, CameraLens, createAnalyzer, build) — exactly the state 02-01-SUMMARY.md §Known Stubs predicted for end-of-Plan-02-02. All Mockito/CameraX/ML Kit imports resolve.
- **Gradle sync + assembleDebug green:** `./gradlew :app:assembleDebug` produces `app-debug.apk` (81.9 MB — ML Kit bundled model + CameraX libs) with no new warnings beyond Phase 1's pre-existing Hilt-generated deprecated-API note.

## Task Commits

Each task (and the Rule 1+3 auto-fix) was committed atomically:

1. **Task 1 (part 1): version catalog + build.gradle.kts deps** — `6410ba5` (chore)
2. **Task 1 auto-fix (Rule 1 + Rule 3):** Compose BOM revert 2026.04.00 -> 2026.03.00 + compileSdk 35 -> 36 — `9005926` (fix)
3. **Task 2: portrait-lock manifest + Timber.plant in debug** — `11ad2d6` (chore)
4. **Task 3: Hilt CameraModule with two named Executors** — `8b0e4c5` (feat)

(Plan-level docs commit for this SUMMARY.md + STATE.md + ROADMAP.md comes in the final-commit step below.)

## Quoted Diffs

### gradle/libs.versions.toml — `[versions]` block (after)

```toml
# Compose — Phase 2 intended bump to 2026.04.00, but that BOM is not yet published
# on Google Maven as of 2026-04-19 execution time. Keep 2026.03.00 (Phase 1 working value);
# revisit when 2026.04.00 ships. Deviation logged in 02-02-SUMMARY.md (Rule 1 auto-fix).
composeBom = "2026.03.00"

# CameraX family (uniform 1.6.0 — Phase 2)
camerax = "1.6.0"

# ML Kit Face Detection (bundled model)
mlkitFace = "16.1.7"

# Logging
timber = "5.0.1"

# Tests
junit4 = "4.13.2"
androidxTestExtJunit = "1.3.0"
espressoCore = "3.7.0"
mockito = "5.11.0"
mockitoKotlin = "5.2.1"
```

### AndroidManifest.xml — MainActivity node diff

**Before (Phase 1):**
```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:theme="@style/Theme.Bugzz">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

**After (Phase 2 Plan 2):**
```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:screenOrientation="portrait"
    android:configChanges="orientation|screenSize|keyboardHidden"
    android:theme="@style/Theme.Bugzz">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### BugzzApplication.kt — onCreate diff (key delta)

**Added imports:**
```kotlin
import timber.log.Timber
```

**onCreate() — Timber.plant placed inside the existing `BuildConfig.DEBUG` guard (T-02-02 gate):**
```kotlin
override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())   // T-02-02 gate: biometric/face-log data only in debug
        enableStrictMode()
    }
}
```

Release builds now plant no Timber tree, so downstream `Timber.tag("FaceTracker").v(...)` calls in FaceDetectorClient (Plan 02-03) become no-ops in release APKs. Enforced by grep-context check — `Timber.plant` line is sandwiched between `if (BuildConfig.DEBUG) {` (L13) and `}` (L16).

### CameraModule.kt — full content

```kotlin
package com.bugzz.filter.camera.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Named
import javax.inject.Singleton

/**
 * Camera / detector / render DI module (Phase 2).
 *
 * Provides the two single-thread executors declared in D-18:
 *   - cameraExecutor — MlKitAnalyzer analyze callback, ImageCapture callback, VideoRecordEvent listener
 *   - renderExecutor — OverlayEffect.onDrawListener (wrapped by OverlayEffectBuilder's HandlerThread)
 *
 * CameraController, FaceDetectorClient, OverlayEffectBuilder, DebugOverlayRenderer each declare
 * their own @Singleton with constructor @Inject — no explicit @Provides needed for them.
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    @Named("cameraExecutor")
    fun provideCameraExecutor(): Executor =
        Executors.newSingleThreadExecutor { r -> Thread(r, "BugzzCameraExecutor") }

    @Provides
    @Singleton
    @Named("renderExecutor")
    fun provideRenderExecutor(): Executor =
        Executors.newSingleThreadExecutor { r -> Thread(r, "BugzzRenderExecutor") }
}
```

### app/build.gradle.kts — compileSdk + new deps (key deltas)

```kotlin
android {
    namespace = "com.bugzz.filter.camera"
    compileSdk = 36  // Bumped 35 -> 36 in Phase 02-02: CameraX 1.6.0 requires compileSdk >= 36.
                     // targetSdk stays 35 (locked by CLAUDE.md to match reference app 1:1).
                     // android.suppressUnsupportedCompileSdk=36 in gradle.properties is pre-armed from Phase 1.
    ...
}

dependencies {
    ...
    // CameraX 1.6.0 (Phase 2) — uniform family
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.effects)
    implementation(libs.androidx.camera.mlkit.vision)
    implementation(libs.androidx.camera.compose)

    // ML Kit Face Detection (bundled ~3-4MB model; no Play Services download race)
    implementation(libs.mlkit.face.detection)

    // Logging (debug tree planted in BugzzApplication when BuildConfig.DEBUG)
    implementation(libs.timber)

    // Test — Mockito for CameraControllerTest (ProcessCameraProvider mocking)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Compose BOM 2026.04.00 not yet published on Google Maven**

- **Found during:** Task 1 verification (`./gradlew :app:assembleDebug` after committing deps)
- **Issue:** Plan's `must_haves.truths` + Task 1 `<action>` prescribed bumping `composeBom = "2026.03.00"` → `composeBom = "2026.04.00"` (per CLAUDE.md Executive Recommendation + 02-RESEARCH.md §Standard Stack). Gradle resolution failed with `Could not find androidx.compose:compose-bom:2026.04.00`. Verified via `:app:dependencyInsight --configuration debugRuntimeClasspath --dependency compose-bom` — BOM 2026.04.00 does not exist at either Google Maven (`https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.04.00/compose-bom-2026.04.00.pom`) or Maven Central as of 2026-04-19.
- **Root cause:** The research corpus anticipated an April 2026 BOM that has not yet shipped. The assertion was optimistic rather than misreported.
- **Fix:** Reverted `composeBom` to `"2026.03.00"` (Phase 1's working value). Added an inline TOML comment documenting the revert + revisit trigger. No Phase 2 code depends on 2026.04.00-specific Compose APIs — `CameraXViewfinder` composable ships in `androidx.camera:camera-compose:1.6.0` (separate artifact, not BOM-pinned), so Phase 2's Compose surface is unchanged.
- **Files modified:** `gradle/libs.versions.toml`
- **Commit:** `9005926`
- **Follow-up:** Before Phase 3 planning, recheck `https://developer.android.com/develop/ui/compose/bom/bom-mapping`; bump to 2026.04.00 (or whatever is current) in a standalone `chore(deps)` commit. Not blocking.

**2. [Rule 3 - Blocking] CameraX 1.6.0 requires compileSdk >= 36**

- **Found during:** Task 1 verification (`./gradlew :app:assembleDebug` after reverting BOM)
- **Issue:** Build failed with `Unable to strip the following libraries ... Dependency 'androidx.camera:camera-compose:1.6.0' requires libraries and applications that depend on it to compile against version 36 or later of the Android APIs.` Current project: `compileSdk = 35`. CameraX 1.6.0 (and all dependent camera-effects / camera-compose / camera-mlkit-vision / camera-viewfinder artifacts) ship AAR metadata demanding `compileSdk>=36`.
- **Root cause:** CameraX 1.6 (released Mar 2026) targets API 36; plan did not anticipate this.
- **Fix:** Bumped `app/build.gradle.kts` `compileSdk = 35` → `compileSdk = 36`. Kept `targetSdk = 35` (locked by `./CLAUDE.md` Constraints — must match reference app 1:1). Phase 1 pre-armed `android.suppressUnsupportedCompileSdk=36` in `gradle.properties`, so the cross-version warning-as-error is silenced automatically. Android SDK 36.1 is installed at `C:/Users/Admin/AppData/Local/Android/Sdk/platforms/android-36.1` — resolution clean.
- **Files modified:** `app/build.gradle.kts`
- **Commit:** `9005926` (combined with BOM revert)
- **Verification:** Post-bump `./gradlew :app:assembleDebug` produces `app-debug.apk` with `BUILD SUCCESSFUL`; no new lint/build warnings beyond Phase 1's pre-existing Hilt-generated deprecated-API note.

**Total deviations:** 2 auto-fixed (Rule 1 + Rule 3), no architectural changes needed. No Rule 4 escalations.

## Authentication Gates

None — plan is pure-build/DI infrastructure, no network calls, no external service setup.

## Issues Encountered

- **Pre-existing unstaged `.planning/config.json` modification:** At session start, `.planning/config.json` had orchestrator-level modifications (from `init execute-phase`). Left untouched during per-task commits (staged only task-specific files); will be absorbed by the final `docs(02-02)` metadata commit.
- **Windows CRLF warnings:** Git warned `LF will be replaced by CRLF the next time Git touches it` on every committed file. Benign on Windows hosts; matches repo convention from Phase 1. No `.gitattributes` override needed.
- **Mockito test compile still RED (expected):** `./gradlew :app:compileDebugUnitTestKotlin` fails with `Unresolved reference` errors targeting `CameraController`, `OneEuroFilter`, `FaceDetectorClient`, `OverlayEffectBuilder`, `CameraLens`. These are SUT references — Plans 02-03 + 02-04 close the gap. Plan 02-02's job was only to unblock the Mockito DSL imports, which it did (no `Unresolved reference 'whenever'` / `'mock'` remain).

## User Setup Required

None.

## Known Stubs

None in the Phase 2 runtime surface. The Nyquist RED state from Plan 02-01 is intentionally preserved — Plans 02-03 (FaceDetectorClient + OneEuroFilter) and 02-04 (CameraController + OverlayEffectBuilder) will add the SUT classes that turn the 4 test files GREEN. This plan is pure infrastructure wiring.

## Next Plan Readiness

- Plan 02-03 (detector layer) can proceed: `com.google.mlkit:face-detection:16.1.7` + `androidx.camera:camera-mlkit-vision:1.6.0` + Timber on classpath; `FaceDetectorOptionsTest` + `OneEuroFilterTest` waiting to flip GREEN.
- Plan 02-04 (camera + render layer) can proceed: full CameraX 1.6.0 family + `@Named` Executors injectable via Hilt; `OverlayEffectBuilderTest` + `CameraControllerTest` waiting to flip GREEN (with the provider-factory seam Plan 02-01-SUMMARY.md §Known Stubs called out).
- Plans 02-05 (UI) and 02-06 (device handoff) unblocked transitively.

## Self-Check: PASSED

- [x] `gradle/libs.versions.toml` contains `camerax = "1.6.0"`, `mlkitFace = "16.1.7"`, `timber = "5.0.1"`, `mockito = "5.11.0"`, `mockitoKotlin = "5.2.1"`
- [x] `gradle/libs.versions.toml` contains all 8 `androidx-camera-*` library aliases + `mlkit-face-detection` + `timber` + `mockito-core` + `mockito-kotlin`
- [x] `app/build.gradle.kts` contains 8 `implementation(libs.androidx.camera.*)` lines + `implementation(libs.mlkit.face.detection)` + `implementation(libs.timber)` + 2 `testImplementation(libs.mockito.*)` lines
- [x] `app/build.gradle.kts` `compileSdk = 36`
- [x] `app/src/main/AndroidManifest.xml` MainActivity has `android:screenOrientation="portrait"` (line 33)
- [x] `app/src/main/AndroidManifest.xml` MainActivity has `android:configChanges="orientation|screenSize|keyboardHidden"` (line 34)
- [x] `app/src/main/java/com/bugzz/filter/camera/BugzzApplication.kt` contains `import timber.log.Timber`
- [x] `Timber.plant(Timber.DebugTree())` is inside `if (BuildConfig.DEBUG) { ... }` block (L13-16)
- [x] `enableStrictMode()` call preserved (Phase 1 behavior)
- [x] `app/src/main/java/com/bugzz/filter/camera/di/CameraModule.kt` exists
- [x] `CameraModule.kt` contains `@Module`, `@InstallIn(SingletonComponent::class)`, `object CameraModule`, 2× `@Provides`, 2× `@Singleton`, 2× `@Named`, thread names `BugzzCameraExecutor` + `BugzzRenderExecutor`
- [x] `./gradlew :app:assembleDebug` succeeds (`BUILD SUCCESSFUL`, produces `app-debug.apk` 81.9 MB)
- [x] `./gradlew :app:compileDebugUnitTestKotlin` surfaces ONLY `Unresolved reference` errors for SUT classes (CameraController/OneEuroFilter/FaceDetectorClient/OverlayEffectBuilder); all Mockito/CameraX/ML Kit imports resolve
- [x] Commit `6410ba5` (chore(02-02-01) deps) present in `git log`
- [x] Commit `9005926` (fix(02-02-01) BOM+compileSdk) present in `git log`
- [x] Commit `11ad2d6` (chore(02-02-02) manifest+Timber) present in `git log`
- [x] Commit `8b0e4c5` (feat(02-02-03) CameraModule) present in `git log`

---
*Phase: 02-camera-preview-face-detection-coordinate-validation*
*Completed: 2026-04-19*
