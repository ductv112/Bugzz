package com.bugzz.filter.camera.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugzz.filter.camera.BuildConfig

/**
 * Phase 3 Camera Compose surface — extends Phase 2 with three-button layout and capture UX.
 *
 * Phase 3 additions (Plan 03-04):
 *  - Shutter button (72dp circle, white fill, gray stroke 2dp) — Alignment.BottomCenter,
 *    24dp bottom padding (D-13/D-15). Tap fires HapticFeedback + vm.onShutterTapped().
 *  - TEST RECORD button moved from BottomCenter → BottomStart (D-14, still DEBUG-only).
 *  - Cycle Filter button (OutlinedButton {Text("Cycle")}) — Alignment.BottomEnd, DEBUG-only (D-10).
 *  - Capture-flash AnimatedVisibility overlay — 150ms white alpha fade on captureFlashVisible (D-16).
 *  - LaunchedEffect Toast for OneShotEvent.PhotoSaved ("Saved to gallery") + PhotoError (D-12/D-35).
 *
 * Phase 2 behavior preserved:
 *  - CAMERA permission only (D-26/27); reuses Phase 1 stub pattern (rationale + Settings CTA).
 *  - Binds CameraController on permission grant; rebinds on lens change.
 *  - DisposableEffect wires OrientationEventListener enable/disable (D-08).
 *  - Flip button Alignment.TopEnd (D-24).
 *  - TEST RECORD still BuildConfig.DEBUG gated (D-04/D-05 no audio).
 *  - No RECORD_AUDIO contract (D-05/D-26/T-02-06).
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

    // CAMERA permission only — D-26/27. Reuses Phase 1 StubScreens pattern.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> vm.onPermissionResult(granted) }

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

    // One-shot Toast events — Phase 2 TEST RECORD + CameraError + Phase 3 Photo outcomes.
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

                // Flip button top-right (D-24) — text-label fallback; icon polish deferred to Phase 6.
                OutlinedButton(
                    onClick = { vm.onFlipLens() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                ) {
                    Text("Flip")
                }

                // TEST RECORD — Phase 3 D-14: moved from BottomCenter to BottomStart (DEBUG-only).
                if (BuildConfig.DEBUG) {
                    Button(
                        onClick = { vm.onTestRecord() },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 24.dp),
                    ) {
                        Text(if (uiState.isRecording) "REC..." else "TEST 5s")
                    }
                }

                // Shutter — Phase 3 D-13/D-15 — 72dp circle, white fill, gray stroke 2dp, BottomCenter.
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

                // Cycle Filter — Phase 3 D-10 — debug-only, BottomEnd.
                // OutlinedButton text fallback (matches Phase 2 D-24 icon-fallback pattern).
                if (BuildConfig.DEBUG) {
                    OutlinedButton(
                        onClick = { vm.onCycleFilter() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 24.dp),
                    ) {
                        Text("Cycle")
                    }
                }

                // Capture flash — Phase 3 D-16 — 150ms white alpha fade overlay.
                AnimatedVisibility(
                    visible = uiState.captureFlashVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 75)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150)),
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
                }
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
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant permission")
                    }
                    Button(onClick = {
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
    }
}
