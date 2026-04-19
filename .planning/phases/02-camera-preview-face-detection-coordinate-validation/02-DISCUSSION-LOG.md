# Phase 2: Camera Preview + Face Detection + Coordinate Validation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-19
**Phase:** 02-camera-preview-face-detection-coordinate-validation
**Mode:** `--chain` (interactive discuss → auto plan+execute)
**Areas discussed:** Debug overlay content, Test video-record trigger UX, Orientation policy, Preview scale type

---

## Gray Area Selection

| Area | Discussed |
|------|-----------|
| Debug overlay content | ✓ |
| Test video-record trigger UX | ✓ |
| Orientation policy | ✓ |
| Preview scale type | ✓ |

User selected all four presented gray areas.

---

## Debug overlay content

| Option | Description | Selected |
|--------|-------------|----------|
| Red box + landmark dots | Red boundingBox + contour landmark dots (nose/eyes/jawline). Validates contour ingestion for Phase 3 anchoring. ~30 dots/face. | ✓ |
| Chỉ red bounding box | Minimum per CAM-06. Proves matrix but no landmark visibility. | |
| Full debug HUD | Red box + landmark dots + trackingId + FPS + detection latency. Verbose but cluttered. | |

**User's choice:** Red box + landmark dots
**Notes:** Matched recommended default. Landmark dots kept as Phase 2 value-add that de-risks Phase 3 sprite anchoring; trackingId stability and FPS moved to logcat/profiler instead of HUD.

---

## Test 5-second video recording trigger

| Option | Description | Selected |
|--------|-------------|----------|
| Debug-only TEST button | Visible only when `BuildConfig.DEBUG`. Tap → 5s record → auto-stop → toast URI. No audio. | ✓ |
| Long-press preview | Hidden gesture on preview surface. | |
| Reuse future record button | Build Phase 5 record button early with 5s test mode. | |

**User's choice:** Debug-only TEST button
**Notes:** Matched recommended default. Scope-clean (no premature RECORD_AUDIO prompt, no Phase 5 record-button redesign). Runbook handoff stays mechanical.

---

## Orientation policy

| Option | Description | Selected |
|--------|-------------|----------|
| Portrait-locked app-wide | Manifest `screenOrientation=portrait`. Verify matrix via `OrientationEventListener` → `setTargetRotation()` across 4 device rotations while UI stays upright. | ✓ |
| Rotation-responsive | UI + preview rotate with device. Visual 4-way test but adds Compose + CameraX rotation edge cases. | |
| Portrait-locked, defer landscape test to Phase 7 | Only verify portrait in Phase 2. | |

**User's choice:** Portrait-locked app-wide
**Notes:** Matched recommended default. Matches reference-app convention. Matrix still validated in all four sensor rotations by physically rotating the Xiaomi 13T — CameraX's `getSensorToBufferTransform()` updates via `targetRotation`, overlay should remain aligned without UI rotation.

---

## CameraXViewfinder preview scale type

| Option | Description | Selected |
|--------|-------------|----------|
| FIT_CENTER | Letterbox black bars, no crop, clean coord math for Phase 2. | ✓ |
| FILL_CENTER | No black bars, fills viewport, coord transform handles crop. | |

**User's choice:** FIT_CENTER
**Notes:** Matched recommended default. FILL_CENTER revisit is documented as Phase 6 polish concern (re-verify overlay alignment after scale-type change).

---

## Claude's Discretion

- Exact Hilt module wiring shape (`@Provides` vs `@Binds`)
- `OrientationEventListener` emission threshold
- Timber tree setup and log tag format
- 1€ filter Kotlin implementation details (from paper/JS reference)
- Whether to reuse Phase 1 stub composable signatures or fully replace
- Test scaffold additions (`OneEuroFilter` unit test, CameraController lifecycle smoke test)
- Compose composable tree structure inside `CameraScreen`
- Lifecycle-binding site (Activity vs Composable)
- Lens-flip button icon source (Material Icons extended vs drawable asset)

## Deferred Ideas

- FPS counter / detection latency HUD → Phase 7 profiler
- FILL_CENTER preview scale → Phase 6 polish
- Production record button + RECORD_AUDIO + 60s cap → Phase 5
- Multi-face contour workaround → Phase 3 fallback policy
- Dev-menu runtime toggles → Phase 6 if needed
- Xiaomi MIUI camera HAL workarounds → Phase 7 cross-OEM matrix
- Thermal listener → Phase 5
- `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion=28` → not needed
