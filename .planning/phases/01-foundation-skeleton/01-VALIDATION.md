---
phase: 01
slug: foundation-skeleton
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-18
---

# Phase 01 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Phase 01 is pure scaffolding — most "validation" is Gradle build success + manual device install. Few unit tests apply; instrumented test runs require a device.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 (unit) + AndroidX Test runner 1.3.0 (instrumented) |
| **Config file** | `app/build.gradle.kts` (testOptions block — default) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` |
| **Full suite command** | `./gradlew :app:assembleDebug :app:testDebugUnitTest` |
| **Estimated runtime** | ~45–120 seconds (first run with cold caches); ~10–20 seconds warm |

Gradle commands must be invoked with:
- `ANDROID_HOME=C:/Users/Admin/AppData/Local/Android/Sdk`
- `JAVA_HOME=C:/Program\ Files/Android/Android\ Studio/jbr` (or via `gradle.properties` `org.gradle.java.home`)

---

## Sampling Rate

- **After every task commit:** `./gradlew help` (fast sync check)
- **After every plan wave:** `./gradlew :app:testDebugUnitTest`
- **Before `/gsd-verify-work`:** Full suite green — `./gradlew :app:assembleDebug :app:testDebugUnitTest`
- **Max feedback latency:** 120 seconds (cold), 30 seconds (warm)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 0 | FND-01 | — | N/A | build | `./gradlew help` | W0 | ⬜ pending |
| 01-01-02 | 01 | 0 | FND-02 | — | N/A | grep | `grep -q 'compose-bom' gradle/libs.versions.toml` | ✅ | ⬜ pending |
| 01-02-01 | 02 | 1 | FND-03 | — | N/A | grep | `grep -q '@HiltAndroidApp' app/src/main/java/com/bugzz/filter/camera/BugzzApplication.kt` | ✅ | ⬜ pending |
| 01-02-02 | 02 | 1 | FND-04 | — | N/A | grep | `grep -q 'SplashRoute' app/src/main/java/com/bugzz/filter/camera/nav/Routes.kt` | ✅ | ⬜ pending |
| 01-03-01 | 03 | 1 | FND-05 | — | N/A | grep | `grep -q 'android.permission.CAMERA' app/src/main/AndroidManifest.xml` | ✅ | ⬜ pending |
| 01-03-02 | 03 | 1 | FND-06 | — | N/A | grep | `grep -q 'rememberLauncherForActivityResult' app/src/main/java/com/bugzz/filter/camera/**/*.kt` | ✅ | ⬜ pending |
| 01-03-03 | 03 | 2 | FND-07 | — | N/A | grep | `grep -q 'StrictMode' app/src/main/java/com/bugzz/filter/camera/BugzzApplication.kt` | ✅ | ⬜ pending |
| 01-04-01 | 04 | 2 | FND-02 | — | N/A | build | `./gradlew :app:testDebugUnitTest` | ✅ | ⬜ pending |
| 01-04-02 | 04 | 2 | FND-08 | — | N/A | build | `./gradlew :app:assembleDebug` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Task IDs are illustrative — actual IDs will be set by the planner. This map will be refreshed by the planner after PLAN.md is finalized.*

---

## Wave 0 Requirements

- [ ] `gradle/libs.versions.toml` exists with all required version aliases
- [ ] `app/build.gradle.kts` declares Hilt, KSP, Compose Compiler plugins
- [ ] `gradlew` wrapper bootstrap completes (via user's Gradle 9.3.1 install)
- [ ] `./gradlew help` succeeds (validates toolchain end-to-end)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| App opens on real device without crash | FND-08 | Requires physical Android 9+ device plugged via USB ADB; instrumented tests cannot verify "opens without crashing" in isolation | 1. User plugs phone + enables USB debugging. 2. `adb install app/build/outputs/apk/debug/app-debug.apk`. 3. `adb shell am start -n com.bugzz.filter.camera/.MainActivity`. 4. Observe Splash → Home navigable with stub Text labels. 5. `adb logcat -d -s AndroidRuntime:E` returns empty. |
| Runtime CAMERA permission prompt shown | FND-06 | Depends on Android OS permission dialog UI, not app-internal state | Navigate to Camera stub → observe OS permission prompt → tap Deny → observe "Open Settings" CTA rendered (not blank screen) |
| StrictMode violations logged in debug | FND-07 | Requires runtime log inspection, not compile-time check | `adb logcat -d -s StrictMode` after cold-start to Home — zero violations expected (some 1st-launch disk reads acceptable) |
| LeakCanary debug toast on leak | FND-07 | LeakCanary auto-initializes via ContentProvider at app start; presence inferred from logcat | `adb logcat -d -s LeakCanary` — "LeakCanary install" or similar marker appears once per process |

---

## Validation Sign-Off

- [ ] All automatable tasks have `<automated>` verify steps in PLAN.md
- [ ] Sampling continuity: Gradle build sampling after each wave
- [ ] Wave 0 covers toolchain bootstrap
- [ ] No watch-mode flags (Gradle `--continuous` off)
- [ ] Manual-only verifications documented above with exact instructions for user handoff
- [ ] `nyquist_compliant: true` set in frontmatter once planner fills in final task IDs

**Approval:** pending
