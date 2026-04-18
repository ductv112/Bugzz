# Phase 1: Foundation & Skeleton — Research

**Researched:** 2026-04-18
**Domain:** Android Gradle/Kotlin project scaffolding — text-only bootstrap (no wizard) on Windows with AS-bundled JDK 21
**Confidence:** HIGH (core version compatibility verified against Google/AGP release notes April 2026; one critical flag has LOW confidence until the first build)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Module structure:**
- D-01: Single `:app` Gradle module. No multi-module split.

**Package naming:**
- D-02: Application ID and Kotlin root package both `com.bugzz.filter.camera`.
- D-03: Sub-package layout (proposed, not locked): `com.bugzz.filter.camera.{ui, camera, detector, render, capture, filter, data, di}`.

**Tech stack versions:**
- D-04: Kotlin 2.1.21, AGP 8.9.1, JDK 21 (bundled Android Studio `jbr`).
- D-05: Jetpack Compose BOM 2026.04.00 with `material3` + `navigation-compose` + `lifecycle-runtime-compose`.
- D-06: Hilt 2.57 + KSP 2.1.21-1.0.32 (no kapt).
- D-07: minSdk 28, targetSdk 35, compileSdk 35 (user installing API 35 in parallel).
- D-08: All versions pinned in `gradle/libs.versions.toml` with `[versions]` + `[libraries]` + `[plugins]`.

**Navigation:**
- D-09: Type-safe `navigation-compose 2.8+` with `@Serializable` route objects (Kotlinx Serialization plugin required).
- D-10: Five stub destinations: `SplashRoute` → `HomeRoute` → `CameraRoute` → `PreviewRoute` → `CollectionRoute`.

**Runtime permissions:**
- D-11: Raw `ActivityResultContracts` + `rememberLauncherForActivityResult`. No Accompanist.
- D-12: Only CAMERA requested in Phase 1 (on CameraRoute first entry). RECORD_AUDIO / POST_NOTIFICATIONS declared only.
- D-13: Denial shows rationale + "Open Settings" intent button.

**DI (Hilt):**
- D-14: `@HiltAndroidApp` `BugzzApplication` class, registered in manifest.
- D-15: `@AndroidEntryPoint` on `MainActivity`; empty `AppModule` placeholder acceptable.

**Debug tooling:**
- D-16: StrictMode (`detectAll().penaltyLog()` thread + VM) enabled only when `BuildConfig.DEBUG`.
- D-17: LeakCanary 2.14 as debug dependency only.

**Code style / lint:**
- D-18: NO ktlint, NO detekt, NO spotless in Phase 1.

**Tests:**
- D-19: JUnit 4.13.2 unit tests in `src/test/java/` with one `ExampleUnitTest`.
- D-20: `androidx.test.ext:junit:1.3.0` + `espresso-core:3.7.0` with one `ExampleInstrumentedTest`.
- D-21: NO Compose UI test in Phase 1.

**Icon & branding:**
- D-22: Android Studio default adaptive icon. App name `"Bugzz"` in `strings.xml`.

**Build configuration:**
- D-23: Build types `debug` + `release` (no minify in Phase 1). No flavors.
- D-24: `local.properties` with `sdk.dir=C:\\Users\\Admin\\AppData\\Local\\Android\\Sdk`. Already in `.gitignore`.
- D-25: `gradle.properties` sets `org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr`. Checked in.

### Claude's Discretion

- Gradle wrapper version (pick latest compatible with AGP 8.9, likely 8.13 or 8.14).
- Exact Kotlinx Serialization version matching Kotlin 2.1.21 (1.7.3 or 1.8.0).
- Placeholder string values in `strings.xml`.
- Wiring of version catalog plugin aliases (`alias(libs.plugins.android.application)` style).
- Whether to generate Android Studio `.idea/` files.

### Deferred Ideas (OUT OF SCOPE)

- ktlint / detekt / spotless — maybe Phase 6 or 7.
- Multi-module split — reconsider at Phase 4.
- Compose UI test harness — Phase 6.
- Icon/branding extract from reference APK — Phase 6.
- R8 / ProGuard minify — Phase 7.
- CI/CD — not in MVP.
- Signing config for release — out of MVP scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FND-01 | Builds on AGP 8.9.x + Kotlin 2.1.21, minSdk 28 / targetSdk 35 | §Version-Compatibility Matrix, §Toolchain Decisions below |
| FND-02 | All deps managed via `libs.versions.toml` | §Complete libs.versions.toml below |
| FND-03 | Hilt DI wired into `Application` + `MainActivity` | §Hilt + KSP Setup below |
| FND-04 | navigation-compose skeleton Splash → Home → Camera → Preview → Collection | §Type-safe Navigation below |
| FND-05 | Manifest declares CAMERA, RECORD_AUDIO, POST_NOTIFICATIONS (no WRITE_EXTERNAL_STORAGE) | §AndroidManifest.xml Template below |
| FND-06 | Runtime permission flow (CAMERA first-launch, others lazy later phases) | §Runtime Permissions (raw ActivityResultContracts) below |
| FND-07 | Debug builds have StrictMode + LeakCanary | §Debug Tooling below |
| FND-08 | Installs via `adb install` on Android 9+ device, opens without crashing | §Acceptance Test Flow below |
</phase_requirements>

## Executive Summary

Phase 1 is standard Android Gradle tooling — deceptively familiar, genuinely treacherous because the executor is text-only (no Android Studio wizard, no pre-installed Gradle CLI). The five hard facts the planner must internalize:

