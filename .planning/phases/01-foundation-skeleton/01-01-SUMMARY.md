---
phase: 01-foundation-skeleton
plan: 01
subsystem: infra
tags: [gradle, gradle-wrapper, version-catalog, kotlin, agp, ksp, compose-bom, hilt, android-build]

# Dependency graph
requires:
  - phase: none (Wave 0 — project bootstrap)
    provides: greenfield project with .planning/, .gitignore, reference/
provides:
  - Working Gradle 8.13 wrapper invokable as ./gradlew from project root
  - Version catalog gradle/libs.versions.toml as single source of truth for all dependency versions
  - Root settings.gradle.kts with pluginManagement + FAIL_ON_PROJECT_REPOS + include(:app)
  - Root build.gradle.kts with 6 plugin aliases (application, kotlin-android, kotlin-compose, kotlin-serialization, ksp, hilt-android) apply false
  - gradle.properties with jbr JDK 21 pointer, AndroidX flag, compileSdk=36 escape hatch pre-armed
  - local.properties with Windows SDK path (gitignored)
  - Gradle 8.13 distribution cached at C:/Users/Admin/.gradle/wrapper/dists/gradle-8.13-bin/ for instant subsequent builds
affects: [01-02 app-module, 01-03 hilt-navigation-stubs, 01-04 debug-tooling-tests, all subsequent phases]

# Tech tracking
tech-stack:
  added:
    - Gradle 8.13 (build engine)
    - AGP 8.9.1 (pinned, not yet applied)
    - Kotlin 2.1.21 (pinned, not yet applied)
    - KSP 2.1.21-2.0.2 (pinned, not yet applied — Rule 1 corrected from 2.1.21-1.0.32)
    - Compose BOM 2026.04.00 (pinned)
    - Hilt 2.57 (pinned)
    - Navigation Compose 2.8.9 (pinned)
    - Kotlinx Serialization 1.8.0 (pinned)
    - LeakCanary 2.14 (pinned)
  patterns:
    - "Version catalog: all library + plugin versions pinned in gradle/libs.versions.toml; no version strings in build.gradle.kts files"
    - "Compose BOM: compose artifacts list declared WITHOUT version.ref (only the BOM entry has version.ref); Gradle platform() aligns transitively in app module"
    - "FAIL_ON_PROJECT_REPOS repositoriesMode: dependency repositories declared centrally in settings.gradle.kts, project-level repositories {} blocks rejected"
    - "org.gradle.java.home in gradle.properties: JDK location committed to repo so team members do not need JAVA_HOME set in shell env"
    - "local.properties gitignored: sdk.dir is machine-specific and never committed"
    - "Windows .properties files: every backslash doubled (C:\\\\Program Files\\\\... form)"

key-files:
  created:
    - gradle/libs.versions.toml
    - settings.gradle.kts
    - build.gradle.kts
    - gradle.properties
    - local.properties (gitignored)
    - gradlew
    - gradlew.bat
    - gradle/wrapper/gradle-wrapper.properties
    - gradle/wrapper/gradle-wrapper.jar
  modified: []

key-decisions:
  - "Gradle wrapper version 8.13 — minimum supported by AGP 8.9.x, pinned with distributionSha256Sum for supply-chain verification"
  - "Bootstrap strategy: invoked user's pre-installed Gradle 9.3.1 (C:/Users/Admin/.gradle/wrapper/dists/gradle-9.3.1-bin/...) with JAVA_HOME=<jbr> to generate the wrapper — no curl download needed"
  - "KSP version corrected from 2.1.21-1.0.32 (does not exist on public repos) to 2.1.21-2.0.2 (latest KSP2 stable for Kotlin 2.1.21)"
  - "android.suppressUnsupportedCompileSdk=36 pre-armed in gradle.properties so downstream plans' compileSdk=35 does not fail when API 35 platform is not yet installed"

patterns-established:
  - "Plan verification: after each config change, run ./gradlew help to validate end-to-end toolchain before declaring task done"
  - "Bootstrap deviation handling: when settings.gradle.kts references an :app subproject before the subproject exists, create an empty app/ directory to unblock Gradle's project configuration"
  - "Git hygiene: gradle-wrapper.jar, gradlew, gradlew.bat are COMMITTED (part of supply-chain trust boundary); .gradle/, build/, local.properties are GITIGNORED"

