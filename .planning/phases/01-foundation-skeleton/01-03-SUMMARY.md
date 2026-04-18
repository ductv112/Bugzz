---
phase: 01-foundation-skeleton
plan: 03
subsystem: infra
tags: [android, kotlin, manifest, permissions, compose, activity-result-contracts, junit, androidx-test, resources, adaptive-icon, material-theme]

# Dependency graph
requires:
  - phase: 01-02 (App module + Hilt + Compose nav shell)
    provides: "@HiltAndroidApp BugzzApplication, @AndroidEntryPoint MainActivity, 5 stub composables (SplashScreen/HomeScreen/CameraScreen/PreviewScreen/CollectionScreen) + internal StubContent helper, Compose NavHost, StrictMode scaffold already installed in BugzzApplication.onCreate()"
provides:
  - "app/src/main/AndroidManifest.xml with exactly 3 uses-permission (CAMERA, RECORD_AUDIO, POST_NOTIFICATIONS), zero WRITE_EXTERNAL_STORAGE, camera hardware feature required"
  - "<application android:name=\".BugzzApplication\"> + <activity .MainActivity> wired with MAIN+LAUNCHER intent filter and android:exported=true"
  - "6 resource files: values/{strings,themes,colors}.xml + xml/{data_extraction,backup}_rules.xml + mipmap-anydpi-v26/ic_launcher{,_round}.xml + drawable/ic_launcher_foreground.xml"
  - "Theme.Bugzz style extending android:Theme.Material.Light.NoActionBar (no AppCompat dependency)"
  - "CameraScreen permission-gated composable using raw ActivityResultContracts.RequestPermission() (no Accompanist) — CAMERA requested on first entry; denial shows rationale + Settings.ACTION_APPLICATION_DETAILS_SETTINGS Open Settings CTA"
  - "Unit test scaffold (JUnit 4.13.2) + instrumented test scaffold (AndroidX Test JUnit4) — unit test runs green"
  - "./gradlew :app:compileDebugKotlin passes end-to-end (manifest + resources + Kotlin + KSP + Hilt all compile together)"
  - "./gradlew :app:testDebugUnitTest passes (1 test, 0 failures, 0 errors)"
affects: [01-04 (debug-tooling-tests: StrictMode verification + LeakCanary validation + assembleDebug), all future phases that add activities/services/permissions or UI tests]

# Tech tracking
tech-stack:
  added:
    - "AndroidX Activity Result APIs (ActivityResultContracts.RequestPermission, rememberLauncherForActivityResult) — exercised directly by CameraScreen"
    - "androidx.core:core-ktx Uri extension (String.toUri) — replaces deprecated Uri.parse for settings deep-link"
    - "JUnit 4.13.2 (unit test runner, exercised)"
    - "androidx.test.ext:junit:1.3.0 + espresso-core:3.7.0 (instrumented test scaffold; compiles, device run deferred to Plan 04)"
  patterns:
    - "Permission flow pattern: check with ContextCompat.checkSelfPermission → launch with rememberLauncherForActivityResult → denial branch renders rationale + ACTION_APPLICATION_DETAILS_SETTINGS deep-link. Reuse of internal StubContent helper avoids duplicating Scaffold/Column scaffolding."
    - "Resource layout: values/ for strings+themes+colors (single colors.xml for ic_launcher_background — no separate file per AAPT2 dedup rule), xml/ for backup rules, mipmap-anydpi-v26/ for adaptive icons, drawable/ for vector foreground."
    - "Manifest pattern: 3 <uses-permission> only; <uses-feature camera required=true> as hard hardware gate; tools:targetApi on attribute-level to suppress newer-API lint warnings; XML theme is minimal since Compose drives its own MaterialTheme inside setContent{}."
    - "Test scaffold pattern: src/test/java + src/androidTest/java mirror of main package; placeholder tests prove the runner wiring (trivial arithmetic for unit, InstrumentationRegistry.targetContext.packageName for instrumented)."

