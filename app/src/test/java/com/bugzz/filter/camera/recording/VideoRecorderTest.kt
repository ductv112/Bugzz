package com.bugzz.filter.camera.recording

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RED scaffold for VID-04 (durationLimit) + VID-05 (mirror mode wired) +
 * audioEnabled toggle behavior. SUT lands in Plan 05-03.
 *
 * Expected API shape (Plan 05-03):
 *   - VideoRecorder @Singleton @Inject constructor(CameraController, cameraExecutor)
 *   - startRecording(audioEnabled: Boolean): Recording — calls prepareRecording + withDurationLimit(60s) + optional withAudioEnabled
 *   - stopRecording(recording: Recording) — delegates to recording.stop()
 *   - finalize(pendingDiscard: Boolean, uri: Uri?) — if pendingDiscard && uri != null, deletes via MediaStore
 *
 * RecordingState sealed interface (D-22):
 *   Idle / Active(elapsedMs, hasAudio) / Stopping / Error(message)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoRecorderTest {

    @Ignore("Plan 05-03 lands VideoRecorder SUT")
    @Test
    fun durationLimit_setTo60Seconds() {
        Assert.fail("Plan 05-03 lands SUT")
    }

    @Ignore("Plan 05-03 lands VideoRecorder SUT")
    @Test
    fun startRecording_audioEnabledTrue_callsWithAudioEnabled() {
        Assert.fail("Plan 05-03 lands SUT")
    }

    @Ignore("Plan 05-03 lands VideoRecorder SUT")
    @Test
    fun startRecording_audioEnabledFalse_omitsWithAudioEnabled() {
        Assert.fail("Plan 05-03 lands SUT")
    }

    @Ignore("Plan 05-03 lands VideoRecorder SUT")
    @Test
    fun stopRecording_invokesRecordingStop() {
        Assert.fail("Plan 05-03 lands SUT")
    }

    @Ignore("Plan 05-03 lands VideoRecorder SUT")
    @Test
    fun finalize_pendingDiscardFlag_deletesPendingUri() {
        Assert.fail("Plan 05-03 lands SUT")
    }
}