requirements-completed: [FND-01, FND-02]

# Metrics
duration: 18min
completed: 2026-04-18
---

# Phase 1 Plan 1: Gradle Toolchain Bootstrap Summary

**Gradle 8.13 wrapper + libs.versions.toml version catalog pinning AGP 8.9.1 / Kotlin 2.1.21 / KSP 2.1.21-2.0.2 / Compose BOM 2026.04.00 / Hilt 2.57, validated end-to-end via ./gradlew help on Windows+jbr JDK 21**

## Performance

- **Duration:** 18 min
- **Started:** 2026-04-18T17:05:00Z
- **Completed:** 2026-04-18T17:23:00Z
- **Tasks:** 2
- **Files created:** 9 (5 hand-written config + 4 wrapper-generated, one modified post-generation)

## Accomplishments

- `./gradlew help` succeeds end-to-end (BUILD SUCCESSFUL in 1s on cached configuration) using Gradle 8.13 on JDK 21.0.10 (Android Studio bundled jbr).
- Version catalog (`gradle/libs.versions.toml`) is the single source of truth for all current + future dependency versions, with Compose BOM pattern pre-wired.
- All 6 Gradle plugin aliases (application, kotlin-android, kotlin-compose, kotlin-serialization, ksp, hilt-android) resolve from Google Maven + Maven Central + Gradle Plugin Portal.
- Gradle 8.13 distribution fetched (SHA-256 pinned at `20f1b117...`) and cached at `C:/Users/Admin/.gradle/wrapper/dists/gradle-8.13-bin/` — subsequent runs on this machine are instant.

## Task Commits

Each task was committed atomically:

1. **Task 1: Write version catalog + root config files** — `98eaa9c` (feat)
2. **Task 2: Bootstrap Gradle wrapper via pre-installed Gradle 9.3.1** — `0bfff7a` (chore)

_Note: Task 2 also bundled a Rule 1 bug fix (KSP version correction) since the fix was necessary for wrapper's smoke test (`./gradlew help`) to succeed, and the fix modified `gradle/libs.versions.toml` from Task 1._

## Files Created/Modified

- `gradle/libs.versions.toml` — version catalog: all pinned versions + library + plugin aliases (created in Task 1; KSP version corrected in Task 2)
- `settings.gradle.kts` — pluginManagement (google, mavenCentral, gradlePluginPortal) + dependencyResolutionManagement (FAIL_ON_PROJECT_REPOS, google, mavenCentral) + `rootProject.name = "Bugzz"` + `include(":app")`
- `build.gradle.kts` — top-level plugins block with 6 plugin aliases `apply false`
- `gradle.properties` — org.gradle.java.home (bundled jbr), JVM args (-Xmx4g), parallel/caching/configuration-cache enabled, android.useAndroidX, android.suppressUnsupportedCompileSdk=36, android.nonTransitiveRClass
- `local.properties` — sdk.dir (gitignored, not staged)
- `gradlew` — bash launcher (8618 bytes, executable)
- `gradlew.bat` — Windows launcher (2896 bytes)
- `gradle/wrapper/gradle-wrapper.properties` — distributionUrl=gradle-8.13-bin.zip + distributionSha256Sum pinned
- `gradle/wrapper/gradle-wrapper.jar` — bootstrap binary (46175 bytes, within 40-80 KB sanity range)

## Decisions Made

