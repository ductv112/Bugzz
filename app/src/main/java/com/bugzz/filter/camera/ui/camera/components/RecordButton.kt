package com.bugzz.filter.camera.ui.camera.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Production Record button per UI-SPEC §1.
 *
 * 56dp red circle (#FFE53935) with 2dp white border.
 * Idle state: empty interior (plain red circle).
 * Recording state: 20dp white rounded-square (4dp corner) centered inside — universal stop icon.
 * Stopping state: recording visual + enabled=false to prevent double-tap.
 *
 * AnimatedContent 200ms FastOutSlowInEasing cross-fade for inner content swap (Idle ↔ Recording).
 * Outer red circle and white border are static — no animation on the container.
 *
 * D-07 + D-26 isStopping guard — pass isStopping=true during RecordingState.Stopping
 * to prevent double-tap while awaiting Finalize.
 *
 * Reusable: shared by CameraScreen (Plan 05-04) and InsectFilterScreen (Plan 05-05).
 */
@Composable
fun RecordButton(
    isRecording: Boolean,
    isStopping: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFFE53935))
            .border(width = 2.dp, color = Color.White, shape = CircleShape)
            .clickable(enabled = !isStopping) { onTap() }
            .semantics {
                role = Role.Button
                contentDescription = when {
                    isStopping  -> "Stopping recording"
                    isRecording -> "Stop recording"
                    else        -> "Start recording"
                }
            },
    ) {
        // AnimatedContent swaps inner content (nothing ↔ stop square) with 200ms cross-fade.
        // Outer red circle + white border remain static.
        AnimatedContent(
            targetState = isRecording,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(tween(200, easing = FastOutSlowInEasing)),
                    initialContentExit = fadeOut(tween(200, easing = FastOutSlowInEasing)),
                )
            },
            label = "recordButtonContent",
        ) { recording ->
            if (recording) {
                // Recording state: white 20dp rounded-square (stop icon)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White),
                )
            } else {
                // Idle state: no inner element — red circle with white border is the full visual
                Box(modifier = Modifier.size(20.dp))
            }
        }
    }
}
