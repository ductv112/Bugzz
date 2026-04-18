# Feature Research

**Domain:** Android AR face filter prank camera (bug/insect theme)
**Researched:** 2026-04-18
**Confidence:** HIGH (reference APK disassembled directly; category expectations cross-checked against 4+ competitor apps)

## Reference APK Forensics Summary

**Evidence source:** Unzipped `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` (v1.2.7, 67MB base APK) and scanned resources + DEX string tables. Raw binary AXML could not be parsed (no apktool/androguard available on host), so findings are derived from:

1. Layout filenames (`res/layout/*.xml`) — 327 total, ~85 app-specific after filtering framework/SDK
2. Drawable filenames (`res/drawable/`, `res/drawable-hdpi/`, `res/drawable-xhdpi/`) — 640 files total
3. Raw resource files (`res/raw/`, `res/anim/`, `res/animator/`, `res/navigation/`, `res/font/`)
4. JSON asset configs (`assets/ad_config.json`, `assets/iap_id.json`, `assets/fo_config.json`, `assets/onboarding/*.json`)
5. Native libs / ML models (`assets/mlkit_facemesh/`, `assets/materials/*.filamat`, `assets/shaders/`, `assets/environments/`)
6. DEX string tables (6 dex files, grepped for `[a-z_]{10,80}` tokens matching feature keywords)
7. Backend URL extraction from DEX

**Critical architectural finding:** Filter assets are **NOT bundled** in the APK. They are downloaded on demand from `https://stores.volio.vn/stores/api/v5.0/public/` (Volio Group, Vietnamese publisher). The APK only ships:

- **3 Lottie files** used as home-screen / splash / info animations: `home_filter.json`, `home_lottie.json`, `spider_prankfilter.json`, `splash_1.json`, `splash_2.json`, plus 3 Lottie onboarding screens in `assets/onboarding/onboarding_{1,2,3}.json` and `demo.json`.
- **ML Kit Face Mesh + MediaPipe models** (`assets/mlkit_facemesh/face_landmark_with_attention.tflite`, `face_mesh_graph.binarypb`, `face_short_range_graph.binarypb`, `facedetector-front.f16.tflite`).
- **Filament 3D renderer materials** (`assets/materials/*.filamat` — 13 files including `video_texture_chroma_key.filamat`, `opaque_textured.filamat`, `transparent_textured.filamat`, `view_renderable.filamat`).
- **Media3/ExoPlayer GLSL shaders** for video pipeline.

**Therefore: the actual filter count in the reference is server-driven and dynamic.** We cannot extract a "ground-truth" filter inventory from the APK. Competitor analysis + Play Store reviews ("to try each insect you have to watch a 30 second ad") suggest ~25-60 filters across bug/insect + face-morph + time-warp categories.

**Screens identified from `res/layout/` app-specific fragments (26 confirmed):**

`splash`, `language`, `welcome`, `welcome_back`, `onboarding`, `policy`, `home`, `face_filter` (filter picker for face mode), `filter` (filter picker for "insect-on-anything" mode), `record` (live camera), `music` (music picker), `time_warp` (TimeWarp Scan feature), `trending_video` (trending showcase), `my_collection` (saved outputs gallery), `preview_my_collection`, `preview_result`, `result_preview`, `result`, `setting`. Plus single-purpose activities: `main` (NavHostFragment only — single-activity nav graph via `res/navigation/main_nav.xml` + `splash_nav.xml`).

**Dialogs:** `allow_microphone`, `delete`, `download_music`, `exit_save_video`, `feedback` (rating feedback w/ star selector `ic_start_rate_selected/_un_selected`), `full_iap`, `premium_new` (weekly/lifetime plans), `inter_full`, `rate` (in-app rating prompt), `loading_reward_ads`.

**Record-screen control icons (from DEX `ic_record_*` strings):** `back`, `start` (+ `_new` variant), `end` (+ `_new` variant), `flip` (camera switch), `flash_on/_off`, `coundown_on/_off` (countdown toggle — note typo `coundown`), `music` / `music_add` / `music_remove`, `permission`, `loading`. **No** `_grid`, `_timer` (in minutes sense), `_beauty`, `_intensity`, `_shake` icons exist — those features are not in the reference.

**Countdown values:** Presence of `item_record_countdown.xml` item layout implies a countdown picker UI (likely 3/5/10s preset buttons — standard in TikTok/IG).

**Trending tab icons:** `ic_trending_preview_camera`, `_favourite`, `_favourite_enable`, `_favourite_state`, `_music`, `_pause`, `_play`, `_share`. Confirms Trending is a scrollable video feed of curated filter demos with per-item favourite + share + "use this filter in camera" jump.

**Onboarding:** 3 Lottie screens (`onboarding_1/2/3.json`) — this is a 3-step onboarding carousel, the Android prank-camera category default.

**Permissions present (from `manifest.json`):** CAMERA, RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, POST_NOTIFICATIONS, FOREGROUND_SERVICE, VIBRATE, INTERNET, plus ad/billing permissions. Notable: **no SENSOR permission** (no shake-to-scare feature confirmed absent).

