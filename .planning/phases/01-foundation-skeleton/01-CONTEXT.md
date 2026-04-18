# Phase 1: Foundation & Skeleton - Context

**Gathered:** 2026-04-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver a buildable, runnable Android skeleton on a real Android 9+ device with all scaffolding in place so subsequent phases focus exclusively on camera + render + capture work. Scope is limited to: Gradle tooling + dependency catalog, Hilt DI, Compose navigation shell with 5 stub screens, AndroidManifest + permission contracts, StrictMode + LeakCanary in debug, unit/instrumented test scaffold. Out of scope: any camera code, any ML Kit code, any rendering, production UI beyond stub placeholders, icon/branding assets.

</domain>

<decisions>
## Implementation Decisions

### Module Structure
- **D-01:** Single `:app` Gradle module. No multi-module split in Phase 1. Rationale: solo dev, greenfield, pre-mature modularization hurts velocity; can extract modules later if Phase 4+ reveals natural boundaries.

### Package Naming
- **D-02:** Application ID: `com.bugzz.filter.camera`. Java/Kotlin root package identical: `com.bugzz.filter.camera`. This differs from the reference app's `com.insect.filters.funny.prank.bug.filter.face.camera` — avoids package-name conflict and matches the Bugzz branding.
- **D-03:** Sub-package layout (proposed by Phase 2+ planner, not locked here): `com.bugzz.filter.camera.{ui, camera, detector, render, capture, filter, data, di}`.

### Tech Stack Versions (from research SUMMARY.md, locked)
- **D-04:** Kotlin **2.1.21**, AGP **8.9.1**, JDK **21** (bundled Android Studio `jbr`), Gradle wrapper compatible with AGP 8.9.
- **D-05:** Jetpack Compose BOM **2026.04.00** with `material3` + `navigation-compose` + `lifecycle-runtime-compose`.
- **D-06:** Hilt **2.57** + KSP **2.1.21-1.0.32** (no kapt).
- **D-07:** minSdk **28**, targetSdk **35**, compileSdk **35** (user installing API 35 via Android Studio SDK Manager in parallel).
- **D-08:** All versions pinned in `gradle/libs.versions.toml` (version catalog, TOML format with `[versions]` + `[libraries]` + `[plugins]` sections).

### Navigation
- **D-09:** **Type-safe** navigation via `navigation-compose 2.8+` with `@Serializable` route objects (Kotlinx Serialization plugin required). Compile-time route validation over string routes.
- **D-10:** Five navigation destinations in Phase 1 (stubs only, empty Scaffolds with route name Text label):
  - `SplashRoute` → `HomeRoute` → `CameraRoute` → `PreviewRoute` → `CollectionRoute`.
  - No back-stack customization yet; default behavior.

### Runtime Permissions
- **D-11:** Use **raw `ActivityResultContracts` + `rememberLauncherForActivityResult`**. No Accompanist Permissions (deprecated by Google; migration guidance is to raw contracts).
- **D-12:** **Only CAMERA permission** requested in Phase 1 (on CameraRoute first entry). `RECORD_AUDIO` and `POST_NOTIFICATIONS` NOT requested in Phase 1 — they're lazy at video-record / notification-emit time in later phases. Manifest declares all three.
- **D-13:** Permission denial shows an inline rationale + "Open Settings" intent button; no blank screen.

### DI (Hilt)
- **D-14:** `@HiltAndroidApp` `BugzzApplication` class; registered in manifest.
- **D-15:** `@AndroidEntryPoint` on `MainActivity`. Hilt modules created as needed by later phases (nothing to inject in Phase 1 yet — empty `AppModule` placeholder acceptable).

### Debug Tooling
- **D-16:** StrictMode `ThreadPolicy.Builder().detectAll().penaltyLog()` + `VmPolicy.Builder().detectAll().penaltyLog()` enabled only in debug builds (via `BuildConfig.DEBUG` check in Application.onCreate).
- **D-17:** LeakCanary (`com.squareup.leakcanary:leakcanary-android:2.14`) as debug dependency only. Zero production footprint.

### Code Style / Lint
- **D-18:** **NO ktlint, NO detekt, NO spotless** in Phase 1. Solo dev, no CI, deferred to later phase if churn becomes a problem. Android Studio's built-in inspections + Kotlin compiler warnings are sufficient.

### Tests
- **D-19:** Unit test scaffold: `src/test/java/` with JUnit 4 (`junit:junit:4.13.2`) — NOT JUnit 5 (Android tooling friction). Single passing placeholder test (`ExampleUnitTest.kt`) proving runner works.
- **D-20:** Instrumented test scaffold: `src/androidTest/java/` with `androidx.test.ext:junit:1.3.0` + `espresso-core:3.7.0`. Single passing placeholder `ExampleInstrumentedTest.kt`.
- **D-21:** NO Compose UI test in Phase 1 (`androidx.compose.ui:ui-test-junit4` deferred to Phase 6 when production UI exists).

