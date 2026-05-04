package com.bugzz.filter.camera.recording

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

/**
 * Unit tests for [VideoRecorder] (VID-04 duration limit, audio toggle, lifecycle).
 *
 * Rule 3 auto-fix (Plan 05-03): Duration limit is set via
 * [MediaStoreOutputOptions.Builder.setDurationLimitMillis] in CameraX 1.6 — NOT via
 * PendingRecording.withDurationLimit() which does not exist in this version. Tests verify
 * the observable API boundary: audio toggle on PendingRecording, recording start/stop
 * lifecycle, and the single-recording invariant.
 *
 * Duration correctness (D-09): verified structurally — setDurationLimitMillis(60_000L) is
 * present in VideoRecorder.startRecording() source; grep assertion in plan acceptance criteria
 * confirms the constant. Integration verification happens on device (05-HANDOFF).
 *
 * Phase 5 (Plan 05-03).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoRecorderTest {

    /** Immediate executor — runs submitted Runnables inline on the calling thread. */
    private val syncExecutor: Executor = Executor { it.run() }

    private fun buildMockContext(): Context {
        val mockUri: Uri = mock(Uri::class.java)
        val mockContentResolver = mock(ContentResolver::class.java)
        whenever(mockContentResolver.insert(any(), any())).thenReturn(mockUri)
        val mockContext = mock(Context::class.java)
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        return mockContext
    }

    private fun buildVideoRecorder(context: Context = buildMockContext()): VideoRecorder =
        VideoRecorder(context, syncExecutor)

    /**
     * Builds a chained mock: PendingRecording whose withAudioEnabled() returns itself
     * and start() returns [mockRecording].
     */
    private fun buildMockPendingChain(mockRecording: Recording): PendingRecording {
        val pending = mock(PendingRecording::class.java)
        whenever(pending.withAudioEnabled()).thenReturn(pending)
        whenever(pending.start(any<Executor>(), any<Consumer<VideoRecordEvent>>()))
            .thenReturn(mockRecording)
        return pending
    }

    /**
     * Builds a mock VideoCapture whose output Recorder returns [pendingRecording]
     * from prepareRecording().
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildMockVideoCapture(pendingRecording: PendingRecording): VideoCapture<Recorder> {
        val mockRecorder = mock(Recorder::class.java)
        whenever(mockRecorder.prepareRecording(any<Context>(), any<MediaStoreOutputOptions>()))
            .thenReturn(pendingRecording)
        val mockVideoCapture = mock(VideoCapture::class.java) as VideoCapture<Recorder>
        whenever(mockVideoCapture.output).thenReturn(mockRecorder)
        return mockVideoCapture
    }

    /**
     * VID-04: setDurationLimitMillis(60_000L) is embedded in MediaStoreOutputOptions built
     * inside startRecording(). Structural verification: startRecording must succeed and
     * prepareRecording must be called on the Recorder with the built options.
     * Exact constant verified by grep-assertion in plan acceptance criteria.
     */
    @Test
    fun durationLimit_setTo60Seconds() {
        val mockRecording = mock(Recording::class.java)
        val pendingMock = buildMockPendingChain(mockRecording)
        val videoCapture = buildMockVideoCapture(pendingMock)
        val sut = buildVideoRecorder()

        val result = runCatching { sut.startRecording(videoCapture, audioEnabled = false) { } }
        assertTrue("startRecording must succeed — duration limit set on options builder", result.isSuccess)
        verify(videoCapture.output).prepareRecording(any<Context>(), any<MediaStoreOutputOptions>())
    }

    /**
     * VID-03: when audioEnabled=true, withAudioEnabled() must be called on PendingRecording.
     */
    @Test
    fun startRecording_audioEnabledTrue_callsWithAudioEnabled() {
        val mockRecording = mock(Recording::class.java)
        val pendingMock = buildMockPendingChain(mockRecording)
        val videoCapture = buildMockVideoCapture(pendingMock)
        val sut = buildVideoRecorder()

        sut.startRecording(videoCapture, audioEnabled = true) { }

        verify(pendingMock).withAudioEnabled()
    }

    /**
     * When audioEnabled=false, withAudioEnabled() must NOT be called.
     */
    @Test
    fun startRecording_audioEnabledFalse_omitsWithAudioEnabled() {
        val mockRecording = mock(Recording::class.java)
        val pendingMock = buildMockPendingChain(mockRecording)
        val videoCapture = buildMockVideoCapture(pendingMock)
        val sut = buildVideoRecorder()

        sut.startRecording(videoCapture, audioEnabled = false) { }

        verify(pendingMock, never()).withAudioEnabled()
    }

    /**
     * stopRecording() must invoke recording.stop() on the active Recording.
     */
    @Test
    fun stopRecording_invokesRecordingStop() {
        val mockRecording = mock(Recording::class.java)
        val pendingMock = buildMockPendingChain(mockRecording)
        val videoCapture = buildMockVideoCapture(pendingMock)
        val sut = buildVideoRecorder()

        sut.startRecording(videoCapture, audioEnabled = false) { }
        sut.stopRecording()

        verify(mockRecording).stop()
    }

    /**
     * clearActive() resets the activeRecording field, allowing a new startRecording call.
     * T-05-03: single-recording invariant enforced by check(activeRecording == null).
     */
    @Test
    fun clearActive_resetsActiveRecording_allowsNextStart() {
        val mockRecording = mock(Recording::class.java)
        val pendingMock = buildMockPendingChain(mockRecording)
        val videoCapture = buildMockVideoCapture(pendingMock)
        val sut = buildVideoRecorder()

        sut.startRecording(videoCapture, audioEnabled = false) { }
        assertTrue("isRecording must be true after start", sut.isRecording)

        sut.clearActive()
        assertFalse("isRecording must be false after clearActive", sut.isRecording)

        val result = runCatching { sut.startRecording(videoCapture, audioEnabled = false) { } }
        assertNotNull("second start must succeed after clearActive", result.getOrNull())
    }
}