key-files:
  created:
    - app/src/main/AndroidManifest.xml
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values/themes.xml
    - app/src/main/res/values/colors.xml
    - app/src/main/res/xml/data_extraction_rules.xml
    - app/src/main/res/xml/backup_rules.xml
    - app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
    - app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
    - app/src/main/res/drawable/ic_launcher_foreground.xml
    - app/src/test/java/com/bugzz/filter/camera/ExampleUnitTest.kt
    - app/src/androidTest/java/com/bugzz/filter/camera/ExampleInstrumentedTest.kt
  modified:
    - app/src/main/java/com/bugzz/filter/camera/ui/screens/StubScreens.kt
    - .gitignore

key-decisions:
  - "ic_launcher_background color defined inline in values/colors.xml rather than a dedicated values/ic_launcher_background.xml file — matches the plan's explicit guidance and avoids AAPT2 duplicate-resource errors if both were present."
  - "CameraScreen `else` branch (waiting for launcher result) renders StubContent with empty actions rather than a blank screen — gives the user visible continuity (\"Camera route — Phase 1 stub\") during the ~1-frame window between LaunchedEffect and launcher callback. Plan's research comment allowed blank/spinner here; StubContent is the minimum-surprise choice."
  - ".gitignore updated to ignore .kotlin/ — Kotlin 2.1's incremental compilation cache directory (new vs. Kotlin 1.9). Auto-added when gradle spawns for compileDebugKotlin. Treating as generated (Rule 6 untracked-file protocol)."

patterns-established:
  - "Per-task atomic commits on main working tree: feat(01-03) for manifest+resources, feat(01-03) for CameraScreen rewrite, test(01-03) for test scaffolds. All 3 commits pass their corresponding verification (file existence, grep content, compileDebugKotlin for task 2, testDebugUnitTest for task 3)."
  - "Runtime gradle command shape: JAVA_HOME=\"C:/Program Files/Android/Android Studio/jbr\" ./gradlew :app:<task>. Carry forward to Plan 04."
  - "File-creation via the Write tool — never heredoc/cat. Edits to existing files via Edit tool after Read."

requirements-completed: [FND-05, FND-06, FND-07]

# Metrics
duration: 4min
completed: 2026-04-18
---

# Phase 1 Plan 3: AndroidManifest + Resources + CAMERA Permission Flow + Test Scaffolds Summary

**App module now has a launchable `.MainActivity` with exactly 3 manifest permissions (CAMERA/RECORD_AUDIO/POST_NOTIFICATIONS, zero WRITE_EXTERNAL_STORAGE), a raw-ActivityResultContracts CAMERA permission gate on CameraScreen with Open Settings deep-link on denial, and a passing JUnit runner — `:app:compileDebugKotlin` and `:app:testDebugUnitTest` both succeed**

## Performance

- **Duration:** ~4 min (wall clock) — most was two `./gradlew` invocations (35s + 27s)
- **Started:** 2026-04-18T17:26:11Z
- **Completed:** 2026-04-18T17:29:43Z
- **Tasks:** 3 (all committed atomically on main)
- **Files created:** 11
- **Files modified:** 2 (StubScreens.kt rewrite, .gitignore `.kotlin/` addition)

## Accomplishments

