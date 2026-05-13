package com.bugzz.filter.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.metrics.performance.JankStats
import com.bugzz.filter.camera.perf.PerfReporter
import com.bugzz.filter.camera.ui.BugzzApp
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var perfReporter: PerfReporter

    /**
     * JankStats observer — null in release builds (debug-only by BuildConfig.DEBUG gate AND
     * library is debugImplementation so release classpath does not even contain the type).
     *
     * Internal for [com.bugzz.filter.camera.ui.MainActivityJankStatsTest] visibility (Phase
     * 02-05 internal-seam pattern; Decision #14 STATE.md).
     */
    internal var jankStats: JankStats? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BugzzApp()
                }
            }
        }

        // Phase 7 D-01 + RESEARCH Pattern 1 — JankStats frame timing observer.
        // Defense in depth: BuildConfig.DEBUG check AND library is debugImplementation
        // (release classpath does not even contain JankStats; dead-code elimination strips
        // this entire branch from release dex).
        if (BuildConfig.DEBUG) {
            jankStats = JankStats.createAndTrack(window) { frameData ->
                perfReporter.record(frameData.frameDurationUiNanos / 1_000_000L)
                if (frameData.isJank) {
                    Timber.tag("Perf").d(
                        "jank dur=%dms states=%s",
                        frameData.frameDurationUiNanos / 1_000_000L,
                        frameData.states.joinToString { "${it.key}=${it.value}" },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        jankStats?.isTrackingEnabled = true
    }

    override fun onPause() {
        jankStats?.isTrackingEnabled = false
        super.onPause()
    }
}
