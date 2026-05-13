---
phase: 07
plan: 06
subsystem: docs
tags: [phase-7, handoff, runbook, cross-oem, device-matrix, prf-01, prf-02, prf-03, prf-04, prf-05, d-19, d-20c, d-15, d-21]
status: complete
created: 2026-05-13
completed: 2026-05-13
duration_seconds: 258
duration_human: ~4m 18s
wave: 4
autonomous: true
depends_on: [07-05]
requirements: [PRF-05]
dependency_graph:
  requires:
    - .planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-HANDOFF.md
    - .planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-CHECKPOINT.md
    - .planning/phases/07-performance-device-matrix/07-CONTEXT.md
    - .planning/phases/07-performance-device-matrix/07-RESEARCH.md
    - .planning/phases/07-performance-device-matrix/07-PERF-REPORT.md
    - scripts/verify-audio-sync.sh
  provides:
    - .planning/phases/07-performance-device-matrix/07-HANDOFF.md  # Device runbook for Phase 7 cross-OEM verification
  affects:
    - .planning/phases/07-performance-device-matrix/07-07-PLAN.md  # Plan 07-07 device executor consumes this runbook
tech-stack:
  added: []   # Pure documentation plan — no code, no deps
  patterns:
    - "Handoff-mirrors-prior-phase: 07-HANDOFF.md structurally mirrors 06-HANDOFF.md (known-good Phase 6 format) with Phase 7 hard-gate substitutions"
    - "Bilingual runbook pattern: Vietnamese hints land on every user-facing decision point (matches 06-HANDOFF precedent + memory user_profile.md)"
    - "Soft-gate carry pattern: Step 11 (8 items per D-21) preserves Phase 4+5 deferred UAT verbatim from prior phase handoff — soft-gate inheritance keeps the verification surface monotonic across phases"
key-files:
  created:
    - .planning/phases/07-performance-device-matrix/07-HANDOFF.md
  modified: []
decisions: []
metrics:
  duration_seconds: 258
  tasks_completed: 1
  files_modified: 1
  commits: 1
---

# Phase 7 Plan 06: Cross-OEM Device Runbook Authoring Summary

Author `07-HANDOFF.md` — the Phase 7 cross-OEM device matrix runbook covering 5 hard gates (PRF-01..05), 3 Phase-7-only verifications (D-19 / D-20c / D-15), and 8 soft gates (Phase 4+5 deferred UAT per D-21), with Vietnamese bilingual hints, mirroring the Phase 6 handoff format that delivered 13/13 hard gates GREEN.

## What Got Built

A single 38 KB documentation deliverable: `.planning/phases/07-performance-device-matrix/07-HANDOFF.md`.

The runbook is the script Plan 07-07's device-checkpoint executor will follow on Xiaomi 13T (primary OEM per D-12) + a best-effort secondary OEM (Samsung A-series, Pixel A-series, or Firebase Test Lab opt-in per D-13 / D-14). No code changes; no test impact; the existing 197 / 0 ignored / 0 failures suite + 9 D-32 grep-asserts + ~20 MB release APK envelope all carry forward unchanged.

### Section inventory

The runbook ships 18 logical sections (10 Step headings + 8 supporting blocks) per the plan's `<interfaces>` contract:

1. Title + device + APK references + Vietnamese summary block
2. Pre-flight (USB debug, MIUI settings, adb path, ffprobe, apkanalyzer, pm clear, stayon usb, pm grant for CAMERA + RECORD_AUDIO + POST_NOTIFICATIONS, bc fallback) — 14 numbered items including Vietnamese hint on debug-vs-release APK roles
3. What you are verifying — three tables: 6-row hard gates, 3-row Phase-7-only verification, 8-row soft gates
4. Known expected findings (14 bullets: WebP renders identical to PNG, JankStats cold-start jank is normal, debug-only Perf log emission, MIUI install blocker, etc.)
5. **Step 0** — APK + device connectivity (`adb devices` + `ffprobe -version` + release APK install + logcat capture)
6. **Step 1** — PRF-04 release APK ≤ 40 MB (`du -h`)
7. **Step 2** — PRF-04 / D-24 9 D-32 grep-asserts survive R8 (`apkanalyzer dex packages` + 9 grep patterns)
8. **Step 3** — PRF-01 + PRF-02 measurement (switch to debug APK; 30s Face Filter session; awk aggregation for median fps + median/p95 detect latency)
9. **Step 4** — PRF-03 60s video audio drift via `./scripts/verify-audio-sync.sh`
10. **Step 5** — D-19 pre-warmed thermal stress (5×60s warmup + 1 measurement session)
11. **Step 6** — D-20c LeakCanary LAUNCHER disabled (`pm dump` + `monkey -c LAUNCHER` + `dumpsys window`)
12. **Step 7** — D-15 reference APK comparison (`install` + `install-multiple` fallback; `INSTALL_FAILED_MISSING_SPLIT` → DEFERRED)
13. **Step 8** — PRF-05 secondary OEM smoke test (3 strategies: user-sourced device / Firebase Test Lab 30 min/day / N/A per D-13 conditional)
14. **Step 11** — 8 soft gates (11a–11h verbatim from 06-HANDOFF.md Step 11 per D-21)
15. Logcat filter cheatsheet (Phase 7 perf-specific additions on top of Phase 1-6 tags)
16. OEM Quirks placeholder per device (Xiaomi 13T / Samsung / Pixel)
17. Final sign-off table (1 row per Step + soft-gate summary line)
18. Sign-off protocol + Phase 7 gap-closure path (inline-fix vs `07-gaps-NN-PLAN.md`)

