package com.bugzz.filter.camera.ui.insect

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugzz.filter.camera.ui.camera.FilterPicker
import com.bugzz.filter.camera.ui.camera.OneShotEvent
import com.bugzz.filter.camera.ui.camera.RecordingState
import com.bugzz.filter.camera.ui.camera.components.RecordButton
import com.bugzz.filter.camera.ui.camera.components.RecordingIndicator
import kotlinx.coroutines.launch

/**
 * InsectFilter mode screen — free-placement sticker with multi-touch gestures (Phase 5, Plan 05-05).
 *
 * 9-layer Box per UI-SPEC §5 (z-order, bottom → top):
 *   1. CameraXViewfinder — full-screen camera preview
 *   2. Sticker overlay — rendered by StickerRenderer via OverlayEffect canvas (NOT a Compose layer)
 *   3. Full-screen gesture handler (pointerInput) — detectTransformGestures; gated by isRecording
 *   4. FilterPicker strip — BottomCenter, 104dp from bottom, alpha 0.5f when recording (D-23)
 *   5. Shutter button — 72dp circle BottomCenter 24dp bottom, LONG_PRESS haptic (D-13)
 *   6. RecordButton — BottomStart 24dp, extracted from ui/camera/components/ (Plan 05-04)
 *   7. Flip button — TopEnd, disabled during recording (D-23)
 *   8. RecordingIndicator — TopCenter 24dp, extracted from ui/camera/components/ (Plan 05-04)
 *   9. Capture-flash overlay — AnimatedVisibility full-screen white (Phase 3 D-16)
 *
 * Also includes: AlertDialog (D-10 / VID-09), BackHandler (D-24), lazy RECORD_AUDIO (D-12 / VID-10),
 * SnackbarHost for rationale, and one-shot Toast collector.
 *
 * KEY DIFFERENCES from CameraScreen:
 *   - Face detection disabled — InsectFilterViewModel.bind() does not attach MlKitAnalyzer (D-05)
 *   - Full-screen gesture layer for sticker pan/zoom/rotate (D-03 / UI-SPEC §7)
 *   - No Settings gear (HomeScreen only per Phase 4 D-19)
 *   - No debug overlay (face detection off; DebugOverlayRenderer skips on emptyList())
 *
 * Uses extracted RecordButton + RecordingIndicator shared composables (WARNING 9 option A closure).
 *
 * Phase 5, D-01..D-24, MOD-03..07, VID-09, VID-10.
 */