### Icon & Branding
- **D-22:** Phase 1 uses Android Studio default adaptive icon (green Android logo). App display name: `"Bugzz"` string resource in `strings.xml`. Icon swap + reference-style branding deferred to Phase 6 (UX Polish).

### Build Configuration
- **D-23:** Three build types: `debug` (default AGP behavior + StrictMode + LeakCanary), `release` (minify disabled in Phase 1, enable R8 in Phase 7). Flavors: none (single release channel).
- **D-24:** `local.properties` generated with `sdk.dir=C:\\Users\\Admin\\AppData\\Local\\Android\\Sdk`. Added to `.gitignore` (already present).
- **D-25:** `gradle.properties` sets `org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr` so Gradle finds JDK 21 without shell env vars. This is Windows-specific but checked in since it's the team's single-dev reality.

### Claude's Discretion
- Gradle wrapper version (Claude picks latest compatible with AGP 8.9 — likely 8.11.1 or 8.12).
- Exact Kotlinx Serialization version (pick stable matching Kotlin 2.1.21 — 1.7.3 or 1.8.0).
- Placeholder string values in strings.xml ("Bugzz", "Home", "Camera", "Preview", "Collection").
- How to wire version catalog plugin aliases (`alias(libs.plugins.android.application)` style).
- Whether to generate Android Studio `.idea/` files or leave them to first IDE open.

</decisions>

<canonical_refs>
## Canonical References

Downstream agents MUST read these before planning or implementing.

### Project specs
- `.planning/PROJECT.md` — project vision, locked tech decisions, MVP scope, env paths
- `.planning/REQUIREMENTS.md` — 67 v1 requirements; Phase 1 covers FND-01..08
- `.planning/ROADMAP.md` §Phase 1 — goal and success criteria for this phase
- `.planning/research/STACK.md` — prescriptive version catalog for all deps
- `.planning/research/SUMMARY.md` — resolved UI-toolkit/face-tracking/3D-engine decisions

### External docs
- [Android Gradle Plugin 8.9 release notes](https://developer.android.com/build/releases/gradle-plugin) — AGP/Kotlin/JDK compatibility matrix
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) — maps BOM 2026.04.00 to artifact versions
- [navigation-compose type-safe routes](https://developer.android.com/develop/ui/compose/navigation#type-safety) — `@Serializable` route API
- [ActivityResultContracts.RequestPermission](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.RequestPermission) — the Google-recommended permission API

### Reference APK (do not include in our binary, but reference for behavior)
- `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` — reference app binary
- `reference/manifest.json` — reference app permission set + targetSdk

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None. Greenfield project — `d:/ClaudeProject/appmobile/Bugzz/` contains only `.planning/`, `reference/`, `.git/`, `.gitignore`, `CLAUDE.md`. No existing Android/Kotlin source.

### Established Patterns
- None in the app yet. Convention to be established in Phase 1 and followed by Phase 2+.

### Integration Points
- `.gitignore` at repo root already ignores: `reference/*.apk`, `reference/*.xapk`, `*.xapk`, `/build/`, `*/build/`, `.gradle/`, `local.properties`, `*.iml`, `.idea/`, `*.jks`, `*.keystore`, `keystore.properties`, `*.class`, `.DS_Store`. Phase 1 does NOT need additional gitignore entries.
- Git remote `origin` set to `https://github.com/ductv112/Bugzz.git` (not yet pushed).
- Git user identity set locally: `ductv` / `ductv112@gmail.com`.

</code_context>

<specifics>
## Specific Ideas

- User wants feature parity with reference app minus monetization. Phase 1 establishes the scaffolding to enable all of Phase 2+ work; the skeleton should make those downstream phases "just add code," not "refactor the skeleton."
- User will plug in real Android 9+ phone via USB ADB after Phase 1 produces a buildable debug APK. Phase 1 must successfully `./gradlew :app:assembleDebug` on CLI before handoff.
- User installed Android SDK at `C:\Users\Admin\AppData\Local\Android\Sdk` with API 36.1 platform; is installing API 35 via Android Studio SDK Manager in parallel with Phase 1 execution. Plan must not block if API 35 is not yet installed at gradle-sync time — fall back is `compileSdk 36` with accepted warning, or wait-and-retry.

</specifics>

<deferred>
## Deferred Ideas

- **ktlint / detekt / spotless**: may add in Phase 6 or Phase 7 if code style churn becomes a real problem.
- **Multi-module split**: reconsider at Phase 4 (Filter Catalog) when `render/` package may benefit from isolation.
- **Compose UI test harness**: defer to Phase 6 when production UI exists.
- **Icon + branding extract from reference APK**: defer to Phase 6 (UX Polish).
- **R8 / ProGuard minify**: defer to Phase 7 (Performance).
- **CI/CD (GitHub Actions)**: not in MVP scope; user builds locally.
- **Signing config for release**: defer until user decides on Play Store path (explicitly out of scope per PROJECT.md).

</deferred>

---

*Phase: 01-foundation-skeleton*
*Context gathered: 2026-04-18*
