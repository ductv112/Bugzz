# Phase 6 Handoff — UX Polish (Splash + Home + Onboarding + Preview + Collection + Share) Device Runbook

**Device:** Xiaomi 13T (HyperOS / MIUI), Android 13+ (target preview Android 15)
**APK:** `app/build/outputs/apk/debug/app-debug.apk` (Plan 06-08 Task 1 produced — ~91.96 MB / ~87.7 MiB, 172 unit tests GREEN, 0 ignored, 0 failures)
**Debug APK SHA-256:** `4bce44134561f90d846c924d81c6c07615211eaa3e0b101df5dc24a2f0d24eea`
**Phase 5 baseline (reference):** 84 MB / 143 unit tests
**Phase 6 delta:** **+~7.96 MB APK** (lottie-compose 6.7.1 + media3-exoplayer 1.4.1 + media3-ui 1.4.1 + Phase 6 production code), **+29 unit tests** (172 - 143)

> Vietnamese / Tiếng Việt: Đây là runbook để verify Phase 6 trên Xiaomi 13T. Mỗi gate dưới đây có 2 dạng kết quả: **HARD gate** (fail → block sign-off, phải tạo gap-plan) và **SOFT gate** (fail → log + defer, không block).

---

## Pre-flight (user-side)

1. Phase 4/5 prerequisites still met:
   - Settings → Additional settings → Developer options → USB debugging: ON
   - "Install via USB": ON (MIUI-specific)
   - "USB debugging (Security settings)": ON (MIUI-specific)
2. USB cable connected between phone and this PC (data-capable, not charge-only).
3. When prompted on phone, tap "Allow" for the RSA fingerprint dialog.
4. `adb` at `C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe` (Phase 1–5 validated path).
5. ~250 MB free storage on device (88 MB APK + photo/video artifacts).
6. Phone NOT in battery-saver / power-saving mode (Splash Lottie + ExoPlayer playback should run free).
7. Previous Bugzz debug build already on device from Phase 5 — new install replaces it automatically with `-r`.
8. At least one share target installed (Zalo / Messenger / WhatsApp / Gmail) — required for SHR-01..04. If none, document SHR steps as N/A with reason.
9. Pre-clean DataStore + previous artifacts (forces first-launch + empty-state flow):

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm clear com.bugzz.filter.camera
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell rm -rf /sdcard/DCIM/Bugzz
```

> Lưu ý: `pm clear` **xoá DataStore** → `onboarding_completed=false` → lần khởi động kế tiếp sẽ chạy onboarding (cần thiết cho UX-02). `rm -rf DCIM/Bugzz` xoá hết artifact cũ → cần thiết cho UX-07 EmptyState test.

Keep ADB USB connection alive. Enable keep-awake during USB:

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell svc power stayon usb
```

---

## What you are verifying

| Criterion | Description | Hard gate? |
|-----------|-------------|------------|
| **UX-01** | Splash shows Lottie + "Bugzz" brand text + auto-advances ≤ 2s | Yes — Step 1 |
| **UX-02** | First-launch onboarding (3 pages, swipe + Skip + Get Started) — second launch goes straight to Home | Yes — Step 2 |
| **UX-03** | HomeScreen: Settings gear + My Collection + Face/Insect Filter all wired | Yes — Step 3 |
| **UX-04** | Preview screen: Coil AsyncImage for photo, ExoPlayer for video, 4-icon action bar | Yes — Step 4 |
| **UX-05** | Collection grid: 3-column LazyVerticalGrid, photos via Coil, videos via MediaMetadataRetriever + play-overlay, newest first | Yes — Step 5 |
| **UX-06** | Collection → Preview navigation: tap thumbnail opens correct URI; Retake from Collection-entered Preview returns to Collection | Yes — Step 6 |
| **UX-07** | Empty state: Lottie + "No bugs captured yet" + "Open Camera" button | Yes — Step 7 |
| **UX-08** | Delete confirmation: AlertDialog "Delete this artifact?" + Cancel keeps file, Delete removes from MediaStore | Yes — Step 8 |
| **UX-09** | Settings stub: 4 rows, BuildConfig.VERSION_NAME, 2 Toast placeholders, About read-only, Back arrow | Yes — Step 9 |
| **SHR-01** | Preview → Share button → Android share sheet appears with installed targets | Yes — Step 10 |
| **SHR-02** | Share intent uses `Intent.ACTION_SEND` + `EXTRA_STREAM` (FileProvider/MediaStore URI) | Yes — Step 10 (logcat) |
| **SHR-03** | MIME type matches artifact (image/jpeg or video/mp4) — receiving app opens correctly | Yes — Step 10 |
| **SHR-04** | Receiving app shows photo/video with overlay intact (filter baked into bytes, not separate layer) | Yes — Step 10 |
| **Phase 4+5 deferred UAT bonus (D-33)** | 8 soft items — multi-face, fps, gestures, flip survival, audio sync, RECORD_AUDIO permission, thermal observation, sticker drag axis | Soft — Step 11 |