**Strings exposing UX copy (extracted from DEX):**
- "Let's prank", "Let's see which bug will make them scream", "Scare or share", "No ads just pranks"
- "Ready to prank like a pro", "Ready record for laughs", "One tap instant laughter"
- "Add bugs or any insect and make everyone scream laugh"
- "Filter could not be loaded please check your network connection" (confirms network-dependent filters)
- "Downloading filter", "Downloading music", "Preparing to share"
- "Congrats the watermark has been removed", "Remove watermark", "Limited for use of premium features"
- "No videos yet, create a new one" (my collection empty state)
- "Subscribed users have unlimited use and access to all of its premium features"
- "Are you sure you want to delete this file", "Do you really want to exit"
- "How do you feel about the app, your feedback is important to us", "Oh no, please leave us some feedback"
- "Share to Instagram / TikTok / YouTube / Facebook / Messenger" (and generic share intent)

**Shake-to-scare investigation:** DEX contains `accelerometer`, `device_shake`, `shake_phone` strings — but **all occurrences are in `classes3.dex`** which is the AppLovin/Pangle/Mbridge ad-SDK bundle ("shake to skip ad" anti-fraud). No shake handler in the app's own classes. **Verdict: reference app does NOT have shake-to-scare as a user feature.** Absence confirmed by missing SENSOR permission in manifest.

## Feature Landscape

### Table Stakes (Users Expect These)

Users of a "face filter camera" on Android will uninstall if any of these are missing or broken.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Live camera preview with front/back camera and flip button | Category default since Snapchat 2011 | M | CameraX Preview + PreviewView. Front camera is the primary; back camera support is table-stakes cheap. **Confirmed in ref** (`ic_record_flip`). |
| Real-time face detection + overlay tracking on preview | Core value of the product | XL | ML Kit Face Mesh (attention model) is what the reference ships (`assets/mlkit_facemesh/face_landmark_with_attention.tflite`). 468 landmarks give accurate bug attachment points. Must run per-frame on ImageAnalysis pipeline, <100ms/frame. |
| Apply filter and see it in live preview | This IS the app | XL | Rendering overlay composited on camera frames. Reference uses Google Filament (`.filamat` files) for 3D renderable + `view_renderable.filamat` for 2D Lottie-like overlays. Alternative for our clone: simpler Canvas/OpenGL sprite renderer for 2D bugs; Filament only if we need 3D models. |
| Capture photo with filter burned in | Can't share what you can't save | M | CameraX ImageCapture + GL offscreen render. Must include both camera frame and filter overlay. Save via MediaStore to `DCIM/Bugzz/`. |
| Record video with filter burned in + audio | "Prank" videos are the primary share artifact | L | CameraX VideoCapture + custom video effect processor OR Media3 Transformer post-process. Audio from microphone. Typical limit 15-60s (see below). **Confirmed in ref**: `MAX_DURATION`/`MIN_DURATION` in DEX. |
| Save output to device gallery (MediaStore) | Users expect Photos app to see output | S | MediaStore API with `Environment.DIRECTORY_DCIM`/Bugzz subfolder. Scoped storage required from Android 10+. |
| Share via Android share sheet (ACTION_SEND) | Users share on WhatsApp/Zalo/etc. that app-specific buttons don't know about | S | Generic share intent + FileProvider for content URIs. **Confirmed in ref** (`ic_share`, `image_share_filepaths.xml`). |
| Runtime permission requests (Camera, Mic) | Android 6+ mandatory; Android 13+ also POST_NOTIFICATIONS | S | ActivityResult APIs. Deny rationale dialog + "settings" fallback. **Confirmed**: `dialog_allow_microphone.xml`, `ic_mic_permission_dialog`. |
| Filter picker (horizontal strip or grid) | Users need to browse what's available | M | Reference has TWO pickers: `fragment_face_filter` (face-only filters) and `fragment_filter` (insect-on-anything filters). Horizontal RecyclerView + swipe-to-change during live preview is typical. |
| Output preview/result screen | Review before sharing | M | Static photo/video preview with buttons: Save, Share, Delete, Back. **Confirmed**: `fragment_result`, `fragment_preview_result`. |
| Splash screen | Perf perception + loading SDKs | S | **Confirmed**: `fragment_splash`, `activity_splash`, Lottie `splash_1.json`/`splash_2.json`. |
| Onboarding flow (first run) | Market default for consumer apps | S | 3-screen Lottie carousel. **Confirmed**: `fragment_onboarding`, 3 Lottie JSONs. |
| Empty state for gallery | Non-broken first-run experience | XS | "No videos yet, create a new one" string + illustration. **Confirmed**: `item_no_item_my_collection.xml`, `ic_no_item_my_collection`. |
| Delete saved output | Users need to clean up | XS | Confirmation dialog + MediaStore delete. **Confirmed**: `dialog_delete.xml`, "are you sure you want to delete this file". |
| Exit-without-save warning during recording | Prevents accidental loss | XS | **Confirmed**: `dialog_exit_save_video.xml`, "do you really want to exit". |

### Differentiators (Competitive Advantage)