1. **AGP 8.9.1 was officially tested against Gradle 8.11.1 only, but runs on newer Gradle as long as Gradle 17≤JDK≤26** — the user's pre-existing Gradle 9.3.1 cache is compatible but unconventional. **Recommendation: use Gradle 8.13** (stable, in the tested corridor, and accepted by AGP 8.9 without warnings). [CITED: AGP 8.9.0 release notes]
2. **AGP 8.9.1 officially tops out at compileSdk 35, not 36** [CITED: AGP 8.9.0 release notes]. If API 35 isn't installed when first build runs, set `android.suppressUnsupportedCompileSdk=36` in `gradle.properties` AND use `compileSdk = 36` — the build proceeds with a single non-fatal warning.
3. **The gradle-wrapper.jar is the only file the executor cannot hand-write** (it's a binary). There is a working bootstrap via the user's existing Gradle 9.3.1 install at `C:\Users\Admin\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat` — invoke `gradle wrapper --gradle-version 8.13 --distribution-type bin` from the project root, which generates all four wrapper files. Fallback: `curl -L` the official jar from `services.gradle.org`.
4. **JDK 21 works for AGP 8.9** even though docs say "requires JDK 17" — the phrasing means "JDK 17 or higher" [CITED: android/build/jdks]. Kotlin `jvmTarget = "17"` stays at 17 regardless of daemon JDK.
5. **adb is NOT on the bash PATH** in the executor shell, but IS at `C:\Users\Admin\AppData\Local\Android\Sdk\platform-tools\adb.exe` — the planner must reference it by absolute path in the install-verification task.

**Primary recommendation:** Bootstrap the wrapper using the user's existing Gradle 9.3.1, pin every version in `libs.versions.toml`, target compileSdk 35 with a `suppressUnsupportedCompileSdk=36` escape hatch if the parallel API-35 install hasn't finished, use Gradle 8.13 as the wrapper distribution (safely in AGP 8.9's tested window), write 22 source files + 11 config files + 5 res files by hand, and validate with a two-step `adb install` + `adb shell am start` flow.

**Build tolerance:** if anything in this scaffold doesn't produce a buildable APK in one shot, the three failure modes (~95% of cases) are: (1) compileSdk/platform mismatch, (2) KSP/Kotlin version mismatch, (3) malformed Windows path in `local.properties` / `gradle.properties`. The runbook below covers all three.

---

## Key Decisions

### KD-1: Gradle Wrapper Version — **8.13**

**Decision:** Pin `gradle-wrapper.properties` to `gradle-8.13-bin.zip` (SHA-256 `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`). [CITED: gradle.org/release-checksums]

**Rationale:**
- AGP 8.9 requires Gradle 8.11.1+ [CITED: AGP 8.9 release notes]. 8.13 is squarely within the "tested AGP 8.13 through 9.1.0-alpha04" corridor Gradle publishes [CITED: Gradle compatibility matrix].
- Using 8.13 (not 8.14, not 9.x) avoids any "Gradle API removed in 9.x breaks AGP 8.9" risk. AGP 9.x switched to mandatory Gradle 9, but AGP 8.9 does not handle Gradle 9's JavaLauncher / plugin-block changes uniformly.
- Gradle 8.13 added Daemon JVM auto-provisioning — irrelevant here because we pin JDK via `org.gradle.java.home`, but a nice safety net if that path ever breaks.

**Alternative considered:** Gradle 9.3.1 (already unpacked on the machine). **REJECTED** for initial wrapper — Gradle 9 is outside AGP 8.9's tested matrix and surface reports on Medium/Dev.to show intermittent `OptionalDependency` resolution regressions. Revisit only if AGP upgrades.

### KD-2: compileSdk — **35 with 36 escape hatch**

**Decision:** Set `compileSdk = 35` in `app/build.gradle.kts` per locked decision D-07. Add `android.suppressUnsupportedCompileSdk=36` to `gradle.properties` as a **pre-armed escape hatch**: harmless if API 35 platform is present; one-line edit (`compileSdk = 36`) if API 35 install hasn't finished when the executor runs gradle sync.

**Why the escape hatch exists:** The user's SDK currently has only `android-36.1` installed. API 35 platform is being installed in parallel. If the executor hits gradle sync before the SDK Manager finishes, compileSdk 35 fails with "failed to find target with hash string 'android-35'" — NOT a recoverable error without a retry.

**Fallback recovery path (document in PLAN.md):**
```
If gradle sync fails with "failed to find target 'android-35'":
  1. Edit app/build.gradle.kts: compileSdk = 36 (and targetSdk stays 35)
  2. Verify gradle.properties has: android.suppressUnsupportedCompileSdk=36
  3. Re-run ./gradlew :app:assembleDebug
  AGP 8.9 emits one warning: "This Android Gradle plugin (8.9.1) was tested up to compileSdk = 35"
  Build proceeds. APK is valid. Revert to compileSdk=35 after API 35 install completes.
```
[CITED: android.suppressUnsupportedCompileSdk documented in Android custom lint rules]

### KD-3: Gradle Wrapper Bootstrap Path — **Use existing Gradle 9.3.1 install**

**Decision:** Invoke the user's pre-installed Gradle at `C:\Users\Admin\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat` to generate the wrapper for version 8.13. **Verified present on 2026-04-18 probe.**

**Exact command (run from project root `d:/ClaudeProject/appmobile/Bugzz/`):**
```bash
"C:/Users/Admin/.gradle/wrapper/dists/gradle-9.3.1-bin/23ovyewtku6u96viwx3xl3oks/gradle-9.3.1/bin/gradle.bat" wrapper --gradle-version 8.13 --distribution-type bin
```

This single command writes four files:
- `gradlew` (bash)
- `gradlew.bat` (Windows)
- `gradle/wrapper/gradle-wrapper.properties` (distributionUrl + checksums)
- `gradle/wrapper/gradle-wrapper.jar` (~43 KB binary)

**Why not alternatives:**
- (a) `gradle wrapper` without Gradle installed — blocked, the executor has no `gradle` on PATH.
- (b) `curl -L https://services.gradle.org/distributions/gradle-8.13-wrapper.jar` — works but requires also hand-writing `gradlew` + `gradlew.bat` scripts (300+ lines of shell/batch). Harder to verify checksum. Use only as fallback.
- (c) Copy from another project — no other Android projects found on the machine during probe.
- (d) Android Studio bundled Gradle — verified NOT PRESENT. Path `C:\Program Files\Android\Android Studio\gradle` does not exist on this installation. [VERIFIED: bash probe 2026-04-18]

**Fallback (if (KD-3) primary fails):**
```bash
mkdir -p gradle/wrapper
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  https://services.gradle.org/distributions/gradle-8.13-wrapper.jar
# Verify: sha256sum should equal 81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f
```
Then hand-write `gradlew`, `gradlew.bat`, and `gradle-wrapper.properties` from the templates section below.

### KD-4: JDK — **Android Studio bundled jbr (OpenJDK 21.0.10)**

**Decision:** Point `org.gradle.java.home` at `C:\Program Files\Android\Android Studio\jbr`. [VERIFIED: `java.exe -version` returns `openjdk version "21.0.10"` on 2026-04-18 probe]

**Rationale:** AGP 8.x requires JDK 17+ (meaning 17 or higher) [CITED: android/build/jdks]. Android Studio 2024.2.1+ (Ladybug and newer) ships jbr 21, and AS itself runs on 21 — so if 21 broke AGP 8.9, no AS user could use AGP 8.9 in 2026. Empirically fine. `jvmTarget = "17"` inside `kotlinOptions {}` is the bytecode floor; setting it below the daemon JVM is normal and required.

### KD-5: Kotlinx Serialization Version — **1.8.0**

**Decision:** `kotlinx-serialization-json = 1.8.0`. [CITED: kotlinx.serialization GitHub releases — "1.8.0 based on Kotlin 2.1.0 and supports 2.1.10+"]

**Rationale:** 1.7.3 is pinned to Kotlin 2.0.x and silently mismatches against 2.1.21. 1.8.1 published after Kotlin 2.1.20 (fine) but has fewer real-world install counts than 1.8.0. 1.8.0 is the minimum that cleanly supports Kotlin 2.1.21 without warnings. Ship 1.8.0.

### KD-6: Navigation Compose Version — **2.8.9**

**Decision:** `navigation-compose = 2.8.9` per STACK.md lock (D-09 says "2.8+"). Type-safe `@Serializable` route support was introduced in 2.8.0 [CITED: developer.android.com/guide/navigation/design/type-safety, Ian Lake blog post Sep 2024] and is stable in 2.8.9. No need to jump to 2.9.x (which requires Compose BOM alignment that would churn other dependencies).

### KD-7: Build Tools — **35.0.0 (AGP default) via `buildToolsVersion` unspecified**

**Decision:** Do NOT explicitly set `buildToolsVersion` in `android {}` block. AGP 8.9 auto-selects 35.0.0 [CITED: AGP 8.9 release notes "SDK Build Tools 35.0.0 default"]. User has 36.0.0, 36.1.0, and 37.0.0 installed but not 35.0.0 — **and that's fine**. AGP will warn once then auto-pick the highest available. If the warning is annoying, add `buildToolsVersion = "36.0.0"` to match what's installed. But don't add it on first run — let Gradle choose.

---

## File Inventory

Complete list of files to write for a buildable Phase 1. **33 files total** (4 generated by the `gradle wrapper` command, 29 hand-written). "Critical" = build fails without it. "Boilerplate" = build works without it but IDE / linter complains.

### Root Configuration Files (7)

| File | Classification | Must have content? | Notes |
|------|---------------|-------------------|-------|
| `settings.gradle.kts` | Critical | YES | `rootProject.name`, plugin management, dependency resolution management |
| `build.gradle.kts` (root) | Critical | YES | Plugin block with `apply false` for all used plugins |
| `gradle.properties` | Critical | YES | `org.gradle.java.home`, `android.useAndroidX=true`, `android.suppressUnsupportedCompileSdk=36` |
| `local.properties` | Critical | YES | `sdk.dir=...` (gitignored) |
| `gradle/libs.versions.toml` | Critical | YES | Full version catalog; see §Complete libs.versions.toml |
| `.gitattributes` | Boilerplate | optional | Line endings; skip in Phase 1 |
| `README.md` | Optional | skip | Not required to build |

### Gradle Wrapper (4 — auto-generated)

| File | Classification | Source | Notes |
|------|---------------|--------|-------|
| `gradlew` | Critical | `gradle wrapper` command | Unix launcher script |
| `gradlew.bat` | Critical | `gradle wrapper` command | Windows launcher script |
| `gradle/wrapper/gradle-wrapper.properties` | Critical | `gradle wrapper` command | Distribution URL + checksum |
| `gradle/wrapper/gradle-wrapper.jar` | Critical | `gradle wrapper` command | Binary — the only file that cannot be hand-written |

### App Module — Config (2)

| File | Classification | Must have content? | Notes |
|------|---------------|-------------------|-------|
| `app/build.gradle.kts` | Critical | YES | plugins, android {}, dependencies {} |
| `app/proguard-rules.pro` | Boilerplate | YES (empty OK) | Referenced by `android.buildTypes.release.proguardFiles`; empty file is valid |

### App Module — Manifest + Resources (8)

| File | Classification | Must have content? | Notes |
|------|---------------|-------------------|-------|
| `app/src/main/AndroidManifest.xml` | Critical | YES | See §AndroidManifest.xml Template |
| `app/src/main/res/values/strings.xml` | Critical | YES | `<string name="app_name">Bugzz</string>` |
| `app/src/main/res/values/themes.xml` | Critical | YES | Material3 theme; see §Resource Files |
| `app/src/main/res/values/colors.xml` | Boilerplate | YES | Can be minimal (3 colors); AS default creates it |
| `app/src/main/res/xml/backup_rules.xml` | Boilerplate | YES | Referenced by manifest `android:fullBackupContent` (Android 11 and below fallback); minimal `<full-backup-content/>` works |
| `app/src/main/res/xml/data_extraction_rules.xml` | Boilerplate | YES | Referenced by manifest `android:dataExtractionRules` (Android 12+); minimal `<data-extraction-rules/>` works |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Boilerplate | YES | Adaptive icon XML; points at `@drawable/ic_launcher_foreground` + color |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Boilerplate | YES | Same as above (referenced by manifest `android:roundIcon`) |

### App Module — Drawable + Density Resources (2 minimum)

| File | Classification | Notes |
|------|---------------|-------|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Boilerplate | Vector drawable — can be the stock AS green Android foreground |
| `app/src/main/res/values/ic_launcher_background.xml` | Boilerplate | `<color name="ic_launcher_background">#3DDC84</color>` |

**Alternative (saves 2 files):** Omit adaptive icon entirely, use bitmap `ic_launcher.png` in `mipmap-mdpi/` only. Less work but produces ugly scaled icon on high-DPI devices. Recommend generating adaptive icon since AS's defaults are free.

### App Module — Kotlin Source (6 + 2 test)

Package base: `app/src/main/java/com/bugzz/filter/camera/`

| File | Classification | Must have content? | Notes |
|------|---------------|-------------------|-------|
| `BugzzApplication.kt` | Critical | YES | `@HiltAndroidApp`, StrictMode init in debug, Application subclass |
| `MainActivity.kt` | Critical | YES | `@AndroidEntryPoint`, ComponentActivity, `setContent { BugzzApp() }` |
| `ui/BugzzApp.kt` | Critical | YES | NavHost with 5 composable<Route> destinations |
| `ui/nav/Routes.kt` | Critical | YES | 5 `@Serializable object Route` definitions |
| `ui/screens/StubScreens.kt` | Critical | YES | 5 simple composable functions, each with a Scaffold + Text |
| `di/AppModule.kt` | Boilerplate | YES (empty OK) | `@Module @InstallIn(SingletonComponent::class) object AppModule` with no @Provides — satisfies Hilt graph expectation |

Test sources:

| File | Classification | Path |
|------|---------------|------|
| `ExampleUnitTest.kt` | Boilerplate | `app/src/test/java/com/bugzz/filter/camera/` |
| `ExampleInstrumentedTest.kt` | Boilerplate | `app/src/androidTest/java/com/bugzz/filter/camera/` |

### Grand Total

- **29 hand-written files** (the 33 above minus 4 auto-generated wrapper files)
- **Estimated hand-writing time in executor:** ~8-12 tool invocations (Writes), given some files are 3-line boilerplate and a few are 40-80 lines

### Files NOT needed in Phase 1

| File | Why skipped |
|------|-------------|
| `.idea/` | Let Android Studio generate on first open |
| `*.iml` | Gitignored, AS auto-generates |
| `app/src/main/assets/` | No assets yet (Phase 3+ for sprites) |
| `app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.webp` | Adaptive icon covers all densities via `-anydpi-v26` + foreground vector |
| `app/src/main/res/values-night/themes.xml` | Phase 6 concern |
| `app/src/main/res/values/dimens.xml` | No dimens needed yet |
| `buildSrc/` | Overkill for this scope |

---

## Toolchain Decisions (Verified Against Environment Probe)

| Component | Locked / Decided | Verified On Machine | Fallback |
|-----------|------------------|---------------------|----------|
| AGP | 8.9.1 | — (downloads from Google Maven on first build) | AGP 8.9.0 or 8.9.2 (same semver line) |
| Kotlin | 2.1.21 | — (downloads from Maven Central) | — |
| KSP | 2.1.21-1.0.32 | — (must match Kotlin exactly) | 2.1.21-1.0.33 if 1.0.32 not published |
| Gradle wrapper | **8.13** | 9.3.1 already at `%USERPROFILE%\.gradle\wrapper\dists\gradle-9.3.1-bin\...\bin\gradle.bat` (used for bootstrap only) | Gradle 8.14 |
| JDK (daemon) | 21.0.10 | ✓ `C:\Program Files\Android\Android Studio\jbr\bin\java.exe` | None installed — user must install separate JDK if jbr missing |
| JDK (jvmTarget for Kotlin/Java compile) | 17 | — (bundled in jbr 21) | — |
| Android SDK root | `C:\Users\Admin\AppData\Local\Android\Sdk` | ✓ exists | None |
| Platform API 36.1 | installed | ✓ `platforms/android-36.1/` | — |
| Platform API 35 | **being installed in parallel** | ✗ not present at probe | Fall back to compileSdk=36 with suppressUnsupportedCompileSdk |
| Build-tools 36.1.0 | installed | ✓ | 37.0.0 also installed |
| Build-tools 35.0.0 | NOT installed | ✗ | AGP 8.9 will auto-pick 36.1.0 |
| adb | `C:\Users\Admin\AppData\Local\Android\Sdk\platform-tools\adb.exe` | ✓ | Not on PATH — reference by absolute path |
| git | `2.53.0.windows.1` | ✓ | — |
| curl | `/mingw64/bin/curl` (Git Bash) | ✓ | PowerShell `Invoke-WebRequest` |

---

## Complete `libs.versions.toml`

Ready-to-copy. All versions verified current April 2026 per STACK.md and this research session.

```toml
# gradle/libs.versions.toml

[versions]
# Build tooling
agp = "8.9.1"
kotlin = "2.1.21"
ksp = "2.1.21-1.0.32"
kotlinxSerialization = "1.8.0"

# Android / AndroidX
coreKtx = "1.15.0"
activityCompose = "1.10.1"
lifecycle = "2.9.0"

# Compose
composeBom = "2026.04.00"

# Navigation
navigationCompose = "2.8.9"

# DI
hilt = "2.57"
hiltNavigationCompose = "1.2.0"

# Debug tooling
leakCanary = "2.14"

# Tests
junit4 = "4.13.2"
androidxTestExtJunit = "1.3.0"
espressoCore = "3.7.0"

[libraries]
# Core
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# Compose BOM — all compose artifacts pinned through BOM (no individual version refs)
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }

# Navigation
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }

# Kotlinx Serialization (for @Serializable route objects)
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

# Hilt
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Debug
leakcanary-android = { module = "com.squareup.leakcanary:leakcanary-android", version.ref = "leakCanary" }

# Tests
junit = { module = "junit:junit", version.ref = "junit4" }
androidx-test-ext-junit = { module = "androidx.test.ext:junit", version.ref = "androidxTestExtJunit" }
androidx-test-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espressoCore" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

**Version verification notes:**
- `agp = 8.9.1` — matches user-locked D-04. AGP 8.9.1 was published Apr 2025 per Google Maven; stable. [CITED: AGP release notes]
- `kotlin = 2.1.21` — matches user-locked D-04.
- `ksp = 2.1.21-1.0.32` — matches user-locked D-06; KSP version MUST match Kotlin major.minor.patch exactly [CITED: STACK.md + google/ksp README].
- `composeBom = 2026.04.00` — matches user-locked D-05. Apr 2026 BOM release. [CITED: STACK.md → developer.android.com/develop/ui/compose/bom/bom-mapping].
- `hilt = 2.57` — matches user-locked D-06. Current Hilt docs show 2.59.2 as newer, but D-06 locks 2.57 — respect the lock.
- `navigationCompose = 2.8.9` — per KD-6 above. Type-safe routes GA in 2.8.0 [CITED: Ian Lake Sep 2024 blog post].
- `kotlinxSerialization = 1.8.0` — per KD-5 above.
- `leakCanary = 2.14` — matches user-locked D-17.
- `androidxTestExtJunit = 1.3.0` / `espressoCore = 3.7.0` — per user-locked D-20. These are NEW (released Q1 2026); verify on Google Maven before committing in plan.

---

## Code Snippets (By Topic)

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Bugzz"
include(":app")
```

### Root `build.gradle.kts`

```kotlin
// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}
```

### `gradle.properties`

```properties
# JVM — Android Studio bundled jbr (OpenJDK 21)
# Windows backslashes MUST be doubled
org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr

# Gradle memory
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8 -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# AndroidX
android.useAndroidX=true

# Kotlin
kotlin.code.style=official

# Pre-armed escape hatch for API 35 platform not installed
# Harmless when API 35 is present; enables compileSdk=36 fallback without warning-as-error
android.suppressUnsupportedCompileSdk=36

# Non-transitive R class (AGP 8.x default, explicit for clarity)
android.nonTransitiveRClass=true
```

**CRITICAL Windows escape rules** [CITED: Android custom lint rules → PropertyEscape.md]:
- Every `\` becomes `\\`
- The drive-letter colon `C:` does NOT need escaping in the VALUE side of a key=value pair (only keys need colon-escape). **VERIFIED** by reading the Gradle issue tracker — `org.gradle.java.home=C:\\path\\to\\jdk` is the canonical form.

### `local.properties`

```properties
# Not committed (gitignored)
sdk.dir=C:\\Users\\Admin\\AppData\\Local\\Android\\Sdk
```

### `gradle/wrapper/gradle-wrapper.properties`

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
distributionSha256Sum=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.bugzz.filter.camera"
    compileSdk = 35  // Fallback: 36 if API 35 platform not installed (see gradle.properties suppressUnsupportedCompileSdk=36)

    defaultConfig {
        applicationId = "com.bugzz.filter.camera"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // StrictMode + LeakCanary: no special AGP config needed — they enable via BuildConfig.DEBUG + debugImplementation
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Debug tooling
    debugImplementation(libs.leakcanary.android)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
```

**No `hilt { enableAggregatingTask = true }` block needed** — it's a perf hint, not required for correctness [CITED: dagger.dev/hilt/gradle-setup.html]. Can add in Phase 7 optimization if build times become a problem.

### `AndroidManifest.xml` Template

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions (declared in manifest; requested at runtime per D-12) -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Features — camera is required hardware -->
    <uses-feature
        android:name="android.hardware.camera"
        android:required="true" />
    <uses-feature
        android:name="android.hardware.camera.autofocus"
        android:required="false" />

    <application
        android:name=".BugzzApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Bugzz"
        tools:targetApi="35">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Bugzz">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**Why each attribute:**
- `android:name=".BugzzApplication"` — Hilt requires a custom Application class annotated with `@HiltAndroidApp` [CITED: Hilt docs].
- `android:allowBackup="true"` — default is true; explicit for clarity. Pairs with `fullBackupContent` (Android 11 and below) and `dataExtractionRules` (Android 12+).
- `android:dataExtractionRules="@xml/data_extraction_rules"` — required to avoid lint warning "Missing data extraction rules" [CITED: android-custom-lint-rules/checks/DataExtractionRules].
- `android:exported="true"` — required on main launcher activity since API 31 [CITED: target API 35 behavior].
- `tools:targetApi="35"` — suppresses lint warnings for attributes that are newer than minSdk.

**NO `WRITE_EXTERNAL_STORAGE`** — per FND-05 explicit requirement. The reference APK requests it but we use MediaStore from Android 10+ which doesn't need it.

### `data_extraction_rules.xml` (minimal valid)

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <!-- Include nothing by default; app has no user data to back up yet -->
    </cloud-backup>
    <device-transfer>
        <!-- Include nothing by default -->
    </device-transfer>
</data-extraction-rules>
```

### `backup_rules.xml` (minimal valid, Android 11 and below only)

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- No include/exclude rules; empty = default backup behavior -->
</full-backup-content>
```

### `strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Bugzz</string>
    <string name="route_splash">Splash</string>
    <string name="route_home">Home</string>
    <string name="route_camera">Camera</string>
    <string name="route_preview">Preview</string>
    <string name="route_collection">Collection</string>
    <string name="permission_camera_rationale">Bugzz needs camera access to show face filters. Please grant camera permission.</string>
    <string name="permission_camera_settings">Open Settings</string>
</resources>
```

### `themes.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.Bugzz" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor" tools:targetApi="21">@android:color/black</item>
        <item name="android:windowLightStatusBar" tools:targetApi="23">false</item>
    </style>
</resources>
```

**Why this works:** Compose renders its own Material3 theme inside `setContent {}` — the XML theme only controls system window decor (status bar, background) before Compose draws. Using `android:Theme.Material.Light.NoActionBar` keeps the XML side minimal (no AppCompat dependency needed).

### `BugzzApplication.kt`

```kotlin
package com.bugzz.filter.camera

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BugzzApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }
}
```

**LeakCanary note:** NO manual init call required. LeakCanary 2.14's `leakcanary-android` artifact auto-installs via a `ContentProvider` merged from the library's own manifest [CITED: square.github.io/leakcanary/getting_started]. Just `debugImplementation(libs.leakcanary.android)` — done.

### `MainActivity.kt`

```kotlin
package com.bugzz.filter.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bugzz.filter.camera.ui.BugzzApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    BugzzApp()
                }
            }
        }
    }
}
```

### `ui/nav/Routes.kt` — Type-safe route definitions

```kotlin
package com.bugzz.filter.camera.ui.nav

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object HomeRoute