@Composable
fun InsectFilterScreen(
    viewModel: InsectFilterViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val view = LocalView.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()

    // Derived recording state booleans (D-22).
    val isRecording = uiState.recordingState is RecordingState.Active
    val isStopping = uiState.recordingState is RecordingState.Stopping
    val elapsedMs = (uiState.recordingState as? RecordingState.Active)?.elapsedMs ?: 0L

    // AlertDialog visibility driven by BackHandler during record (D-10 / D-24).
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    // BackHandler intercepts system back press during recording → show discard dialog (D-24 / VID-09).
    // Declared early in composable body per UI-SPEC §Implementation Note 4.
    BackHandler(enabled = isRecording) {
        showDiscardDialog = true
    }

    // Snackbar host state for RECORD_AUDIO denial rationale (D-12 / VID-10).
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Lazy RECORD_AUDIO permission launcher (D-12 / VID-10).
    // First record tap triggers this; on denial shows Snackbar with "Open Settings" CTA.
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.onRecordTapped(audioEnabled = true)
        } else {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Microphone needed for video sound.",
                    actionLabel = "Open Settings",
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val activity = context as? Activity
                    if (activity != null) {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${activity.packageName}".toUri(),
                        )
                        activity.startActivity(intent)
                    }
                }
            }
        }
    }

    // Bind camera on first composition (D-05 — InsectFilter bind, no MlKitAnalyzer).
    LaunchedEffect(lifecycleOwner) {
        viewModel.bind(lifecycleOwner)
    }

    // One-shot Toast events: PhotoSaved/PhotoError (Phase 3), VideoSaved/VideoError (Phase 5),
    // FilterLoadError (Phase 3 WR-05). Mirrors CameraScreen event collector pattern.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OneShotEvent.PhotoSaved ->
                    Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
                is OneShotEvent.PhotoError ->
                    Toast.makeText(context, "Photo failed: ${event.message}", Toast.LENGTH_LONG).show()
                is OneShotEvent.FilterLoadError ->
                    Toast.makeText(context, "Filter failed to load", Toast.LENGTH_SHORT).show()
                is OneShotEvent.VideoSaved ->
                    Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show()
                is OneShotEvent.VideoError ->
                    Toast.makeText(context, "Recording failed: ${event.message}", Toast.LENGTH_LONG).show()
                else -> Unit
            }
        }
    }

    // InsectFilter reuses the same 15-filter FilterCatalog (D-01).
    // uiState.filters is already List<FilterSummary> populated by InsectFilterViewModel.init.
    val selectedId = uiState.selectedFilterId ?: ""

    // Root Box — same as CameraScreen (Color.Black background, fillMaxSize).
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ---- LAYER 1: CameraXViewfinder — full-screen camera preview ----
        // ImplementationMode.EXTERNAL = SurfaceView backend (Phase 2 STATE #15 canonical).
        // onGloballyPositioned reports preview pixel dimensions for D-02 sticker centering.
        surfaceRequest?.let { sr ->
            CameraXViewfinder(
                surfaceRequest = sr,
                implementationMode = ImplementationMode.EXTERNAL,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        val size = coords.size
                        // bitmapSize: placeholder 200×200; StickerRenderer overrides via
                        // setActiveFilter() with actual frame dimensions (D-02 / UI-SPEC §6).
                        viewModel.onPreviewSizeChanged(
                            size = IntSize(size.width, size.height),
                            bitmapSize = IntSize(200, 200),
                        )
                    },
            )
        }

        // ---- LAYER 2: Sticker overlay ----
        // Drawn by StickerRenderer inside OverlayEffect canvas (NOT a Compose layer — UI-SPEC §6).
        // Sticker is baked into preview, video, and photo output automatically via OverlayEffect.
        // No Compose composable here — sticker rendering is entirely in the render layer (D-06/D-20).

        // ---- LAYER 3: Full-screen gesture handler ----
        // Covers full preview area; sits above viewfinder but below control buttons.
        // pointerInput(isRecording) key causes block restart on isRecording state change —
        // correctly detaches gesture detection during recording (D-23 / UI-SPEC §4 / T-05-04).
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(isRecording) {
                    if (isRecording) return@pointerInput  // D-23: gesture lock during record
                    detectTransformGestures { _, pan, zoom, rotation ->
                        viewModel.onStickerGesture(
                            pan = pan,
                            zoom = zoom,
                            rotationDelta = rotation,
                        )
                    }
                }
                .semantics { invisibleToUser() },
        )

        // ---- LAYER 4: FilterPicker strip ----
        // 100% identical to CameraScreen picker — same 15-filter FilterCatalog (D-01).
        // BottomCenter, padding(bottom = 104dp): shutter 72dp + 24dp bottom + 8dp gap = 104dp.
        // alpha 0.5f when recording (D-23 / UI-SPEC §4 lock-during-record treatment).
        FilterPicker(
            filters = uiState.filters,
            selectedId = selectedId,
            onSelect = { id ->
                if (!isRecording) viewModel.onFilterSelected(id)
                // Locked during recording — tap silently ignored per UI-SPEC §4.
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp)
                .alpha(if (isRecording) 0.5f else 1f),
        )

        // ---- LAYER 5: Shutter button ----
        // 72dp white circle, gray border 2dp, BottomCenter 24dp from bottom (Phase 3 D-13).
        // NOT locked during recording — concurrent ImageCapture+VideoCapture supported (D-23).
        // Haptic: LONG_PRESS (Phase 3 D-15 inherited).
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(BorderStroke(2.dp, Color.Gray), CircleShape)
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    viewModel.onShutterTapped()
                },
        )

        // ---- LAYER 6: RecordButton — extracted shared composable (Plan 05-04) ----
        // BottomStart 24dp from edges; 56dp red circle (#FFE53935), 2dp white border.
        // Idle: empty circle. Recording: white 20dp stop-square. Stopping: disabled.
        // REUSES ui/camera/components/RecordButton (WARNING 9 option A closure).
        RecordButton(
            isRecording = isRecording,
            isStopping = isStopping,
            onTap = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (isRecording) {
                    viewModel.onStopRecording()
                } else {
                    // Lazy RECORD_AUDIO check on first record tap (D-12 / VID-10).
                    val audioGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (audioGranted) {
                        viewModel.onRecordTapped(audioEnabled = true)
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp),
        )

        // ---- LAYER 7: Flip button ----
        // TopEnd, disabled during recording (D-23 / UI-SPEC §4).
        // Material3 disabled state applies its own alpha automatically — no manual alpha override.
        OutlinedButton(
            onClick = { viewModel.onFlipLens() },
            enabled = !isRecording,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
        ) {
            Text("Flip")
        }

        // ---- LAYER 8: RecordingIndicator — extracted shared composable (Plan 05-04) ----
        // TopCenter 24dp from top; pill with 10dp red dot (1Hz blink) + MM:SS timer.
        // AnimatedVisibility 300ms fadeIn/fadeOut; hidden when not recording.
        // REUSES ui/camera/components/RecordingIndicator (WARNING 9 option A closure).
        RecordingIndicator(
            isRecording = isRecording,
            elapsedMs = elapsedMs,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        )

        // ---- LAYER 9: Capture-flash overlay ----
        // Phase 3 D-16 — 75ms fade-in, 150ms fade-out full-screen white overlay.
        // captureFlashVisible set true on photo success → false after delay (WR-04).
        AnimatedVisibility(
            visible = uiState.captureFlashVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 75)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White))
        }

        // Snackbar host for RECORD_AUDIO rationale (D-12 / VID-10).
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // AlertDialog — exit-during-record confirmation (D-10 / UI-SPEC §3 / VID-09).
    // Triggered by BackHandler(enabled = isRecording) on system back press.
    // Placed outside the inner Box so it renders above all content.
    // "Cancel" (right/confirmButton) resumes recording — no state change.
    // "Discard" (left/dismissButton, error color) stops + deletes pending MP4.
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    text = "Recording in progress",
                    // UI-SPEC §3 + §Typography: Phase 5 2-weight system — override Material3
                    // titleMedium (SemiBold 600) with explicit Medium (500).
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to discard this recording?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                // "Cancel" on right (Material3 confirmButton slot) — safe action, resumes recording.
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            },
            dismissButton = {
                // "Discard" on left (Material3 dismissButton slot) — destructive, error color.
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.onDiscardRecording()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Discard")
                }
            },
        )
    }
}