Features that set one prank-filter app apart. The reference app includes all of these; some are cuttable for MVP.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Large filter library (25-50+ bugs) with categories | More variety = more return visits | L (content, not code) | Reference fetches from `stores.volio.vn`. For our clone: hardcode ~15-25 bundled for v1, add CDN later if ever. Categories likely: `ant`, `spider`, `cockroach`, `worm`, `beetle`, `fly`, `scorpion`, `centipede`, plus face-morph filters (non-bug) like `faceart` warp. |
| Countdown timer before capture/record (3s/5s/10s presets) | Solo users need hands-free start | S | **Confirmed in ref** (`item_record_countdown.xml`, `ic_record_coundown_on/_off`). Simple chained post-delayed UI. |
| Flash / torch control (front flash = screen fill; back flash = LED) | Low-light prank scenarios | S | **Confirmed** (`ic_record_flash_on/_off`). CameraX `Camera.cameraControl.enableTorch()`. Front flash is a white overlay trick. |
| Music track overlay on video | TikTok-style vibe; makes pranks punchier | L | **Confirmed**: `fragment_music.xml`, `ic_record_music_add/_remove`, `dialog_download_music.xml`. Music is also downloaded from server. Mixing: user-record audio vs muted + music vs blended. Use Media3 Transformer or FFmpeg-Mobile. |
| Trending / explore feed of filter demos | Discovery + "wow, I want that one" | M | **Confirmed**: `fragment_trending_video.xml`, `item_trending_preview.xml`, with favourite + share + "use in camera" actions. Videos are hosted samples (`p16-sign-sg.tiktokcdn.com` URL appears in DEX). Low priority for personal clone. |
| My Collection (gallery of saved outputs) inside the app | Don't force users to dig through Photos | S | **Confirmed**: `fragment_my_collection.xml`, `fragment_preview_my_collection.xml`. MediaStore query filtered to our DCIM/Bugzz folder. |
| Home screen with two entry modes ("Face Filter" + "Insect Filter") | Differentiates face-tracked vs position-anywhere filters | S | **Confirmed**: `btn_face_filter_home.png` + `btn_insect_filter_home.png`, `HomeFragmentToFaceFilter` + `HomeFragmentToFilter` nav actions. "Face Filter" = bug locked to face landmarks. "Insect Filter" = drag/resize/rotate a bug anywhere on frame (not face-tracked). Both modes can capture photo/video. |
| Drag / resize / rotate filter placement (Insect Filter mode) | Prank on pets, walls, table | M | 2-finger gesture on overlay. Per app description: "drag, resize, rotate to blend perfectly". No face tracking in this mode — pure AR sticker. |
| TimeWarp Scan filter | Viral TikTok trend since 2020; sister-app Fazee (same publisher) has this | L | **Confirmed**: `fragment_time_warp.xml`, `TimeWarp` + `time_warp` tokens. Blue scanning line freezes image top-to-bottom or side-to-side — clever pixel-copy over time per scan position. |
| Watermark on free output + premium removal | Monetization hook; viral distribution | S | **Confirmed**: `ic_watermark`, `bg_remove_watermark`, `toast_custom_remove_watermark.xml`, "congrats the watermark has been removed". Draw app-logo PNG at bottom-right of output. Defer watermark UI to v1; defer premium-removal to monetization milestone. |
| In-app rating prompt (stars + feedback reasons) | Review count game | S | **Confirmed**: `dialog_rate.xml`, `layout_dialog_rate.xml`, `ic_start_rate_selected`, `img_bg_rate_*`. 5-star selector → high star = Play Store link, low star = feedback form (`dialog_feedback.xml`, `item_reason_feedback.xml`). Defer to v1.x (no Play Store publication in our scope). |
| Direct-share buttons for Instagram/TikTok/YouTube/FB (+ generic share) | One-tap to platform user cares about | S | **Confirmed**: `img_icon_facebook`, `img_icon_intar` (Instagram), `img_icon_titok` (TikTok), `img_icon_youtube`, `item_social_share.xml`, strings `share_to_facebook/instagram/youtube`. Package-name-targeted `ACTION_SEND` intents with package fallback. |
| Welcome-back screen for returning users | Re-engagement / churn reduction | S | **Confirmed**: `activity_welcome_back.xml`, `fragment_welcome` variants, `Native_WellcomeBack` ad slot. For clone: skip (just goes to home). |
| Language selector (30+ locales) | Global reach | M | **Confirmed**: `fragment_language.xml`, `activity_language.xml`, `item_language.xml`, `ic_language_select`. Per `manifest.json`, reference lists 95 locales. **MVP: English-only per PROJECT.md constraints.** |
| Settings screen (rate app, feedback, privacy policy, about) | Standard consumer app | S | **Confirmed**: `fragment_setting.xml`, `item_setting.xml`, `fragment_policy.xml`, `dialog_feedback.xml`. |

### Anti-Features (Deliberately NOT Building for MVP)

Things the reference app has OR other category apps have that we should skip. Some are deferred per PROJECT.md explicit scope; others are domain pitfalls.