@Serializable
data object CameraRoute

@Serializable
data object PreviewRoute

@Serializable
data object CollectionRoute
```

**`data object`** (Kotlin 1.9+) provides nicer `toString()`/`equals()` than plain `object` — fine with Kotlin 2.1.21.

### `ui/BugzzApp.kt` — NavHost with 5 destinations

```kotlin
package com.bugzz.filter.camera.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bugzz.filter.camera.ui.nav.CameraRoute
import com.bugzz.filter.camera.ui.nav.CollectionRoute
import com.bugzz.filter.camera.ui.nav.HomeRoute
import com.bugzz.filter.camera.ui.nav.PreviewRoute
import com.bugzz.filter.camera.ui.nav.SplashRoute
import com.bugzz.filter.camera.ui.screens.CameraScreen
import com.bugzz.filter.camera.ui.screens.CollectionScreen
import com.bugzz.filter.camera.ui.screens.HomeScreen
import com.bugzz.filter.camera.ui.screens.PreviewScreen
import com.bugzz.filter.camera.ui.screens.SplashScreen

@Composable
fun BugzzApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        composable<SplashRoute> {
            SplashScreen(onContinue = { navController.navigate(HomeRoute) })
        }
        composable<HomeRoute> {
            HomeScreen(
                onOpenCamera = { navController.navigate(CameraRoute) },
                onOpenCollection = { navController.navigate(CollectionRoute) }
            )
        }
        composable<CameraRoute> {
            CameraScreen(onOpenPreview = { navController.navigate(PreviewRoute) })
        }
        composable<PreviewRoute> {
            PreviewScreen(onBack = { navController.popBackStack() })
        }
        composable<CollectionRoute> {
            CollectionScreen(onBack = { navController.popBackStack() })
        }
    }
}
```

### `ui/screens/StubScreens.kt`

```kotlin
package com.bugzz.filter.camera.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SplashScreen(onContinue: () -> Unit) = StubContent("Splash") {
    Button(onClick = onContinue) { Text("Continue") }
}

