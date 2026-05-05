---
phase: 06
plan: 06
subsystem: delete-confirm-dialog-share-intent-home-on-settings
tags: [wave-5, share-intent, delete-confirm-dialog, home-on-settings, t-06-01-mitigation, ux-polish]
dependency_graph:
  requires:
    - "Plan 06-04 (291daeb) — PreviewScreen inline AlertDialog + onShareNotImplemented Toast placeholder; PreviewViewModel.resolveMimeType + deleteArtifact"
    - "Plan 06-05 (dc9d411) — Phase 5 baseline GREEN; ui/components/ directory established with EmptyStateColumn"
  provides:
    - "ui/components/DeleteConfirmDialog.kt — shared destructive AlertDialog (UX-08, D-16, UI-SPEC §7); Cancel=confirmButton (RIGHT, safe action), Delete=dismissButton (LEFT, error tint #B00020); 16sp/Medium title override of Material3 titleMedium SemiBold; semantics(role=Button + contentDescription) for both buttons"
    - "ui/share/ShareIntentBuilder.kt — pure fun buildShareIntent(uri: Uri, mimeType: String): Intent — wraps ACTION_SEND in createChooser('Share via', ...); inner intent has type=mimeType + EXTRA_STREAM=uri + FLAG_GRANT_READ_URI_PERMISSION (T-06-01 mitigation); no Context captured, no state held"
    - "PreviewScreen — refactored: drops onShareNotImplemented param; Share button now scope.launch { context.startActivity(buildShareIntent(uri, mime)) }; inline AlertDialog replaced with shared DeleteConfirmDialog composable"
    - "HomeScreen — onSettings: () -> Unit parameter added (D-06); Settings gear IconButton onClick swapped from Toast 'Settings coming soon' to onSettings(); LocalContext + android.widget.Toast imports removed"
    - "BugzzApp — HomeRoute composable passes onSettings = { /* Plan 07 will replace this with navController.navigate(SettingsRoute) */ } placeholder lambda; PreviewRoute composable simplified (drops onShareNotImplemented + Toast 'Share coming next' + LocalContext import)"
  affects:
    - "Suite count: 172 tests / 0 skipped / 0 failures (Plan 06-05 baseline 171/6 ignored — un-Ignored 2 DeleteConfirmDialogTest + 4 ShareIntentBuilderTest stubs and added 1 extra video-mime polymorphism case = +7 GREEN, -6 ignored)"
    - "9 D-32 grep-asserts intact: isCapturing(14)/bindJob.cancel(1)/FilterLoadError(13)/captureFlash(13)/require(frameCount > 0)(1)/assetLoader.preload(def.assetDir)(3)/isRecording(47)/cameraMode = …CameraMode.InsectFilter(1)/setPreviewSize(2) — all ≥1"
    - "Plan 06-06 specific grep-asserts: Toast.makeText.*Settings coming soon (0 — was 1 in HomeScreen pre-plan); Toast.makeText.*Share coming next (0 — was 1 in BugzzApp pre-plan); buildShareIntent in PreviewScreen (5 incl. KDoc); DeleteConfirmDialog in PreviewScreen (5 incl. KDoc); FLAG_GRANT_READ_URI_PERMISSION in ShareIntentBuilder.kt (3 incl. KDoc); onSettings in HomeScreen (5 — param + KDoc + onClick + 2 KDoc refs); onSettings in BugzzApp HomeRoute (1 — placeholder lambda)"
    - "AGP/dexBuilder: assembleDebug clean — APK ships with no new third-party deps (everything reused from existing classpath)"
