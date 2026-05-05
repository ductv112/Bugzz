# Phase 6: UX Polish - Discussion Log

> **Audit trail only.** Decisions in CONTEXT.md.

**Date:** 2026-05-04
**Mode:** Auto-locked recommended defaults per user delegation ("Tiếp" continuing autonomous chain)

## Areas auto-decided (no user-presented gray-area selection — full delegation)

| Area | Auto-decision summary |
|------|------------------------|
| Splash | 1.5s auto-advance, Lottie via PNG-flipbook reuse OR Lottie JSON copy (Claude discretion at impl), conditional first-launch routing via DataStore onboarding_completed key |
| Onboarding | 3-page HorizontalPager, Lottie + text per page, Skip top-end + Next button + page indicator dots, sets onboarding_completed=true on Skip/Get Started |
| Home polish | Phase 4 D-19 layout retained; Settings + Collection nav wired (replacing placeholders) |
| Preview/Result | Photo (Coil) or video (Media3 ExoPlayer); 4 actions Done/Share/Delete/Retake; AlertDialog for Delete |
| Collection | LazyVerticalGrid 3-col, MediaStore query DCIM/Bugzz/, photo+video thumbnails with play-icon overlay, empty state with "Open Camera" CTA |
| Share | ACTION_SEND with content URI + MIME + GRANT_READ_URI_PERMISSION; createChooser; no FileProvider |
| Settings | 4-row stub (Version, Privacy, Rate, About) — placeholder Toasts for non-functional items |
| Architecture | New packages: ui/{splash,onboarding,preview,collection,settings,components}; data/CollectionRepository; FilterPrefsRepository extension for onboarding_completed |

## All 34 D-decisions auto-locked to Recommended (D-01..D-34 in 06-CONTEXT.md)

Highlights:
- D-22 Lottie 6.7.1 + Media3 1.4.1 added to libs.versions.toml
- D-23 Single DataStore Preferences instance — extends FilterPrefsRepository (no new repo)
- D-26 New ui/components/ for shared composables (DeleteConfirmDialog, EmptyStateColumn, LottiePlayer)
- D-32 All 9 Phase 3+4+5 fix preservation grep-asserts carry forward
- D-33 Phase 4+5 deferred UAT items folded into 06-HANDOFF as opportunistic close
- D-34 Plan budget note — planner may need 8-10 plans / 5-6 waves; split to 6a/6b if budget exceeded (Phase 6 kept whole for now)

## Claude's Discretion items

12 items left to executor judgement at impl time — see CONTEXT §Claude's Discretion.

## Deferred Ideas

Captured in CONTEXT §deferred. Summary: multi-select delete, album grouping, filter quality settings, debug overlay toggle, music overlay, watermark, real privacy/rate URLs, localization, trending feed, analytics, onboarding A/B.
