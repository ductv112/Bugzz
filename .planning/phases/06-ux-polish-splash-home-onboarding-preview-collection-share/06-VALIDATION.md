---
phase: 06
slug: ux-polish-splash-home-onboarding-preview-collection-share
status: draft
nyquist_compliant: false
wave_0_complete: true
created: 2026-05-04
---

# Phase 06 — Validation Strategy

> Per-phase validation contract. Extends Phase 5 harness with 8 new test files. Phase 6 is the LARGEST UX phase — 5 production screens + Lottie + Media3 + Share intent.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Mockito 5.11 + Mockito-Kotlin + Robolectric 4.13 + Turbine + DataStore test factory (all from Phase 5) + lottie-compose (no test impact) + Media3 (no test impact) |
| **Config file** | `gradle/libs.versions.toml` + `app/build.gradle.kts` (+`lottie-compose 6.7.1`, `+media3-ui 1.4.1`, `+media3-exoplayer 1.4.1`) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` (with `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`) |
| **Full suite command** | `./gradlew :app:testDebugUnitTest :app:assembleDebug` |
| **Estimated runtime** | ~90 seconds (Phase 5 baseline 75s + 8 new test classes) |

---

## Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest -x lintDebug`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- **Before `/gsd-verify-work`:** Full suite green AND clean APK AND manual device acceptance per 06-HANDOFF.md
- **Max feedback latency:** 100 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Test Type | Automated Command | Status |
|---------|------|------|-------------|------------|-----------|-------------------|--------|
| 06-01-NN | 01 (Wave 0 scaffolds) | 0 | UX-01..09, SHR-01..04 | T-06-01..06 | unit scaffolds RED | `./gradlew :app:testDebugUnitTest` | ✅ Wave 0 RED scaffolds GREEN |
| 06-02-NN | 02 (deps + DataStore extension + Lottie asset copy) | 1 | (infra) | T-06-04, T-06-05 | unit | `./gradlew :app:testDebugUnitTest --tests "*FilterPrefsRepositoryTest*onboarding*"` | ⬜ |
| 06-03-NN | 03 (SplashScreen + Onboarding + Routes nav graph) | 2 | UX-01, UX-02 | T-06-04 | unit | `./gradlew :app:testDebugUnitTest --tests "*SplashViewModelTest* *OnboardingViewModelTest* *OnboardingPagerStateTest*"` | ⬜ |
| 06-04-NN | 04 (Routes PreviewRoute breaking change + PreviewScreen photo+video) | 3 | UX-04 | T-06-03 (ExoPlayer leak), T-06-01 | unit + manual | `./gradlew :app:testDebugUnitTest --tests "*PreviewViewModelTest*"` + 06-HANDOFF | ⬜ |
| 06-05-NN | 05 (CollectionRepository + CollectionScreen + EmptyStateColumn) | 4 | UX-05, UX-06, UX-07 | T-06-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*CollectionRepositoryTest* *CollectionViewModelTest*"` | ⬜ |
| 06-06-NN | 06 (DeleteConfirmDialog + Share intent + HomeScreen polish) | 5 | UX-08, UX-09, SHR-01..04 | T-06-01, T-06-06 | unit | `./gradlew :app:testDebugUnitTest --tests "*DeleteConfirmDialogTest* *ShareIntentBuilderTest*"` | ⬜ |
| 06-07-NN | 07 (Settings + StubScreens delete + nav graph close-out) | 5 | UX-09 | T-06-06 | unit + manual | `./gradlew :app:testDebugUnitTest` + 06-HANDOFF | ⬜ |
| 06-08-NN | 08 (Clean build + 06-HANDOFF + device checkpoint + post-PASS close-out) | 6 | All Phase 6 reqs | T-06-01..06 | manual (device) | 06-HANDOFF.md sign-off + Phase 4+5 deferred UAT bonus | ⬜ |

### Per-Requirement Test Specification

| Req ID | Behavior | Test Type | Command | File |
|--------|----------|-----------|---------|------|
| UX-01 | Splash routes to Onboarding (first launch) OR Home (subsequent) | unit | `*SplashViewModelTest*` | ❌ W0 |
| UX-02 | Onboarding completion sets DataStore flag; HorizontalPager nav (Skip+Next+GetStarted) | unit | `*OnboardingViewModelTest* *OnboardingPagerStateTest*` | ❌ W0 |
| UX-03 | HomeScreen settings/collection nav | manual | 06-HANDOFF | — |
| UX-04 | PreviewScreen renders Image (photo) or PlayerView (video); deleteArtifact MediaStore.delete | unit | `*PreviewViewModelTest*` | ❌ W0 |
| UX-05 | CollectionRepository MediaStore query DCIM/Bugzz/ filtered to image/jpeg + video/mp4 | unit | `*CollectionRepositoryTest*` | ❌ W0 |
| UX-06 | Tap collection item → PreviewRoute(uri) | manual | 06-HANDOFF | — |
| UX-07 | Empty state when no MediaItems | unit | `*CollectionViewModelTest*emptyState*` | ❌ W0 |
| UX-08 | DeleteConfirmDialog Cancel/Confirm callbacks | unit | `*DeleteConfirmDialogTest*` | ❌ W0 |
| UX-09 | Settings 4-row stub renders + back nav | manual | 06-HANDOFF | — |
| SHR-01 | buildShareIntent → Intent.ACTION_SEND with EXTRA_STREAM | unit | `*ShareIntentBuilderTest*` | ❌ W0 |
| SHR-02 | buildShareIntent.type = mime from MediaItem | unit | same | ❌ W0 |
| SHR-03 | Intent.createChooser wrapped | unit | same | ❌ W0 |
| SHR-04 | Shared overlay intact (architectural — Phase 3 OverlayEffect bake) | manual | 06-HANDOFF playback | — |

