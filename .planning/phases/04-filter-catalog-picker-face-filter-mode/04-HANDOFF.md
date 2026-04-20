# Phase 4 Handoff — Filter Catalog + Picker + Face Filter Mode Device Runbook

**Device:** Xiaomi 13T (HyperOS / MIUI)
**APK:** `app/build/outputs/apk/debug/app-debug.apk` (Plan 04-08 Task 1 produced — 83 MB, 106 unit tests GREEN)
**Reference APK:** `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk`
**Reference APK SHA-256 (T-04-02 asset provenance):** `616c6990331ea59f36f135221f15aff2cdb5c290dfde0cd6c91f898fa26e7859`
(Same APK used for Phase 3 and Phase 4 sprite extraction — unchanged. Verify it matches `reference/APK_SHA256.txt`.)

---

## Prerequisites (user-side)

1. Phase 3 prerequisites still met:
   - Settings → Additional settings → Developer options → USB debugging: ON
   - "Install via USB": ON (MIUI-specific)
   - "USB debugging (Security settings)": ON (MIUI-specific)
2. USB cable connected between phone and this PC (data-capable, not charge-only).
3. When prompted on phone, tap "Allow" for the RSA fingerprint dialog.
4. `adb` at `C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe` (Phase 1, 2, 3 validated path).
5. Google Photos installed + signed into an account on Xiaomi 13T (preinstalled — confirm in app drawer).
6. Phone NOT in battery-saver or thermal-throttle mode (cool device preferred for smooth preview).
7. ~100 MB storage free on device (83 MB APK install; a few captured JPEGs for regression check).
8. Previous Bugzz debug build already on device from Phase 3 — new install replaces it automatically with `-r` flag.

---

## What you are verifying

| Criterion | Description | Hard gate? |
|-----------|-------------|------------|
| **CAT-01** | FilterCatalog bundles exactly 15 filters covering STATIC/CRAWL/SWARM/FALL behaviors | Yes — unit-test verified; device sanity spot-check in Step 5 (count thumbnails) |
| **CAT-02** | Each filter has valid id, displayName, thumbnail, sprite atlas, behavior, landmark anchor | Yes — unit-test verified; Step 5 confirms thumbnails load from assets |
| **CAT-03** | LazyRow picker renders + scrolls smoothly; rapid-tap 10 swaps in 5s without visible rebind or freeze | Yes — Step 10 |
| **CAT-04** | Tap filter → swap within ~1 preview frame; no freeze, no black flash | Yes — Step 10 |
| **CAT-05** | Last-used filter persists across app restart (DataStore round-trip) | Yes — Step 12 |
| **MOD-01** | Home has Face Filter (filled/enabled) + Insect Filter (outlined/disabled) + settings gear + My Collection | Yes — Steps 2 + 3 |
| **REN-02 / STATIC** | Spider or bug sprite snaps to anchor (nose, forehead, cheek) and tracks head movement | Yes — Step 6 |
| **REN-02 / CRAWL** | Bug traverses face perimeter continuously; looping, no teleport | Yes — Step 7 |
| **REN-02 / SWARM** | 5–8 bug instances drift toward nose, respawn at face edge | Yes — Step 8 |
| **REN-02 / FALL** | Bugs rain from top of preview; gravity fall; despawn at bottom; max ~8 simultaneous | Yes — Step 9 |
| **MOD-02 #5** | Multi-face scene (2 faces in frame) — no crash; primary gets full anchor; secondary gets bbox-center bug | Soft — Step 11 (ML Kit behavior on printed photos varies; "no crash + primary gets bug" is sufficient) |
| **Regression** | Phase 3 shutter + JPEG capture still works with Phase 4 pipeline | Yes — Step 13 |

**Hard gates** (Steps 2, 3, 4, 6, 7, 8, 9, 10, 12, 13): failure blocks Phase 4 sign-off — create `04-gaps-0N-PLAN.md` before advancing to Phase 5.

**Soft gates** (Steps 5-smoothness, 11-multi-face): document the finding; Phase 4 may still close if you agree to a follow-up plan.

---

## Known expected findings (NOT bugs — flag only if different)

