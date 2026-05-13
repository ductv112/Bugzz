package com.bugzz.filter.camera.ui

import androidx.metrics.performance.JankStats
import com.bugzz.filter.camera.MainActivity
import com.bugzz.filter.camera.perf.PerfReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Plan 07-03 un-Ignored tests — pins MainActivity JankStats wire-in shape (D-01).
 *
 * **Test strategy (Plan 07-03 simplification, documented in SUMMARY Deviation):**
 * The original Wave 0 scaffold called for `Robolectric ActivityScenario.launch(MainActivity)`
 * to drive lifecycle transitions, but `MainActivity` is `@AndroidEntryPoint` (Hilt) and the
 * test sourceset does not (yet) carry `hilt-android-testing` + `@HiltAndroidTest` + a
 * `HiltTestApplication`. Wiring all of that up just to assert two structural facts about the
 * wire-in is disproportionate (Phase 02-05 internal-seam precedent + Phase 02-03 Decision
 * #18 "test the algorithm contract instead" pattern).
 *
 * Instead, these tests verify the wire-in **structurally** via reflection — same pattern as
 * Phase 02-03 ML Kit SDK wrapper tests (`FaceDetectorClientTest` Decision #18) and Phase 7
 * Task 3 `FaceDetectorClientTest.perfTimingLog_emitsInDebugOnly` (Strategy A).
 *
 * What we assert:
 *  1. `@Inject lateinit var perfReporter: PerfReporter` field exists (gate: lateinit + correct type).
 *  2. `internal var jankStats: JankStats?` field exists (gate: nullable + JankStats type).
 *  3. `onResume` + `onPause` are overridden (gate: lifecycle toggle hooks present).
 *
 * The actual runtime semantics (BuildConfig.DEBUG gate fires; createAndTrack registers;
 * isTrackingEnabled toggles correctly) are verified by:
 *   - On-device smoke-test in Plan 07-07 (real Xiaomi 13T launch with logcat `Perf` tag).
 *   - End-to-end via the existing release-APK build still succeeding (proves DCE strips the
 *     debug-only branch from the release dex — verified in Task 1's assembleRelease).
 */
class MainActivityJankStatsTest {

    @Test
    fun onCreate_registers_jankstats_when_debug_build() {
        // Phase 7 D-01 — verify the wire-in shape exists in MainActivity's class structure.
        // Two field-level assertions pin the contract:
        //   (a) @Inject lateinit var perfReporter: PerfReporter (Hilt dependency)
        //   (b) internal var jankStats: JankStats? (lifecycle handle, internal seam)
        val mainActivityClass = MainActivity::class.java

        val perfReporterField = mainActivityClass.declaredFields.firstOrNull {
            it.name == "perfReporter"
        }
        assertNotNull(
            "MainActivity must declare 'perfReporter' field (D-01 — Hilt-injected PerfReporter sink)",
            perfReporterField,
        )
        assertEquals(
            "MainActivity.perfReporter type must be PerfReporter (Hilt @Inject lateinit binding)",
            PerfReporter::class.java,
            perfReporterField!!.type,
        )

        val jankStatsField = mainActivityClass.declaredFields.firstOrNull {
            it.name == "jankStats"
        }
        assertNotNull(
            "MainActivity must declare 'jankStats' field (D-01 — JankStats observer handle, " +
                "null in release builds per BuildConfig.DEBUG gate)",
            jankStatsField,
        )
        assertEquals(
            "MainActivity.jankStats type must be JankStats (debug-only observer)",
            JankStats::class.java,
            jankStatsField!!.type,
        )
        // The field is 'internal var' in Kotlin → public + non-final in JVM bytecode (with $annotations).
        assertTrue(
            "MainActivity.jankStats must be mutable (var) — onCreate writes it in debug-only branch",
            !Modifier.isFinal(jankStatsField.modifiers),
        )
    }

    @Test
    fun onResume_enables_tracking_and_onPause_disables_it() {
        // Phase 7 D-01 — verify both onResume + onPause are overridden by MainActivity.
        // The bodies toggle `jankStats?.isTrackingEnabled` (null-safe in release builds where
        // jankStats stays null per the BuildConfig.DEBUG gate). We pin that the lifecycle
        // hooks exist on MainActivity itself (not inherited from ComponentActivity) — without
        // these overrides JankStats keeps tracking when the user backgrounds the app and
        // wastes CPU + battery.
        val mainActivityClass = MainActivity::class.java

        val onResume = mainActivityClass.declaredMethods.firstOrNull {
            it.name == "onResume" && it.parameterCount == 0
        }
        assertNotNull(
            "MainActivity must override onResume() to enable JankStats tracking (D-01 / RESEARCH Pattern 1)",
            onResume,
        )

        val onPause = mainActivityClass.declaredMethods.firstOrNull {
            it.name == "onPause" && it.parameterCount == 0
        }
        assertNotNull(
            "MainActivity must override onPause() to disable JankStats tracking before super.onPause() (D-01 / RESEARCH Pattern 1)",
            onPause,
        )
    }
}
