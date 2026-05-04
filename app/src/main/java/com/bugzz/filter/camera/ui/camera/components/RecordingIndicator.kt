package com.bugzz.filter.camera.ui.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Recording indicator pill per UI-SPEC §2.
 *
 * Rendered at TopCenter with padding(top = 24dp) from CameraScreen/InsectFilterScreen.
 * Visibility: AnimatedVisibility 300ms fadeIn/fadeOut driven by [isRecording].
 *
 * Layout (horizontal pill):
 *   [10dp red dot — 1Hz LinearEasing blink alpha 1.0↔0.5] [8dp spacer] [MM:SS 16sp/Medium/White]
 *
 * Backdrop: Color(0xCC000000) 80%-opaque black pill RoundedCornerShape(16dp) — ensures
 * readability on any camera scene (bright outdoors, white backgrounds).
 *
 * Red dot blink: infiniteRepeatable + RepeatMode.Reverse — alpha oscillates 1.0f↔0.5f at 1Hz
 * (500ms per half-cycle) with LinearEasing per UI-SPEC §Motion (utility status lamp, not a
 * choreographic animation — linear pulse matches camera-app convention).
 *
 * Timer format: "%02d:%02d" from [elapsedMs] — "00:00" through "01:00".
 * Source: RecordingState.Active.elapsedMs incremented by ViewModel Status events.
 *
 * Semantics: liveRegion=Polite on the outer modifier — TalkBack announces timer updates
 * after current speech completes.
 *
 * Reusable: shared by CameraScreen (Plan 05-04) and InsectFilterScreen (Plan 05-05).
 */
@Composable
fun RecordingIndicator(
    isRecording: Boolean,
    elapsedMs: Long,
    modifier: Modifier = Modifier,
) {
    val mins = (elapsedMs / 60_000L).toInt()
    val secs = ((elapsedMs % 60_000L) / 1_000L).toInt()
    val timerText = "%02d:%02d".format(mins, secs)

    // 1Hz blink: 500ms per half-cycle, LinearEasing, alpha oscillates 1.0f ↔ 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "recordBlinkTransition")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recordDotBlink",
    )

    AnimatedVisibility(
        visible = isRecording,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = modifier,
    ) {
        Surface(
            color = Color(0xCC000000),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Recording: $timerText"
            },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                // Red dot — 10dp, CircleShape, blinks at 1Hz (alpha 1.0↔0.5 LinearEasing)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = dotAlpha)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Timer text — 16sp / Medium (500) / White (UI-SPEC §Typography + §2)
                Text(
                    text = timerText,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    ),
                )
            }
        }
    }
}