- Black letterbox bars on top/bottom of camera preview — expected (Phase 2 D-10 FIT_CENTER scale type; FILL_CENTER is Phase 6).
- Debug bounding-box (red stroked rect) + orange contour centroid dots visible BEHIND the bug sprite in the debug build — expected. Both DebugOverlayRenderer and FilterEngine draw every frame (D-27 draw order: FilterEngine first, DebugOverlayRenderer on top). This is correct in DEBUG builds; release build omits the debug overlay entirely.
- Insect Filter button on Home visually disabled (greyed text + faint border) — expected (Phase 4 D-19).
- Settings gear on Home shows Toast "Settings coming soon" only — expected (Phase 6 UX-09 wires real settings content).
- "My Collection" opens a stub screen — expected (Phase 6 UX-01 wires real collection).
- Secondary face in multi-face scene gets bug at bbox-center (mid-forehead), no contour dots — expected (Phase 2 PITFALLS §13: ML Kit contour mode only provides contours for the primary face; secondary face gets bbox-center fallback per Phase 4 D-22).
- Picker thumbnail for CRAWL and SWARM filters shows a static `frame_00.png` — expected (thumbnail = first frame; the animated behavior is visible in the live preview after selecting the filter).
- MIUI/HyperOS OEM quirks — document anything unexpected under §OEM Quirks at the end. Do NOT attempt to fix; Phase 7 handles cross-OEM matrix.

---

## Step 1 — Verify APK and device connectivity

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
```

Expected:
- APK file listed, size ~83 MB (Phase 4 adds ~4 MB sprite atlas vs Phase 3 79 MB; debug unminified).
- Device output: `<xiaomi-13t-serial>    device` (not `unauthorized` or `offline`).

If `unauthorized`: tap "Allow" on the RSA fingerprint dialog on the phone, re-run.
If `offline`: unplug/replug USB; toggle USB debugging OFF then ON.

PASS / FAIL: ________

---

## Step 2 — Install Bugzz debug APK + launch

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
```

Expected: `Performing Streamed Install ... Success`; app opens on Splash screen → Continue → Home screen.

If `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (signing key mismatch from a non-debug build):
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" uninstall com.bugzz.filter.camera
```
Then re-install with `-r` flag.

If MIUI shows "Install unknown apps" blocker: enable for the install source, retry.

