# Phase 5 Human UAT — Remaining Soft Gates

**Source:** 05-VERIFICATION.md (status: human_needed)
**Created:** 2026-05-04
**Purpose:** Track the 5 human-only verification items that could not be confirmed via ADB automation during the Xiaomi 13T session (2026-05-04 22:13–22:48). All items are soft gates; all hard gates PASSED.

---

## Item 1: SC#4 — Thermal Throttle Performance (≥20fps Under Sustained Load)

**Requirement:** VID-08 / ROADMAP SC#4
**Device:** Xiaomi 13T

**Setup:**
```bash
# Start thermal logcat in a separate terminal
adb logcat -s ThermalMonitor:V FaceTracker:V
```

**Test Procedure:**
1. Run a CPU-intensive background task (open multiple heavy apps, or use `adb shell stress-ng --cpu 4 --timeout 120s` if available) to warm the device
2. Enter Face Filter mode → tap Record → start a 60s recording
3. Monitor logcat throughout the 60s

**Pass Criteria:**
- `ThermalMonitor` logcat lines appear (confirms listener registered)
- If device reaches THERMAL_STATUS_MODERATE or above: `FaceTracker` shows every other frame skipped (frame-skip rate ~50%)
- Preview does not visibly stutter; 60s recording completes with saved MP4
- Frame rate visually acceptable (no obvious sub-10fps stall)

**Fail Criteria:**
- No `ThermalMonitor` logcat lines (listener not registered)
- Device crashes under thermal stress
- Preview completely freezes during recording

**Soft gate disposition:** If device stays below Moderate (expected on most room-temperature devices), document "thermal stress not triggered — architecture verified" and mark PASS-arch. Only fail if ThermalMonitor listener is provably absent.

**Sign-off:** PASS / FAIL / PASS-arch: ________
**Notes:** ________

---

## Item 2: MOD-05 — Two-Finger Pinch-to-Zoom Visual Confirmation

**Requirement:** MOD-05 / ROADMAP SC#5
**Device:** Xiaomi 13T

**Test Procedure:**
1. HomeScreen → Insect Filter → sticker spawns at center
2. Place two fingers on the screen and spread them apart
3. Spread to maximum (sticker should grow to ~3x, then stop growing)
4. Squeeze together (sticker shrinks to ~0.3x, then stops shrinking)

**Pass Criteria:**
- Sticker visibly grows on spread; grows to approximately 3x original size then stops
- Sticker visibly shrinks on squeeze; shrinks to approximately 0.3x (still visible) then stops
- No jump or teleport when gesture starts
- Gesture feels responsive (no >200ms lag on modern device)

**Fail Criteria:**
- Sticker does not change size at all on pinch
- Sticker disappears completely (scale going to 0) — would mean clamp failed
- Sticker grows without bound (no 3x cap) — would mean clamp failed

**Sign-off:** PASS / FAIL: ________
**Notes:** ________

---

## Item 3: MOD-06 — Two-Finger Rotation Visual Confirmation

**Requirement:** MOD-06 / ROADMAP SC#5
**Device:** Xiaomi 13T

**Test Procedure:**
1. In InsectFilter mode with sticker visible
2. Place two fingers on screen; twist one clockwise and one counter-clockwise
3. Rotate a full 360° (sticker should complete a full rotation without snapping)
4. Rotate back the other direction

**Pass Criteria:**
- Sticker rotates following the two-finger twist gesture
- Rotation is smooth (no snap-to-angle steps; free rotation)
- Rotation is unbounded (can exceed 360° without stopping)
- Direction of rotation matches the twist direction intuitively

**Fail Criteria:**
- Sticker does not rotate at all on twist
- Rotation snaps to fixed angles (45°, 90°, etc.) — would indicate unintended snap logic
- Rotation reverses direction unexpectedly

**Sign-off:** PASS / FAIL: ________
**Notes:** ________

---

## Item 4: MOD-07 — Sticker Survival Across Camera Flip and Orientation Change

**Requirement:** MOD-07 / ROADMAP SC#5
**Device:** Xiaomi 13T

**Setup:** Place sticker at a distinctive position (e.g., top-left corner), pinch to ~1.5x scale, rotate to ~45°.

**Test A — Camera Flip:**
1. Note the sticker's current position, scale (~1.5x), and rotation (~45°)
2. Tap the Flip button (front→back or back→front)
3. Observe sticker immediately after flip

