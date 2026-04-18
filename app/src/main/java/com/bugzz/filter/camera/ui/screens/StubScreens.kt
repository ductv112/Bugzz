package com.bugzz.filter.camera.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

@Composable
fun SplashScreen(onContinue: () -> Unit) = StubContent("Splash") {
    Button(onClick = onContinue) { Text("Continue") }
}

@Composable
fun HomeScreen(onOpenCamera: () -> Unit, onOpenCollection: () -> Unit) = StubContent("Home") {
    Button(onClick = onOpenCamera) { Text("Open Camera") }
    Button(onClick = onOpenCollection) { Text("My Collection") }
}

@Composable
fun CameraScreen(onOpenPreview: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        showRationale = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    when {
        hasCameraPermission -> {
            StubContent("Camera (granted)") {
                Button(onClick = onOpenPreview) { Text("Go to Preview") }
            }
        }
        showRationale -> {
            StubContent("Camera permission needed") {
                Text("Bugzz needs camera access to show face filters.")
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant permission")
                }
                Button(onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:${context.packageName}".toUri()
                    )
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            }
        }
        else -> {
            StubContent("Camera") { /* transient: waiting for launcher result */ }
        }
    }
}

@Composable
fun PreviewScreen(onBack: () -> Unit) = StubContent("Preview") {
    Button(onClick = onBack) { Text("Back") }
}

@Composable
fun CollectionScreen(onBack: () -> Unit) = StubContent("Collection") {
    Button(onClick = onBack) { Text("Back") }
}

@Composable
internal fun StubContent(label: String, actions: @Composable () -> Unit) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$label route — Phase 1 stub")
            actions()
        }
    }
}