**Hard gates** (Steps 1–10): failure blocks Phase 6 sign-off — create `06-gaps-0N-PLAN.md` per 05-gaps-01/02 precedent.
**Soft gates** (Step 11): document finding in 06-CHECKPOINT.md Notes; Phase 6 may still close with user sign-off.

---

## Known expected findings (NOT bugs — flag only if different)

- **Splash**: black background + 200dp Lottie bug-crawl loop + 24sp/Medium "Bugzz" white text below — auto-advances around 1.5s (acceptable window 1.0–2.0s per D-02).
- **Onboarding** appears ONLY on first launch (or after `pm clear`). Second launch → Splash → Home, skipping onboarding (D-07).
- **Active page indicator dot** is 12dp red `#FFE53935`; inactive dots are 8dp `#9E9E9E` gray (D-04 — only Phase 6 expansion of accent-record scope).
- **HomeScreen** buttons unchanged from Phase 4/5 layout — Settings gear was previously a Toast stub, now wires to SettingsScreen (Plan 06-07).
- **Preview screen** shows the artifact full-screen with `ContentScale.Fit` (letterbox bars top/bottom on portrait videos OK).
- **Collection grid**: 3 columns derived from `GridCells.Adaptive(120.dp)` × screen width — on Xiaomi 13T's ~412dp width = 3 columns at 4dp gap.
- **Video thumbnails** in Collection: greyed background (#2A2A2A) with center 24dp white play-arrow overlay until MediaMetadataRetriever resolves the frame; first-time render may show placeholder for ~50ms then snap to thumbnail.
- **EmptyState** Lottie at 120dp + "No bugs captured yet" + 16dp "Open Camera" Button.
- **DeleteConfirmDialog** is a Material3 AlertDialog with title "Delete this artifact?", body short, Cancel + Delete (red text) buttons.
- **SettingsScreen** TopAppBar back arrow returns to Home; 4 rows (Privacy, Rate, About, Version) — Privacy + Rate show Toasts, About is read-only, Version shows `BuildConfig.VERSION_NAME = "0.1.0"` (current).
- **Share sheet** is the Android system bottom sheet with installed apps that handle image/jpeg or video/mp4 (Zalo, Photos, Files, Drive, Gmail, etc).
- **Phase 5 carry**: 56dp red Record button + recording indicator + 60s auto-stop + RECORD_AUDIO permission flow — all unchanged in Phase 6.
- **Debug overlays** (red bbox + orange contour dots) still visible BEHIND filter overlay in FaceFilter mode — expected (DebugOverlayRenderer is debug-only).
- **InsectFilter mode** still has gestures (drag/pinch/rotate) + free-placement sticker (Phase 5 D-05 — face detection disabled).
- **Picker thumbnails 50% alpha during recording** (Phase 5 D-23 — unchanged).
- **MIUI/HyperOS install blocker** "Install unknown apps" — expected first-time; enable for install source and retry.

---

## Step 0 — Verify APK + device connectivity

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
```

Expected:
- APK file listed, size ~92 MB (Phase 5 was 84 MB; +~8 MB delta = lottie-compose + media3-exoplayer + media3-ui + Phase 6 code).
- Device output: `<xiaomi-13t-serial>    device` (not `unauthorized` or `offline`).

If `unauthorized`: tap "Allow" on RSA fingerprint dialog on phone, re-run.
If `offline`: unplug/replug USB; toggle USB debugging OFF then ON.

Install + start logcat capture:

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -c
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat > phase6-run.log &
```

PASS / FAIL: ________

---

## Step 1 — UX-01 Splash (HARD GATE)

**Pre-condition:** App data freshly cleared (Pre-flight #9 done).

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
```

Tiếng Việt: Mở app sau khi đã `pm clear`.

Verify:
- [ ] Black background (`#FF121212`) full-screen
- [ ] 200dp Lottie animation playing (bug crawl loop) at center
- [ ] 24sp/Medium white "Bugzz" text 16dp below the Lottie
- [ ] Auto-advances within ~1.5s (acceptable 1.0–2.0s — D-02). If too slow → fail; if too fast (<800ms) → fail (text not legible).
- [ ] Splash transitions to **Onboarding** (first launch) — confirms D-07

If splash hangs > 3s: `SplashViewModel.delay(1500)` likely broken or nav graph not signalling completion. Hard blocker.
If splash skips entirely (no Lottie visible): Lottie asset path wrong or `lottie-compose` failed to bundle. Hard blocker.

PASS / FAIL: ________

---

## Step 2 — UX-02 Onboarding (HARD GATE)

**Continuation of Step 1.** App should now be on the Onboarding pager (page 0).

### 2a — Page 0 content (English copy per UI-SPEC §3 / D-03)
- [ ] Title: **"Welcome to Bugzz"** (24sp/Medium/white)
- [ ] Body: **"Bug filters that crawl on your face. Pranks made easy."** (14sp/normal/white80%)
- [ ] 200dp Lottie loop above title
- [ ] 3 page indicator dots at bottom: dot 0 = 12dp red, dots 1+2 = 8dp gray
- [ ] Skip button (TopEnd, 16dp top + 16dp end)
- [ ] Next button (BottomCenter, label = "Next")

### 2b — Swipe to page 1
- Swipe left
- [ ] Page transitions; indicator updates (dot 1 = 12dp red; dots 0+2 = 8dp gray)
- [ ] Title: **"Pick a filter"**
- [ ] Body: **"15 bug filters with 4 behaviors. Static, crawl, swarm, fall."**
- [ ] Next button still labelled "Next"

### 2c — Swipe to page 2
- Swipe left
- [ ] Indicator: dot 2 = 12dp red; dots 0+1 = 8dp gray
- [ ] Title: **"Capture and share"**
- [ ] Body: **"Photo or video. Share to friends instantly."**
- [ ] Next button label changes to **"Get Started"**

### 2d — Tap "Get Started"
- [ ] Onboarding dismisses
- [ ] Navigates to **HomeScreen** (NOT back to Splash)
- [ ] DataStore `onboarding_completed = true` (persisted)

### 2e — Force-stop + relaunch (D-07 verification — second launch test)
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am force-stop com.bugzz.filter.camera
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
```

- [ ] Splash plays (~1.5s)
- [ ] Splash → **HomeScreen directly** (no onboarding pager appears)

If onboarding re-appears on second launch: DataStore write didn't commit, or `onboardingCompleted` flow not collected on Splash. Hard blocker.

### 2f — Skip button alternative path (re-run after pm clear if user wants — optional within UX-02)
- After clearing data again: launch → Splash → Onboarding page 0 → tap "Skip" (TopEnd)
- [ ] Should also navigate directly to HomeScreen + persist `onboarding_completed = true`

PASS / FAIL (UX-02): ________

---

## Step 3 — UX-03 HomeScreen polish (HARD GATE)

In HomeScreen (post-onboarding):

- [ ] Top-right **Settings gear** icon visible
- [ ] Top-right **My Collection** button visible (per Phase 4 layout — 2 entry points: gear + Collection)
- [ ] Center: **Face Filter** + **Insect Filter** buttons (Phase 4/5 carry)

Tap each:
- [ ] **Settings gear** → `SettingsScreen` opens (TopAppBar "Settings" + 4 rows visible) — see Step 9 for full UX-09
- [ ] Back from Settings → returns to HomeScreen (no Splash/Onboarding)
- [ ] **My Collection** → `CollectionScreen` opens (grid OR EmptyState — see Step 5/7)
- [ ] Back from Collection → returns to HomeScreen
- [ ] **Face Filter** → `CameraScreen` (front camera + filter picker active, Phase 4 regression)
- [ ] **Insect Filter** → `InsectFilterScreen` (front camera + sticker spawned at center, Phase 5 regression)

If any button does nothing: nav graph regression. Hard blocker.
If Settings gear opens HomeScreen instead of SettingsScreen: Plan 06-07 wiring not committed. Hard blocker.

PASS / FAIL: ________

---

## Step 4 — UX-04 Preview after capture (HARD GATE)

Setup: from HomeScreen → Face Filter → CameraScreen.

### 4a — Photo preview
- [ ] Tap shutter button (BottomCenter circle) → flash overlay → JPEG saved
- [ ] Auto-navigate to **PreviewScreen** with the captured photo (no Toast "Saved to gallery" — replaced by direct nav per D-09)
- [ ] Photo renders full-screen via Coil `AsyncImage` with `ContentScale.Fit`
- [ ] Bottom action bar 80dp Surface (`#1E1E1E`) with 4 icons + 10sp labels:
  - **Done** (Check icon)
  - **Share** (Share icon)
  - **Delete** (Delete icon)
  - **Retake** (Refresh / camera-rotate icon)

### 4b — Done button
- [ ] Tap **Done** → returns to CameraScreen
- [ ] File remains saved — verify:
  ```bash
  "C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell ls -la /sdcard/DCIM/Bugzz/*.jpg
  ```
  Expected: at least one `Bugzz_*.jpg` file present.

### 4c — Retake button
- Capture another photo → on PreviewScreen → tap **Retake**
- [ ] Returns to CameraScreen
- [ ] File remains saved (Retake does NOT delete; only Delete deletes)

### 4d — Video preview
- In CameraScreen → tap red Record button → wait 5s → tap Record again to stop
- [ ] After save, auto-navigate to PreviewScreen with the captured video URI
- [ ] **ExoPlayer** auto-plays the video on loop (no manual play tap needed per D-08)
- [ ] Same 4-icon action bar visible

If photo preview shows broken/missing image: Coil load error. Check logcat for `AsyncImage` errors.
If video preview is silent + no frames: ExoPlayer state not bound. Hard blocker.
If action bar missing or wrong number of icons: PreviewScreen layout regression. Hard blocker.

PASS / FAIL (UX-04 photo): ________
PASS / FAIL (UX-04 video): ________

---

## Step 5 — UX-05 Collection grid (HARD GATE)

**Pre-condition:** Capture at least 2 photos + 2 videos in Steps 4a/4d so grid has content. From HomeScreen → My Collection.

- [ ] CollectionScreen opens with TopAppBar (back arrow + "My Collection" title or similar)
- [ ] **3-column LazyVerticalGrid** with 4dp gap
- [ ] Each cell is a square (1:1 aspect ratio)
- [ ] Photos render via Coil with `ContentScale.Crop`
- [ ] Videos render via MediaMetadataRetriever-extracted Bitmap (or `#2A2A2A` placeholder briefly) **WITH** a centered 24dp white play-arrow icon overlay
- [ ] Sort order: **newest first** (most recently captured at top-left; older items below)

Sanity:
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell ls -la /sdcard/DCIM/Bugzz/
```
Expected: each `Bugzz_*.jpg` and `Bugzz_*.mp4` from Step 4 corresponds to one grid cell.

If grid is empty but files exist on disk: `CollectionRepository` query failure (T-06-02 selectionArgs binding). Hard blocker.
If grid shows files NOT in `DCIM/Bugzz/`: T-06-02 path-traversal regression. **CRITICAL** hard blocker.
If video cells show no thumbnail forever: MediaMetadataRetriever release leak or extraction failure. Hard blocker.
If grid shows < 3 columns on portrait: spacing regression. Soft (visual).

PASS / FAIL: ________

---

## Step 6 — UX-06 Collection → Preview navigation (HARD GATE)

From CollectionScreen (continuation of Step 5):

### 6a — Tap thumbnail
- [ ] Tap a photo thumbnail → PreviewScreen opens with that photo URI (verify the photo matches what was tapped)
- [ ] Action bar 4 icons present
- [ ] Tap **Retake** (or Done — D-13 says Retake from Collection returns to Collection, not Camera)
- [ ] Returns to **CollectionScreen** (NOT CameraScreen)

### 6b — Tap video thumbnail
- [ ] Tap a video thumbnail → PreviewScreen opens, ExoPlayer auto-plays
- [ ] Tap **Retake** → returns to CollectionScreen

### 6c — Done from Collection-entered Preview
- Tap a thumbnail → on PreviewScreen → tap **Done**
- [ ] Returns to CollectionScreen

If Retake from Collection-entered Preview returns to CameraScreen: nav graph regression — back-stack misconfigured. Hard blocker.
If wrong artifact opens: URI passed via `PreviewRoute(uri)` not deserializing correctly. Hard blocker.

PASS / FAIL: ________

---

## Step 7 — UX-07 Empty state (HARD GATE)

**Pre-condition:** Clear all artifacts so Collection is empty.

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell rm -rf /sdcard/DCIM/Bugzz
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am force-stop com.bugzz.filter.camera
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
```

Then HomeScreen → My Collection:
- [ ] CollectionScreen renders **EmptyStateColumn** instead of grid:
  - 120dp Lottie animation (bug crawl loop)
  - 16sp/Medium heading: **"No bugs captured yet"** (or similar EmptyState heading per UI-SPEC §8)
  - Material3 Button: **"Open Camera"**
- [ ] Tap **"Open Camera"** → navigates to **HomeScreen** (D-13 standard back-stack + popUpTo HomeRoute inclusive)

Tiếng Việt: Bấm "Open Camera" → quay về HomeScreen.

If EmptyState shows with files still on disk: CollectionRepository filter regression. Hard blocker.
If EmptyState button does nothing or crashes: lambda not wired in BugzzApp CollectionRoute. Hard blocker.

PASS / FAIL: ________

---

## Step 8 — UX-08 Delete confirmation (HARD GATE)

Re-capture at least 1 photo so Collection has content. Open from Collection → Preview.

- Tap **Delete** icon on Preview action bar
- [ ] AlertDialog appears with:
  - Title: **"Delete this artifact?"** (or D-26 wording — verify against `DeleteConfirmDialog.kt`)
  - Body: short confirmation text
  - **Cancel** button (left or default-style)
  - **Delete** button (right, red text via `MaterialTheme.colorScheme.error`)

### 8a — Cancel path
- [ ] Tap **Cancel** → dialog dismisses
- [ ] Artifact still present in Collection grid + on disk:
  ```bash
  "C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell ls /sdcard/DCIM/Bugzz/
  ```

### 8b — Delete path
- Re-open the artifact in Preview → tap **Delete** → tap **Delete** in dialog
- [ ] Dialog dismisses
- [ ] Artifact removed from MediaStore — verify: file gone from `DCIM/Bugzz/` listing
- [ ] Pop back to Camera (if entered from Camera) OR Collection (if entered from Collection)

If Cancel deletes the file: button order swap regression. Hard blocker.
If Delete leaves file on disk but UI shows it gone: PreviewViewModel.deleteArtifact() exception swallowed silently. Verify logcat.

PASS / FAIL: ________

---

## Step 9 — UX-09 Settings stub (HARD GATE)

From HomeScreen → Settings gear (top-right):

- [ ] **TopAppBar** with title **"Settings"** + back arrow (top-left)
- [ ] **4 rows** visible:
  1. **Privacy Policy** — tappable
  2. **Rate the App** — tappable
  3. **About** — read-only with trailing tagline
  4. **Version** — read-only with `BuildConfig.VERSION_NAME`

### 9a — Version row
- [ ] Trailing text shows **"0.1.0"** (current `versionName` per `output-metadata.json`)

### 9b — Privacy Policy tap
- [ ] Toast appears with text matching D-XX wording (typically: **"Coming in next release"** or **"Privacy Policy coming soon"**)

### 9c — Rate the App tap
- [ ] Toast appears (typically: **"Coming when published to Play Store"**)

### 9d — About row
- [ ] Trailing text shows tagline like **"Bugzz — Bug filter prank camera"** (or per UI-SPEC §9 wording)
- [ ] Tap is a no-op (read-only — no Toast, no nav)

### 9e — Back navigation
- [ ] Tap back arrow (TopAppBar) → returns to HomeScreen
- [ ] Press device back button → also returns to HomeScreen (not exit-app)

If Settings TopAppBar missing back arrow: regression. Hard blocker.
If Version shows stale string (e.g. "1.0" or empty): BuildConfig binding broken. Hard blocker.
If Privacy/Rate Toasts missing: Plan 06-07 wiring incomplete. Hard blocker.

PASS / FAIL: ________

---

## Step 10 — SHR-01..04 Share intent (HARD GATE)

**Pre-condition:** at least one photo + one video in Collection. Open one in Preview.

### 10a — SHR-01 Share sheet appears
- Tap **Share** icon on Preview action bar
- [ ] Android system share sheet ("Share via" / "Chia sẻ qua") slides up from bottom
- [ ] Share sheet shows installed apps that handle `image/jpeg` (e.g. Zalo, Photos, Files, Drive, Gmail)

If no share sheet appears: `ShareIntentBuilder.buildShareIntent` failed; check logcat for ActivityNotFoundException. Hard blocker.
If share sheet shows but only "No apps to handle" text: no share targets installed → mark **N/A** with reason; not a code bug.

### 10b — SHR-02 Intent payload (logcat verification)
While Step 10a sheet is open:
```bash
grep -i "ACTION_SEND\|Intent.*image/jpeg\|Intent.*video/mp4\|EXTRA_STREAM" phase6-run.log | tail -10
```
- [ ] At least one log line containing `android.intent.action.SEND` + `EXTRA_STREAM` with a `content://` URI

### 10c — SHR-03 + SHR-04 — Pick a target + verify content
- Tap a target (e.g. Zalo "Save to drafts" or Gmail "Compose")
- [ ] Receiving app launches
- [ ] Receiving app shows the shared photo with **bug filter overlay still visible** (overlay is baked into the JPEG bytes, not a separate layer)
- [ ] Repeat for a video → receiving app plays MP4 with **filter + audio intact**

Tiếng Việt: Bấm "Share" → chọn 1 app → verify ảnh hiện ra trong app đó **có bug filter overlay** đè trên mặt (filter đã được bake vào JPEG/MP4 thật).

If receiving app gets file but no filter visible: Phase 3 (photo) or Phase 5 (video) overlay-baking regression — but this is OUTSIDE Phase 6 share scope. Document but not a Phase 6 hard blocker (filter-baking is Phase 3/5 territory).
If receiving app shows wrong artifact or empty: URI scheme / FileProvider bug. Hard blocker.

PASS / FAIL (SHR-01 sheet appears): ________
PASS / FAIL (SHR-02 ACTION_SEND in logcat): ________
PASS / FAIL (SHR-03 MIME correct + receiving app opens): ________
PASS / FAIL (SHR-04 overlay intact in shared artifact): ________

---

## Step 11 — Soft gates: Phase 4+5 deferred UAT bonus (D-33)

These are **NOT Phase 6 hard requirements**. Log results to 06-CHECKPOINT.md Notes column. Phase 6 sign-off does not depend on these.

### 11a — (Phase 4 carry) Multi-face 2-person scene
- HomeScreen → Face Filter → bring a second face into frame (printed photo ~8x8 cm or second person)
- [ ] No crash with 2 faces
- [ ] Primary face (larger bbox) gets full FaceLandmarkMapper-anchored sprites
- [ ] Secondary face gets bbox-center bug
- Result: ✅ / ❌ / N/A — Notes: ________

### 11b — (Phase 4 carry) FPS subjective smoothness over 30s
- In Face Filter, hold the camera on a face + select Swarm filter (e.g. "Bug B Swarm")
- [ ] Preview feels smooth for 30s — no visible stutter
- Result: ✅ / ❌ — Notes: ________

### 11c — (Phase 5 carry) Pinch + rotate gestures on InsectFilter sticker
- HomeScreen → Insect Filter → spawn sticker → two-finger pinch (zoom) + two-finger twist (rotate)
- [ ] Sticker scales smoothly within `[0.3x..3.0x]` clamp
- [ ] Rotation is smooth + unbounded
- Result: ✅ / ❌ — Notes: ________

### 11d — (Phase 5 carry) Sticker survives camera flip + screen orientation
- In Insect Filter, position sticker at top-left + scale ~1.5x + rotate ~45°
- Tap Flip button (front ↔ back)
- [ ] Sticker preserved at same screen position/scale/rotation
- App is portrait-locked → orientation change inapplicable → **N/A** for orientation, but flip is testable.
- Result: ✅ / ❌ / N/A — Notes: ________

### 11e — (Phase 5 carry) Audio sync subjective on video playback
- Capture a 5–10s video with audible speech/sound
- Play in Preview ExoPlayer (or pull MP4 + play in Google Photos)
- [ ] No audible 1s+ lead/lag between lip movement and sound (PRF-03 formal sync is Phase 7 ≤50ms)
- Result: ✅ / ❌ — Notes: ________

### 11f — (Phase 5 carry) Fresh-install RECORD_AUDIO permission dialog
- After `pm clear` + reinstall: HomeScreen → Face Filter → tap red Record
- [ ] System "Allow Bugzz to record audio?" dialog appears immediately on first record tap
- Result: ✅ / ❌ — Notes: ________

### 11g — (Phase 5 carry) ThermalMonitor extended recording stress
- Record 60s+ video (let auto-stop fire at 60s)
- Check logcat:
  ```bash
  grep -i "ThermalMonitor\|MODERATE\|SEVERE\|frame_skip\|drawSkipped" phase6-run.log | tail -20
  ```
- [ ] `ThermalMonitor` log lines present (listener registered)
- [ ] Document observed thermal status (NONE/LIGHT/MODERATE/SEVERE)
- Result: NONE / LIGHT / MODERATE / SEVERE — Notes: ________

### 11h — (Phase 5 gap-02 visual polish) Sticker drag axis matches finger
- In Insect Filter, drag sticker around the screen
- [ ] Drag direction visually matches finger movement (no axis swap, no inversion)
- This was Phase 5 gap-02 fix `de27c4e` — should be solid.
- Result: ✅ / ❌ — Notes: ________

---

## Logcat filter cheatsheet

Use during any step to filter relevant tags:

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -s \
  CameraVM:I InsectFilterVM:I PreviewVM:I CollectionVM:I OnboardingVM:I SplashVM:I \
  ShareIntentBuilder:V CollectionRepository:V FilterPrefsRepository:V ThermalMonitor:V \
  VideoRecorder:V FaceTracker:V OverlayEffect:V
```

For crash diagnosis:

```bash
grep -E "FATAL|AndroidRuntime|Exception" phase6-run.log | tail -30
```

For nav graph regressions:

```bash
grep -i "navigate\|popUpTo\|Routes\." phase6-run.log | tail -20
```

For share intent diagnosis:

```bash
grep -i "ACTION_SEND\|EXTRA_STREAM\|FileProvider\|content://" phase6-run.log | tail -20
```

For ExoPlayer leak detection (T-06-03):

```bash
grep -i "ExoPlayer\|releasePlayer\|MediaSource\|player.release" phase6-run.log | tail -20
```

---

## OEM Quirks (Xiaomi 13T / HyperOS / MIUI)

Document anything unexpected here. Do NOT fix in this checkpoint — record for Phase 7 cross-OEM matrix.

-
-

---

## Final sign-off table

| Step | Requirement | Outcome | Notes |
|------|-------------|---------|-------|
| 0 | APK + device connectivity | PASS / FAIL | |
| 1 | UX-01: Splash Lottie + brand text + auto-advance ≤2s | PASS / FAIL | |
| 2 | UX-02: First-launch Onboarding 3 pages + Skip + Get Started + DataStore persist + second-launch skip | PASS / FAIL | |
| 3 | UX-03: HomeScreen Settings + Collection + Face/Insect Filter all wired | PASS / FAIL | |
| 4 | UX-04: Photo preview + Video preview + 4-icon action bar + Done/Retake nav | PASS / FAIL | |
| 5 | UX-05: Collection 3-column grid + Coil/MediaMetadataRetriever thumbnails + newest-first sort | PASS / FAIL | |
| 6 | UX-06: Tap thumbnail → Preview with correct URI + Retake from Collection-entered Preview returns to Collection | PASS / FAIL | |
| 7 | UX-07: EmptyState Lottie + heading + "Open Camera" button | PASS / FAIL | |
| 8 | UX-08: AlertDialog Cancel keeps file + Delete removes from MediaStore | PASS / FAIL | |
| 9 | UX-09: SettingsScreen 4 rows + Toasts + BuildConfig.VERSION_NAME + back arrow | PASS / FAIL | |
| 10 | SHR-01..04: Share sheet + ACTION_SEND logcat + MIME match + overlay intact in receiving app | PASS / FAIL | |
| 11 | Phase 4+5 deferred UAT bonus (8 soft items per D-33) | SOFT (✅/❌/N/A per item) | |

**Hard gate summary:**
- Hard gates: Steps 1–10 (excluding soft annotations in Step 11)
- All hard gates PASS: **Yes / No**

---

## Sign-off

When all hard gates (Steps 1–10) PASS:

1. Reply to executor: `PASS — proceed to close-out` with:
   - Result count (e.g., "10/10 PASS" or "9/10 — Step X N/A reason")
   - Soft-gate result table (8 items)
   - Any OEM quirk notes

2. The Plan 08 Task 3 executor will then:
   a. Flip `06-VALIDATION.md` frontmatter `nyquist_compliant: false → true` and `status: draft → complete`
   b. Add approval line: `**Approval:** PASS — YYYY-MM-DD via 06-CHECKPOINT.md`
   c. Author `06-CHECKPOINT.md` with per-gate result table + soft-gate notes + device evidence file list
   d. Author `06-08-SUMMARY.md` covering Plan 08 + Phase 6 close-out
   e. Update `ROADMAP.md` — Phase 6 row [x]
   f. Run `gsd-tools roadmap update-plan-progress 06`
   g. Update `STATE.md`: Current Position = Phase 6 complete → Phase 7 next
   h. Update `REQUIREMENTS.md` — mark UX-01..09 + SHR-01..04 complete
   i. Final commit `docs(phase-06): complete phase execution — 13/13 hard gates verified + N/M soft gates`

---

## Phase 6 Gap-Closure Path

If any **hard-gate step** (Steps 1–10) FAILS:

1. **Trivial issues** (typo, copy mismatch, off-grid spacing): fix inline before sign-off + amend SUMMARY (per 05-gaps-01/02 inline-fix precedent — fix in same commit thread, no new plan needed).

2. **Non-trivial issues** (logic bug, nav crash, ExoPlayer leak, MediaStore filter regression): create `06-gaps-0N-PLAN.md` per Phase 2/3/4/5 gap-closure precedent.
   - Phase 6 sign-off blocked until gap plan executes + passes re-verification.
   - Do NOT advance to Phase 7 until all hard gates PASS.

3. **Environmental N/A** (e.g., no share targets installed for SHR-01..04): document as N/A with reason; proceed if no other hard fails.

If any **soft-gate** (Step 11) FAILS:
- Document finding in sign-off table Notes column.
- Phase 6 may still close if user agrees to defer to Phase 7 cross-OEM matrix.

If ALL hard gates PASS + user signs off → Phase 6 fully signed off. Plan 08 Task 3 (close-out) runs next.

---

*Phase 6 handoff runbook — 11 steps covering UX-01..09 + SHR-01..04 (13 REQs) + 8 deferred UAT bonus checks per D-33.*
*Follows `05-HANDOFF.md` format. Phase 6 acceptance gate: all hard gates PASS on Xiaomi 13T (or hard gates PASS + soft gates documented with user agreement, or N/A items documented with reason).*
*APK: `app/build/outputs/apk/debug/app-debug.apk` (~92 MB, Plan 06-08 Task 1, 172 unit tests GREEN, 0 ignored).*
*9/9 D-32 grep-asserts verified post-clean-build at Plan 08 Task 1.*