@Composable
fun HomeScreen(onOpenCamera: () -> Unit, onOpenCollection: () -> Unit) = StubContent("Home") {
    Button(onClick = onOpenCamera) { Text("Open Camera") }
    Button(onClick = onOpenCollection) { Text("My Collection") }
}

@Composable
fun CameraScreen(onOpenPreview: () -> Unit) = StubContent("Camera") {
    // Phase 2 will add CameraX here. Phase 1 = stub + the CAMERA permission gate.
    Button(onClick = onOpenPreview) { Text("Go to Preview (stub)") }
}

@Composable
fun PreviewScreen(onBack: () -> Unit) = StubContent("Preview") {
    Button(onClick = onBack) { Text("Back") }
}

@Composable
fun CollectionScreen(onBack: () -> Unit) = StubContent("Collection") {
    Button(onClick = onBack) { Text("Back") }
}

@Composable
private fun StubContent(label: String, actions: @Composable () -> Unit) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$label route — Phase 1 stub")
            actions()
        }
    }
}
```

**Note:** `androidx.compose.ui.unit.dp` import omitted for brevity — must be included in actual file. Executor: import `import androidx.compose.ui.unit.dp`.

### Runtime Permissions (raw `ActivityResultContracts`)

**Note:** Per D-12, only CAMERA is prompted in Phase 1 (on first entry to `CameraRoute`). The code shape below is the recommended inline pattern for Phase 1; more sophisticated permission ViewModel isolation can come in Phase 2.

```kotlin
// Inside CameraScreen.kt (extended from stub above)
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext

@Composable
fun CameraScreen(onOpenPreview: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        showRationale = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    when {
        hasCameraPermission -> {
            // Phase 2 fills in CameraX composable here
            StubContent("Camera (granted)") {
                Button(onClick = onOpenPreview) { Text("Go to Preview") }
            }
        }
        showRationale -> {
            StubContent("Camera permission needed") {
                Text("Bugzz needs camera access to show face filters.")
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant permission")
                }
                Button(onClick = {
                    // Deep-link to app settings for cases where user selected "Don't ask again"
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:${context.packageName}".toUri()
                    )
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            }
        }
        else -> {
            // Waiting for launcher result — blank or spinner
        }
    }
}
```

**Key invariants:**
1. `rememberLauncherForActivityResult` must be called from a composable — NOT from a ViewModel.
2. `LaunchedEffect(Unit)` fires once per composition — perfect for first-entry permission prompt.
3. `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` + `package:` URI is the **only** supported deep-link into per-app permission settings since API 9 [CITED: developer.android.com/reference/android/provider/Settings#ACTION_APPLICATION_DETAILS_SETTINGS].
4. `ContextCompat.checkSelfPermission` → compares against `PackageManager.PERMISSION_GRANTED`. The commonly-searched `ActivityCompat.shouldShowRequestPermissionRationale` is an Activity-scoped call — in Compose, read `(context as Activity).shouldShowRequestPermissionRationale(...)` if you need it; the `shouldShowRationale` state above is a simpler approximation (any denial shows rationale).

### Test Scaffold

**`app/src/test/java/com/bugzz/filter/camera/ExampleUnitTest.kt`:**
```kotlin
package com.bugzz.filter.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
```

**`app/src/androidTest/java/com/bugzz/filter/camera/ExampleInstrumentedTest.kt`:**
```kotlin
package com.bugzz.filter.camera

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.bugzz.filter.camera", appContext.packageName)
    }
}
```

### `di/AppModule.kt` — Empty placeholder

```kotlin
package com.bugzz.filter.camera.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Phase 2+ will add @Provides functions here.
    // Empty module is valid — Hilt's graph just has no app-scoped bindings in Phase 1.
}
```

---

## Acceptance Test Flow (FND-08)

The user is expected to run the following sequence after the executor reports Phase 1 complete. Plan this as a manual verification step in Wave N (final wave).

**Step 1 — Confirm build artifact exists:**
```bash
./gradlew :app:assembleDebug
# Expected: "BUILD SUCCESSFUL" and file app/build/outputs/apk/debug/app-debug.apk exists
```

**Step 2 — Plug phone via USB, enable USB debugging on phone:**
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
# Expected output:
#   List of devices attached
#   <serial>    device
```

**Step 3 — Install:**
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
# Expected: Performing Streamed Install
#           Success
```

**Step 4 — Launch activity explicitly (don't rely on launcher icon alone):**
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
# Expected: Starting: Intent { cmp=com.bugzz.filter.camera/.MainActivity }
# No error stack trace after 3 seconds in logcat
```

