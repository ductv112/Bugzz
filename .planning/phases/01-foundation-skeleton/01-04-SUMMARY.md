---
phase: 01-foundation-skeleton
plan: 04
subsystem: infra
tags: [android, gradle, assembleDebug, apk, device-verification, adb, handoff, checkpoint-pending]
status: checkpoint-pending

# Dependency graph
requires:
  - phase: 01-03 (AndroidManifest + Resources + CAMERA permission flow + test scaffolds)
    provides: "AndroidManifest.xml with .BugzzApplication + .MainActivity wired (MAIN/LAUNCHER + exported); 3 uses-permission (CAMERA/RECORD_AUDIO/POST_NOTIFICATIONS); CameraScreen permission flow; JUnit unit tests passing; :app:compileDebugKotlin green"
provides:
  - "app/build/outputs/apk/debug/app-debug.apk (32,501,879 bytes) — fully linked, packaged debug APK with 8 dex files, 3 ABI native libs, resources, Hilt codegen, Compose Material3"
  - ".planning/phases/01-foundation-skeleton/01-04-HANDOFF.md — 190-line user-facing runbook covering adb device detect -> install -> launch -> 12-step visual checklist -> StrictMode/LeakCanary logcat verification"
  - "FND-01 satisfied (buildable): ./gradlew :app:assembleDebug exits 0 from clean state"
  - "FND-08 satisfied PENDING USER VERIFICATION: artifact + runbook ready; user action gates the close"
affects: [all phase-2 plans (CameraX core stack will build on top of verified-launchable skeleton); Phase-1 close (STATE/ROADMAP flip to Complete only after user signs off on Task 3 checkpoint)]

# Tech tracking
tech-stack:
  added:
    - "No new dependencies — this plan exercises the existing dependency graph end-to-end."
  patterns:
    - "Automation-first gate: executor runs :app:clean + :app:assembleDebug and produces a copy-pasteable handoff runbook; checkpoint task is purely user-side (physical device plug-in). Zero manual steps for Claude before the checkpoint."
    - "Absolute-adb-path discipline: every adb invocation in HANDOFF.md uses the full `C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe` path (research Gotcha G-10) — no reliance on PATH mutation."
    - "PASS/FAIL checklist table pattern: 12 discrete gates mapped to on-device user actions, each with expected text/behavior, collapsing FND-06 + FND-08 verification into a single walk-through."

key-files:
  created:
    - .planning/phases/01-foundation-skeleton/01-04-HANDOFF.md
  modified: []
  produced_artifacts:
    - app/build/outputs/apk/debug/app-debug.apk (gitignored build output)

key-decisions:
  - "APK size deviation (32.5 MB vs plan's 1-15 MB bound) accepted as a plan-estimate error, not a build bug. The APK is structurally valid (8 dex files, proper manifest, native libs for arm64-v8a/x86/x86_64) and the size is entirely explained by two unminified Compose + Hilt dex files (18.5 MB + 12.9 MB). Release variant (future phase) with R8 shrink will be much smaller. Documented in HANDOFF.md Step 1 so user isn't surprised."
  - "Task 1 produces no git commit because the APK artifact is build output (gitignored). The proof-of-task-completion is the artifact on disk + the BUILD SUCCESSFUL gradle output, not a commit. Task 2 (HANDOFF.md) is the only task in this plan with a git commit."
  - "Human-verify checkpoint (Task 3) explicitly NOT auto-approved per orchestrator instruction. User must physically plug in phone and exercise the 12-step checklist; closing Phase 1 requires their 'approved' signal."

requirements-completed: [FND-01]
requirements-pending-verification: [FND-08]

# Metrics
duration: "~3m 16s (wall clock): 53s for :app:assembleDebug + ~2 min writing/verifying HANDOFF.md"
completed: 2026-04-18
started: 2026-04-18T17:33:47Z
stopped-at-checkpoint: 2026-04-18T17:37:03Z
---

# Phase 1 Plan 4: Debug APK Build + FND-08 Device-Verification Handoff Summary

**Clean `./gradlew :app:assembleDebug` produced a 32.5 MB installable debug APK on first attempt (BUILD SUCCESSFUL in 53s, no fallbacks needed); 190-line handoff runbook `01-04-HANDOFF.md` written with copy-pasteable absolute-path adb commands + 12-step visual verification checklist covering FND-06/07/08. Plan is CHECKPOINT-PENDING — Task 3 (physical device install) requires user action.**