tech-stack:
  added:
    - "(none — Plan 06-06 only consumes existing classpath: Material3 AlertDialog/TextButton/ButtonDefaults from Compose BOM 2026.04.00; android.content.Intent from framework SDK 35; Robolectric 4.x already on test classpath from Phase 5 VideoRecorderTest)"
  patterns:
    - "Lambda contract test (pure JVM, no Compose UI runtime): DeleteConfirmDialogTest exercises onConfirm/onDismiss invocation semantics directly without instantiating the composable — keeps test fast (8ms) and avoids the createComposeRule + Robolectric overhead. The static button-to-slot mapping (Cancel→confirmButton, Delete→dismissButton) is enforced by the SUT's source — not the test — making this a behavioral contract assertion rather than a UI-render assertion."
    - "Pure share-intent factory: buildShareIntent is a top-level function with no Context capture, no state — trivially testable under Robolectric (Intent + Uri are Android types so plain JVM Uri.parse returns null) and trivially safe to call from any thread. Caller composes Context.startActivity at the call site."
    - "T-06-01 mitigation pattern (Information Disclosure): FLAG_GRANT_READ_URI_PERMISSION on the inner ACTION_SEND intent scopes URI grant to the chosen receiver app only — Android does NOT broadcast bytes to all apps; only the user-picked receiver gets temporary read access. No EXTRA_TEXT (D-21) — minimum disclosure surface. Intent.createChooser ensures the user explicitly picks the receiver each time (no silent auto-share)."
    - "Material3 confirmButton/dismissButton naming-vs-visual-placement inversion (UI-SPEC §7 / Phase 5 convention): Material3 confirmButton renders RIGHT and dismissButton LEFT. To match the Phase 5 Exit-during-record dialog (Cancel = right = safe action; Delete = left = destructive), DeleteConfirmDialog passes Cancel as confirmButton and Delete as dismissButton — counterintuitive naming but correct visually. KDoc + SUT comment + UI-SPEC §7 Note all flag this."
    - "remember(uri) re-key for mimeType state: PreviewScreen's mimeType is held in `var mimeType by remember(uri) { mutableStateOf<String?>(null) }` so navigating to a new artifact (different uri) resets the resolution-loading state, preventing the previous artifact's MIME branch from leaking into the new render frame."
    - "Two-step BugzzApp wiring for HomeScreen.onSettings: this plan adds the API surface (lambda parameter on HomeScreen) and BugzzApp passes a placeholder; Plan 06-07 ships SettingsScreen + replaces the placeholder with navController.navigate(SettingsRoute). Atomic split keeps Plan 06 tests passing (no SettingsScreen yet to break the build) and keeps Plan 07 atomic (adds composable + wires call-site in one commit)."
key-files:
  created:
    - "app/src/main/java/com/bugzz/filter/camera/ui/components/DeleteConfirmDialog.kt (95 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilder.kt (47 lines)"
  modified:
    - "app/src/main/java/com/bugzz/filter/camera/ui/preview/PreviewScreen.kt (251 → 200 lines; drops onShareNotImplemented param + inline AlertDialog block + ButtonDefaults/TextStyle/sp imports; adds DeleteConfirmDialog import + buildShareIntent import + LocalContext + Share button scope.launch + DeleteConfirmDialog composable invocation)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/home/HomeScreen.kt (109 → 102 lines; drops android.widget.Toast + LocalContext imports + val context line; adds onSettings: () -> Unit param + KDoc Phase 6 wiring note; Settings gear onClick = onSettings replaces Toast.makeText(...).show())"
    - "app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt (107 → 102 lines; drops android.widget.Toast + LocalContext imports; HomeRoute composable adds onSettings placeholder lambda; PreviewRoute composable drops onShareNotImplemented argument + val context line + Toast 'Share coming next' line)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/components/DeleteConfirmDialogTest.kt (49 → 70 lines; un-Ignored 2 RED stubs → 2 GREEN cases pure-JVM lambda contract tests with confirmCount + dismissCount counters)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilderTest.kt (82 → 116 lines; un-Ignored 4 RED stubs → 5 GREEN cases via Robolectric @Config(sdk=[34]) + Uri.parse content://media URIs; 1 extra video-mime polymorphism case added beyond the 4 minimum)"