- **Wrapper bootstrap via pre-installed Gradle 9.3.1 (not curl)** — user already had Gradle 9.3.1 cached; invoking `gradle wrapper --gradle-version 8.13` generates all 4 wrapper files atomically with correct shell+batch scripts, avoiding the ~300 lines of hand-authored launcher scripts the curl fallback would require.
- **Empty `app/` directory at bootstrap time** — `settings.gradle.kts` declares `include(":app")`, so Gradle refuses to configure until `app/` exists. Created empty directory; Plan 02 will fill it with real sources. (This is a deviation — see below.)
- **Temporary stub `build.gradle.kts` / `settings.gradle.kts` during wrapper generation** — Gradle 9.3.1's `wrapper` task evaluates the project model before executing, which means it tries to resolve the 6 real plugins from the version catalog (and needs `app/` to exist). Temporarily swapped in minimal stubs (`rootProject.name = "Bugzz-bootstrap"`), ran `gradle wrapper`, then restored the real files. Result is byte-identical to committing the real config first.
- **KSP version `2.1.21-2.0.2` (not `2.1.21-1.0.32` as research + plan stated)** — the latter does not exist on Maven Central or Gradle Plugin Portal. For Kotlin 2.1.21, the only published KSP plugin versions are RC releases + `2.1.21-2.0.1` + `2.1.21-2.0.2` (all under the KSP2 version line). Research's citation was aspirational — the "1.0.x" line stopped shipping at Kotlin 2.1.20. Picked `2.1.21-2.0.2` as the latest stable.
- **Left `app/` directory empty and untracked** — git does not track empty directories, and Plan 02 will populate `app/` with the real module sources + manifest. No `.gitkeep` needed since Plan 02 commits will establish the directory naturally.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] KSP version `2.1.21-1.0.32` does not exist on public repositories**
- **Found during:** Task 2 (running `./gradlew help` after wrapper bootstrap)
- **Issue:** Research and plan both specified `ksp = "2.1.21-1.0.32"` as the exact version. However, Maven Central's `com.google.devtools.ksp.gradle.plugin/maven-metadata.xml` shows the last `1.0.x` version is `2.1.20-1.0.32` (pinned to Kotlin 2.1.20, not 2.1.21). For Kotlin 2.1.21, KSP switched to the `2.0.x` version line: only `2.1.21-RC-2.0.0`, `2.1.21-RC2-2.0.1`, `2.1.21-2.0.1`, `2.1.21-2.0.2` exist. The pin `2.1.21-1.0.32` is impossible — it is the cross-product of Kotlin 2.1.21 with the now-discontinued KSP1 patch number.
- **Fix:** Updated `gradle/libs.versions.toml` line 6: `ksp = "2.1.21-2.0.2"` (latest stable KSP2 for Kotlin 2.1.21 per Maven Central metadata).
- **Files modified:** `gradle/libs.versions.toml`
- **Verification:** After the fix, `./gradlew help` returned `BUILD SUCCESSFUL`; plugin resolution succeeded for all 6 aliased plugins.
- **Committed in:** `0bfff7a` (Task 2 commit, which bundled wrapper bootstrap + KSP fix since the fix was a precondition for Task 2's verification step).

**2. [Rule 3 - Blocking] `app/` directory missing at wrapper bootstrap time**
- **Found during:** Task 2 (first `gradle wrapper` invocation)
- **Issue:** `settings.gradle.kts` declares `include(":app")`, but Plan 02 (not this plan) creates the `app/` directory. Gradle refused to execute the `wrapper` task with `Configuring project ':app' without an existing directory is not allowed`.
- **Fix:** Created empty `app/` directory (`mkdir -p app`). No files added to it — Plan 02 will populate.
- **Files modified:** created `app/` (empty directory; git does not track it)
- **Verification:** `gradle wrapper` progressed past the project-configuration check. Empty `app/` causes no harm to `./gradlew help` since there is no `app/build.gradle.kts` yet, and Gradle treats a buildfile-less subproject as a no-op.
- **Committed in:** Not separately committed (empty directory, untracked by git).

**3. [Rule 3 - Blocking] `gradle wrapper` task evaluated plugin block before running**
- **Found during:** Task 2 (second `gradle wrapper` invocation after `app/` fix)
- **Issue:** Gradle 9.3.1's `wrapper` task, unlike older versions, evaluates the full project model at configuration time, which required resolving all 6 plugins from the version catalog. Plugin resolution failed with the original (bogus) KSP version, blocking wrapper generation.
- **Fix:** Temporarily renamed `build.gradle.kts` → `build.gradle.kts.tmp` and `settings.gradle.kts` → `settings.gradle.kts.tmp`, wrote minimal stubs (`rootProject.name = "Bugzz-bootstrap"` / empty comment), ran `gradle wrapper`, then restored the real files and deleted stubs. Result is byte-identical to the original Task 1 files.
- **Files modified:** transiently — no net change to committed files
- **Verification:** `gradle wrapper` produced the 4 expected files (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`); real Task 1 files restored byte-for-byte.
- **Committed in:** N/A (no file changes committed)

**4. [Rule 2 - Missing Critical] `distributionSha256Sum` not included in Gradle-generated wrapper properties**
- **Found during:** Task 2 (post wrapper-generation verification)
- **Issue:** Gradle 9.3.1's `wrapper` task did NOT include the `distributionSha256Sum=...` line that the plan required for supply-chain verification. Without this line, Gradle will download the distribution ZIP without checksum validation — a security regression vs the plan's acceptance criteria.
- **Fix:** Rewrote `gradle/wrapper/gradle-wrapper.properties` to include `distributionSha256Sum=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78` (Gradle 8.13 distribution SHA-256 from the plan's research, cited from gradle.org/release-checksums).
- **Files modified:** `gradle/wrapper/gradle-wrapper.properties`
- **Verification:** `grep -q '20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78' gradle/wrapper/gradle-wrapper.properties` → PASS. Gradle 8.13 download completed successfully (sha256 validated implicitly by Gradle's wrapper-validator during first fetch).
- **Committed in:** `0bfff7a` (Task 2 commit)

**5. [Context — JAVA_HOME not set in shell]** (not strictly a deviation, but worth documenting for continuity)
- Both the pre-installed Gradle 9.3.1 launcher (used for bootstrap) and `./gradlew` itself require a JVM to start. The plan's `org.gradle.java.home` in `gradle.properties` tells the Gradle DAEMON which JDK to use for compilation, but the launcher script needs `JAVA_HOME` or `JAVA_OPTS` set before it can even parse gradle.properties. In this execution environment, `JAVA_HOME` is not set system-wide. Workaround: prefix all Gradle invocations with `export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"` for the duration of the command. This is NOT a committed change — it is an executor-shell hygiene detail. Downstream plans' agents must replicate this or the user must set `JAVA_HOME` in their shell profile (Git Bash `~/.bashrc`) before running `./gradlew` manually.

---

**Total deviations:** 4 auto-fixed (1 bug, 2 blocking, 1 missing critical) + 1 environmental note
**Impact on plan:** All auto-fixes were necessary for the plan's own success criterion (`./gradlew help` succeeds) to be satisfied. No scope creep. The KSP version correction is the most material — it propagates forward: Plan 02 and all downstream plans must use `2.1.21-2.0.2` (already reflected in the committed `libs.versions.toml`). The must_have "KSP is exactly `2.1.21-1.0.32`" from the plan frontmatter is UNSATISFIABLE and has been overridden to `2.1.21-2.0.2` out of necessity.

## Issues Encountered

- **First `./gradlew help` attempt downloaded Gradle 8.13 (~120MB zip) over the network** — expected per plan; took ~1 min including unpack. Subsequent invocations are instant (`BUILD SUCCESSFUL in 1s`).
- **No `.gitattributes` was generated by Gradle's `wrapper` task** — plan mentioned to delete it if present; absent here so no action needed.
- **`gradlew` file had CRLF line-ending warnings on staging** — expected on Windows+Git Bash; Git's `core.autocrlf` default handles this. Committed as-is.
- **Configuration cache reports `[Incubating]` problems report** — informational only, not a build failure. Safe to ignore in Phase 1.

## User Setup Required

None — all toolchain bits are self-contained within the repo + cached `~/.gradle/` directories that the user already has populated. Downstream plans' executors should export `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"` before `./gradlew` invocations unless the user sets it in `~/.bashrc`.

## Next Phase Readiness

- **Plan 02 (app module + AndroidManifest + resources + icon + build.gradle.kts)** is UNBLOCKED. `./gradlew help` works, version catalog is populated, plugin portal resolution verified. Plan 02 can now:
  1. Add `app/build.gradle.kts` applying the 6 aliased plugins and declaring `android { ... compileSdk = 35 ... }`.
  2. Wire dependencies via `libs.androidx.core.ktx`, `platform(libs.androidx.compose.bom)`, `libs.androidx.compose.ui`, etc.
  3. Run `./gradlew :app:assembleDebug` — should produce a debug APK once the manifest + stub Activity are in.
- **Plan 03 (Hilt + Navigation stubs)** and **Plan 04 (StrictMode + LeakCanary + tests)** are unblocked but depend on Plan 02 completing first.

### Known constraints forwarded

- If user has not installed API 35 by the time Plan 02 runs, `android.suppressUnsupportedCompileSdk=36` is pre-armed — Plan 02 can set `compileSdk = 36` as the fallback without reconfiguring gradle.properties.
- KSP pin is now `2.1.21-2.0.2`; Plan 02's `app/build.gradle.kts` should use `alias(libs.plugins.ksp)` — the version flows transitively from `libs.versions.toml` and requires no edits to the app module config.

## Self-Check: PASSED

**Files verified on disk:**
- `d:/ClaudeProject/appmobile/Bugzz/gradle/libs.versions.toml` — FOUND (72 lines, versions match, KSP=2.1.21-2.0.2)
- `d:/ClaudeProject/appmobile/Bugzz/settings.gradle.kts` — FOUND
- `d:/ClaudeProject/appmobile/Bugzz/build.gradle.kts` — FOUND (6 `apply false` lines)
- `d:/ClaudeProject/appmobile/Bugzz/gradle.properties` — FOUND (contains `android.suppressUnsupportedCompileSdk=36`)
- `d:/ClaudeProject/appmobile/Bugzz/local.properties` — FOUND (gitignored, not staged)
- `d:/ClaudeProject/appmobile/Bugzz/gradlew` — FOUND (8618 bytes, executable bit set)
- `d:/ClaudeProject/appmobile/Bugzz/gradlew.bat` — FOUND (2896 bytes)
- `d:/ClaudeProject/appmobile/Bugzz/gradle/wrapper/gradle-wrapper.properties` — FOUND (contains `gradle-8.13-bin.zip` + sha256)
- `d:/ClaudeProject/appmobile/Bugzz/gradle/wrapper/gradle-wrapper.jar` — FOUND (46175 bytes)

**Commits verified in git log:**
- `98eaa9c` — FOUND ("feat(01-01): add Gradle version catalog and root build config")
- `0bfff7a` — FOUND ("chore(01-01): bootstrap Gradle 8.13 wrapper and fix KSP version")

**End-to-end verification (exit 0 = PASS):**
- `./gradlew help` → `BUILD SUCCESSFUL` (reuses configuration cache: 1s)
- `./gradlew --version` → `Gradle 8.13` on `Launcher JVM: 21.0.10 (JetBrains s.r.o.)` and `Daemon JVM: C:\Program Files\Android\Android Studio\jbr (from org.gradle.java.home)`

---

### Shell output of `./gradlew --version` (captured verbatim)

```
------------------------------------------------------------
Gradle 8.13
------------------------------------------------------------

Build time:    2025-02-25 09:22:14 UTC
Revision:      073314332697ba45c16c0a0ce1891fa6794179ff

Kotlin:        2.0.21
Groovy:        3.0.22
Ant:           Apache Ant(TM) version 1.10.15 compiled on August 25 2024
Launcher JVM:  21.0.10 (JetBrains s.r.o. 21.0.10+-14961533-b1163.108)
Daemon JVM:    C:\Program Files\Android\Android Studio\jbr (from org.gradle.java.home)
OS:            Windows 11 10.0 amd64
```

### First-sync download stats

- Gradle 8.13 distribution ZIP: ~120 MB (downloaded in ~50s from services.gradle.org)
- Target cache: `C:/Users/Admin/.gradle/wrapper/dists/gradle-8.13-bin/`
- Distribution SHA-256 validated on download (plan's `distributionSha256Sum=20f1b117...`)

---
*Phase: 01-foundation-skeleton*
*Completed: 2026-04-18*