Launch logcat in a second terminal (keep running through all steps):
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -c
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat > phase4-run.log
```

If you see: crash on launch → check `adb logcat | grep -E "FATAL|AndroidRuntime"` for a stack trace and paste into `04-CHECKPOINT.md` findings. Common culprits: Hilt binding-missing error for `FilterPrefsRepository` (regression from 04-05), serialization error on `CameraRoute` (regression from 04-07).

PASS / FAIL: ________

---

## Step 3 — Home screen UI check (MOD-01 hard gate)

**This is a HARD GATE step. Failure blocks Phase 4 sign-off.**

After landing on Home screen, verify the following layout (04-UI-SPEC §Component Specs §1):

- [ ] Settings gear icon visible at top-right (48dp tap target, `Icons.Default.Settings`)
- [ ] "Face Filter" button — 200×80dp, Material3 filled/primary, text "Face Filter", visually enabled (full color, no grey)
- [ ] "Insect Filter" button — 200×80dp, Material3 outlined, text "Insect Filter", visually **disabled** (greyed text + faint border, no ripple on tap)
- [ ] "My Collection" button — near bottom, 160×56dp, outlined, visually enabled
- [ ] Layout is portrait-locked — rotating the phone does NOT change the layout (buttons stay centered)

If any button is missing or incorrectly styled: document in 04-CHECKPOINT.md § Step 3 findings. Check `adb logcat` for Compose rendering errors.

PASS / FAIL: ________

---

## Step 4 — Settings gear + Insect Filter disabled behavior (MOD-01)

- Tap the **settings gear** icon (top-right) — expect Toast "Settings coming soon" (brief bottom-of-screen toast; no navigation)
- Tap the **"Insect Filter"** button — expect NO response (button is `enabled=false`; Material3 suppresses tap events automatically; no ripple, no navigation, no Toast)

If Settings gear shows nothing (no Toast): Toast wiring regression from Plan 04-07. Check `HomeScreen.kt` LocalContext wiring.

If Insect Filter navigates anywhere: `enabled=false` was not applied correctly in `HomeScreen.kt`.

PASS / FAIL: ________

---

## Step 5 — Face Filter navigation + picker strip visible + restored filter (CAT-05 initial, MOD-01)

Tap **"Face Filter"** from Home.

Expected (04-UI-SPEC §Component Specs §2):
- [ ] CameraScreen opens (front camera live preview visible)
- [ ] Picker strip visible at bottom of preview — dark semi-transparent background, ~100dp tall, horizontally scrollable
- [ ] Picker contains exactly **15 thumbnails** (count by scrolling fully left-to-right; thumbnails are 72dp × 72dp with bug sprite image + filter name label below)
- [ ] On FIRST launch of Phase 4 build: picker is auto-scrolled to center on `spider_nose_static` ("Spider Nose" — the DEFAULT_FILTER_ID)
- [ ] On SUBSEQUENT launches (after Step 12): picker is auto-scrolled to and highlights the last-used filter selected in Step 12
- [ ] 72dp white shutter circle visible at bottom-center above picker strip
- [ ] "Flip" text button (or flip icon) visible at top-right of camera screen
- [ ] DEBUG build only: "TEST 5s" button visible at bottom-left (BuildConfig.DEBUG gated)
- [ ] NO "Cycle" button (removed in Plan 04-06 — if visible, it is a regression)

If CameraScreen does NOT open (stays on Home or crashes): check logcat for Hilt graph errors or `CameraRoute` serialization failures. Hard blocker.

If picker strip is absent: check `CameraScreen.kt` for FilterPicker composable wiring regression from Plan 04-05.

PASS / FAIL: ________

---

## Step 6 — STATIC behavior visual verification (REN-02 hard gate)

**This is a HARD GATE step. Failure blocks Phase 4 sign-off.**

Ensure "Spider Nose" (`spider_nose_static`) is selected in the picker (default on first launch).

Expected:
- [ ] Spider sprite appears on your nose tip within 1 preview frame of the filter being selected
- [ ] Move head left/right/up/down — sprite follows the nose tip smoothly
- [ ] Sprite animation (flipbook) plays at a visible pace — the spider appears to crawl in place (not a frozen single frame)
- [ ] Sprite scale is reasonable — approximately 15-25% of face width (23-frame spider sprite from `spider_prankfilter.json`)

Tap another STATIC filter from the picker (e.g., "Bug A Forehead" — `bugA_forehead_static`, or "Bug C Chin" — `bugC_chin_static`). Verify:
- [ ] Sprite changes to the new bug sprite AND moves to the new anchor (forehead or chin)
- [ ] Animation plays for the new sprite

If sprite is visible but not animating (frozen single frame): `frameCount=1` fallback triggered or `frameDurationNanos = 0`. Check `AssetLoader` + `SpriteManifest` for the affected filter. Document in 04-CHECKPOINT.md.

If sprite anchor is off (e.g., appears on forehead when it should be nose): `FaceLandmarkMapper` anchor-type regression from Phase 3. Hard blocker.

PASS / FAIL: ________

---

## Step 7 — CRAWL behavior visual verification (REN-02 hard gate)

**This is a HARD GATE step. Failure blocks Phase 4 sign-off.**

Tap a CRAWL filter in the picker — `bugB_crawl` ("Bug B Crawl") OR `spider_jawline_crawl` ("Spider Crawl") OR `bugC_crawl` ("Bug C Crawl").

Expected:
- [ ] A single bug sprite is visible traversing along the face perimeter (jawline / face-edge contour)
- [ ] Motion is continuous — the bug loops around the face contour without stopping
- [ ] Direction is consistent (CW or CCW — either is acceptable as long as it is stable, not random)
- [ ] Speed is reasonable — visibly moving, not frozen or blinking; not so fast it blurs
- [ ] No teleporting — the bug moves smoothly from point to point on the contour path (36-point linear interpolation per D-08)
- [ ] Move your head — the crawl path moves with your face (anchored to FaceContour.FACE contour in sensor coordinates)

If bug is frozen at one contour point: progress delta is not advancing (`dt` may be zero or `frameTimeNanos` is not updating the BehaviorState).
If bug teleports: contour interpolation is using wrong wrap logic (progress > 1.0 case).

PASS / FAIL: ________

---

## Step 8 — SWARM behavior visual verification (REN-02 hard gate)

**This is a HARD GATE step. Failure blocks Phase 4 sign-off.**

Tap a SWARM filter — `spider_swarm` ("Spider Swarm") OR `bugA_swarm` ("Bug A Swarm") OR `bugB_swarm` ("Bug B Swarm").

Expected:
- [ ] 5–8 bug instances visible simultaneously (exact count per `swarmCount` in `FilterDefinition.behaviorConfig`)
- [ ] Each bug is drifting toward the anchor (typically nose tip or forehead)
- [ ] When a bug gets close to the anchor (within ~20% of face bbox width), it respawns at a random point on the face bbox edge
- [ ] The swarm stays active indefinitely — no bug freezes or disappears permanently
- [ ] No single bug stays stationary for more than ~1 second

If fewer than 5 bugs visible: check `BehaviorState.Swarm.instances` initialization in `FilterEngine.createBehaviorState`.
If bugs don't drift toward anchor: velocity initialization in `SwarmBehavior.tick` may not be computing `(anchor - position).normalize()` correctly.

PASS / FAIL: ________

---

## Step 9 — FALL behavior visual verification (REN-02 hard gate)

**This is a HARD GATE step. Failure blocks Phase 4 sign-off.**

Tap a FALL filter — `bugC_fall` ("Bug C Rain") OR `bugA_fall` ("Bug A Rain") OR `bugB_fall` ("Bug B Rain").

Expected:
- [ ] Bugs spawn at the top of the preview area at random horizontal positions
- [ ] Each bug falls at a constant downward speed (gravity simulation — ~50% of preview height per second per D-10)
- [ ] Bug disappears when it reaches the bottom of the preview
- [ ] New bugs spawn approximately every 200–400ms
- [ ] Maximum ~8 bugs visible simultaneously (cap enforced via `FallConfig.maxInstances`)
- [ ] Rain is continuous — bugs keep appearing and falling as long as the filter is selected

If no bugs spawn: check `FallBehavior.tick` for `nextSpawnNanos` initialization issue (should start at `frameTimeNanos` or 0).
If bugs stay at the top and don't move: gravity velocity update (`position.y += velocity.y * dt`) may be missing.

PASS / FAIL: ________

---

## Step 10 — Rapid-tap stress test (CAT-03 hard gate, CAT-04 hard gate)

**This is a HARD GATE step. Failure blocks Phase 4 sign-off.**

Within approximately 5 seconds, tap **10 different filters** as fast as you can. The filters don't need to be distinct — tap across the picker strip randomly, aiming for at least 5–8 different filter IDs.

Expected:
- [ ] Each tap immediately highlights the tapped thumbnail in the picker (white border + 1.15× scale — optimistic UI responds on tap frame)
- [ ] Preview transitions to the new filter within ~1 preview frame (~33ms); no visible delay between picker highlight and filter change
- [ ] No preview freeze at any point during the 10 taps
- [ ] **No black flash** (a black flash indicates a CameraX rebind — the worst failure mode for this step)
- [ ] No "Camera in use" crash or error Toast
- [ ] After all 10 taps settle, the filter actively rendering matches the last thumbnail you tapped

After the rapid-tap, check logcat:
```bash
grep -i "CameraInUseException\|Camera in use\|rebind\|CameraController" phase4-run.log | tail -20
```
Expected: zero `CameraInUseException` lines.

If you see a black flash: CameraX is being rebound on filter swap — hard blocker. The `FilterEngine.setFilter()` path should NOT call `cameraController.unbind()`. Check `CameraViewModel.onSelectFilter()` for any rebind call.

If picker highlight lags (takes >1 frame to update): `selectedFilterId` optimistic update in `CameraViewModel.onSelectFilter()` may not be triggering recomposition correctly.

PASS / FAIL: ________

---

## Step 11 — Multi-face test (MOD-02 soft gate)

**This is a SOFT GATE step.** ML Kit contour detection behavior on secondary faces (and printed photos) varies by device and scene quality. The bar for PASS is: (a) no crash AND (b) primary face gets a bug. Secondary face rendering is best-effort.

Option A: Find a friend — both hold your faces in front of the front camera simultaneously.
Option B: Print a face photo at approximately 8×8 cm; hold it alongside your own face at arm's length.

Expected:
- [ ] No crash when 2 faces are detected simultaneously
- [ ] Primary face (the one with the larger bounding box) gets a full anchor-resolved bug sprite
- [ ] Secondary face gets a bug sprite at its bounding-box center (mid-forehead area) — visible but without the precise landmark-anchor placement the primary face gets
- [ ] FPS remains visually smooth (no more jank than with a single face)
- [ ] For SWARM or FALL filters with 2 faces: total bug count stays reasonable (soft cap D-14: if total draw calls > 20, instance counts are halved). Observe whether the device handles it gracefully.

Even if the secondary face does not render a bug reliably (e.g., ML Kit doesn't detect the printed photo consistently), Phase 4 closes if there is no crash and the primary face renders correctly.

Document the actual outcome (secondary face bug visible? consistent? printed photo detected at all?) in the Notes column of 04-CHECKPOINT.md Step 11.

PASS / FAIL / SOFT: ________

---

## Step 12 — DataStore persistence across app restart (CAT-05 hard gate)

**This is a HARD GATE step. Failure blocks Phase 4 sign-off.**

1. In the picker, tap **"Bug C Rain"** (`bugC_fall`) filter. Observe the FALL bugs raining from the top of the preview.
2. Note that the picker strip highlights "Bug C Rain" (white border + larger scale).
3. Exit the app: press the Android Home button.
4. Force-stop the app using one of these methods:
   - From Recents, swipe the Bugzz card away. **Then wait 3 seconds.**
   - OR: Long-press the Bugzz app icon → App info → Force stop.
5. Re-launch the app:
   ```bash
   "C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
   ```
6. Tap "Face Filter" from the Home screen to enter the camera.

Expected:
- [ ] Picker auto-scrolls to "Bug C Rain" and highlights it (white border + 1.15× scale) immediately — without any user interaction
- [ ] Camera preview immediately shows FALL behavior bugs raining from the top — no need to manually re-tap the filter
- [ ] No freeze or delay on the restored-filter load (DataStore read is async but should resolve within 1 second)

If the picker defaults to "Spider Nose" (DEFAULT_FILTER_ID `spider_nose_static`) instead of restoring "Bug C Rain": DataStore write/read regression. Debug:
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell run-as com.bugzz.filter.camera ls files/datastore/
```
Expected: file `bugzz_prefs.preferences_pb` present (confirms DataStore has written at least one value). If absent, the `FilterPrefsRepository.writeLastUsed()` was never called.

