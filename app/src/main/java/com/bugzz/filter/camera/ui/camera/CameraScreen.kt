package com.bugzz.filter.camera.ui.camera

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugzz.filter.camera.ui.camera.components.RecordButton
import com.bugzz.filter.camera.ui.camera.components.RecordingIndicator
import kotlinx.coroutines.launch

/**
 * Phase 5 Camera Compose surface — extends Phase 4 with production video recording UI.
 *
 * Phase 5 additions (Plan 05-04):
 *  - Production RecordButton (56dp red circle, 2dp white border) at Alignment.BottomStart,
 *    padding(start=24dp, bottom=24dp). Replaces Phase 2/4 debug record button (D-07).
 *  - RecordingIndicator (TopCenter pill, 10dp red dot 1Hz blink, MM:SS timer) shown while
 *    isRecording (D-08).
 *  - BackHandler(enabled = isRecording) intercepts back press → showDiscardDialog = true (D-24).
 *  - AlertDialog "Recording in progress" / "Discard" / "Cancel" per UI-SPEC §3 (D-10).
 *  - Lazy RECORD_AUDIO permission launcher on first record tap — rationale Snackbar with
 *    "Open Settings" CTA reusing Phase 1 D-13 pattern (D-12).
 *  - Lock-during-record: FilterPicker alpha(0.5f), Flip button enabled=!isRecording (D-11/D-23).
 *  - OneShotEvent.VideoSaved/VideoError wired to Toast (Phase 5 D-07).
 *  - Extracted RecordButton + RecordingIndicator to ui/camera/components/ for InsectFilterScreen
 *    reuse (Plan 05-05 — WARNING 9 option A closure).
 *
 * Phase 3+4 behaviors preserved (D-26 mandate):
 *  - Shutter button (72dp circle, white fill, gray stroke) at BottomCenter (D-13).
 *  - Capture-flash AnimatedVisibility full-screen white overlay (D-16).
 *  - FilterPicker strip at BottomCenter padding(bottom=104dp) (D-15/D-17).
 *  - Flip button at TopEnd (D-24).
 *  - CAMERA permission launcher (Phase 2).
 *  - OneShotEvent.PhotoSaved/PhotoError/FilterLoadError Toasts (Phase 3).
 */
