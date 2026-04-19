# Phase 3: First Filter End-to-End + Photo Capture - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-19
**Phase:** 03 — First Filter End-to-End + Photo Capture
**Areas discussed:** Filter #1 spec + sprite source, Shutter UX (photo capture), BboxIouTracker + multi-face policy, Front-camera mirror convention (CAP-04)

---

## Area 1: Filter #1 Spec + Sprite Source

### Q1.1 — Which bug + anchor + behavior for first production filter?

| Option | Description | Selected |
|--------|-------------|----------|
| Ant on nose, STATIC (Recommended) | Lowest-risk; matches ROADMAP §Phase 3 example; NOSE_TIP anchor easy to verify visually; validates REN-01/03/04/05/06/07 without CRAWL complexity | ✓ |
| Spider on forehead, CRAWL | High-signal but risky: CRAWL path walking logic adds Phase 3 scope; deferred by CONTEXT.md D-04 | |
| Cockroach on cheek, STATIC | Equivalent risk level; LEFT_CHEEK anchor tests different contour population | |

**User's choice:** Ant on nose, STATIC (Recommended)
**Notes:** Locked as D-01. Second filter for REN-07 swap validation becomes spider-on-forehead STATIC in Q1.3.

---

### Q1.2 — Sprite source for ant-on-nose filter?

| Option | Description | Selected |
|--------|-------------|----------|
| Extract 1 bug from reference APK (Recommended) | `apktool d` on reference APK, extract ant frames to `assets/sprites/ant/`; Phase 4 extracts catalog remainder | ✓ |
| Extract full 15-25 catalog now | Front-load asset work; risks blocking Phase 3 if asset format is non-trivial | |
| Claude-generated placeholder PNG | Fastest but not a real "production filter"; would require Phase 4 swap | |

**User's choice:** Extract 1 bug from reference APK (Recommended)
**Notes:** Wave 0 prerequisite task; asset path inside APK to be confirmed at extraction time. Locked as D-05.

---

### Q1.3 — Animated flipbook or single still image?

| Option | Description | Selected |
|--------|-------------|----------|
| Animated flipbook (Recommended) | Multi-frame sprite; fully validates REN-05 flipbook logic + LruCache load/evict + REN-08 stress | ✓ |
| Single still image Phase 3 → flipbook Phase 4 | Simpler Phase 3; defers REN-05 runtime validation | |
| Flipbook if APK has it, else fallback still | Adaptive; Claude decides at extraction time | |

**User's choice:** Animated flipbook (Recommended)
**Notes:** Locked as D-03. D-07 covers the degenerate `frameCount=1` fallback if reference APK unexpectedly ships single-frame assets.

---

### Q1.4 — REN-02 "4 behaviors" scope: full impl or STATIC + stub 3?

| Option | Description | Selected |
|--------|-------------|----------|
| STATIC full + architect stubs for 3 (Recommended) | `BugBehavior` sealed interface + `BugState` data class sized for all 4; CRAWL/SWARM/FALL = `TODO("Phase 4")` | ✓ |
| Implement all 4 behaviors + 4 demo filters in Phase 3 | Scope creep; pulls Phase 4 MOD-01/02 work into Phase 3 | |

**User's choice:** STATIC full + architect stubs for 3 (Recommended)
**Notes:** Locked as D-04. Phase 4 fills CRAWL/SWARM/FALL when the catalog ships.

---

## Area 2: Shutter UX (Photo Capture)

### Q2.1 — What happens after shutter tap?

| Option | Description | Selected |
|--------|-------------|----------|
| Stay on camera + Toast (Recommended) | No nav change; Toast "Saved to gallery"; verify externally via Google Photos | ✓ |
| Navigate PreviewRoute stub with bitmap | Reuses Phase 1 stub + adds Coil; pulls Phase 6 UX-04 forward slightly | |
| Scaffold minimal preview screen + basic actions | Significant scope creep; Phase 6 UX-04 overlap | |

**User's choice:** Stay on camera + Toast (Recommended)
**Notes:** Locked as D-12. English-only UI per PROJECT.md (Vietnamese only in conversation).

---

### Q2.2 — Shutter button design + relationship to Phase 2 TEST RECORD?

| Option | Description | Selected |
|--------|-------------|----------|
| Shutter circle bottom-center, keep TEST RECORD debug (Recommended) | 72dp white circle bottom-center; TEST RECORD moved to BottomStart with BuildConfig.DEBUG gate preserved | ✓ |
| Shutter + Record share bottom-center slot | Layout collision risk; doesn't match reference UX | |
| Shutter replaces TEST RECORD entirely | Loses Phase 2 CAM-06 regression-debug lever | |

**User's choice:** Shutter circle bottom-center, keep TEST RECORD debug (Recommended)
**Notes:** Locked as D-13 + D-14.

---

### Q2.3 — REN-07 instant filter swap validation approach?

| Option | Description | Selected |
|--------|-------------|----------|
| 2 filters + debug toggle button (Recommended) | Extract second bug (spider) from APK; `Cycle Filter` debug button cycles A→B→A | ✓ |
| 1 filter + unit test setFilter API | Cheaper Phase 3 but defers runtime risk to Phase 4 picker UI | |
| 2 filters but reuse ant sprite with different anchor | Cheaper extraction; doesn't stress LruCache eviction on separate asset set | |

