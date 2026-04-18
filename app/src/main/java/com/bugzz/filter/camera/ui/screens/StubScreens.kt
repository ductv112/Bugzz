package com.bugzz.filter.camera.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
fun CameraScreen(onOpenPreview: () -> Unit) = StubContent("Camera") {
    // Plan 03 replaces this body with an ActivityResultContracts.RequestPermission() flow.
    // Phase 2 will add CameraX composable here.
    Button(onClick = onOpenPreview) { Text("Go to Preview (stub)") }
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