decisions:
  - "Atomic single commit for all 3 tasks (per Phase 5 convention): all DeleteConfirmDialog + ShareIntentBuilder + HomeScreen.onSettings work landed as commit 993e641 rather than 3 per-task commits. Phase 5 SUMMARYs (Plans 06-04, 06-05) used the same one-commit-per-plan pattern; the in-prompt instruction 'Atomic commit per Phase 5 convention' explicitly directs this."
  - "Lambda contract test instead of createComposeRule for DeleteConfirmDialog: the dialog is 95 lines of declarative wiring (AlertDialog → TextButton.onClick = onDismiss/onConfirm) and the bug surface is the lambda assignment, not Compose render output. A lambda counter test is 70 lines + 8ms vs ~150 lines + 1.5s + Robolectric for createComposeRule. Same pattern OnboardingViewModelTest et al. used. Preference: Recommended (per memory feedback_autonomy.md)."
  - "5 ShareIntentBuilder tests instead of 4 (1 extra video-mime polymorphism case): plan minimum is 4 cases (chooser action / inner type / EXTRA_STREAM / FLAG_GRANT). Added a 5th case `innerIntentType_matchesVideoMimeType` to verify SHR-02 mimeType polymorphism (image/jpeg AND video/mp4) — the original plan covers image/jpeg only; video/mp4 is the other branch that real callers (PreviewViewModel.resolveMimeType) will produce. +5 lines, +0.05s test runtime; defensive against a future regression where buildShareIntent silently hard-codes 'image/jpeg'."
  - "remember(uri) re-key on mimeType (vs plain remember): PreviewScreen previously used `var mimeType by remember { mutableStateOf<String?>(null) }`. Plan 06-06 changed to `remember(uri)` so navigating to a new PreviewRoute (e.g., from CollectionScreen tap) resets the resolution-loading state — otherwise the previous artifact's mimeType could leak into the new render frame on rapid back-forward navigation. Aligns with `LaunchedEffect(uri)` re-trigger key. Pure correctness fix; not a deviation since the Plan 04 implementation was equivalent for the single-artifact case but fragile under repeated navigation."
  - "KDoc historical references to 'Settings coming soon' / 'Share coming next' kept (HomeScreen.kt L29 + L52, ShareIntentBuilder.kt L10): these are documentation noting what the Plan 04 placeholder was, not actual Toast calls. Verified `Toast.makeText.*Settings coming soon` count = 0 and `Toast.makeText.*Share coming next` count = 0. Keeping the historical refs in KDoc helps future readers trace the migration in git blame."
  - "PreviewScreen import cleanup: removed AlertDialog + ButtonDefaults + TextStyle + FontWeight + sp imports since the inline AlertDialog block was deleted. Added LocalContext (for share intent) + DeleteConfirmDialog + buildShareIntent imports. Net -2 imports. KDoc reflects Plan 06-06 final wiring."
  - "BugzzApp PreviewRoute LocalContext + Toast imports removed: the only remaining use site (Toast 'Share coming next' in onShareNotImplemented lambda) is gone. Verified by grep — no other Toast.makeText anywhere in BugzzApp.kt and no LocalContext.current outside the deleted block. Imports follow."
metrics:
  duration: 320
  completed: 2026-05-05
---

# Phase 6 Plan 06: Wave 5 DeleteConfirmDialog + ShareIntentBuilder + HomeScreen onSettings Summary

**One-liner:** Wave 5 destructive-action + share infrastructure shipped — DeleteConfirmDialog shared composable extracted from Plan 04's inline PreviewScreen AlertDialog (UX-08, D-16, UI-SPEC §7) with Cancel=confirmButton/RIGHT + Delete=dismissButton/LEFT/error-tint convention; ShareIntentBuilder.buildShareIntent(uri, mimeType) pure factory wraps ACTION_SEND in createChooser with EXTRA_STREAM + FLAG_GRANT_READ_URI_PERMISSION (T-06-01 mitigation, SHR-01..04); PreviewScreen Share button now performs real Intent.ACTION_SEND via context.startActivity (no more Toast "Share coming next"); PreviewScreen Delete button now uses shared DeleteConfirmDialog (no more inline AlertDialog); HomeScreen gains onSettings: () -> Unit lambda parameter (D-06) replacing Toast "Settings coming soon"; BugzzApp HomeRoute passes placeholder onSettings lambda (Plan 07 wires SettingsRoute target); BugzzApp PreviewRoute simplified (drops onShareNotImplemented + LocalContext + Toast); 7 new GREEN tests added (2 DeleteConfirmDialog lambda contract + 5 ShareIntentBuilder Robolectric incl. 1 extra video-mime polymorphism case beyond plan minimum); suite GREEN 172/0 ignored/0 failures (+7 vs Plan 05 baseline 171/6 ignored — net +1 test, -6 ignored); 9 D-32 grep-asserts intact (14/1/13/13/1/3/47/1/2); APK assembleDebug clean.