PASS / FAIL: ________

---

## Step 13 — Phase 3 regression check (photo capture)

**This is a HARD GATE step (regression of CAP-02 from Phase 3).**

With any filter active (STATIC recommended for easiest verification — select "Spider Nose"):
1. Hold your face in the camera frame with the bug sprite visible.
2. Tap the **72dp white shutter button** (bottom-center).

Expected:
- [ ] Brief white flash overlay (D-16 — 150ms fade)
- [ ] Haptic buzz (D-15)
- [ ] Toast "Saved to gallery" appears within ~1 second
- [ ] Open Google Photos → DCIM/Bugzz/ album → newest JPEG shows the **bug sprite baked into the photo** at the same position as the live preview
- [ ] Camera continues live preview after capture — no freeze, no rebind, no "Camera in use" error

If the JPEG is saved WITHOUT the bug sprite baked in: Phase 4 changes broke the `OverlayEffect` IMAGE_CAPTURE target wiring. Hard blocker.

Check logcat after capture:
```bash
grep -i "ImageCapture\|OverlayEffect\|onError" phase4-run.log | tail -20
```

PASS / FAIL: ________

---

## OEM Quirks (Xiaomi 13T / HyperOS / MIUI)

Document anything unexpected here during the run. Do NOT fix — just record for Phase 7 cross-OEM matrix.

