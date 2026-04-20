# Phase 4: Filter Catalog + Picker + Face Filter Mode - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-20
**Phase:** 04 — Filter Catalog + Picker + Face Filter Mode
**Areas discussed:** Catalog scope + extraction, CRAWL/SWARM/FALL behavior design, Filter picker UX + Home redesign, Multi-face rendering policy

---

## Area 1: Catalog Scope + Extraction

### Q1.1 — How many filters to ship?

| Option | Description | Selected |
|--------|-------------|----------|
| 15 filters — trimmed set (Recommended) | ~7-8 MB sprites, ≤ 40 MB PRF-04, manageable test matrix | ✓ |
| 20 filters — mid-range | ~10 MB sprites, comfortable | |
| 25 filters — max match reference | ~12-13 MB sprites, highest test burden | |

**User's choice:** 15 filters (Recommended)
**Notes:** Locked as D-01. Leaves room for future milestone expansion.

---

### Q1.2 — Sprite source?

| Option | Description | Selected |
|--------|-------------|----------|
| Re-extract all 15 from reference APK (Recommended) | Wave 0 systematic Lottie layer scan; supersedes `03-gaps-01-PLAN.md` | ✓ |
| Inherit ant + fix spider (03-gaps-01) + add 13 separately | Fragmented extraction; higher risk of same layer-selection bug | |
| Non-reference source (OpenGameArt etc.) | Avoids IP concern but breaks feature-parity goal | |

**User's choice:** Re-extract all from reference APK (Recommended)
**Notes:** Locked as D-03 + D-04 + D-05. `03-gaps-01-PLAN.md` officially superseded; spider fix folded into Phase 4 Wave 0 extraction pass.

---

### Q1.3 — Thumbnail source for picker?

| Option | Description | Selected |
|--------|-------------|----------|
| First frame via Coil 2.7 runtime scale (Recommended) | Zero extra asset bytes, Coil internal cache | ✓ |
| Separate thumbnail asset per filter | ~15 KB × 15 = ~225 KB extra | |
| Runtime off-screen render via FilterEngine | Overly complex | |

**User's choice:** First frame via Coil (Recommended)
**Notes:** Locked as D-06 + D-07. Adds `io.coil-kt:coil-compose:2.7.0` dependency in Wave 0.

---

## Area 2: CRAWL / SWARM / FALL Design

### Q2.1 — CRAWL path?

| Option | Description | Selected |
|--------|-------------|----------|
| Jawline loop via FaceContour.FACE (Recommended) | 36-point perimeter traversal | ✓ |
| Nose-forehead shuttle | Simpler but less visually rich | |
| Random walk within bbox | Less "crawl" feel | |

**User's choice:** Jawline loop (Recommended)
**Notes:** Locked as D-08.

---

### Q2.2 — SWARM mechanics?

| Option | Description | Selected |
|--------|-------------|----------|
| 5-8 bugs drift toward anchor from bbox (Recommended) | Organic feel, per-filter configurable | ✓ |
| 3 fixed bugs pulse scale/alpha | Simpler; less "swarm" feel | |
| 10-12 bugs orbit anchor | Stress risk on mid-tier device | |

**User's choice:** 5-8 bugs drift (Recommended)
**Notes:** Locked as D-09.

---

### Q2.3 — FALL mechanics?

| Option | Description | Selected |
|--------|-------------|----------|
| Spawn across preview width, gravity fall, exit bottom (Recommended) | Classic "raining bugs" | ✓ |
| Spawn from anchor fall down | Less clearly "FALL" behavior | |
| Physics with bounce + spin | Scope creep | |

**User's choice:** Spawn across width (Recommended)
**Notes:** Locked as D-10.

---

### Q2.4 — FilterEngine state schema refactor?

| Option | Description | Selected |
|--------|-------------|----------|
| Sealed BehaviorState per variant, keyed by TrackedId (Recommended) | Type-safe per-behavior shape, multi-face support | ✓ |
| Generic BugState with union fields | Unused-field bloat, error-prone | |
| Per-behavior state internal to impl | Hidden cross-face carry-over risk | |