### Structural verification (acceptance criteria)

| Criterion | Required | Actual |
|-----------|----------|--------|
| File exists, > 5 KB | yes | yes — 38 KB |
| `grep -c "^## Step "` | ≥ 9 | **10** (Step 0..8 + Step 11) |
| `grep -c "PRF-0[1-5]"` | ≥ 5 | **46** |
| `grep -c "PASS / FAIL"` | ≥ 8 | **16** |
| `grep -c "Tiếng Việt\|Vietnamese"` | ≥ 5 | **15** |
| Step 11 sub-items (11a–11h) | 8 | **8** |
| D-19 / D-20c / D-15 / D-24 referenced | yes | yes — 24 hits combined |
| Firebase Test Lab "30 min/day" wording (RESEARCH §Alternatives correction) | yes | yes — line 507 verbatim + line 521 quota math |
| Final sign-off table with rows for Step 0..8 + Step 11 | yes | yes — 10-row table |
| 9 D-32 grep-asserts intact | yes | yes — 14 / 3 / 13 / 13 / 1 / 3 / 47 / 1 / 1 (post-Plan-07-04 baseline preserved) |
| 1 atomic commit | yes | yes — `419a19f` |

## How It Was Built

Pure read-then-write workflow. No code, no tests, no deps.

1. **Loaded context (parallel reads):**
   - `07-06-PLAN.md` (the plan itself)
   - `STATE.md` (Position: Plan 07-04 complete, ready for 07-05 + 07-06)
   - `06-HANDOFF.md` full file (606 lines — the known-good Phase 6 handoff to mirror)
   - `06-CHECKPOINT.md` Soft Gates section (D-21 soft-gate verbatim source)
   - `07-CONTEXT.md` (D-12 through D-24 decisions)
   - `07-PERF-REPORT.md` (Plan 07-05 Task 1 scaffold — referenced in Step 3 + Step 4 to avoid re-measurement if Plan 07-05 Task 2 already locked PRF-01..03 numbers)
   - `07-RESEARCH.md` §Alternatives (Firebase Test Lab 30 min/day correction)

2. **Verified preconditions:**
   - Release APK file present at `app/build/outputs/apk/release/app-release.apk` — 20 MB (well under 40 MB cap)
   - 9 D-32 grep-asserts intact in `app/src/main/java/` source tree

3. **Authored 07-HANDOFF.md in one Write call** — 38 KB markdown mirroring 06-HANDOFF.md structure with Phase 7 hard-gate substitutions (PRF-01..05 replace UX-01..09 + SHR-01..04; D-19 / D-20c / D-15 add as Phase-7-only block; Step 11 verbatim from 06-HANDOFF).

4. **Ran the 7 verification queries** specified in the plan's `<verification>` block — all pass.

5. **Single atomic commit** `419a19f`.

## Deviations from Plan

None — plan executed exactly as written.

- All 7 acceptance criteria met or exceeded.
- 9 D-32 grep-asserts verified intact pre-write (post-Plan-07-04 baseline preserved unchanged — this is a documentation-only plan).
- File size 38 KB falls within the rollback `<` 15 KB threshold buffer; Step 11 (soft gates) did not need to split into a separate `07-HANDOFF-SOFT-GATES.md` file.
- No authentication gates encountered (pure local file authoring).
- No architectural decisions surfaced — every Phase 7 add (PRF-04 size method, D-24 R8 survival method, PRF-01 + PRF-02 debug-APK rationale, PRF-03 ffprobe script, D-19 thermal protocol, D-20c LeakCanary disable verification, D-15 reference install best-effort, PRF-05 three-strategy fallback) was pre-specified by CONTEXT D-12 through D-24 + Plan 07-06 `<action>` block.