- 
- 

---

## Final sign-off

After completing all 13 steps, create `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-CHECKPOINT.md` with this structure:

```markdown
# Phase 4 Device Checkpoint — Xiaomi 13T sign-off

**Date:** <YYYY-MM-DD>
**Device:** Xiaomi 13T (HyperOS build: ___)
**APK SHA-256:** <run: sha256sum app/build/outputs/apk/debug/app-debug.apk>

## Results

| Step | Criterion | Outcome | Notes |
|------|-----------|---------|-------|
| 1 | APK + device connectivity | PASS / FAIL | |
| 2 | Install + launch (no crash) | PASS / FAIL | |
| 3 | Home UI — 4 elements present | PASS / FAIL | |
| 4 | Settings Toast + Insect Filter disabled | PASS / FAIL | |
| 5 | Camera opens + 15 thumbnails + picker visible | PASS / FAIL | |
| 6 | STATIC behavior — anchor + track + animate | PASS / FAIL | |
| 7 | CRAWL behavior — continuous contour traversal | PASS / FAIL | |
| 8 | SWARM behavior — 5-8 instances drift + respawn | PASS / FAIL | |
| 9 | FALL behavior — rain + gravity + despawn | PASS / FAIL | |
| 10 | Rapid-tap 10 swaps in 5s — no rebind | PASS / FAIL | |
| 11 | Multi-face — no crash + primary gets bug | PASS / FAIL / SOFT | |
| 12 | DataStore persist — bugC_fall restored across restart | PASS / FAIL | |
| 13 | Phase 3 regression — shutter JPEG with baked bug | PASS / FAIL | |

## Hard gate summary

All hard gates PASS: <Yes / No>

If No — list failing hard gates and proposed gap-closure plan(s):
- 

## Soft gate notes

- 

## OEM Quirks observed

- 

## Decision

- [ ] Phase 4 CLOSED — reply "Phase 4 closed — proceed" to Task 4 (VALIDATION flip)
- [ ] Phase 4 BLOCKED — file gap-closure plans before advancing; reply "Phase 4 blocked — step N failed"
```