| Feature | Why Requested | Why Problematic for Us | Alternative |
|---------|---------------|------------------------|-------------|
| **Monetization: AdMob banners / interstitials / rewarded-ad filter unlocks** | Reference app has AdMob + AppLovin + Pangle/Mbridge + Vungle/Mintegral full stack (`ad_config.json` lists 30+ ad slots). | PROJECT.md "Out of Scope (MVP)" explicitly defers. Solo personal use, no revenue need. Adds 15MB+ SDK bloat. | Build the app clean; hook monetization as a later milestone feature flag. |
| **Monetization: Google Play Billing IAP (weekly + lifetime)** | `iap_id.json` confirms `id_weekly` subscription + `id_lifetime` one-time. | PROJECT.md deferred. Requires Play Developer account + tax setup. | Treat all filters as unlocked in MVP. Hide premium dialog entirely. |
| **Social account / login (Facebook/Google sign-in)** | Reference bundles Facebook SDK (`com.facebook.*` drawables/layouts). | Offline-first per PROJECT.md constraints. No backend. Login adds complexity + privacy surface. | Share via Android share intent only. No login anywhere. |
| **Cloud sync / cross-device backup** | Many camera apps offer this. | No backend; PROJECT.md "app chạy fully offline". | Local-only gallery via MediaStore. Users rely on Google Photos backup for cloud. |
| **Server-driven filter catalog (remote CDN fetch)** | Reference uses `stores.volio.vn`. Lets publisher ship new filters without APK update. | Requires backend infra we don't have. Breaks offline-first. Latency = "downloading filter" stalls UX. Licensing of filter assets from stores.volio.vn would be piracy. | Bundle 15-25 filters directly in assets/ or download once at install. Treat the filter list as a build-time constant. |
| **Social feed / following other users / user-generated content sharing inside app** | TikTok-style engagement. | Not in reference. Requires backend, moderation, legal (DSA/CSAM compliance). Massive scope explosion. | Trending tab is pre-curated static demos only (if we build it at all). |
| **Beauty filter / skin smoothing / makeup AR** | Very common in face-camera category. | Not in reference (bug prank is thematic opposite of beautification). Different ML pipeline (segmentation + color transfer). | Explicitly excluded per PROJECT.md. |
| **AR stickers beyond bug theme (hats, glasses, animal ears)** | Common category feature. | Off-theme. Scope creep. Reference is bug-only. | Stay on theme; all filters are insect/creepy-crawly. Exception: TimeWarp Scan (non-bug but in reference). |
| **Shake-to-scare / shake phone to spawn bug** | Intuitive for prank theme — reviewers might expect this. | **Not in reference** (no SENSOR permission, no app-level accelerometer usage). Adds scope without precedent. | Skip. If desired later, add as v2 differentiator. |
| **Per-filter sound effects / scream audio / volume slider** | Would enhance prank value. | **Not in reference** (audio assets limited to `.wav`/`.mp3` counts of 3-5 per DEX which are all ad-SDK internal; no insect sound effect assets found in `assets/`). Reference delegates audio to user-selected music track only. | Skip per-filter sounds for MVP. Use reference's approach: optional music overlay. Could add in v2. |
| **Intensity slider per filter** | Category trend in 2025 face filter apps (CapCut, BeautyPlus). | **Not in reference.** Adds rendering complexity (parameter-driven shader). | Skip — "bug is there or it isn't" is binary enough. |
| **Multi-face detection (>1 face at once)** | Common in category (apps detect up to 6 faces). | ML Kit Face Mesh supports it automatically. But reference likely renders filter on the "primary/largest" face only (typical). | Single-face for MVP. ML Kit returns up to multiple; we filter to largest. Multi-face can be v1.x add if trivial. |
| **Grid lines overlay on viewfinder** | Pro cameras have this. | **Not in reference** (no `ic_record_grid` asset). Not relevant to prank use case. | Skip. |
| **Self-timer in minutes (long-exposure style)** | Reference has `Timer`/`CountDown` keywords, but... | `CountDown` in reference = 3/5/10 second countdown, not long timer. No evidence of long self-timer. | 3/5/10s countdown only. |
| **Video editing post-capture (trim, filters, text, effects)** | Full editor is common (CapCut, InShot). | Reference has no edit UI — just record + preview + save. Scope explosion. | Skip. Record-and-ship only. User can edit in CapCut if they want. |
| **Photo editing (crop, filters, stickers post-capture)** | FaceArt-category apps do this. | Not in reference. | Skip. |
| **Cross-app sticker packs / AR marketplace** | Snap Lens Store model. | Massive infra. | Skip. |
| **Face swap / face morph between two faces** | Face28 category has this. | Reference is bug-overlay, not face-warp. Different ML pipeline (TPS warp, not mesh overlay). | Skip for bug-theme fidelity. TimeWarp is a closely-related effect we DO keep per reference. |

## Feature Dependencies

```
Camera Permission Runtime Request
    └──required by──> Live Camera Preview (CameraX)
                           └──required by──> Face Detection Pipeline (ML Kit Face Mesh)
                           │                      └──required by──> Face Filter (bug-on-face) Rendering
                           │                                             └──required by──> Photo Capture with Filter
                           │                                             └──required by──> Video Record with Filter
                           └──required by──> Insect Filter (drag-anywhere) Rendering
                                                  └──required by──> Photo Capture with Filter
                                                  └──required by──> Video Record with Filter

Microphone Permission Runtime Request
    └──required by──> Video Record with Audio
                           └──optionally uses──> Music Track Overlay
                                                     └──requires──> Music Picker UI

Storage / MediaStore Access
    └──required by──> Save Photo/Video output
                           └──required by──> My Collection (read back saved outputs)
                                                  └──required by──> Delete Saved Item
                                                  └──required by──> Share Saved Item (later)
    └──required by──> Watermark Compositing (draw logo PNG into output before save)

Share Feature
    └──requires──> Saved Output File + FileProvider setup
    └──enhanced by──> Direct-share buttons (FB/IG/TikTok/YT) — optional, falls back to generic share sheet

Filter Picker
    └──requires──> Filter Catalog (bundled JSON + asset sprites/models)
    └──optionally requires──> Remote CDN (EXCLUDED from MVP)

Countdown Timer
    └──enhances──> Photo Capture / Video Record Start (chained delay before shutter)

Flash / Torch
    └──enhances──> Photo Capture / Video Record (low-light use case)
    └──limited by──> Front-camera phones: simulated via white screen overlay

TimeWarp Scan
    └──requires──> Live Camera Preview
    └──independent of──> Face Detection (TimeWarp is pixel-column copy, not face-tracked)
    └──independent of──> Filter Picker (separate fragment, accessed from dedicated entry)

Trending Feed
    └──requires──> Curated video samples (local assets OR remote fetch)
    └──cross-references──> Filter Picker (to "use this filter in camera")
    └──EXCLUDED from MVP (no backend, no asset licensing)

Onboarding
    └──shown once per install
    └──requires──> Lottie runtime (airbnb/lottie-android)

In-App Rating
    └──requires──> Google Play Core rating API
    └──DEFERRED (not publishing to Play Store per PROJECT.md)
```

