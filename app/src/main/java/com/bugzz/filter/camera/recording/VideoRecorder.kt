package com.bugzz.filter.camera.recording

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.annotation.RequiresPermission
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Wraps CameraX Recorder + VideoCapture for production recording (D-21).
 *
 * Responsibilities:
 *   - 60-second duration limit (D-09)
 *   - Conditional microphone audio (D-12, D-18)
 *   - DCIM/Bugzz MediaStore output with Bugzz_YYYYMMDD_HHmmss.mp4 filename (D-17)
 *   - Single-recording invariant via activeRecording field + check() guard (T-05-03)
 *
 * Thread safety: [startRecording] and [stopRecording] are called from the ViewModel on
 * viewModelScope (main thread); callbacks fire on cameraExecutor. [clearActive] is called
 * from the Finalize event callback.
 *
 * Phase 5.
 */
@Singleton
class VideoRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("cameraExecutor") private val cameraExecutor: Executor,
) {
    @Volatile
    private var activeRecording: Recording? = null

    /**
     * Starts a new recording on [videoCapture].
     *
     * @param videoCapture The bound [VideoCapture<Recorder>] from [CameraController].
     * @param audioEnabled If true, RECORD_AUDIO permission must already be granted (D-12).
     * @param onEvent Callback dispatched on [cameraExecutor] for each [VideoRecordEvent].
     * @return The active [Recording] handle (caller should retain for [stopRecording]).
     * @throws IllegalStateException if a recording is already in progress (T-05-03).
     */
    fun startRecording(
        videoCapture: VideoCapture<*>,
        audioEnabled: Boolean,
        onEvent: (VideoRecordEvent) -> Unit,
    ): Recording {
        check(activeRecording == null) { "Recording already in progress (T-05-03)" }

        @Suppress("UNCHECKED_CAST")
        val recorder = videoCapture.output as Recorder
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "Bugzz_$timestamp.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Bugzz")
        }
        // setDurationLimitMillis: 60_000L ms = 60 seconds (D-09).
        // NOTE: duration limit is set on OutputOptions.Builder in CameraX 1.6 — NOT on
        // PendingRecording (the plan's withDurationLimit(PendingRecording) API does not exist
        // in this version; Rule 3 auto-fix: use the correct OutputOptions.Builder API).
        val options = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
            .setContentValues(values)
            .setDurationLimitMillis(60_000L)  // D-09: 60-second cap
            .build()

        var pending: PendingRecording = recorder.prepareRecording(context, options)

        if (audioEnabled) {
            pending = withAudioEnabledChecked(pending)
        }

        val recording = pending.start(cameraExecutor) { event -> onEvent(event) }
        activeRecording = recording
        Timber.tag("VideoRecorder").i("start audio=%s file=%s", audioEnabled, filename)
        return recording
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    private fun withAudioEnabledChecked(p: PendingRecording): PendingRecording = p.withAudioEnabled()

    /**
     * Stops the active recording. The [VideoRecordEvent.Finalize] callback will fire
     * asynchronously; callers must handle it to update UI state.
     */
    fun stopRecording() {
        activeRecording?.stop()
        Timber.tag("VideoRecorder").i("stop requested")
    }

    /**
     * Clears the [activeRecording] reference after a [VideoRecordEvent.Finalize] event.
     * Must be called from the Finalize handler to re-enable the single-recording invariant.
     */
    fun clearActive() {
        activeRecording = null
    }

    /** Returns true if a recording is currently in progress. */
    val isRecording: Boolean get() = activeRecording != null
}