- **FND-05 satisfied:** `app/src/main/AndroidManifest.xml` declares exactly `CAMERA`, `RECORD_AUDIO`, `POST_NOTIFICATIONS` (count = 3) and zero occurrences of `WRITE_EXTERNAL_STORAGE`. Wires `.BugzzApplication` + `.MainActivity` (exported + MAIN/LAUNCHER intent-filter) + `<uses-feature android.hardware.camera required=true>` + icon/theme/backup-rules references.
- **FND-06 satisfied:** `CameraScreen` rewritten to use raw `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` (no Accompanist). First entry prompts CAMERA via `LaunchedEffect(Unit)`; denial branch renders rationale text + "Grant permission" button + "Open Settings" button (deep-link via `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` + `package:` URI). Only CAMERA is requested — RECORD_AUDIO/POST_NOTIFICATIONS stay lazy per D-12.
- **FND-07 satisfied (verified, not re-added):** `BugzzApplication.kt` still contains `@HiltAndroidApp`, StrictMode thread+VM policies guarded by `BuildConfig.DEBUG`, and LeakCanary 2.14 remains a `debugImplementation` in `app/build.gradle.kts` (auto-install via its own merged ContentProvider, no manual init).
- **6 resource files** created: `values/{strings,themes,colors}.xml`, `xml/{backup,data_extraction}_rules.xml`, `mipmap-anydpi-v26/ic_launcher{,_round}.xml`, `drawable/ic_launcher_foreground.xml`. `ic_launcher_background` color defined in a single file (`values/colors.xml`) per AAPT2 dedup rule.
- **Test scaffolds** live under `app/src/test/` (JUnit 4) and `app/src/androidTest/` (AndroidJUnit4 + InstrumentationRegistry). Unit test `ExampleUnitTest.addition_isCorrect` runs green.
- **End-to-end build health verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL (no missing-manifest error, no Kotlin source errors, no KSP/Hilt errors). `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL (1 test, 0 failures, 0 errors per TEST-com.bugzz.filter.camera.ExampleUnitTest.xml).

## Task Commits

1. **Task 1: AndroidManifest.xml + 9 resource files** — `feb92e2` (feat)
2. **Task 2: CameraScreen permission-flow rewrite (+ `.gitignore` `.kotlin/`)** — `4bd0d0a` (feat)
3. **Task 3: Unit + instrumented test scaffolds** — `39e1fb0` (test)

(Plan metadata / SUMMARY commit is the orchestrator's responsibility per plan note.)

## Files Created/Modified

**Created (11):**
- `app/src/main/AndroidManifest.xml` — permissions + app + MainActivity + camera feature + icon/theme/backup wiring (40 lines)
- `app/src/main/res/values/strings.xml` — app_name=Bugzz + 5 route labels + 2 permission rationale strings
- `app/src/main/res/values/themes.xml` — `Theme.Bugzz` extends `android:Theme.Material.Light.NoActionBar`, black status bar
- `app/src/main/res/values/colors.xml` — `black`, `white`, `ic_launcher_background` (#3DDC84)
- `app/src/main/res/xml/data_extraction_rules.xml` — empty `<cloud-backup>` + `<device-transfer>` (Android 12+)
- `app/src/main/res/xml/backup_rules.xml` — empty `<full-backup-content>` (Android 11 and below)
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — adaptive icon (bg + fg + monochrome all referencing same foreground vector)
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` — identical adaptive icon for `android:roundIcon`
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — vector (white circle on launcher background); 108dp viewport
- `app/src/test/java/com/bugzz/filter/camera/ExampleUnitTest.kt` — JUnit 4 placeholder (`assertEquals(4, 2 + 2)`)
- `app/src/androidTest/java/com/bugzz/filter/camera/ExampleInstrumentedTest.kt` — AndroidJUnit4 placeholder asserting `targetContext.packageName == "com.bugzz.filter.camera"`

**Modified (2):**
- `app/src/main/java/com/bugzz/filter/camera/ui/screens/StubScreens.kt` — `CameraScreen` body rewritten with full permission flow (LaunchedEffect + launcher + when{granted/rationale/else}); other 4 screens + `internal fun StubContent(...)` byte-identical-in-signature with Plan 02.
- `.gitignore` — appended `.kotlin/` (Kotlin 2.1 incremental-compile cache directory created by `./gradlew :app:compileDebugKotlin`)

## Verification Evidence

**Manifest invariants (per FND-05):**
- `grep -c 'uses-permission' app/src/main/AndroidManifest.xml` → **3** (exact target)
- `grep -c 'WRITE_EXTERNAL' app/src/main/AndroidManifest.xml` → **0** (reference APK requests this; we explicitly do not)
- `grep -c 'ic_launcher_background' app/src/main/res/values/colors.xml` → **1** (single source; no duplicate `.xml` file per AAPT2 rule)

**Compile health:**
```
> Task :app:compileDebugKotlin
BUILD SUCCESSFUL in 35s
16 actionable tasks: 11 executed, 5 up-to-date
```

**Unit test results (`app/build/test-results/testDebugUnitTest/TEST-com.bugzz.filter.camera.ExampleUnitTest.xml`):**
```xml
<testsuite name="com.bugzz.filter.camera.ExampleUnitTest" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-04-18T17:29:20.299Z" time="0.003">
  <testcase name="addition_isCorrect" classname="com.bugzz.filter.camera.ExampleUnitTest" time="0.003"/>
```