### Dependency Notes

- **Filter Picker requires Filter Catalog:** For MVP, catalog is a static `List<Filter>` in code + bundled sprites in `assets/filters/`. No network needed.
- **Music overlay requires post-capture audio mux:** CameraX VideoCapture writes mic-only track; to overlay music we need Media3 Transformer (re-encode) or a custom MediaMuxer pipeline. This makes "add music" L-complexity — consider deferring from v1 to v1.x.
- **TimeWarp is architecturally different from face filters:** it keeps a per-column pixel buffer and renders the frozen portion + live portion split. Does NOT use ML Kit. Separate render pipeline. Consider deferring to v1.x.
- **Watermark compositing must happen at save time, not preview time:** otherwise users see the logo overlaid on viewfinder (distracting). Draw logo PNG onto final bitmap/video only.
- **Multi-face ordering (if we support it later):** ML Kit returns faces in detection order. Sort by bounding-box area descending; render filter only on largest for v1. Multi-face rendering is v1.x.
- **The two entry modes (Face Filter vs Insect Filter) share the Record fragment:** they're effectively different render modes on the same camera screen. Unify in code: `RenderMode { FACE_TRACKED, FREE_PLACEMENT }`.

## MVP Definition

### Launch With (v1) — Core Prank Experience

Minimum viable product — validates the core AR face filter experience.

- [ ] **CameraX live preview** with front/back camera + flip button — [the "camera app" foundation; S complexity on CameraX]
- [ ] **ML Kit Face Mesh face detection per frame** — [core value per PROJECT.md; XL]
- [ ] **Face Filter rendering** (bug sprite locked to face landmarks, animated frame loop) — [THE feature; XL]
- [ ] **Photo capture with filter composited** — [first artifact users can share; M]
- [ ] **Video record with filter + mic audio** (15-60s limit) — [primary social artifact; L]
- [ ] **Runtime permission flows** (Camera, Mic, Notifications on Android 13+) — [Android requirement; S]
- [ ] **Bundled filter catalog: 15-25 bug filters** (spider, ant, cockroach, worm, beetle, fly, scorpion, centipede, wasp, tick, + face-morph TimeWarp as a single bonus) — [category table-stakes volume; L content]
- [ ] **Horizontal filter picker** at bottom of camera screen (swipeable thumbnails) — [M]
- [ ] **Preview/Result screen** (photo or video) with Save/Share/Delete/Back — [M]
- [ ] **Save to MediaStore** under `DCIM/Bugzz/` — [S]
- [ ] **Share via generic Android share sheet** (ACTION_SEND + FileProvider) — [S]
- [ ] **My Collection screen** (list saved outputs from MediaStore query) with play/delete — [S]
- [ ] **Empty state** for My Collection — [XS]
- [ ] **Splash screen** (Lottie or static) — [S]
- [ ] **3-screen onboarding carousel** (Lottie, first-run only) — [S]
- [ ] **Home screen** with two mode buttons: "Face Filter" (tracked) + "Insect Filter" (free-placement) — [S]
- [ ] **Insect Filter mode** — bug as 2D AR sticker with pinch-zoom, rotate, drag gestures — [M]
- [ ] **Exit-without-save confirmation dialog** during recording — [XS]
- [ ] **Delete confirmation dialog** in My Collection — [XS]

### Add After Validation (v1.x) — Polish + Expansion

Features to add once the core prank loop is working and validated on-device.

- [ ] **Countdown timer (3s / 5s / 10s presets)** before capture — [S; high user value]
- [ ] **Flash / torch control** (LED for back cam, white-screen overlay for front cam) — [S]
- [ ] **Music picker + overlay on video output** (bundled tracks, no download) — [L; adds Media3 Transformer]
- [ ] **Direct-share buttons** (Instagram, TikTok, Facebook, YouTube, Messenger) with generic fallback — [S]
- [ ] **Watermark overlay** on output (app logo PNG bottom-right) — [S]
- [ ] **Expand filter catalog to 30-50 filters** (add more bug types + face-morph variants) — [L content]
- [ ] **Multi-face support** (apply filter to all detected faces, not just largest) — [M]
- [ ] **Settings screen** (about, privacy policy, rate, feedback form) — [S]
- [ ] **Gallery preview with share/delete** directly from My Collection — [S]
- [ ] **Filter search / filter categories** (Bugs / Spiders / Insects / Face-Morph tabs) — [S, only worth it if catalog >30]

### Future Consideration (v2+) — Deferred

Features to defer until validation or explicitly excluded.