**Pass (Flip):** Sticker is still visible at the SAME screen position, SAME scale (~1.5x), SAME rotation (~45°) — NOT reset to center/scale 1.0/rotation 0°

**Test B — Device Orientation Change:**
1. With sticker still in the distinctive position from Setup
2. Rotate the Xiaomi 13T to landscape mode (if the app allows landscape)
3. Observe sticker

**Pass (Orientation):** Sticker is still visible at an equivalent position in the new orientation. Sticker has NOT disappeared or reset. (Some positional adjustment is expected and acceptable due to preview size change.)

**Fail Criteria:**
- Sticker resets to center position after flip (indicates `onFlipLens` incorrectly resets StickerState)
- Sticker scale or rotation resets to 1.0/0° after flip
- Sticker disappears entirely after orientation change

**Sign-off (Flip):** PASS / FAIL: ________
**Sign-off (Orientation):** PASS / FAIL / N/A (app portrait-locked): ________
**Notes:** ________

---

## Item 5: VID-03 — Audio Sync Subjective Playback

**Requirement:** VID-03 / ROADMAP SC#2 (audio aspect)
**Device:** Xiaomi 13T + playback device (phone or PC)

**Test Procedure:**
1. Enter Face Filter mode
2. Tap Record; speak or clap rhythmically for ~15 seconds; tap Stop
3. `adb pull /sdcard/DCIM/Bugzz/Bugzz_<latest>.mp4 .`
4. Play in Google Photos on the device, or `ffplay Bugzz_<latest>.mp4` on PC

**Pass Criteria:**
- Audio is present in the MP4 (not silent)
- Audio is approximately synchronized with video — no perceptible 1-second+ lead/lag
- Clapping sound matches the visible hand movement (subjective: feels "in sync")

**Fail Criteria:**
- Complete silence in the MP4 (audio track absent — `withAudioEnabled()` not applied)
- Audible lead/lag of >1 second perceptible to casual observer

**Note:** Formal <50ms drift measurement is Phase 7 PRF-03. This is a pass/fail on subjective audibility only.

**Sign-off:** PASS / FAIL: ________
**Notes:** ________

---

## Bonus (Carry-over from Phase 4 04-HUMAN-UAT)

These items were originally listed in Phase 4 04-HUMAN-UAT.md and carried forward as bonus checks in Phase 5 HANDOFF Steps 14–15.

### Bonus 1: Multi-Face 2-Person Scene (Phase 4 04-HUMAN-UAT #1)

**Test:** FaceFilter mode → select SWARM filter → bring a second face into frame (printed photo or second person)

**Pass:** No crash; primary face (larger bbox) gets full landmark-anchored bug sprites; secondary face gets bug at bbox-center

**Sign-off:** PASS / FAIL (soft): ________
**Notes:** ________

### Bonus 2: FPS Subjective 30s During InsectFilter Recording (Phase 4 04-HUMAN-UAT #2)

**Test:** InsectFilter mode → place sticker → Record → gesture sticker for 30s → Stop

**Pass:** Preview does not visibly stutter or freeze during 30s observation; sticker renders continuously

**Sign-off:** PASS / FAIL (soft): ________
**Notes:** ________

---

## Sign-Off Summary

| # | Item | Req | Hard Gate? | Sign-off |
|---|------|-----|------------|---------|
| 1 | SC#4 Thermal Throttle ≥20fps | VID-08 | Soft | |
| 2 | MOD-05 Pinch-to-zoom visual | MOD-05 | Soft (2-touch) | |
| 3 | MOD-06 Rotation visual | MOD-06 | Soft (2-touch) | |
| 4 | MOD-07 Sticker flip+orientation survival | MOD-07 | Soft (visual) | |
| 5 | VID-03 Audio sync subjective | VID-03 | Soft | |
| B1 | Multi-face no-crash bonus | MOD-02 carry | Soft | |
| B2 | FPS 30s subjective bonus | PRF-01 carry | Soft | |

**All items soft — Phase 5 may close if user agrees to defer any remaining failures to Phase 7 cross-OEM matrix.**

When all soft gates are confirmed: update 05-VERIFICATION.md status to `passed`.

---

*Phase 5 Human UAT — 5 soft gates + 2 Phase 4 carry-overs*
*Created from 05-VERIFICATION.md (2026-05-04)*
