# Bugzz

## What This Is

Bugzz là một Android AR camera app chạy trên điện thoại: bật camera trước, detect khuôn mặt, và phủ các hiệu ứng con côn trùng/bug (kiến, gián, nhện, sâu...) bò trên mặt người theo thời gian thực. User chụp ảnh hoặc quay video có filter rồi chia sẻ — một app "prank" để làm trò với bạn bè.

Sản phẩm là **clone feature-parity** của app tham chiếu `com.insect.filters.funny.prank.bug.filter.face.camera` (tên Play Store: "Bugzz Filters Prank", version 1.2.7). Mục đích cá nhân — chủ dự án dùng để học stack AR + face filter và sử dụng riêng, không phát hành Play Store ở giai đoạn hiện tại.

## Core Value

**Live AR preview mượt + bug animation bám chính xác landmark khuôn mặt.** Nếu filter giật hoặc bug không bám theo mặt, mọi thứ khác (chụp, quay, share) vô nghĩa — đây là phần "wow" của app. Mọi tradeoff về kiến trúc và UI đều phục vụ chất lượng live preview.

## Requirements

### Validated

- [x] Live camera preview qua CameraX với face detection realtime *(Validated in Phase 2)*
- [x] Overlay animated bug sprites bám theo face contour (ML Kit landmark) *(Validated in Phase 3 — ant-on-nose production filter tracking landmarks on Xiaomi 13T)*
- [x] Chụp ảnh với filter đã áp → lưu vào device gallery *(Validated in Phase 3 — 31 JPEGs saved to DCIM/Bugzz/ via MediaStore, bug sprite baked in per OverlayEffect IMAGE_CAPTURE target)*
- [x] Camera permission handling (runtime permissions, Android 9+) *(Validated in Phase 1)*
- [x] Hỗ trợ cả camera trước và sau *(Front-cam validated Phase 2+3; back-cam binding validated Phase 2)*

### Active

- [ ] Danh sách filter chọn được (swipe/grid picker) — match số lượng + loại trong app reference *(Phase 4)*
- [ ] Quay video với filter + audio → lưu vào device gallery *(Phase 5)*
- [ ] Share artifact (ảnh/video) ra social app (FB, IG, TikTok, Zalo...) qua Android share intent *(Phase 6)*
- [ ] UI/UX clone y hệt reference: splash, home, camera screen, filter picker, preview & save screen *(Phase 6)*

### Out of Scope (MVP)

- **Monetization (AdMob + AppLovin + Google Play Billing IAP)** — reference app có sẵn nhưng user yêu cầu defer tới milestone sau
- **Localization (i18n multi-locale)** — chỉ English UI, khớp reference
- **Cloud sync / account** — app chạy fully offline, không cần backend
- **Beauty filter / retouch** — reference là bug prank thuần, không có beauty mode
- **Play Store publication** — chỉ cá nhân, build APK sideload qua ADB
- **iOS port** — Android-only

## Context

- **App tham chiếu**: APK đã được extract tại `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` (base APK, 67MB) — research agent sẽ phân tích manifest, layouts, assets, resources để xác định feature set đầy đủ và extract asset sprite bug để tái sử dụng.
- **Chủ dự án**: Solo developer, có Android Studio, không có Google Play Developer account (chưa cần).
- **Ngôn ngữ giao tiếp**: Tiếng Việt (chủ dự án); UI app bằng tiếng Anh (match reference).
- **Device test**: Điện thoại Android thật qua USB ADB — bắt buộc để test camera/face detection chính xác.
- **Reference metadata** (từ manifest.json):
  - Package: `com.insect.filters.funny.prank.bug.filter.face.camera`
  - Version: 1.2.7 (versionCode 31)
  - minSdk 28, targetSdk 35
  - Permissions đáng chú ý: CAMERA, RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, POST_NOTIFICATIONS, FOREGROUND_SERVICE, VIBRATE, INTERNET, AD_ID, BILLING
  - SDKs: AppLovin (mediation ads), Google Play Services (AdMob + Billing)

## Constraints

- **Tech stack**: Native Android — Kotlin, CameraX, ML Kit Face Detection (contour mode cho landmark accurate), Jetpack Compose *hoặc* Views (sẽ quyết ở phase 1). Không xét Flutter / React Native / Unity — đã khóa.
- **Target SDK**: minSdk = 28 (Android 9), targetSdk = 35 (Android 15) — match reference app 1:1.
- **Architecture**: MVVM / MVI tiêu chuẩn Android (quyết chi tiết ở phase 1 sau khi research).
- **Performance**: Live preview phải đạt ≥ 24 fps trên thiết bị test (Android 9+ tầm trung). Face detection latency < 100ms/frame.
- **Storage**: Scoped storage (bắt buộc từ Android 10), dùng MediaStore API để lưu ảnh/video vào DCIM/Bugzz.
- **Legal**: Asset extract từ reference APK dùng trực tiếp cho personal use — OK ở giai đoạn này, KHÔNG được dùng nếu sau này public store. Chủ dự án đã xác nhận sẽ thay UI + assets trước khi publish nếu có.
- **Solo dev**: Không có áp lực deadline; ưu tiên code quality + feature parity chính xác hơn tốc độ.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Native Android (Kotlin) — không Flutter/RN/Unity | Hiệu năng AR realtime cần native; match reference 1:1 dễ hơn | — Pending |
| CameraX + ML Kit Face Detection (contour mode) | Stack chuẩn 2025 cho face AR; contour cho landmark chi tiết bám bug chính xác | — Pending |
| minSdk = 28 (Android 9) | Match reference; giảm edge case scoped storage / permissions cũ; modern APIs cleaner | — Pending |
| targetSdk = 35 (Android 15) | Match reference; latest stable | — Pending |
| MVP defer toàn bộ monetization (AdMob, AppLovin, IAP) | Chủ dự án dùng cá nhân, không cần doanh thu; add lại ở milestone sau | — Pending |
| UI tiếng Anh, không i18n ở MVP | Match reference; giảm scope | — Pending |
| Asset sprite bug extract từ reference APK | Personal use only; swap với asset riêng trước khi publish nếu sau này cần | ⚠️ Revisit — bắt buộc swap nếu public store |
| Test trên điện thoại thật qua USB ADB | Emulator không mô phỏng camera/face detection chính xác | — Pending |
| App package name: `com.bugzz.filter.camera` (draft) | Tránh dùng package name của reference; `bugzz` + `filter` + `camera` mô tả đúng; phase 1 sẽ chốt | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-20 after Phase 3 completion — render + capture pipeline validated on Xiaomi 13T with ant-on-nose production filter*