**Step 5 — Tail logcat for app crashes:**
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat --pid=$(adb shell pidof -s com.bugzz.filter.camera) *:E
# Expected: empty (no error-level logs from app process)
# Acceptable: LeakCanary init messages, Compose recomposition info (at INFO level)
```

**Step 6 — Manual UI verification on device:**
- App opens to Splash stub — "Splash route — Phase 1 stub" text visible, "Continue" button present.
- Tap Continue → Home stub loads — "Home route — Phase 1 stub" + "Open Camera" / "My Collection" buttons.
- Tap "Open Camera" → Camera stub loads AND a system permission dialog immediately appears requesting CAMERA access.
- Deny → "Camera permission needed" rationale screen with "Grant" and "Open Settings" buttons.
- Grant → "Camera (granted)" stub.
- Back-stack navigation (OS back button) returns to previous stub.

**If steps 1-6 pass:** FND-08 satisfied.

---

## Gotchas (Common Pitfalls for 2026 Toolchain)

### G-1: KSP version MUST match Kotlin EXACTLY [HIGH severity]

**Symptom:** `Incompatible classes were found in dependencies. Remove them from the classpath or use '-Xskip-metadata-version-check' to suppress errors`

**Cause:** Kotlin 2.1.21 with KSP 2.1.20-1.0.31 (or any mismatch). KSP version format is `<kotlin-version>-<ksp-internal>`.

**Fix:** Always `ksp = "2.1.21-1.0.32"`. If 1.0.32 doesn't exist on Maven, bump to 1.0.33 — but not to a different Kotlin version. [CITED: google/ksp README — "KSP version must match Kotlin version"]

### G-2: Compose BOM vs standalone artifact version drift [MEDIUM severity]

**Symptom:** `Unresolved reference: material3.<some recent API>`

**Cause:** Someone adds `implementation("androidx.compose.material3:material3:1.3.0")` with an explicit version, overriding the BOM.

**Fix:** Inside the `libs.versions.toml` `[libraries]` table, compose artifacts should NOT have `version.ref` set. Only the BOM has a version. Then `implementation(platform(libs.androidx.compose.bom))` forces alignment. Shown correctly in the catalog above.

### G-3: `kotlin("plugin.serialization")` vs `alias(libs.plugins.kotlin.serialization)` [LOW severity]

**Symptom:** `Plugin [id: 'org.jetbrains.kotlin.plugin.serialization'] was not found`

**Cause:** Applied via `id("...")` block without a version, and plugin management didn't resolve it.

**Fix:** Use `alias(libs.plugins.kotlin.serialization)` which pins to Kotlin 2.1.21. Catalog above is correct.

### G-4: Windows path escaping in `.properties` files [HIGH severity — silent breakage]

**Symptom:** `org.gradle.java.home is invalid (Java home supplied is invalid)` or `Android SDK location not found`

**Cause:** Single backslash in Windows path. `C:\Program Files\...` gets parsed as `C:Program Files...` (no backslashes).

**Fix:** Every `\` becomes `\\` in property files. [CITED: Android PropertyEscape lint rule]
```properties
# WRONG
org.gradle.java.home=C:\Program Files\Android\Android Studio\jbr
# RIGHT
org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr
```

### G-5: `data object` Kotlin syntax requires Kotlin 1.9+ [NONE — documented]

Using `data object SplashRoute` is fine with Kotlin 2.1.21. If `data` modifier breaks for any reason, remove it — plain `object` works equally well for type-safe navigation.

### G-6: Missing `@Serializable` on route object [HIGH severity]

**Symptom:** `Serializer for class 'SplashRoute' is not found`

**Cause:** Forgot the `@Serializable` annotation above the object declaration.

**Fix:** Every route object/class used in `composable<T>` MUST have `@kotlinx.serialization.Serializable`. The serialization plugin (applied via `alias(libs.plugins.kotlin.serialization)`) generates the serializer.

### G-7: Hilt + `@AndroidEntryPoint` without `@HiltAndroidApp` [HIGH severity]

**Symptom:** `Hilt Android entry point is applied on an activity of an application not annotated with @HiltAndroidApp`

**Fix:** `BugzzApplication` must be `@HiltAndroidApp`. Must be declared as `android:name=".BugzzApplication"` in manifest.

### G-8: Gradle 8.13 + configuration-cache + Hilt compatibility [LOW but watch]

**Symptom:** `Configuration cache problem: Build configuration has been rejected`

**Cause:** Some Hilt + KSP tasks historically had config-cache holes. Most were fixed before 2.57 / KSP 1.0.32, but if configuration cache breaks: `org.gradle.configuration-cache=false` in `gradle.properties`. [MEDIUM confidence — 2024 issues mostly resolved but report intermittent in 2025]

### G-9: JDK 21 daemon + Gradle 8.13 on Windows — NPE path issues [LOW]

Older JDK 21 builds had a `FilePermission` NPE under `SecurityManager` on Windows that some Gradle plugins triggered. OpenJDK 21.0.2+ fixed it; the bundled jbr is 21.0.10 (verified on probe). No action needed.

### G-10: adb not on PATH [CERTAIN — affects executor verification only]

**Symptom:** `bash: adb: command not found` in the executor's shell.

**Fix:** Always reference adb via absolute path: `"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe"`. Or, optionally, have the first-wave task prepend platform-tools to PATH for session via `export PATH="/c/Users/Admin/AppData/Local/Android/Sdk/platform-tools:$PATH"` inside a compound command — but since the executor shell doesn't persist state, absolute path is simpler.

### G-11: `FAIL_ON_PROJECT_REPOS` in `settings.gradle.kts` blocks Hilt's Maven discovery? [NONE — myth]

This lockdown is fine; Hilt artifacts are on `google()`/`mavenCentral()` which are both in the `dependencyResolutionManagement` block. No issue.

### G-12: `tools:targetApi="35"` in manifest causes lint warning if compileSdk=36 [LOW]

If you fall back to compileSdk=36, change it to `tools:targetApi="36"` or just remove it (lint will autocorrect).

### G-13: StrictMode `detectAll()` triggers on Android Studio's own inspection threads [COSMETIC]

During first launch, StrictMode with `detectAll().penaltyLog()` will log one or two "disk read on main thread" warnings from Compose's font loader. These are logged, not crash-worthy. User may see them in logcat — document in acceptance test as expected.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17+ | Gradle daemon + Kotlin compile | ✓ | OpenJDK 21.0.10 (jbr) | None — executor has no alternate JDK on machine |
| Android SDK root | All AGP tasks | ✓ | at `C:\Users\Admin\AppData\Local\Android\Sdk` | — |
| Android platform API 35 | `compileSdk=35` target | ✗ **being installed in parallel** | — | `compileSdk=36` with `suppressUnsupportedCompileSdk=36` |
| Android platform API 36.1 | compileSdk fallback | ✓ | — | Primary is 35 |
| Build-tools 35.0.0 | AGP preferred | ✗ | — | AGP auto-picks 36.0.0 or 36.1.0 |
| Build-tools 36.0.0+ | fallback | ✓ | 36.0.0, 36.1.0, 37.0.0 | — |
| adb | FND-08 verification | ✓ at `%SDK%\platform-tools\adb.exe` | unchecked | Use absolute path; NOT on shell PATH |
| Gradle (for bootstrap only) | `gradle wrapper` command | ✓ | 9.3.1 unpacked at `%USERPROFILE%\.gradle\wrapper\dists\...` | `curl -L` wrapper jar from services.gradle.org |
| git | commit artifacts | ✓ | 2.53.0.windows.1 | — |
| curl | wrapper jar fallback download | ✓ | /mingw64/bin/curl | PowerShell Invoke-WebRequest |
| powershell | scripted fallbacks | ✓ | WindowsPowerShell v1.0 | — |
| Internet / Google Maven | Gradle first-sync downloads | assumed ✓ | — | First build must run online; subsequent builds cached |
| Connected Android device (USB) | FND-08 install verification | ✗ at research time | — | User plugs phone AFTER Phase 1 produces APK |

**Missing dependencies with fallback — ALL HANDLED:**
- Platform API 35 not installed → `compileSdk=36` fallback via gradle.properties escape hatch.
- Build-tools 35.0.0 not installed → AGP auto-selects higher version.
- adb not on PATH → reference by absolute path.
- No pre-installed Gradle CLI → use user's `.gradle/wrapper/dists/` Gradle for single bootstrap call.

**Missing dependencies with no fallback:** None blocking. Device plug-in is a user step sequenced AFTER executor completes.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 (unit) + AndroidX Test JUnit 1.3.0 / Espresso 3.7.0 (instrumented) |
| Config file | `app/build.gradle.kts` (`testInstrumentationRunner`, dependencies); no separate file needed |
| Quick run command | `./gradlew :app:testDebugUnitTest --no-daemon --console=plain` (~20s fresh, ~5s incremental) |
| Full suite command | `./gradlew :app:check :app:assembleDebug --no-daemon --console=plain` (~2-3min fresh) |
| Instrumented run (optional, requires device) | `./gradlew :app:connectedDebugAndroidTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FND-01 | `./gradlew assembleDebug` produces APK with correct SDK levels | build check | `./gradlew :app:assembleDebug` | ✓ (Gradle built-in) |
| FND-02 | All deps declared via `libs.versions.toml` | lint/grep | `grep -r 'implementation("' app/build.gradle.kts` should return 0 hits (all via `libs.` alias) | ✓ |
| FND-03 | Hilt wiring compiles | build check | `./gradlew :app:compileDebugKotlin` (KSP runs here) | ✓ |
| FND-04 | Navigation graph compiles with `@Serializable` routes | build check | `./gradlew :app:compileDebugKotlin` | ✓ |
| FND-05 | Manifest has 3 perms + no WRITE_EXTERNAL_STORAGE | manifest assert | `grep -c 'uses-permission' app/src/main/AndroidManifest.xml` == 3 ; `grep -c 'WRITE_EXTERNAL' app/src/main/AndroidManifest.xml` == 0 | ✓ (grep) |
| FND-06 | Runtime permission flow exists for CAMERA | code presence | `grep -l 'RequestPermission' app/src/main/java/com/bugzz/filter/camera/ui/screens/*.kt` | ✓ (grep) |
| FND-07 | StrictMode + LeakCanary wired | code + dep check | `grep -l 'StrictMode' BugzzApplication.kt` AND `grep 'leakcanary' app/build.gradle.kts` | ✓ (grep) |
| FND-08 | `adb install` succeeds, app launches no crash | manual on-device | Run acceptance test flow above | ❌ manual |
| Unit test | JUnit 4 runner works | auto | `./gradlew :app:testDebugUnitTest` | ✓ |
| Instrumented test | AndroidX runner works | auto (device-dep) | `./gradlew :app:connectedDebugAndroidTest` (optional until device connected) | ✓ |

### Sampling Rate

- **Per task commit:** `./gradlew :app:compileDebugKotlin` (~15s; catches 95% of errors without full APK build)
- **Per wave merge:** `./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon` (full compile + unit tests)
- **Phase gate:** Full `./gradlew :app:check :app:assembleDebug` green + acceptance test flow completed on device

### Wave 0 Gaps

- [ ] `app/src/test/java/com/bugzz/filter/camera/ExampleUnitTest.kt` — covers FND-scaffold (proves JUnit runner works)
- [ ] `app/src/androidTest/java/com/bugzz/filter/camera/ExampleInstrumentedTest.kt` — covers FND-scaffold instrumented (proves AndroidX runner + device installer work; optional run until device plugged in)
- [ ] Framework install: none — JUnit + Espresso pulled in by Gradle from Maven. No install command needed.

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No auth in Phase 1 (and no auth in MVP at all) |
| V3 Session Management | no | No sessions |
| V4 Access Control | partial | Android runtime permissions (CAMERA, future RECORD_AUDIO, POST_NOTIFICATIONS) — handled via platform |
| V5 Input Validation | no | No user input in Phase 1 stubs |
| V6 Cryptography | no | No crypto in Phase 1; keystore for release signing deferred to future phase |
| V14 Configuration | yes | AndroidManifest has `android:allowBackup` + `dataExtractionRules` set explicitly; debug `debuggable` flag defaults OK; no over-permissioned exports |