## Status

**CHECKPOINT PENDING.** Tasks 1 + 2 complete. Task 3 (`checkpoint:human-verify`) blocked on user: plug in Android 9+ device, run HANDOFF.md runbook, report PASS/FAIL for 12-step checklist + device model + Android version.

## Performance

- **Duration:** ~3 min 16s wall clock (plan non-checkpoint portion)
- **Started:** 2026-04-18T17:33:47Z
- **Paused at checkpoint:** 2026-04-18T17:37:03Z
- **Tasks executed:** 2 of 3 (Task 3 intentionally not executed)
- **Files created:** 1 (HANDOFF.md)
- **Files modified:** 0
- **Build artifacts produced:** 1 (app-debug.apk, gitignored)
- **Commits:** 1 (HANDOFF.md only; APK is build output)

## Accomplishments

### Task 1 — Full clean debug build (automated)

- `./gradlew :app:clean` → BUILD SUCCESSFUL in 2s (1 actionable task)
- `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL in 53s** (41 actionable tasks; 23 executed, 18 from cache)
- **No fallbacks taken:**
  - `compileSdk=35` worked on first try (API 35 platform was installed in parallel by Wave 0; the pre-armed `android.suppressUnsupportedCompileSdk=36` escape hatch was never needed).
  - Compose BOM 2026.03.00 (Wave 1 fallback) resolved cleanly.
  - Configuration cache enabled — `Configuration cache entry stored.` printed on both runs.
  - No KSP / Hilt / manifest-merger errors.
  - No `INSTALL_PARSE_FAILED_MANIFEST_MALFORMED`-class issues.
- **APK on disk:** `app/build/outputs/apk/debug/app-debug.apk` — 32,501,879 bytes (~32.5 MB)
- **APK composition (via `unzip -l`):**
  - `classes.dex` 18.5 MB — main app + Compose runtime (unminified)
  - `classes8.dex` 12.9 MB — Hilt codegen + Compose Material3 + LeakCanary runtime
  - `classes2-7.dex` small shards (~230 KB total) — multi-dex partitioning
  - `resources.arsc` 529 KB — resource table (Material theme, colors, strings, drawables)
  - `lib/{arm64-v8a,x86,x86_64}/libandroidx.graphics.path.so` (~30 KB total) — native library for Compose text path rendering
  - `res/mipmap-*/leak_canary_icon.png` — LeakCanary's own icon (proof debug tooling is linked)
  - `META-INF/**/LICENSE.txt` — standard AndroidX license shards
- **FND-01 requirement status: PASS.** Source tree produces installable APK from `./gradlew :app:assembleDebug` starting from clean.

### Task 2 — FND-08 handoff runbook (automated)

Created `.planning/phases/01-foundation-skeleton/01-04-HANDOFF.md` (190 lines).

Runbook covers, in order:
1. User-side prerequisites (Developer Options, USB debugging, RSA fingerprint)
2. APK artifact confirmation
3. `adb devices` detection with `unauthorized`/`no-device` troubleshooting
4. `adb install -r` with `INSTALL_FAILED_*` fallbacks
5. `adb shell am start -n com.bugzz.filter.camera/.MainActivity` launch
6. 12-step visual verification checklist (Splash → Home → Camera permission gate → deny-rationale → Open Settings deep-link → Grant → Camera-granted stub → Preview → Collection → back-stack)
7. AndroidRuntime:E logcat crash-watch (with pidof-less fallback for older Android 9 builds)
8. StrictMode logcat check (FND-07)
9. LeakCanary logcat check (FND-07)
10. PASS-path guidance + FAIL-path `/gsd-debug` handoff
11. Appendix: adb-on-PATH short form + adb-server-reset recipe

Verification-by-grep confirmed: absolute-adb-path appears 11× (criterion: ≥5), launch target 3×, install-r 1×, devices 2×, all required keywords (Splash route, Camera permission, Open Settings, Grant permission, StrictMode, LeakCanary) present.

## Task Commits

1. **Task 1: Clean debug build** — NO COMMIT (APK is gitignored build output; BUILD SUCCESSFUL is the proof, artifact is on disk)
2. **Task 2: HANDOFF.md** — `29a41b4` (docs)
3. **Task 3: User device verification** — NOT EXECUTED (checkpoint awaiting user)

## Files Created/Modified

**Created (1):**
- `.planning/phases/01-foundation-skeleton/01-04-HANDOFF.md` — FND-08 user runbook (190 lines)

**Build artifacts produced (not committed):**
- `app/build/outputs/apk/debug/app-debug.apk` — 32,501,879 bytes, debug-signed, installable

## Verification Evidence

**Gradle clean build chain:**
```
> Task :app:clean
BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed

> Task :app:assembleDebug
(41 tasks, 23 executed / 18 from cache)
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so.
BUILD SUCCESSFUL in 53s
```

The "Unable to strip" note is informational — the NDK `strip` tool isn't present in the JBR bundled with Android Studio (Windows), so AGP packages the `.so` unstripped. Debug behavior expected; release build will pull strip from the full NDK. Not a deviation.

**APK on disk:**
```
-rw-r--r-- 1 Admin 197121 32501879 Apr 19 00:34 app/build/outputs/apk/debug/app-debug.apk
```

**APK structural integrity:**
- `unzip -p app-debug.apk AndroidManifest.xml` — succeeds, returns binary XML
- 8 classes*.dex entries present, total 31.7 MB of dex
- `resources.arsc` present (529 KB)
- Multi-ABI native libs for 3 architectures

**HANDOFF.md grep invariants:**
- `grep -c 'C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe'` → **11** (criterion: ≥5)
- `grep -c 'com.bugzz.filter.camera/.MainActivity'` → **3**
- `grep -cE 'adb.exe.*install -r'` → **1**
- `grep -cE 'adb.exe.*devices'` → **2**
- `grep -c 'Splash route'` → **1**
- `grep -c 'Camera permission'` → **1**
- `grep -c 'StrictMode'` → **4**
- `grep -c 'LeakCanary'` → **8**

## Deviations from Plan

### Accepted (documented) deviations

**1. [Rule 1 - Plan estimate error] APK size 32.5 MB vs plan's 1-15 MB bound**
- **Found during:** Task 1 (size sanity check `[ "$SIZE" -gt 1000000 ] && [ "$SIZE" -lt 15000000 ]` failed on upper bound)
- **Issue:** Plan's acceptance criterion said 1-15 MB; actual debug APK is 32.5 MB.
- **Root cause:** Plan's estimate did not account for the full debug-variant payload of unminified Compose Material3 (~10 MB of classes) + Hilt generated factories (~3 MB) + LeakCanary runtime (~2 MB) + Navigation + Serialization + Lifecycle + AndroidX Core/Activity/Annotation. In an R8-disabled debug build (which this is; R8 is release-only), these all land in main and supplementary dex shards at full size.
- **Evidence it's not a build bug:** 8 dex files structurally valid; resources.arsc intact; manifest extractable; native libs present for all 3 expected ABIs. APK size breakdown is dominated by 2 dex files totaling 31.4 MB — no runaway asset packaging, no duplicate classes, no mis-packaged resources.
- **Decision:** Accept actual size; document in HANDOFF.md Step 1 so user knows ~32 MB is expected and distinguishes "file listed" from "file matches plan's guess." Release variant with R8 shrink (future phase) will be significantly smaller.
- **Files modified by deviation:** `.planning/phases/01-foundation-skeleton/01-04-HANDOFF.md` (Step 1 size guidance)
- **Rule applied:** Rule 1 (plan guidance was incorrect; actual behavior is correct — adjusted runbook rather than source)

### No fallbacks taken

- `compileSdk=35` was **not** bumped to 36 — API 35 platform is present and build succeeded.
- Compose BOM was not changed from Wave 1's `2026.03.00` pin.
- `androidx.test.ext:junit:1.3.0` / `espresso-core:3.7.0` were not exercised here (instrumented test not run) and thus not stressed; assume-working-until-Plan-5 stands.
- `org.gradle.configuration-cache=true` stayed on — build succeeded with it.
- All Windows `\\` path escapes remained valid.

**Total deviations:** 1 accepted (plan-estimate error, not source bug)
**Impact on plan:** Zero semantic impact. APK is buildable, structurally valid, installable — which is exactly what FND-01 requires. The size criterion was an advisory check; its failure doesn't block FND-01.

## Authentication Gates Encountered

None during Tasks 1-2. Task 3's human-verify is not an auth gate — it's a physical-device verification step.

## Issues Encountered

- **"Unable to strip the following libraries: libandroidx.graphics.path.so"** — Informational AGP warning. The NDK `strip` tool is absent from the Android Studio JBR bundle (only present with the full NDK install, which isn't required for Kotlin/Compose apps). The `.so` is packaged unstripped; size impact is trivial (~10 KB × 3 ABIs). Non-blocking, expected on this toolchain.
- **"Calculating task graph as no cached configuration is available"** on the first gradle run after clean — expected (configuration cache freshly invalidated by clean invocation); second run would reuse it.
- **Git CRLF warning on HANDOFF.md** — Same Windows `core.autocrlf=true` behavior as prior plans; safe, content preserved.

## User Setup Required (for Task 3 checkpoint)

The user must:

1. **Obtain an Android 9+ physical device** (reference APK's minSdk = 28 = Android 9; no emulator — emulators cannot exercise CAMERA permission flow realistically, and FND-08 explicitly requires a real device).
2. **Enable Developer Options + USB debugging** on that device (Settings → About phone → tap Build number 7× → Settings → Developer options → USB debugging ON).
3. **Connect via USB data cable** (not charge-only) and **accept the RSA fingerprint prompt** on the phone the first time `adb devices` is run.
4. **Execute the full HANDOFF.md runbook** end-to-end (8 steps + 12-item checklist).
5. **Report back** with: device model, Android version, 12-item checklist result (ideally "12/12 PASS"), and any logcat oddities.

## Next Phase Readiness

**Phase 2 is BLOCKED** on Task 3 user verification. The orchestrator will:
- Mark Phase 1 Complete in STATE.md and ROADMAP.md **only after** user types "approved" (per plan's resume-signal).
- Record device model + Android version in the final Phase 1 SUMMARY update.
- If user reports a failure, dispatch `/gsd-debug` — do NOT close the plan.

**Known constraints forwarded to Phase 2:**
- Gradle runtime still needs `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"` prefix on every invocation.
- Compose BOM pin remains `2026.03.00`.
- `compileSdk=35` is confirmed working; no need to keep the `compileSdk=36` fallback active (though `android.suppressUnsupportedCompileSdk=36` in `gradle.properties` stays as a defensive hedge).
- adb absolute path: `C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe` — retain for any Phase 2 device interactions.
- Debug APK baseline size (~32 MB) — Phase 2 will add CameraX ~10 MB + ML Kit bundled ~20 MB, expect APK to grow to ~60-70 MB. Still normal debug territory; R8-minified release variant will ship to device in a later phase.
- StubContent `internal` visibility contract preserved.
- `.kotlin/` continues to be gitignored.

## Self-Check: PASSED

**Files verified on disk:**
- `d:/ClaudeProject/appmobile/Bugzz/.planning/phases/01-foundation-skeleton/01-04-HANDOFF.md` — FOUND (190 lines; contains absolute adb path 11×; launch target 3×; StrictMode + LeakCanary sections present)
- `d:/ClaudeProject/appmobile/Bugzz/app/build/outputs/apk/debug/app-debug.apk` — FOUND (32,501,879 bytes; `unzip -l` reads 247 entries cleanly; AndroidManifest.xml extractable)

**Commits verified in git log:**
- `29a41b4` — FOUND (`docs(01-04): add FND-08 device-verification handoff runbook`)

**Gradle verification:**
- `./gradlew :app:clean :app:assembleDebug` → BUILD SUCCESSFUL in 53s
- 8 classes*.dex entries verified in APK
- 3 ABI native libs verified in APK

**Tasks executed:**
- Task 1: clean + assembleDebug ✅
- Task 2: HANDOFF.md write + grep-verification ✅
- Task 3: intentionally NOT executed (checkpoint for user)

---
*Phase: 01-foundation-skeleton*
*Status: checkpoint-pending (Task 3 awaits user verification)*
*Paused: 2026-04-18T17:37:03Z*