```
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 27s
31 actionable tasks: 15 executed, 16 up-to-date
```

## Decisions Made

- **`ic_launcher_background` defined only in `values/colors.xml`** — the plan explicitly warned against creating a second `values/ic_launcher_background.xml`. Followed the plan's one-file rule to avoid AAPT2 duplicate-resource errors.
- **CameraScreen `else` branch renders `StubContent("Camera")` with empty actions** rather than a blank screen or spinner — keeps visual continuity during the sub-second window between `LaunchedEffect` firing and the launcher callback. The research allowed "blank or spinner"; `StubContent` is consistent with the other 4 screens and aligns with Phase 1's stub-first ethos.
- **`.gitignore` updated to exclude `.kotlin/`** — this directory is Kotlin 2.1's incremental compilation cache. It was created by `./gradlew :app:compileDebugKotlin` and isn't covered by Plan 02's gitignore. Per the executor's untracked-file protocol, generated runtime output is `.gitignore`-appended rather than committed. Handled inside the Task 2 commit to keep working tree clean.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added `.kotlin/` to `.gitignore`**
- **Found during:** Task 2 (`git status --short` after `./gradlew :app:compileDebugKotlin`)
- **Issue:** Kotlin 2.1's incremental compilation creates a `.kotlin/` cache directory at the repo root. Wave 1's `.gitignore` ignores `/build/`, `*/build/`, `.gradle/` but not `.kotlin/`. Gradle runs would leave it perpetually untracked, polluting every future `git status`.
- **Fix:** Appended `.kotlin/` under the existing `# Kotlin` section in `.gitignore` (one-line change alongside existing `*.class` rule).
- **Files modified:** `.gitignore`
- **Verification:** `git status --short` after adding the rule no longer lists `.kotlin/` as untracked. Per-task commit confirmed clean tree post-commit.
- **Committed in:** `4bd0d0a` (bundled with Task 2's StubScreens.kt rewrite — same commit because both are pre-conditions for having a clean Task 2 acceptance).

---

**Total deviations:** 1 auto-fixed (1 Rule 3 blocking)
**Impact on plan:** Zero semantic impact. The `.gitignore` change is hygiene; the runtime behavior of the app and build does not change. No plan requirement touched.

## Authentication Gates Encountered

None. Plan execution was fully autonomous; no user secrets or auth tokens were needed.

## Issues Encountered

- **Git CRLF warnings on every new file** — Windows default `core.autocrlf=true` normalization. Same as Plan 02. Safe, content preserved.
- **`[Incubating] Problems report` from Gradle 8.13** — informational only (same as Plan 02 notes). Does not block build.
- **`Hilt_BugzzApplication.java uses or overrides a deprecated API` note during `:app:hiltJavaCompileDebug`** — informational javac hint in Hilt's generated code; does not fail the build and is outside our source scope. Will be addressed when we bump Hilt in a future phase if it becomes a lint error.
- **`aapt.exe` configuration-cache invalidation on first compile** — one-time cost from the SDK install hydrating paths the configuration cache hadn't seen. Second invocation of `:app:testDebugUnitTest` reused the cache cleanly (`Configuration cache entry stored.`).

## User Setup Required

None — all work self-contained within the repo + existing local Android SDK + JDK 21 at `C:/Program Files/Android/Android Studio/jbr`.

## Next Phase Readiness

**Plan 04 (StrictMode runtime verification + LeakCanary validation + `assembleDebug` + device-run preparation)** is fully UNBLOCKED:

- `app/src/main/AndroidManifest.xml` exists and parses — the last hard-missing artifact AGP needed to package an APK.
- `BugzzApplication` wiring and StrictMode installation are verified present (Plan 02 scaffolding not regressed).
- `CameraScreen` permission flow is the final Phase 1 runtime behavior expected on device — Plan 04 (or later) will validate it by granting/denying via adb or UI.
- JUnit runner works; instrumented test scaffold is compile-ready — Plan 04's `:app:connectedDebugAndroidTest` just needs a connected device.

**Known constraints forwarded to Plan 04:**
- Gradle runtime still needs `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"` prefix on every invocation.
- `.kotlin/` is now ignored — any further gradle runs will not pollute the working tree.
- Compose BOM pin is still `2026.03.00` (Plan 02 fallback locked in).
- `androidx.test.ext.junit:1.3.0` and `espresso-core:3.7.0` will first be exercised by device test in Plan 04 — if either fails resolve, the research-flagged fallback is `androidx.test.ext.junit:1.2.1`.
- StubContent's `internal` visibility is now load-bearing (CameraScreen's three StubContent call-sites plus the other 4 screens' single call-sites) — do not downgrade to `private`.

## Self-Check: PASSED

**Files verified on disk:**
- `d:/ClaudeProject/appmobile/Bugzz/app/src/main/AndroidManifest.xml` — FOUND (3 uses-permission, 0 WRITE_EXTERNAL, `.BugzzApplication` + `.MainActivity` both referenced, `android:exported="true"`, MAIN + LAUNCHER intent-filter)
- `d:/ClaudeProject/appmobile/Bugzz/app/src/main/res/values/strings.xml` — FOUND (`<string name="app_name">Bugzz</string>` + 5 route labels + 2 permission strings)
- `d:/ClaudeProject/appmobile/Bugzz/app/src/main/res/values/themes.xml` — FOUND (`<style name="Theme.Bugzz" parent="android:Theme.Material.Light.NoActionBar">`)
- `d:/ClaudeProject/appmobile/Bugzz/app/src/main/res/values/colors.xml` — FOUND (black, white, ic_launcher_background)
- `d:/ClaudeProject/appmobile/Bugzz/app/src/main/res/xml/data_extraction_rules.xml` — FOUND
- `d:/ClaudeProject/appmobile/Bugzz/app/src/main/res/xml/backup_rules.xml` — FOUND
- `d:/ClaudeProject/appmobile/Bugzz/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — FOUND
- `d:/ClaudeProject/appmobile/Bugzz/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` — FOUND
- `d:/ClaudeProject/appmobile/Bugzz/app/src/main/res/drawable/ic_launcher_foreground.xml` — FOUND
- `d:/ClaudeProject/appmobile/Bugzz/app/src/main/java/com/bugzz/filter/camera/ui/screens/StubScreens.kt` — FOUND (rewritten CameraScreen; contains `rememberLauncherForActivityResult`, `ActivityResultContracts.RequestPermission`, `Manifest.permission.CAMERA`, `ACTION_APPLICATION_DETAILS_SETTINGS`; does NOT contain `Manifest.permission.RECORD_AUDIO`, `Manifest.permission.POST_NOTIFICATIONS`, or `accompanist`; 5 screen signatures preserved; `internal fun StubContent` preserved)
- `d:/ClaudeProject/appmobile/Bugzz/app/src/test/java/com/bugzz/filter/camera/ExampleUnitTest.kt` — FOUND (`class ExampleUnitTest`, `@Test fun addition_isCorrect`)
- `d:/ClaudeProject/appmobile/Bugzz/app/src/androidTest/java/com/bugzz/filter/camera/ExampleInstrumentedTest.kt` — FOUND (`@RunWith(AndroidJUnit4::class)`, `"com.bugzz.filter.camera"` package assertion)

**Commits verified in git log:**
- `feb92e2` — FOUND (`feat(01-03): add AndroidManifest and resource files`)
- `4bd0d0a` — FOUND (`feat(01-03): add CAMERA runtime permission flow to CameraScreen`)
- `39e1fb0` — FOUND (`test(01-03): add unit + instrumented test scaffolds`)

**End-to-end gradle verification:**
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL in 35s
- `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL in 27s, 1 test / 0 failures / 0 errors

**Wave 1 non-regression verified:**
- `BugzzApplication.kt` still contains `@HiltAndroidApp`, `StrictMode.setThreadPolicy`, `StrictMode.setVmPolicy`, `BuildConfig.DEBUG` guard — untouched by this plan.
- Other 4 screen composables (SplashScreen, HomeScreen, PreviewScreen, CollectionScreen) signatures byte-identical with Plan 02's versions.
- `internal fun StubContent(label: String, actions: @Composable () -> Unit)` signature preserved.

---
*Phase: 01-foundation-skeleton*
*Completed: 2026-04-18*
