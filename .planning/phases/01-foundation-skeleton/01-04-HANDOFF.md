# Phase 1 Handoff — FND-08 Device Verification Runbook

**Prerequisites (user-side):**
1. Physical Android 9+ device with USB debugging enabled.
   - Settings → About phone → tap Build number 7 times (enables Developer Options)
   - Settings → Developer options → USB debugging: ON
2. USB cable connected between phone and this PC.
3. When prompted on the phone, tap "Allow" for the RSA fingerprint dialog.

**What you are verifying:**
- FND-01: `./gradlew :app:assembleDebug` produced an APK (already done in Task 1 of this plan).
- FND-08: That APK installs on your physical device and the app opens without crashing.
- FND-06: The CAMERA permission flow works as designed (first entry prompt → denial rationale → Open Settings CTA).
- FND-07: StrictMode + LeakCanary are active in the debug build.

***

## Step 1 — Confirm the APK exists

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
```

Expected: file listed, size ~32 MB (debug, unminified — includes Compose + Hilt + Navigation + LeakCanary runtime across 8 dex files).

> NOTE: Original plan estimated 5-8 MB; actual is ~32 MB because the debug variant is unminified multi-dex. This is normal for an R8-disabled debug build with Compose Material3 + Hilt codegen + LeakCanary. The release variant (next milestone) will be significantly smaller once R8 shrinking is enabled.

***

## Step 2 — Confirm the phone is detected

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
```

Expected output:
```
List of devices attached
<your-device-serial>    device
```

If the status is `unauthorized`, tap "Allow" on the phone's RSA fingerprint dialog and re-run.
If no devices are listed, verify USB cable is "data + charging" (not charge-only) and try another USB port.

***

## Step 3 — Install the APK

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected output:
```
Performing Streamed Install
Success
```

If you see `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall first:
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" uninstall com.bugzz.filter.camera
```
Then re-run the install command.

If you see `INSTALL_FAILED_INSUFFICIENT_STORAGE`, free space on the device (~100 MB minimum).

***

## Step 4 — Launch the app

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
```

Expected: the app opens on the phone. Console prints:
```
Starting: Intent { cmp=com.bugzz.filter.camera/.MainActivity }
```

No error stack trace within 3 seconds.

***

## Step 5 — Visual verification on device (FND-08 + FND-06)

Walk through this sequence on the phone. Each step is a PASS/FAIL gate.

| Step | Expected | Pass? |
|------|----------|-------|
| 1. App opens | "Splash route — Phase 1 stub" text visible with "Continue" button | [ ] |
| 2. Tap Continue | Navigates to Home: "Home route — Phase 1 stub" + "Open Camera" + "My Collection" buttons | [ ] |
| 3. Tap "Open Camera" | System CAMERA permission dialog appears: "Allow Bugzz to take pictures and record video?" | [ ] |
| 4. Tap "Don't allow" | Rationale screen renders: "Camera permission needed" + "Grant permission" + "Open Settings" buttons (NOT a blank screen) | [ ] |
| 5. Tap "Open Settings" | Android Settings → App info → Permissions page for Bugzz opens | [ ] |
| 6. Press back, return to app, tap "Grant permission" | System prompt re-appears; tap "While using the app" | [ ] |
| 7. | "Camera (granted)" stub renders with "Go to Preview" button | [ ] |
| 8. Tap "Go to Preview" | "Preview route — Phase 1 stub" with "Back" button | [ ] |
| 9. Tap "Back" | Returns to Camera (granted) stub | [ ] |
| 10. Press OS back twice | Home stub visible | [ ] |
| 11. Tap "My Collection" | "Collection route — Phase 1 stub" with "Back" button | [ ] |
| 12. Tap "Back" | Home stub visible | [ ] |

All 12 boxes checked = FND-08 + FND-06 PASS.

***

## Step 6 — Tail logcat for crashes (sanity check)

While the app is running on the device, run:

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -d -s AndroidRuntime:E --pid=$("C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pidof -s com.bugzz.filter.camera)
```

Expected: empty output (no `FATAL EXCEPTION` lines). LeakCanary init logs and Compose recomposition info are fine.

If `pidof` is not present on your device (older Android 9 builds), use:

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -d *:E | grep -i bugzz
```

***

## Step 7 — StrictMode check (FND-07)

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -d -s StrictMode
```

Expected: at most 1-2 lines about disk reads on main thread from Compose font loader (research Gotcha G-13 — these are expected first-launch, non-crash). No `penaltyDeath` / `StrictMode policy violation` stack traces that crash the app.

***

## Step 8 — LeakCanary presence check (FND-07)

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -d -s LeakCanary
```

Expected: log lines mentioning `LeakCanary` install / initialization. Even in the absence of leaks, LeakCanary emits startup diagnostic messages. A typical first-launch log includes:
```
LeakCanary: LeakCanary is running and ready to detect leaks
```

***

## If all steps pass

Phase 1 is complete. Record "PASS" in the Phase 1 SUMMARY with your device model + Android version. Proceed to `/gsd-plan-phase 2`.

Please also tell the executor:
- Device manufacturer + model (e.g., "Pixel 6a")
- Android version (e.g., "Android 14")
- Checklist result (e.g., "12/12 PASS")
- Any notable logcat lines

## If any step fails

1. Copy the failing step's command + output.
2. Run `/gsd-debug` with the failure description.
3. Do NOT manually edit source — fix via the GSD workflow so regressions are tracked.

***

## Appendix A: Commands if adb is on your PATH

If you set `ANDROID_HOME` and added `platform-tools` to PATH (you haven't in this session), you can use the short form:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.bugzz.filter.camera/.MainActivity
```

Otherwise, use the absolute-path form above.

***

## Appendix B: Quick-resume commands after reboot

If your PC reboots between verification attempts, reconnect device + reverify in one shot:

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" kill-server && \
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" start-server && \
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
```

Expected: `start-server` prints `* daemon started successfully` then `devices` lists your phone.