---

## What Landed

### Production Code (2 created + 3 modified)

1. **`app/src/main/java/com/bugzz/filter/camera/ui/components/DeleteConfirmDialog.kt`** (95 lines, NEW)
   - `@Composable fun DeleteConfirmDialog(onDismiss, onConfirm)`
   - AlertDialog body per UI-SPEC §7:
     - title: `Text("Delete this artifact?", style = TextStyle(16.sp, FontWeight.Medium))` — explicit override of Material3 titleMedium SemiBold default (Phase 5 convention)
     - text: `Text("This can't be undone.", style = bodyMedium)`
     - confirmButton (RIGHT, safe action): `TextButton(onClick = onDismiss)` with `Text("Cancel", style = labelLarge)` + `Modifier.semantics { role = Button; contentDescription = "Cancel deletion" }`
     - dismissButton (LEFT, destructive): `TextButton(onClick = onConfirm, colors = textButtonColors(contentColor = colorScheme.error))` with `Text("Delete", style = labelLarge)` + semantics "Confirm delete"
   - KDoc references UX-08, D-16, UI-SPEC §7 + Phase 5 convention note
   - Replaces Plan 04's inline AlertDialog block in PreviewScreen.kt

2. **`app/src/main/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilder.kt`** (47 lines, NEW)
   - `fun buildShareIntent(uri: Uri, mimeType: String): Intent` — top-level pure function
   - Inner `Intent(ACTION_SEND).apply { type = mimeType; putExtra(EXTRA_STREAM, uri); addFlags(FLAG_GRANT_READ_URI_PERMISSION) }`
   - Returns `Intent.createChooser(sendIntent, "Share via")`
   - KDoc references SHR-01..04, T-06-01 mitigation, D-19..D-21
   - Pure: no Context captured, no state, thread-safe, trivially testable

3. **`app/src/main/java/com/bugzz/filter/camera/ui/preview/PreviewScreen.kt`** (251 → 200 lines, MODIFIED)
   - Signature: drops `onShareNotImplemented: () -> Unit` parameter
   - Imports: drops AlertDialog/ButtonDefaults/TextStyle/FontWeight/sp; adds DeleteConfirmDialog + buildShareIntent + LocalContext
   - Body adds `val context = LocalContext.current`; mimeType state changed to `remember(uri)` re-key
   - Share button onClick: `scope.launch { val mime = mimeType ?: viewModel.resolveMimeType(uri); context.startActivity(buildShareIntent(uri, mime)) }`
   - Inline AlertDialog block replaced with `if (showDeleteDialog) DeleteConfirmDialog(onDismiss = { showDeleteDialog = false }, onConfirm = { showDeleteDialog = false; scope.launch { if (viewModel.deleteArtifact(uri)) onDeleted() } })`
   - KDoc updated: Plan 06-06 wiring noted; Plan 06 TODO comments removed

4. **`app/src/main/java/com/bugzz/filter/camera/ui/home/HomeScreen.kt`** (109 → 102 lines, MODIFIED)
   - Signature gains `onSettings: () -> Unit` parameter (D-06)
   - Imports: removes `android.widget.Toast` + `androidx.compose.ui.platform.LocalContext`
   - Body: removes `val context = LocalContext.current` line
   - Settings gear IconButton: `onClick = onSettings` (was: `Toast.makeText(context, "Settings coming soon", Toast.LENGTH_SHORT).show()`)
   - KDoc updated to reference Phase 6 D-06 + Plan 07 finalization plan

5. **`app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt`** (107 → 102 lines, MODIFIED)
   - Imports: removes `android.widget.Toast` + `androidx.compose.ui.platform.LocalContext`
   - HomeRoute composable: adds `onSettings = { /* Plan 06-07 will replace this with navController.navigate(SettingsRoute) */ }` placeholder lambda
   - PreviewRoute composable: drops `val context = LocalContext.current` line + `onShareNotImplemented = { Toast.makeText(...).show() }` argument

### Test Code (2 modified — un-Ignore + GREEN)

