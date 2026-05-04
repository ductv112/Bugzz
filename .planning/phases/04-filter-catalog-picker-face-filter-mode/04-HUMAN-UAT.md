---
status: partial
phase: 04-filter-catalog-picker-face-filter-mode
source: [04-VERIFICATION.md]
started: 2026-05-04T19:13:00Z
updated: 2026-05-04T19:39:00Z
---

## Current Test

[awaiting opportunistic human testing — soft gates only, NOT blockers for Phase 4 sign-off]

## Tests

### 1. Multi-face 2-person scene (MOD-02 #5)
expected: With a SWARM filter active and a second face in frame (printed photo or second person), bug sprites render on BOTH faces — primary face gets full FaceLandmarkMapper anchor (e.g. NOSE_TIP via contour), secondary face gets bbox-center fallback per D-22. No crash. Soft cap 20 draws/frame respected (verified via logcat `draws=N` field).
result: [pending — no 2nd person available during 2026-05-04 device run; architecture verified via FilterEngineTest multiFace_* synthetic 2-face injection unit tests GREEN]

### 2. FPS subjective smoothness (REN-08 / Step 5 soft gate)
expected: 30-second hold with picker scrolling through filters + active SWARM/FALL filter on face — no visible jank, frame drops, or stutter to naked eye. Formal measured PRF-01 ≥24fps is Phase 7 scope.
result: [pending — automated screenshots showed no obvious jank but user has not done a focused 30s subjective pass]

## Summary

total: 2
passed: 0
issues: 0
pending: 2
skipped: 0
blocked: 0

## Gaps

(none — both items are non-blocker soft gates; Phase 4 closed without them per HANDOFF "Hard gates" definition)

## Notes

- Phase 4 device verification ran 2026-05-04 19:13–19:39 on Xiaomi 13T via orchestrator + adb
- 11/13 HANDOFF hard gates PASS verified inline
- CRAWL visual (originally listed as soft) confirmed PASS via 2-screenshot delta showing bug position progress along face contour
- 1 inline gap fix shipped during run: `04-gaps-01` AssetLoader assetDir API correction (commit `514410c`)
- These 2 deferred items can be checked any time before Phase 7 device matrix run; will surface in `/gsd-progress` and `/gsd-audit-uat`