@Composable
fun CameraScreen(
    onOpenPreview: () -> Unit,
    vm: CameraViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val surfaceRequest by vm.surfaceRequest.collectAsStateWithLifecycle()

    // Derived recording state booleans (D-22).
    val isRecording = uiState.recordingState is RecordingState.Active
    val isStopping = uiState.recordingState is RecordingState.Stopping
    val elapsedMs = (uiState.recordingState as? RecordingState.Active)?.elapsedMs ?: 0L

    // AlertDialog visibility (D-10 / D-24).
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    // BackHandler intercepts back press during recording → show discard dialog (D-24).
    BackHandler(enabled = isRecording) {
        showDiscardDialog = true
    }

    // CAMERA permission — D-26/27. Reuses Phase 1 / Phase 2 pattern.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> vm.onPermissionResult(granted) }

    // Snackbar for RECORD_AUDIO denial rationale (D-12).
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Lazy RECORD_AUDIO permission launcher (D-12).
    // First record tap triggers this; on denial shows Snackbar with "Open Settings" CTA.
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.onRecordTapped(audioEnabled = true)
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

    LaunchedEffect(Unit) {
        val already = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (already) vm.onPermissionResult(true)
        else launcher.launch(Manifest.permission.CAMERA)
    }

    // Bind camera once permission granted; re-bind on lens change.
    LaunchedEffect(uiState.permissionState, uiState.lens, uiState.isDetectorReady) {
        if (uiState.permissionState.isGranted && uiState.isDetectorReady) {
            vm.bind(lifecycleOwner)
        }
    }

    // OrientationEventListener lives through composable lifecycle (D-08).
    DisposableEffect(lifecycleOwner) {
        val listener = vm.orientationListener(context)
        listener.enable()
        onDispose { listener.disable() }
    }

    // One-shot Toast events — Phase 2 CameraError + Phase 3 Photo + Phase 5 Video.
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is OneShotEvent.TestRecordSaved ->
                    Toast.makeText(context, "Saved: ${event.uri}", Toast.LENGTH_LONG).show()
                is OneShotEvent.TestRecordFailed ->
                    Toast.makeText(context, "Record error: ${event.reason}", Toast.LENGTH_LONG).show()
                is OneShotEvent.CameraError ->
                    Toast.makeText(context, "Camera error: ${event.message}", Toast.LENGTH_LONG).show()
                // Phase 3 (D-12 / D-35)
                is OneShotEvent.PhotoSaved ->
                    Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
                is OneShotEvent.PhotoError ->
                    Toast.makeText(context, "Photo error: ${event.message}", Toast.LENGTH_LONG).show()
                is OneShotEvent.FilterLoadError ->
                    Toast.makeText(context, "Filter error: ${event.message}", Toast.LENGTH_LONG).show()
                // Phase 5 (Plan 05-03) — video recording outcomes (UI-SPEC §Copywriting)
                is OneShotEvent.VideoSaved ->
                    Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show()
                is OneShotEvent.VideoError ->
                    Toast.makeText(context, "Recording failed: ${event.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            uiState.permissionState is PermissionState.Granted -> {
                // ImplementationMode.EXTERNAL = SurfaceView backend.
                surfaceRequest?.let { sr ->
                    CameraXViewfinder(
                        surfaceRequest = sr,
                        modifier = Modifier.fillMaxSize(),
                        implementationMode = ImplementationMode.EXTERNAL,
                    )
                }

                // Flip button top-right (D-24) — disabled during recording (D-11/D-23).
                OutlinedButton(
                    onClick = { vm.onFlipLens() },
                    enabled = !isRecording,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                ) {
                    Text("Flip")
                }

                // Filter picker strip — Phase 4 D-15/D-16/D-17/D-18 (04-UI-SPEC §Component Specs §2).
                // Positioned 104dp from bottom: shutter 72dp + 24dp bottom-padding + 8dp gap = 104dp.
                // Lock-during-record: alpha 0.5f when isRecording; taps early-return (D-11/D-23).
                FilterPicker(
                    filters = uiState.filters,
                    selectedId = uiState.selectedFilterId,
                    onSelect = { id ->
                        if (!isRecording) vm.onSelectFilter(id)
                        // Locked during recording — tap silently ignored per UI-SPEC §4.
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 104.dp)
                        .alpha(if (isRecording) 0.5f else 1f),
                )

                // Shutter — Phase 3 D-13/D-15 — 72dp circle, white fill, gray stroke 2dp, BottomCenter.
                // NOT locked during recording — concurrent ImageCapture+VideoCapture supported (D-23).
                val hapticView = LocalView.current
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(BorderStroke(2.dp, Color.Gray), CircleShape)
                        .clickable {
                            hapticView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            vm.onShutterTapped()
                        }
                )

                // Production Record button — Phase 5 D-07.
                // 56dp red circle (#FFE53935), 2dp white border, BottomStart 24dp from edges.
                // Idle: empty circle. Recording: white 20dp stop-square inside.
                // Replaces Phase 2/4 debug record button (D-07).
                RecordButton(
                    isRecording = isRecording,
                    isStopping = isStopping,
                    onTap = {
                        hapticView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        if (isRecording) {
                            vm.onStopRecording()
                        } else {
                            // Lazy RECORD_AUDIO check on first record tap (D-12).
                            val audioGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (audioGranted) {
                                vm.onRecordTapped(audioEnabled = true)
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 24.dp),
                )

                // Recording indicator — Phase 5 D-08.
                // TopCenter pill: 10dp red dot (1Hz LinearEasing blink) + MM:SS timer.
                // AnimatedVisibility fadeIn/fadeOut 300ms, visible only while isRecording.
                RecordingIndicator(
                    isRecording = isRecording,
                    elapsedMs = elapsedMs,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp),
                )

                // Capture flash — Phase 3 D-16 — 150ms white alpha fade overlay.
                AnimatedVisibility(
                    visible = uiState.captureFlashVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 75)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150)),
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
                }

                // Snackbar host for RECORD_AUDIO rationale (D-12).
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            uiState.permissionState is PermissionState.Denied -> {
                // Phase 1 rationale + Settings CTA reuse (D-27).
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Camera permission needed", color = Color.White)
                    Text("Bugzz needs camera access to show face filters.", color = Color.White)
                    androidx.compose.material3.Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) }
                    ) {
                        Text("Grant permission")
                    }
                    androidx.compose.material3.Button(onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri(),
                        )
                        context.startActivity(intent)
                    }) { Text("Open Settings") }
                }
            }
            else -> {
                // PermissionState.Unknown — launcher is firing; transient blank.
            }
        }

        // AlertDialog — Exit-during-record confirmation (D-10 / UI-SPEC §3).
        // Triggered by BackHandler(enabled = isRecording) when user presses back.
        // "Cancel" (right/confirmButton) resumes recording.
        // "Discard" (left/dismissButton, error color) stops + deletes pending MP4.
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = {
                    Text(
                        text = "Recording in progress",
                        // UI-SPEC §3: Phase 5 2-weight system — override Material3 titleMedium (SemiBold)
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
                    // "Cancel" on right — safe/affirmative action per Material3 convention.
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("Cancel")
                    }
                },
                dismissButton = {
                    // "Discard" on left — destructive action, error color (#FFB00020).
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            vm.onDiscardRecording()
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
}