6. **`app/src/test/java/com/bugzz/filter/camera/ui/components/DeleteConfirmDialogTest.kt`** (49 → 70 lines, MODIFIED)
   - Removed both `@Ignore("Plan 06-06 — un-ignore when DeleteConfirmDialog lands")` annotations
   - Removed `markMissing()` stub helper
   - 2 GREEN cases:
     - `onConfirmTap_invokesOnConfirmCallbackOnce`: `confirmCount == 1 && dismissCount == 0` after `onConfirm()` invocation
     - `onCancelTap_invokesOnDismissOnce_doesNotInvokeOnConfirm`: `dismissCount == 1 && confirmCount == 0` after `onDismiss()` invocation
   - Pure JVM, no @RunWith — fastest possible (8ms)
   - KDoc references UX-08 + Phase 5 convention + UI-SPEC §7

7. **`app/src/test/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilderTest.kt`** (82 → 116 lines, MODIFIED)
   - Removed all 4 `@Ignore("Plan 06-06 — un-ignore when ShareIntentBuilder lands")` annotations
   - Removed `markMissing()` stub helper
   - Added `private val photoUri = Uri.parse("content://media/external/images/media/12345")` + videoUri
   - 5 GREEN cases (4 plan minimum + 1 extra polymorphism case):
     - `buildShareIntent_resultActionIsActionChooser`: `result.action == Intent.ACTION_CHOOSER` (SHR-03)
     - `innerIntentType_matchesImageMimeType`: inner `Intent.action == ACTION_SEND && type == "image/jpeg"` (SHR-02 image branch)
     - `innerIntentType_matchesVideoMimeType`: inner `type == "video/mp4"` (SHR-02 video polymorphism — beyond plan minimum)
     - `innerIntentExtraStream_equalsPassedUri`: inner `EXTRA_STREAM == photoUri` (SHR-01)
     - `innerIntent_hasFlagGrantReadUriPermissionSet`: `(inner.flags and FLAG_GRANT_READ_URI_PERMISSION) != 0` (T-06-01 mitigation)
   - `@RunWith(RobolectricTestRunner::class) @Config(sdk = [34])` — same harness as VideoRecorderTest

---

## Verification

### Test Suite

```
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 22s
52 actionable tasks: 12 executed, 40 up-to-date
```

**Aggregate counts:** 33 suites · 172 tests · 0 skipped · 0 failures · 0 errors

**Plan 06-06 contribution:** +7 GREEN tests (2 DeleteConfirmDialog + 5 ShareIntentBuilder), -6 @Ignore stubs un-Ignored. Net suite delta: 171 (Plan 05 GREEN+ignored) → 172 (Plan 06 all GREEN, no ignored).

### APK Assembly

```
> Task :app:assembleDebug
BUILD SUCCESSFUL in 22s
```

`assembleDebug` clean — no new third-party deps; no AGP warnings beyond the pre-existing `Icons.Default.ArrowBack` deprecation that Plan 06-05 SUMMARY explicitly deferred to Plan 07 cleanup pass.

### D-32 Grep-Asserts (9/9 PASS)

| # | Invariant | Pre-plan count | Post-plan count | Status |
|---|-----------|---------------:|----------------:|--------|
| 1 | `isCapturing` | 14 | 14 | INTACT |
| 2 | `bindJob.cancel()` | 1 | 1 | INTACT |
| 3 | `OneShotEvent.FilterLoadError` (KDoc + code) | 13 | 13 | INTACT |
| 4 | `captureFlash` (KDoc + code) | 13 | 13 | INTACT |
| 5 | `require(frameCount > 0)` | 1 | 1 | INTACT |
| 6 | `assetLoader.preload(def.assetDir)` | 3 | 3 | INTACT |
| 7 | `isRecording` | 47 | 47 | INTACT |
| 8 | `cameraMode = …CameraMode.InsectFilter` | 1 | 1 | INTACT |
| 9 | `setPreviewSize` | 2 | 2 | INTACT |

### Plan 06-06 Specific Grep-Asserts