---

## Wave 0 Requirements

8 new test files:
- [x] `app/src/test/java/com/bugzz/filter/camera/ui/splash/SplashViewModelTest.kt` (UX-01) — 3 @Ignored
- [x] `app/src/test/java/com/bugzz/filter/camera/ui/onboarding/OnboardingViewModelTest.kt` (UX-02) — 2 @Ignored
- [x] `app/src/test/java/com/bugzz/filter/camera/ui/onboarding/OnboardingPagerStateTest.kt` (UX-02 pager nav) — 3 @Ignored
- [x] `app/src/test/java/com/bugzz/filter/camera/ui/preview/PreviewViewModelTest.kt` (UX-04) — 4 @Ignored
- [x] `app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt` (UX-05/07) — 4 @Ignored
- [x] `app/src/test/java/com/bugzz/filter/camera/ui/collection/CollectionViewModelTest.kt` (UX-07) — 2 @Ignored
- [x] `app/src/test/java/com/bugzz/filter/camera/ui/components/DeleteConfirmDialogTest.kt` (UX-08) — 2 @Ignored
- [x] `app/src/test/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilderTest.kt` (SHR-01..03) — 4 @Ignored

Plus 1 EXTENSION:
- [x] **EXTEND** `app/src/test/java/com/bugzz/filter/camera/data/FilterPrefsRepositoryTest.kt` with `onboarding_completed` read/write tests (Phase 6 D-23 extension) — 3 @Ignored added; existing 4 GREEN tests untouched

**New deps:** `lottie-compose 6.7.1`, `media3-ui 1.4.1`, `media3-exoplayer 1.4.1` (no new test deps).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| First-launch flow | UX-01 + UX-02 | Real Lottie playback + DataStore + nav transitions | 06-HANDOFF: fresh install → Splash 1.5s → Onboarding 3-page swipe → Get Started → Home; force-stop + relaunch → Splash 1.5s → Home direct |
| Settings stub interactions | UX-09 | Toast / Intent.ACTION_VIEW behaviors | 06-HANDOFF: tap Privacy Policy → Toast "Coming soon"; tap Rate → Toast; verify version string non-empty |
| Video preview playback | UX-04 video case | ExoPlayer real video decode | 06-HANDOFF: capture video Phase 5 → Preview navigates → ExoPlayer auto-plays loop; pause on screen leave |
| Photo preview | UX-04 photo case | Coil real bitmap decode | 06-HANDOFF: capture photo → Preview navigates → AsyncImage shows photo at correct orientation |
| Delete confirmation | UX-08 | Real AlertDialog + MediaStore.delete | 06-HANDOFF: Preview → Delete → AlertDialog → Cancel preserves; Delete confirms → file removed from DCIM/Bugzz/ |
| Collection grid rendering | UX-05 | Real MediaStore query + Coil video thumbnail decode | 06-HANDOFF: open Collection → 3-col grid renders all Phase 5 captures (JPEGs + MP4s with play overlay); empty state if cleared |
| Share intent target picker | SHR-01..03 | Real Android share sheet + installed targets | 06-HANDOFF: Preview → Share → "Share via" picker shows WhatsApp/Instagram/etc.; pick one → app receives URI + opens correctly |
| Shared content overlay intact | SHR-04 | Cross-app artifact verification | 06-HANDOFF: share to WhatsApp self-chat → confirm photo/video has bug overlay baked in |
| Phase 4 + 5 deferred UAT (folded bonus) | various | Carry over from prior phases | 06-HANDOFF includes: multi-face 2-person + fps subjective + pinch/rotate + sticker survival + audio sync + fresh-install RECORD_AUDIO + thermal stress + 05-gaps-02 visual polish |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity preserved
- [ ] Wave 0 covers all 8 new + 1 EXTEND test files
- [ ] No watch-mode flags
- [ ] Feedback latency < 100s
- [ ] `nyquist_compliant: true` flipped post-PASS via Plan 06-08 Task 4

**Approval:** pending