**User's choice:** 2 filters + debug toggle button (Recommended)
**Notes:** Locked as D-02 + D-10 + D-11. Second filter = spider-on-forehead STATIC.

---

### Q2.4 — Shutter feedback (haptic + sound)?

| Option | Description | Selected |
|--------|-------------|----------|
| Haptic only (Recommended) | `HapticFeedback.performHapticFeedback(LONG_PRESS)`; avoids region-locked audio edge cases | ✓ |
| Haptic + `MediaActionSound.SHUTTER_CLICK` | Matches iOS/reference feel; Japan/Korea region-locked sound may clash with Phase 6 settings toggle | |
| No immediate feedback (Toast only on save) | Perceived latency issue; worse UX | |

**User's choice:** Haptic only (Recommended)
**Notes:** Locked as D-15. Phase 6 UX-09 will decide global sound policy.

---

## Area 3: BboxIouTracker + Multi-Face Policy

### Q3.1 — Tracker sensitivity (IoU threshold + dropout retention)?

| Option | Description | Selected |
|--------|-------------|----------|
| Balanced: IoU 0.3, retain 5 frames (Recommended) | MediaPipe tracker defaults; ~165ms retention at 30fps | ✓ |
| Strict: IoU 0.5, retain 3 frames | Too easy to reset 1€ filter state on fast head turn | |
| Loose: IoU 0.2, retain 10 frames | Crosstalk risk on close-together faces | |

**User's choice:** Balanced: IoU 0.3, retain 5 frames (Recommended)
**Notes:** Locked as D-21 (`IOU_MATCH_THRESHOLD = 0.3f`, `MAX_DROPOUT_FRAMES = 5`).

---

### Q3.2 — Multi-face policy: tracker retention count + render targets?

| Option | Description | Selected (auto per user instruction) |
|--------|-------------|--------------------------------------|
| Tracker 2 faces, filter render primary only (Recommended) | Matches Phase 4 CAT success criterion #5; minimal CPU cost | ✓ |
| Tracker all detected (cap 4), filter render primary only | Extra smoother state; zero Phase 4 render refactor | |
| Primary face only, no multi-face in Phase 3 | Breaks Phase 4 CAT-#5; rebuild cost later | |

**User's choice:** Auto-selected Recommended per user mid-discussion instruction ("Bạn tự chạy toàn bộ Phase 3 theo recommended hết đi, ko cần hỏi tôi nữa")
**Notes:** Locked as D-22 (`MAX_TRACKED_FACES = 2`) + D-24 (primary-only render in Phase 3).

---

### Q3.3 — Smoother state handling on lost tracker ID? (auto-decided, not asked)

| Option | Description | Selected (auto) |
|--------|-------------|-----------------|
| Clear state immediately + re-init on same-ID reappear | Simple; matches research default; no stale carry-over | ✓ |
| Fade-out over N frames | Extra complexity for marginal smoothness benefit | |
| Keep state indefinitely | Stale data risk if face re-enters frame | |

**Rationale for auto-default:** Research-aligned; safest behavior; matches Phase 2 D-22 note about re-initialization when same ID reappears.
**Notes:** Locked as D-25.

---

## Area 4: Front-Camera Mirror Convention (CAP-04) — all auto-decided

### Q4.1 — How to determine CAP-04 mirror convention? (auto-decided, not asked)

| Option | Description | Selected (auto) |
|--------|-------------|-----------------|
| Inspect reference APK runtime first + match (Recommended) | Install reference APK on Xiaomi 13T, capture front-cam photo, compare to preview orientation; implement whatever reference does | ✓ |
| Implement mirrored save without inspection | Assumes selfie convention; may not match reference | |
| Implement un-mirrored save without inspection | Assumes photography convention; may not match reference | |

**Rationale for auto-default:** Most rigorous per CAP-04 wording ("match reference app behavior"); Wave 0 task adds ~5 minutes to handoff; override cost is cheap (ImageCapture reversedHorizontal flag).
**Notes:** Locked as D-17 + D-18 + D-19. Default assumption pre-inspection = mirrored (common selfie convention); override on Wave 0 if reference differs.

---

## Claude's Discretion

- Exact Kotlin coroutine plumbing for AssetLoader.preload
- Capture-flash animation implementation details
- PhotoCaptureService as separate class vs methods on CameraController
- Bitmap decode dispatcher (cameraExecutor vs Dispatchers.IO)
- FrameLoop timing source (prefer OverlayEffect.Frame.frameTimeNanos)
- Cycle Filter button label (text fallback; Phase 6 can add Icon)
- manifest.json parser (kotlinx.serialization.json — Phase 1 already has it via nav type-safety)
- DebugOverlayRenderer kill-switch UX (keeps wired behind BuildConfig.DEBUG gate)
- LruCache sizeOf override formula (as long as ≤ 32 MB sane)

## Deferred Ideas

Captured in CONTEXT.md `<deferred>` section. Summary:
- Catalog UI, 15-25 filter asset extraction (Phase 4)
- CRAWL/SWARM/FALL impls (Phase 4)
- Face Filter / Insect Filter mode UI (Phase 4+5)
- Production PreviewRoute (Phase 6)
- Shutter sound + settings toggle (Phase 6)
- FPS HUD, formal ≥24fps measurement, instrumented leak test (Phase 7)
- Video recording (Phase 5)
- Canvas → GL escalation (Phase 7 if Canvas fps < 24)