| Pattern | Pre-plan | Post-plan | Expected |
|---------|---------:|----------:|---------:|
| `Toast.makeText.*Settings coming soon` | 1 (HomeScreen) | **0** | 0 |
| `Toast.makeText.*Share coming next` | 1 (BugzzApp) | **0** | 0 |
| `buildShareIntent` in PreviewScreen.kt | 0 | 5 (incl. KDoc) | ≥1 |
| `DeleteConfirmDialog` in PreviewScreen.kt | 0 | 5 (incl. KDoc) | ≥1 |
| `FLAG_GRANT_READ_URI_PERMISSION` in ShareIntentBuilder.kt | 0 | 3 (incl. KDoc) | ≥1 (T-06-01) |
| `onSettings` in HomeScreen.kt | 0 | 5 (param + KDoc + onClick + 2 KDoc refs) | ≥1 |
| `onSettings` in BugzzApp.kt | 0 | 1 (HomeRoute placeholder) | ≥1 |

### T-06-01 Mitigation Grep-Evidence

```
$ grep -En "FLAG_GRANT_READ_URI_PERMISSION" app/src/main/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilder.kt
3 matches: KDoc threat note + KDoc test reference + actual addFlags() call
```

Verified by `ShareIntentBuilderTest.innerIntent_hasFlagGrantReadUriPermissionSet` GREEN case: `(inner.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0`.

---

## Threat Flags

(none — Plan 06-06 introduces ACTION_SEND share-intent surface as plan-mapped T-06-01; no new unmapped surface)

---

## Deviations from Plan

### Auto-fixed Issues

(none — plan executed exactly as written; no Rule 1/2/3 fixes required)

### Plan-deviation extras (intentional, +0.05s test runtime)

**1. ShareIntentBuilderTest gained 1 extra polymorphism case (5 cases vs 4 minimum)**
- **Reason:** SHR-02 contract states "inner intent type matches passed mimeType" — image/jpeg AND video/mp4 are both real call paths (PreviewViewModel.resolveMimeType returns either). The 4 minimum cases only verify the image branch; adding `innerIntentType_matchesVideoMimeType` verifies polymorphism explicitly, defending against a future regression where buildShareIntent silently hard-codes "image/jpeg".
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilderTest.kt`
- **Cost:** +5 LOC, +0.05s test runtime (Robolectric is the dominant cost; one extra test under the same suite is negligible)
- **Commit:** 993e641 (atomic)

### Auth Gates

(none — no auth-protected operations in this plan)

---

## Plan 06-07 Readiness Sign-Off

**Wave 5 part B (Plan 06-07 — SettingsScreen + nav graph close-out + StubScreens delete) prerequisites:**

- ✅ HomeScreen has `onSettings: () -> Unit` lambda surface (D-06)
- ✅ BugzzApp HomeRoute passes placeholder lambda — Plan 07 swaps to `navController.navigate(SettingsRoute)`
- ✅ ui/components/ established with DeleteConfirmDialog + EmptyStateColumn — Plan 07 SettingsScreen can reuse DeleteConfirmDialog for clear-all confirmation if specified by UI-SPEC §SettingsScreen
- ✅ ShareIntentBuilder pure function — no Plan 07 cross-coupling
- ✅ Plan 06-06 commit 993e641 atomic — Plan 07 starts from clean tree
- ✅ Suite 172 GREEN — Plan 07 baseline established

**StubScreens.kt status:** UNCHANGED per scope contract (Plan 05 SUMMARY also noted this). Plan 07 owns deletion alongside SplashScreen + CameraScreen + PreviewScreen + CollectionScreen stubs.

---

## Self-Check

**Files created:**

- `app/src/main/java/com/bugzz/filter/camera/ui/components/DeleteConfirmDialog.kt` — FOUND
- `app/src/main/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilder.kt` — FOUND

**Files modified:**

- `app/src/main/java/com/bugzz/filter/camera/ui/preview/PreviewScreen.kt` — FOUND (200 lines)
- `app/src/main/java/com/bugzz/filter/camera/ui/home/HomeScreen.kt` — FOUND (102 lines)
- `app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt` — FOUND (102 lines)
- `app/src/test/java/com/bugzz/filter/camera/ui/components/DeleteConfirmDialogTest.kt` — FOUND (70 lines, 0 @Ignore)
- `app/src/test/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilderTest.kt` — FOUND (116 lines, 0 @Ignore)

**Commit:**

- `993e641` — FOUND on master HEAD

**Suite status:**

- 172 tests / 0 skipped / 0 failures / 0 errors — GREEN

**APK assembly:**

- assembleDebug — BUILD SUCCESSFUL

## Self-Check: PASSED