- [ ] **TimeWarp Scan mode** — [L; vertical slice separate from face filters; in reference but architecturally isolated — build v2 if TimeWarp apps are popular]
- [ ] **Trending feed** — [L content curation + backend if hosted videos]
- [ ] **Language localization** — [explicitly excluded in PROJECT.md]
- [ ] **Monetization (ads, IAP)** — [explicitly deferred to separate milestone]
- [ ] **Premium watermark removal** — [coupled to monetization]
- [ ] **In-app rating prompt** — [only relevant if we ever publish]
- [ ] **Welcome-back re-engagement screen** — [personal-use clone doesn't need retention hooks]
- [ ] **Remote filter CDN / hot-loaded filter packs** — [no backend in scope]
- [ ] **Per-filter sound effects** — [not in reference; could differentiate a future v2]
- [ ] **Shake-to-spawn-bug gesture** — [not in reference; speculative differentiator for v2+]

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Live camera preview (CameraX) | HIGH | MEDIUM | **P1** |
| ML Kit Face Mesh detection | HIGH | HIGH | **P1** |
| Face-tracked filter render | HIGH | HIGH | **P1** |
| Photo capture with filter | HIGH | MEDIUM | **P1** |
| Video record with filter | HIGH | HIGH | **P1** |
| Runtime permissions | HIGH | LOW | **P1** |
| Filter picker (horizontal swipe) | HIGH | MEDIUM | **P1** |
| Bundled filter catalog (15-25) | HIGH | MEDIUM (assets) | **P1** |
| Save to MediaStore | HIGH | LOW | **P1** |
| Generic share intent | HIGH | LOW | **P1** |
| Result preview screen | HIGH | MEDIUM | **P1** |
| My Collection (gallery) | MEDIUM | LOW | **P1** |
| Onboarding carousel | MEDIUM | LOW | **P1** |
| Splash screen | LOW | LOW | **P1** |
| Home screen with two modes | MEDIUM | LOW | **P1** |
| Insect Filter mode (drag-resize-rotate sticker) | MEDIUM | MEDIUM | **P1** |
| Front/back camera flip | MEDIUM | LOW | **P1** |
| Countdown timer | MEDIUM | LOW | **P2** |
| Flash / torch | MEDIUM | LOW | **P2** |
| Music overlay on video | MEDIUM | HIGH | **P2** |
| Direct-share deep-link buttons | LOW | LOW | **P2** |
| Watermark on output | LOW (personal use) | LOW | **P2** |
| Multi-face support | LOW | MEDIUM | **P2** |
| Settings screen | LOW | LOW | **P2** |
| Filter categories / search | LOW | LOW | **P2** |
| TimeWarp Scan | MEDIUM | HIGH | **P3** |
| Trending feed | LOW | HIGH | **P3** |
| Language localization | LOW | MEDIUM | **P3** |
| Monetization (ads + IAP) | — | HIGH | **P3 (separate milestone)** |
| Premium / watermark removal flow | — | LOW (after monetization) | **P3** |
| In-app rating prompt | LOW (not publishing) | LOW | **P3** |
| Shake-to-scare | SPECULATIVE | LOW | **Skip (not in ref)** |
| Per-filter sound effects | SPECULATIVE | MEDIUM | **Skip (not in ref)** |

**Priority key:**
- **P1:** Must have for v1 launch (MVP validation). 20 items.
- **P2:** Should have, add in v1.x as polish. 10 items.
- **P3:** Future consideration or explicitly deferred. 6+ items.

## Competitor Feature Analysis

| Feature | **Bugzz Filters Prank (reference)** | **Bug on Face Filter** (BugFilter.BugsFacePrank) | **Funny Insect Prank Filter** (com.insect.prank.filter) | **Fazee** (sister app, same publisher, time-warp focused) | **Our Approach** |
|---------|-------------------------------------|--------------------------------------------------|---------------------------------------------------------|-----------------------------------------------------------|------------------|
| Bug filter count | ~25-50 (server-fetched, exact TBD) | ~20-30 bugs | Smaller, ~10-15 | Few bugs, many face-morph + time-warp | **Bundle 15-25 in v1** |
| Face tracking | ML Kit Face Mesh + Filament | Unknown SDK | Unknown SDK | ML Kit Face Mesh (assumed; same publisher) | **ML Kit Face Mesh (matches ref)** |
| Free placement mode (non-tracked AR sticker) | YES — "Insect Filter" mode | YES (primary mode) | YES | NO | **YES in v1** |
| Photo + Video modes | BOTH | BOTH | BOTH | BOTH | **BOTH in v1** |
| Video duration limit | MAX_DURATION constant in DEX; specific value unknown but industry norm 15-60s | 15s typical | 15s typical | 15-60s | **60s cap in v1** |
| Music overlay | YES (fragment_music, downloaded) | NO | NO | YES | **Defer to v1.x** |
| Countdown timer | YES (3/5/10s) | NO | NO | YES | **v1.x** |
| Flash control | YES | Sometimes | Sometimes | YES | **v1.x** |
| Beauty / skin smoothing | NO | NO | NO | Some | **Skip (off-theme)** |
| Intensity slider | NO | NO | NO | Some | **Skip** |
| Multi-face | Likely single-face render | Single | Single | Mixed | **Single in v1; multi v1.x** |
| Onboarding | 3-screen Lottie | Minimal | Minimal | 3-screen | **3-screen in v1** |
| Trending feed | YES (curated samples) | NO | NO | YES | **Skip (no backend)** |
| TimeWarp Scan | YES (separate fragment) | NO | NO | YES (primary feature) | **Defer to v2+** |
| Direct-share deep-links | YES (IG, TikTok, FB, YT, Messenger) | Generic share only | Generic | YES | **Generic in v1; deep-links v1.x** |
| My Collection in-app | YES | Sometimes | NO | YES | **YES in v1** |
| Watermark on output | YES + premium removal | YES | Sometimes | YES | **v1.x** |
| Ads during filter selection | YES (rewarded ads to unlock) | YES | YES | YES | **Skip entirely in MVP** |
| Subscription (weekly + lifetime) | YES | YES | Sometimes | YES | **Skip in MVP** |
| Backend dependency | CDN for filters + music | Unknown | Unknown | CDN | **Offline-only bundle in MVP** |
| Language support | 95 locales | Few | Few | 30+ | **English-only per PROJECT.md** |
| Shake-to-scare | NO | NO | NO | NO | **Skip (no precedent)** |
| Per-filter sound effects | NO | Some | Some | NO | **Skip v1; maybe v2 differentiator** |

**Key takeaways:**

1. **No competitor in the bug-prank subcategory has shake-to-scare** — it's a feature that sounds right but isn't validated by the market. Skip.
2. **Per-filter sound effects vary** — some lower-tier bug apps ship with screaming audio per filter, but the top-quality apps (Bugzz, Fazee) don't. Suggests audio doesn't move the needle; the bug animation quality does. Skip for v1.
3. **Multi-face is inconsistent** — ML Kit supports it for free, so we can add it cheaply in v1.x without committing to multi-face design in v1.
4. **Filter count matters more than features** — user reviews cite "too few filters" as a churn reason more than "missing countdown timer". Prioritize bundled filter volume (15-25 minimum) over feature breadth.
5. **Direct-share deep-links are a cheap trust signal** — when a user sees an Instagram icon, they know the output is share-friendly. But generic share sheet works fine for MVP.
6. **TimeWarp is a separate product** — same publisher (Volio) has Fazee which is TimeWarp-focused. Bugzz ships TimeWarp as a bonus but it's a different pipeline. Defer to v2+.

## Inheritance Classification: Direct Ref vs Category Default

For downstream REQUIREMENTS.md authors — each feature marked "REF" comes directly from reference APK analysis (confirmed present); "CAT" is a general category default we'd include regardless; "NEW" is a potential differentiator or clarification not directly inherited.

| Feature | Source |
|---------|--------|
| Live camera preview with flip | REF + CAT |
| ML Kit Face Mesh detection | REF (face_landmark_with_attention.tflite in assets) |
| Face-tracked bug filter | REF |
| Insect Filter free-placement mode | REF (btn_insect_filter_home + two entry buttons) |
| Photo capture | REF + CAT |
| Video record with audio | REF + CAT |
| Save to MediaStore DCIM/Bugzz | CAT (package name chosen; reference uses different folder name) |
| Generic share sheet | REF + CAT |
| Direct-share buttons FB/IG/TikTok/YT | REF (img_icon_* strings) |
| My Collection screen | REF (fragment_my_collection) |
| Empty state for Collection | REF (item_no_item_my_collection) |
| Splash screen | REF (fragment_splash + 2 Lottie files) |
| 3-screen onboarding | REF (3 Lottie files in assets/onboarding/) |
| Home with two entry modes | REF (btn_face_filter_home + btn_insect_filter_home) |
| Runtime permissions (camera, mic, notifications) | REF (manifest) + CAT |
| Filter picker horizontal | REF (item_filter.xml + item_face_filter.xml) |
| Countdown timer 3/5/10s | REF (item_record_countdown, ic_record_coundown_on/off) |
| Flash / torch toggle | REF (ic_record_flash_on/off) |
| Music picker + overlay | REF (fragment_music, dialog_download_music) |
| Watermark on output | REF (ic_watermark, bg_remove_watermark) |
| TimeWarp Scan | REF (fragment_time_warp) |
| Trending feed | REF (fragment_trending_video) — SKIPPED for MVP |
| Settings screen | REF (fragment_setting, fragment_policy) |
| Exit-save confirmation dialog | REF (dialog_exit_save_video) |
| Delete confirmation dialog | REF (dialog_delete) |
| In-app rating prompt | REF (dialog_rate) — DEFERRED |
| Welcome / welcome-back screens | REF — SKIPPED for personal clone |
| Language selector (95 locales) | REF — SKIPPED per PROJECT.md |
| Feedback form | REF (dialog_feedback, item_reason_feedback) — DEFERRED |
| Ads (banner/interstitial/rewarded) | REF (ad_config.json 30+ slots) — EXCLUDED per PROJECT.md |
| IAP (weekly + lifetime) | REF (iap_id.json) — EXCLUDED per PROJECT.md |
| Multi-face support | CAT (ML Kit default) |
| Shake-to-scare | NEW speculative — SKIP (no ref, no competitor has it) |
| Per-filter sound effects | NEW speculative — SKIP for v1 |

## Specific Feature-Question Answers (from research prompt)

**Q: How many distinct bug filter types typically in competitors?**
A: 15-50 range. Bugzz reference ships ~25-50 (server-fetched, exact count hidden); direct competitors "Bug on Face Filter" ship ~20-30; low-tier clones ship ~10-15. **Recommendation for v1: bundle 15-25 filters spanning spider / ant / cockroach / worm / beetle / fly / scorpion / centipede / wasp / tick / maggot / mosquito / caterpillar / butterfly / lizard**. This hits table-stakes volume without asset-creation blowing up scope.

**Q: Is "random shake to scare" feature common?**
A: **NO.** Scanned reference APK DEX: accelerometer/shake strings all originate from AppLovin/Pangle/Mbridge ad SDKs (shake-to-skip-ad anti-fraud). The app has no SENSOR permission. No competitor in the bug-prank subcategory advertises this feature. **Skip.** Could be a v2+ differentiator.

**Q: Sound effects: do these apps typically have per-filter sounds? Volume control?**
A: **Mixed, but top-tier apps (Bugzz, Fazee) don't.** Bugzz reference has no per-filter audio assets bundled; audio comes from user-selected music track (`fragment_music.xml`). Lower-quality bug-filter apps sometimes include scream/buzz SFX per filter, but this doesn't correlate with download count or rating. No volume slider — muxer just mixes mic + optional music at fixed levels. **Skip per-filter SFX; defer music overlay to v1.x.**

**Q: Video duration limits?**
A: Reference has `MAX_DURATION` / `MIN_DURATION` constants in DEX but exact values not extractable without deobfuscation. Industry norm: **15s minimum, 60s maximum.** TikTok-style. Recommend 60s cap for v1 with 1s minimum (prevent 0-byte accidental taps).

**Q: Gallery/history screen for saved outputs?**
A: **YES.** Reference has `fragment_my_collection.xml` with `item_my_collection.xml`, `fragment_preview_my_collection.xml`, empty-state `item_no_item_my_collection.xml`, and strings "No videos yet, create a new one", "Are you sure you want to delete this file". **Implement in v1.** Query MediaStore filtered to our DCIM/Bugzz folder.

**Q: Onboarding flow?**
A: **YES — 3 screens.** Three Lottie animations: `assets/onboarding/onboarding_1.json`, `onboarding_2.json`, `onboarding_3.json` (plus `demo.json` which is likely a feature demo / tutorial overlay). Standard 3-slide onboarding carousel (Lottie version 5.12.1 = Bodymovin). **Implement in v1 with airbnb/lottie-android** (well-supported library). Show only on first launch.

**Q: Rating / review prompts?**
A: **YES.** Reference has `dialog_rate.xml` + `layout_dialog_rate.xml` + 5-star selector drawables (`ic_start_rate_selected`, `ic_start_rate_un_selected`, `img_bg_rate_select/unselect`) + branching feedback flow (`dialog_feedback.xml`, `item_reason_feedback.xml` — low stars go to feedback reasons). **DEFERRED** for our clone since PROJECT.md scope is personal use, no Play Store publication. Add in a post-MVP "publish-to-store" milestone if ever needed, and use Google Play In-App Review API (official) rather than custom dialog.

## Sources

### Reference APK direct forensics (HIGH confidence)

- `d:/ClaudeProject/appmobile/Bugzz/reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` — base APK v1.2.7 (31), extracted to `/tmp/bugzz_apk/`. 327 layout files, 640+ drawables, 6 DEX files, 20 top-level asset subfolders.
- `d:/ClaudeProject/appmobile/Bugzz/reference/manifest.json` — permissions + version metadata + 95 locale names.

### Play Store + app listing pages (MEDIUM confidence for feature descriptions)

- [Bugzz: Insect Prank Filters — Google Play](https://play.google.com/store/apps/details?id=com.insect.filters.funny.prank.bug.filter.face.camera&hl=en)
- [Bugzz: Insect Prank Filters on AppBrain](https://www.appbrain.com/app/bugzz-insect-prank-filters/com.insect.filters.funny.prank.bug.filter.face.camera)
- [Insect Filters - Funny Prank on Uptodown](https://insect-filters-funny-prank.en.uptodown.com/android)
- [Bug on Face Filter — Google Play](https://play.google.com/store/apps/details?id=com.BugFilter.BugsFacePrank&hl=en_US)
- [Funny Insect Prank Filter — Google Play](https://play.google.com/store/apps/details?id=com.insect.prank.filter)
- [Fazee (sibling app, same publisher) — Google Play](https://play.google.com/store/apps/details?id=com.face.filters.funny.filter.timewarp.camera.prank)
- [Insect & Bug: Funny Filters — App Store (iOS)](https://apps.apple.com/us/app/insect-bug-funny-filters/id6756876560)

### TimeWarp Scan reference material (MEDIUM confidence)

- [Time Warp Scan — Camera Filter on Google Play](https://play.google.com/store/apps/details?id=com.facescan.timewarp.bluelinefilter.timewarpscanner.scan&hl=en_IN)
- [Time Warp Scanner: Face Scan on Google Play](https://play.google.com/store/apps/details?id=time.warp.face.scan.filter.timewarp)

### Category landscape references (MEDIUM confidence, unverified beyond listing descriptions)

- [7 Best Funny Face Filters App 2025 — Vidnoz](https://www.vidnoz.com/ai-solutions/funny-face-filter.html)
- [13 Best Face Filter Apps in 2025 — trtc.io](https://trtc.io/blog/details/exploring-face-filter-apps-ai-ar-technology)
- [Funny Face Filters Live Camera (Face28) on Google Play](https://play.google.com/store/apps/details?id=com.vysionapps.face28)
- [Volio Group official site](https://volio.group/) — confirmed Vietnamese publisher of reference app.

### Publisher backend URL (HIGH confidence from DEX)

- `https://stores.volio.vn/stores/api/v5.0/public/` — filter catalog CDN endpoint (extracted from `classes.dex`). Confirms filters are server-driven in reference.

---
*Feature research for: Android AR face filter prank camera app (bug/insect theme), feature-parity clone of Bugzz Filters Prank v1.2.7*
*Researched: 2026-04-18*