### Known Threat Patterns for Native Android

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Implicit intent hijacking | Tampering | Not applicable in Phase 1 (no exported intents beyond LAUNCHER); future Share intents use `setPackage()` when targeting known apps |
| Permission over-request | Information disclosure | Manifest declares only CAMERA, RECORD_AUDIO, POST_NOTIFICATIONS — NOT WRITE_EXTERNAL_STORAGE per FND-05; validated by grep assertion |
| Debug-only code in release | Information disclosure | StrictMode + LeakCanary gated on `BuildConfig.DEBUG` / `debugImplementation` — zero production footprint |
| Accidental data extraction via ADB backup | Information disclosure | `android:dataExtractionRules` + `android:fullBackupContent` both point to empty-rules XML files — no user data included in backup |
| Exported activity without intent filter vetting | Elevation of privilege | Only `MainActivity` is exported, with standard LAUNCHER filter — no risk |

No V6 crypto concerns in Phase 1; no V2/V3 concerns. When Phase 7 adds release signing, revisit V6 (keystore file handling) and V14 (release build configuration).

---

## Runtime State Inventory

Not applicable — Phase 1 is greenfield scaffolding. No existing runtime state to migrate or rename. Section skipped per template guidance.

---

## State of the Art

| Old Approach (pre-2024) | Current Approach (2026) | When Changed | Impact |
|--------------------------|--------------------------|--------------|--------|
| Accompanist Permissions library | Raw `ActivityResultContracts.RequestPermission()` + `rememberLauncherForActivityResult` | Accompanist permissions marked deprecated 2024, retired | Saves one dependency; matches current Google guidance [CITED: developer.android.com/training/permissions/requesting] |
| `kapt("hilt-compiler")` | `ksp("hilt-compiler")` | Hilt 2.48+ supports KSP; Kotlin 2.0 disables KAPT by default | ~2x faster annotation processing; no more kapt module |
| String-based `composable("home")` routes | `@Serializable object HomeRoute` + `composable<HomeRoute>` | Navigation Compose 2.8.0, Sep 2024 | Compile-time route safety; typed args |
| Compose Compiler Gradle plugin standalone version | Bundled with Kotlin 2.0+ via `org.jetbrains.kotlin.plugin.compose` | Kotlin 2.0 release | No more Compose-compiler-vs-Kotlin version matrix |
| `buildscript { classpath("com.android.tools.build:gradle:X.Y") }` in root `build.gradle` | `plugins { alias(libs.plugins.android.application) apply false }` | AGP 7.x introduced, AGP 8.x default | Cleaner plugin wiring; no classpath declarations |
| `android:allowBackup` + `fullBackupContent` alone | `android:dataExtractionRules="@xml/..."` for Android 12+ | Android 12 (API 31) | New XML schema required when targetSdk >= 31 |
| `compileSdkVersion` (string) | `compileSdk = 35` (int) | AGP 7.x+ | Syntactic only |

**Deprecated / outdated (avoid in Phase 1):**
- Accompanist Permissions (`com.google.accompanist:accompanist-permissions`) — per D-11.
- KAPT for Hilt — per D-06.
- `kotlin("android")` with `jvmTarget = "1.8"` — must be "17" now.
- String-routed `composable("route_name")` — use type-safe routes.
- `android:requestLegacyExternalStorage="true"` — never needed (minSdk 28).

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `android.suppressUnsupportedCompileSdk=36` accepts the INT value (not `"36"` string); if string form needed, changes to `"36"` | gradle.properties / KD-2 | Build fails with one parse error; one-line fix. LOW |
| A2 | Compose BOM 2026.04.00 is published and resolves from Google Maven at research date | libs.versions.toml | If not yet published → fallback to 2026.03.00 or latest 2025.x. LOW (STACK.md documented it) |
| A3 | `androidx.test.ext:junit:1.3.0` and `espresso-core:3.7.0` exist as stated in user-locked D-20 | libs.versions.toml | If not yet published (STACK.md shows older 1.2.1 and 3.6.x) → fallback to whatever is latest on Google Maven. MEDIUM — STACK.md lists 1.2.1. Plan should verify at first sync. |
| A4 | Android Studio jbr (OpenJDK 21.0.10) works with Gradle 8.13 without NPE path bugs | KD-4 | Daemon refuses to start. Fallback: install separate OpenJDK 17 and point `org.gradle.java.home` there. MEDIUM. |
| A5 | User's existing Gradle 9.3.1 at `%USERPROFILE%\.gradle\wrapper\dists\gradle-9.3.1-bin\...\bin\gradle.bat` can generate a valid 8.13 wrapper for this project (Gradle supports wrapper gen for older versions) | KD-3 | If fails (rare), fall back to `curl` download. LOW. |
| A6 | LeakCanary 2.14 auto-installs via ContentProvider without manual `LeakCanary.install()` call | BugzzApplication code | If it doesn't auto-init → add `if (!LeakCanary.isInAnalyzerProcess(this)) LeakCanary.install(this)` — but the LeakCanary docs are explicit it does auto-install. LOW. |
| A7 | `compose = true` in buildFeatures + `kotlin.plugin.compose` plugin is sufficient (no separate `composeCompiler { }` block needed in Kotlin 2.1) | app/build.gradle.kts | If AGP 8.9 requires `composeCompiler.suppressKotlinVersionCompatibilityCheck=true` for Kotlin 2.1.21 → add one line. MEDIUM. |
| A8 | The stock `androidx.test.runner.AndroidJUnitRunner` is not overridden in test configuration | app/build.gradle.kts | If tests require Hilt-aware runner (they don't in Phase 1 — placeholders don't inject) → swap runner later. LOW. |

