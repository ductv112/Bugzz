package com.bugzz.filter.camera.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugzz.filter.camera.BuildConfig

/**
 * Phase 2 Camera Compose surface — CameraXViewfinder + flip + debug TEST RECORD + permission gate.
 *
 * Wiring:
 *  - CAMERA permission only (D-26/27); reuses Phase 1 stub pattern (rationale + Settings CTA).
 *  - Binds CameraController on permission grant; rebinds on lens change (LaunchedEffect keyed on
 *    uiState.lens triggers CameraController.bind which calls provider.unbindAll internally).
 *  - DisposableEffect wires OrientationEventListener enable/disable (D-08).
 *  - TEST RECORD button gated by BuildConfig.DEBUG (D-04); emits toast on success/failure via
 *    OneShotEvent channel.
 *  - No RECORD_AUDIO contract anywhere (D-05/D-26/T-02-06).
 *
 * Layout:
 *  - Full-black Box fills screen.
 *  - CameraXViewfinder fills the same Box (ImplementationMode.EXTERNAL = SurfaceView / PERFORMANCE
 *    per research §Open Questions #1).
 *  - Flip button: Alignment.TopEnd (D-24).
 *  - TEST RECORD button: Alignment.BottomCenter, visible only when BuildConfig.DEBUG (D-04).
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

    // Bind camera once permission granted; re-bind on lens change. Key on all three dimensions
    // so rebinding follows any of: first grant, lens flip, detector readiness toggle.
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

    // One-shot Toast events (D-04 TEST RECORD URI toast + CameraError surface).
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is OneShotEvent.TestRecordSaved ->
                    Toast.makeText(context, "Saved: ${event.uri}", Toast.LENGTH_LONG).show()
                is OneShotEvent.TestRecordFailed ->
                    Toast.makeText(context, "Record error: ${event.reason}", Toast.LENGTH_LONG).show()
                is OneShotEvent.CameraError ->
                    Toast.makeText(context, "Camera error: ${event.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            uiState.permissionState is PermissionState.Granted -> {
                // ImplementationMode.EXTERNAL = SurfaceView backend (PERFORMANCE equivalent in 1.4+).
                // Verified against viewfinder-core 1.6.0 class dump — enum values are EXTERNAL / EMBEDDED.
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
                // Debug-only TEST RECORD button (D-04 / D-05 no audio).
                if (BuildConfig.DEBUG) {
                    Button(
                        onClick = { vm.onTestRecord() },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                    ) {
                        Text(if (uiState.isRecording) "REC..." else "TEST RECORD 5s")
                    }
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
