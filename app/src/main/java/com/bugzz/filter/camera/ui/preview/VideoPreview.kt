package com.bugzz.filter.camera.ui.preview

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Compose-side ExoPlayer host for [PreviewScreen]'s video render branch (UX-04).
 *
 * Lifecycle contract — RESEARCH §Pattern 3 + Pitfall 2 verbatim:
 *   1. `remember(uri)` constructs ExoPlayer once per URI; if the URI changes (Collection
 *      navigation between items), a fresh player is created and the old one is disposed.
 *   2. `DisposableEffect(lifecycleOwner, exoPlayer)` attaches a `LifecycleEventObserver`
 *      that pauses on `ON_PAUSE` / resumes on `ON_RESUME` so the video does not run while
 *      the screen is backgrounded (battery + audio focus).
 *   3. `onDispose { exoPlayer.release() }` — **non-negotiable T-06-03 mitigation**. Without
 *      release(), ExoPlayer holds AudioFocus + native MediaCodec + SurfaceTexture native
 *      resources beyond the composable's lifetime, causing leaks across multiple
 *      Preview→Back→Preview cycles.
 *
 * [PlayerView]'s built-in transport controls are disabled (`useController = false`) — the
 * action bar in [PreviewScreen] is the only control surface. Auto-play loops via
 * `repeatMode = REPEAT_MODE_ALL` + `playWhenReady = true` per UI-SPEC §5 D-10.
 *
 * Phase 6, Plan 06-04, UX-04 (video case), T-06-03 (mitigated here).
 */
@Composable
fun VideoPreview(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release() // T-06-03 — must release native resources on dispose.
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = modifier.semantics { contentDescription = "Captured video" },
    )
}