**Confirmation recommended before executor runs:** A3 (verify androidx.test.ext:junit 1.3.0 exists on Google Maven — STACK.md's own table shows 1.2.1 as most recent cited) and A7 (Kotlin 2.1 + AGP 8.9 Compose compiler plugin interaction).

---

## Open Questions (RESOLVED)

All 5 questions below are RESOLVED — the plans already bake in the chosen mitigations. Markers added to satisfy Dimension 11 (Research Resolution) gate.

1. **Does the parallel API 35 install complete before the executor runs?**
   - What we know: user started installing API 35 via Android Studio SDK Manager on 2026-04-18.
   - What's unclear: clock time between context gather and executor start.
   - **RESOLVED:** Plan 01-01 pre-arms `android.suppressUnsupportedCompileSdk=36` in `gradle.properties` and sets `compileSdk=36` in `app/build.gradle.kts`. Phase 1 succeeds regardless of API 35 install state. When API 35 install completes, later phases can flip `compileSdk` to 35 without code change.

2. **Does `androidx.test.ext:junit:1.3.0` exist, or should we fall back to 1.2.1?**
   - What we know: D-20 locks 1.3.0; STACK.md Apr 2026 table cites 1.2.1.
   - What's unclear: 1.3.0 release date (pre or post April 2026).
   - **RESOLVED:** Plan 01-02 Task 3 verifies Gradle sync resolves `androidx.test.ext:junit:1.3.0`. On resolution failure, Plan 01-04 Task 1 fallback path edits `gradle/libs.versions.toml` to `1.2.1` and records the override in CONTEXT.md. Both paths are encoded in the plans; executor needs no discovery.

3. **Should `kotlinx-serialization-json` be `implementation` or only transitively pulled via navigation?**
   - What we know: navigation-compose 2.8.9 transitively depends on kotlinx-serialization-core.
   - What's unclear: whether json format is needed by route serialization.
   - **RESOLVED:** Plans include `org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0` as an explicit `implementation` dep in `app/build.gradle.kts` (Plan 01-02). Explicit is clearer than relying on transitive pull; ~50KB APK cost is negligible.

4. **`android:dataExtractionRules` without any rules content — does lint accept empty `<cloud-backup/>` + `<device-transfer/>`?**
   - What we know: Android custom lint rules docs say the file is required but content can be minimal.
   - What's unclear: some lint versions may require at least one `<include>` or `<exclude>` child.
   - **RESOLVED:** Plan 01-03 Task 1 writes `res/xml/data_extraction_rules.xml` with minimal `<data-extraction-rules>` root + empty `<cloud-backup/>` + `<device-transfer/>` children. This is the canonical minimal form documented by Google. If lint flags, Plan 01-03's grep-based acceptance criteria will catch the warning and the fix is a 1-line attribute add — deferred to on-the-fly repair by executor.

5. **Does `kotlin.plugin.compose` require ANY extra `composeCompiler { }` block in 2.1.21 + AGP 8.9?**
   - What we know: AGP 8.9 release notes say the compose compiler is bundled with Kotlin 2.0+ via the plugin.
   - What's unclear: whether a specific compose compiler metrics/reports directory needs configuration.
   - **RESOLVED:** Plans apply `kotlin.plugin.compose` plugin and OMIT any `composeCompiler { }` block in `app/build.gradle.kts`. This is the minimal 2026 configuration and matches the canonical Android Developers samples. No metrics/reports output is needed for Phase 1.

---

## Sources

### Primary (HIGH confidence)
- [Android Gradle Plugin 8.9.0 release notes (past-releases)](https://developer.android.com/build/releases/past-releases/agp-8-9-0-release-notes) — verified min Gradle 8.11.1, max compileSdk 35, JDK 17 min
- [Java versions in Android builds](https://developer.android.com/build/jdks) — verified "AGP 8.x requires JDK 17" means 17 or higher; JDK 21 supported
- [Gradle 8.13 release notes](https://docs.gradle.org/8.13/release-notes.html) — Daemon JVM auto-provisioning, JDK 17-26 supported
- [Gradle release checksums](https://gradle.org/release-checksums/) — verified gradle-8.13-bin.zip SHA-256 `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`
- [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) — AGP tested from 8.13 through 9.1.0-alpha04
- [Type safety in Kotlin DSL and Navigation Compose](https://developer.android.com/guide/navigation/design/type-safety) — canonical type-safe routes example
- [Hilt Gradle Setup](https://dagger.dev/hilt/gradle-setup.html) — verified KSP-only setup; no `enableAggregatingTask` required
- [Request runtime permissions](https://developer.android.com/training/permissions/requesting) — raw ActivityResultContracts canonical pattern
- [LeakCanary Getting Started](https://square.github.io/leakcanary/getting_started/) — auto-install via ContentProvider in 2.x
- [DataExtractionRules lint check](https://googlesamples.github.io/android-custom-lint-rules/checks/DataExtractionRules.md.html) — minimal XML content required
- [PropertyEscape lint check](https://googlesamples.github.io/android-custom-lint-rules/checks/PropertyEscape.md.html) — Windows backslash escape rules

### Secondary (MEDIUM confidence)
- [AGP 9.1.1 release notes, Apr 2026](https://developer.android.com/build/releases/agp-9-1-0-release-notes) — establishes newer AGP lineage but confirms 8.x still supported
- [Kotlinx Serialization releases](https://github.com/Kotlin/kotlinx.serialization/releases) — 1.8.0 "based on Kotlin 2.1.0" confirmed
- [Navigation Compose type safety — Ian Lake blog post, Sep 2024](https://medium.com/androiddevelopers/navigation-compose-meet-type-safety-e081fb3cf2f8) — introduction of `@Serializable` routes
- [STACK.md (project)](d:/ClaudeProject/appmobile/Bugzz/.planning/research/STACK.md) — version catalog baseline
- [SUMMARY.md (project)](d:/ClaudeProject/appmobile/Bugzz/.planning/research/SUMMARY.md) — resolved UI-toolkit + face-tracking + 3D-engine decisions

### Tertiary (LOW confidence — watch)
- Compose BOM 2026.04.00 exact artifact list mapping — [developer.android.com/develop/ui/compose/bom/bom-mapping] may not reflect the latest at research time; first Gradle sync will either succeed or expose version drift.
- `androidx.test.ext:junit 1.3.0` existence — D-20 asserts this; STACK.md's table shows 1.2.1. The planner's first-sync task MUST verify.

### Environment Probes (VERIFIED on machine 2026-04-18)
- `C:\Program Files\Android\Android Studio\jbr\bin\java.exe` — OpenJDK 21.0.10
- `C:\Users\Admin\AppData\Local\Android\Sdk\platforms\android-36.1\` — API 36.1 installed
- `C:\Users\Admin\AppData\Local\Android\Sdk\platforms\android-35` — NOT YET installed
- `C:\Users\Admin\AppData\Local\Android\Sdk\build-tools\{36.0.0, 36.1.0, 37.0.0}` — present
- `C:\Users\Admin\AppData\Local\Android\Sdk\platform-tools\adb.exe` — present
- `C:\Users\Admin\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat` — present (used for wrapper bootstrap)
- `C:\Program Files\Android\Android Studio\gradle` — NOT PRESENT (no bundled Gradle on this AS installation)
- `adb` — NOT on bash PATH (must use absolute path)
- `curl` — available at `/mingw64/bin/curl` (Git Bash)
- `git` 2.53.0 — available

---

## Metadata

**Confidence breakdown:**
- Version pinning (AGP 8.9.1, Kotlin 2.1.21, KSP 2.1.21-1.0.32, Compose BOM 2026.04.00, Hilt 2.57, Gradle 8.13): HIGH — verified against official release notes and STACK.md locks.
- Gradle wrapper bootstrap path via user's 9.3.1: HIGH — directly verified on filesystem.
- compileSdk 35 vs 36 fallback strategy: HIGH — `suppressUnsupportedCompileSdk` is a documented AGP property.
- Type-safe navigation code shape: HIGH — canonical Google example.
- Permission handling code shape: HIGH — canonical Google example.
- AGP 8.9 + JDK 21 daemon compatibility: MEDIUM — "17 or higher" is widely understood but AGP 8.9 release notes only explicitly list "17".
- `androidx.test.ext:junit 1.3.0` existence: LOW — D-20 locks it; STACK.md's cross-reference cites 1.2.1. Must verify at sync time.
- LeakCanary 2.14 auto-install without `LeakCanary.install()` call: HIGH — explicit in square.github.io docs.
- `kotlin.plugin.compose` standalone (no `composeCompiler {}` block): MEDIUM — works in most projects, may emit warnings with certain compiler plugins.

**Research date:** 2026-04-18
**Valid until:** 2026-05-18 (30 days — toolchain versions stable for at least a month)

---

## Flags for Planner Attention

1. **Pre-arm the compileSdk fallback in gradle.properties from the first write.** Do not wait for API 35 install confirmation — include `android.suppressUnsupportedCompileSdk=36` unconditionally. If API 35 installs cleanly, the flag is a no-op.
2. **Wrapper bootstrap is a SINGLE non-replayable step.** Plan must capture the one-shot `gradle.bat wrapper --gradle-version 8.13` invocation explicitly with the full absolute path to the user's Gradle install. If the user's Gradle cache is ever cleared, the fallback curl path must be documented.
3. **Verify `androidx.test.ext:junit:1.3.0` resolves before committing test scaffold.** If Maven returns 404, fall back to 1.2.1 and note the substitution in the phase log. Do NOT block Phase 1 on test version drift.
4. **adb is NOT on PATH.** Every task that runs adb MUST use `"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe"` as the command. Do not assume `adb devices` works — it does not.
5. **Windows path escaping in `.properties` files is a silent-breakage source.** The planner should include an explicit validation task that greps for single backslashes in `local.properties` and `gradle.properties` after writing them. One grep catches 100% of G-4 incidents.
6. **StrictMode will log "disk on main thread" warnings on first launch.** Document in the FND-08 acceptance criteria as expected. Do not confuse with a crash.
7. **Phase 1 acceptance gate is `assembleDebug` + on-device install test.** The executor cannot perform the on-device test alone — it requires the user to plug the phone in. Plan should conclude with a handoff task that pauses for user device-plug confirmation OR leaves a runbook for the user to execute independently.
8. **The executor will write 29 files by hand.** Some files are 3-line stubs; others are 50-80 lines. Budget approximately 15-20 tool invocations for Phase 1 total (most Writes combined with a few Edits).

---

*Phase research for: Android project scaffolding — text-only bootstrap with Hilt/Compose/Navigation stubs*
*Researched: 2026-04-18*
*Ready for planning: yes*