## What's Wired Now

- `07-HANDOFF.md` ready for Plan 07-07's device-checkpoint executor to consume verbatim
- Plan 07-07's three strategies for PRF-05 (user-sourced secondary OEM / Firebase Test Lab / D-13 conditional N/A) all documented with command-level detail
- Step 4 references `./scripts/verify-audio-sync.sh` (Plan 07-05 Task 1 deliverable) — runbook stays runnable even if Plan 07-05 Task 2 has not yet locked PRF-03 numbers
- Step 2 references `apkanalyzer` (Android SDK build-tools) — pre-flight check #10 ensures the tool is on PATH before runbook execution
- Sign-off protocol points to Plan 07-07 Task 3 close-out actions (07-CHECKPOINT.md authoring, 07-VALIDATION.md nyquist flip, REQUIREMENTS.md PRF-03..05 mark-complete, ROADMAP.md Phase 7 row [x])
- Gap-closure path explicit: trivial → inline-fix + amend SUMMARY (per Phase 5 gaps-01 precedent); non-trivial → `07-gaps-NN-PLAN.md`; environmental → document N/A

## Tests

No tests added / changed / removed — pure documentation plan.

Existing suite: 197 / 0 ignored / 0 failures (carried from Plan 07-04 commit `8607c2d`) — unchanged.

9 D-32 grep-asserts intact pre + post (verified counts: 14 / 3 / 13 / 13 / 1 / 3 / 47 / 1 / 1).

## Files Touched

| File | Change | Why |
|------|--------|-----|
| `.planning/phases/07-performance-device-matrix/07-HANDOFF.md` | created (38 KB) | Phase 7 cross-OEM device runbook for Plan 07-07 |

## Commits

- `419a19f` — `docs(07-06): author Phase 7 cross-OEM device runbook (PRF-01..05 + D-19/D-20c/D-15 + 8 deferred UAT per D-21)` — single atomic doc commit

## Next Plan Hand-off

Plan 07-07 (Wave 5 — Phase 7 close-out) consumes `07-HANDOFF.md` as the BLOCKING device-checkpoint script. Sequence:

1. **Plan 07-05 Task 2** (AWAITING USER — runs in parallel with this plan completion): on-device PRF-01 + PRF-02 + PRF-03 measurement on Xiaomi 13T using `scripts/verify-audio-sync.sh` + the `awk` aggregation pipelines now duplicated in 07-HANDOFF.md Step 3 + Step 4. Result: `07-PERF-REPORT.md` filled in with concrete numbers + GL escalation decision row (Deferred per D-18 if median fps ≥ 24; Triggered per D-17 if < 24).
2. **Plan 07-07** (NEXT): device-checkpoint executor runs 07-HANDOFF.md Steps 0..8 + Step 11 against (a) Xiaomi 13T (primary, must PASS), (b) one of {secondary OEM device / Firebase Test Lab Robo run / N/A per D-13 conditional}. Step 3 + Step 4 reference Plan 07-05 Task 2's numbers if already locked — avoids redundant re-measurement.
3. On hard-gate PASS: Plan 07-07 Task 3 close-out fires (07-CHECKPOINT.md author, 07-VALIDATION.md nyquist flip, ROADMAP.md Phase 7 row [x], REQUIREMENTS.md PRF-03..05 mark-complete via `gsd-tools requirements mark-complete`, STATE.md Position = milestone v1 ready).

## Known Stubs

None — runbook is a measurement script, not a code stub.

The TBD placeholders in `07-PERF-REPORT.md` (PRF-01 / PRF-02 / PRF-03 / D-19 / GL escalation rows) are intentional measurement slots filled by the Plan 07-05 Task 2 device session — they are NOT stubs blocking this plan's completion.

## Threat Flags

None. Pure documentation; no new endpoints, auth paths, file-access surfaces, or schema changes at trust boundaries introduced by this plan. The runbook references existing T-07-01 IDS mitigation evidence (Plan 07-03 commit `494e22d` `strings | grep` verification) without changing it.

## Self-Check: PASSED

- File exists: `.planning/phases/07-performance-device-matrix/07-HANDOFF.md` — FOUND (38 KB)
- Commit exists: `419a19f` — FOUND in `git log`
- Structural verification queries (Step count = 10, PRF-0[1-5] = 46, PASS/FAIL = 16, Vietnamese = 15, Step 11 sub-items = 8) — all passed
- 9 D-32 grep-asserts intact — verified pre + post (counts unchanged: 14 / 3 / 13 / 13 / 1 / 3 / 47 / 1 / 1)