**All hard gates PASS:** Reply `Phase 4 closed — proceed` to continue to Task 4.

**Any hard gate FAIL:** Do NOT close Phase 4. File `04-gaps-0N-PLAN.md` per Phase 2/3 gap-closure precedent. Describe the failing step + probable root cause. Re-run 04-HANDOFF.md after gap-closure plans land.

**Soft gates partial fail:** Document in 04-CHECKPOINT.md; agree with user on Phase 5 follow-up or defer to Phase 7 cross-OEM matrix. Phase 4 may still close.

---

## Phase 4 Gap-Closure Path

If any **hard-gate step** (2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13) FAILS:
1. Claude creates `04-gaps-0N-PLAN.md` per Phase 2/3 gap-closure precedent (02-gaps-01, 03-gaps-01).
2. Phase 4 sign-off is blocked until gap plan executes + passes re-verification.
3. Do NOT advance to Phase 5 until all hard gates PASS.

If any **soft-gate step** FAILS (Step 5-smoothness, Step 11-multi-face):
1. Document finding in `04-CHECKPOINT.md` under "Soft gate notes".
2. Create follow-up gap plan if user agrees; otherwise defer to Phase 7 cross-OEM matrix.
3. Phase 4 may still close if user explicitly signs off on the soft-gate failure as deferred.

If ALL hard gates PASS + user signs off → Phase 4 is fully signed off. Task 4 (VALIDATION nyquist flip + STATE/ROADMAP/REQUIREMENTS updates) runs next.

---

*Phase 4 handoff runbook — 13 steps covering CAT-01/02/03/04/05, MOD-01, MOD-02, REN-02 (4 behaviors: STATIC/CRAWL/SWARM/FALL), Phase 3 CAP-02 regression.*
*Follows `03-HANDOFF.md` format. Phase 4 acceptance gate: all hard gates PASS on Xiaomi 13T (or hard gates PASS + soft gates documented with user agreement).*
*APK: `app/build/outputs/apk/debug/app-debug.apk` (83 MB, Plan 04-08 Task 1, 106 unit tests GREEN).*