**User's choice:** Sealed BehaviorState (Recommended)
**Notes:** Locked as D-12 + D-13. Naturally closes code review IN-07.

---

## Area 3: Picker UX + Home Redesign

### Q3.1 — Picker position?

| Option | Description | Selected |
|--------|-------------|----------|
| Inline strip above shutter (Recommended) | 100dp height, 72dp thumbnails, match Snapchat/TikTok | ✓ |
| Bottom sheet swipe-up | Hidden until tap; slower UX | |
| Left vertical rail | Non-standard for portrait camera | |

**User's choice:** Inline strip (Recommended)
**Notes:** Locked as D-15 + D-18 (replaces Phase 3 debug Cycle button).

---

### Q3.2 — Selected state + scroll?

| Option | Description | Selected |
|--------|-------------|----------|
| Border ring + 1.15x scale + center-snap (Recommended) | Clear visual feedback, 200ms animation | ✓ |
| Tint overlay only + free-scroll | Less prominent | |
| No highlight | Confusing once scrolled | |

**User's choice:** Border ring + scale + snap (Recommended)
**Notes:** Locked as D-16 + D-17.

---

### Q3.3 — Home screen redesign?

| Option | Description | Selected |
|--------|-------------|----------|
| 2 buttons + settings + collection icons (Recommended) | Match reference visual spec | ✓ |
| 2 buttons only, defer icons | Doesn't match MOD-01 spec fully | |
| Keep Phase 1 stub + relabel button | Bypasses MOD-01 spec | |

**User's choice:** 2 buttons + settings + collection (Recommended)
**Notes:** Locked as D-19 + D-20 + D-21. "Insect Filter" button disabled Phase 4 (Phase 5 activates).

---

## Area 4: Multi-Face Policy

### Q4.1 — Primary vs secondary rendering?

| Option | Description | Selected |
|--------|-------------|----------|
| Primary full anchor; secondary bbox-center fallback (Recommended) | Matches success criterion #5 verbatim | ✓ |
| Primary only; secondary no bug | Violates spec | |
| Both full anchor | ML Kit contour populates primary only — secondary breaks | |

**User's choice:** Primary full + secondary bbox (Recommended)
**Notes:** Locked as D-22 + D-23 + D-24.

---

### Q4.2 — Draw-call cap?

| Option | Description | Selected |
|--------|-------------|----------|
| Soft cap 20 bugs/frame (Recommended) | Halve SWARM/FALL instances if exceeded | ✓ |
| No cap, measure Phase 7 | Risk of fps drop during Phase 4 handoff | |
| Hard cap 10 | Too strict, cuts visual effect | |

**User's choice:** Soft cap 20 (Recommended)
**Notes:** Locked as D-14.

---

## Claude's Discretion

- Exact 15 filters finalized during reference APK extraction
- CRAWL direction default per filter (CW/CCW)
- Color/tint/theme choices for picker selected state
- Settings gear icon choice + placeholder Toast wording
- Disabled-button visual for "Insect Filter" (greyed vs full + tooltip)
- Thumbnail loading shimmer
- LazyListState rememberSaveable strategy
- DataStore write debounce (default off)
- Exact swarmCount per filter within 5-8 range
- BehaviorConfig default values when manifest.json omits field
- Robolectric/Compose UI test strategy for picker interactions

## Deferred Ideas

Captured in CONTEXT.md `<deferred>` section. Summary:
- 25-filter full set (future milestone)
- Bottom-sheet picker UX (Phase 6)
- Orbit SWARM, physics FALL (future creative expansion)
- Filter SFX, categorization, accessibility labels (Phase 6+)
- Advanced Settings content (Phase 6 UX-09)
- Insect Filter free-placement (Phase 5)
- Canvas-vs-GL escalation (Phase 7)
- Thumbnail polish + shimmer (Phase 6)
