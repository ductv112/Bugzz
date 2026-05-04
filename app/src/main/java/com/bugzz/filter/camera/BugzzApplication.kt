package com.bugzz.filter.camera

import android.app.Application
import android.os.StrictMode
import com.bugzz.filter.camera.thermal.ThermalMonitor
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class BugzzApplication : Application() {

    // D-14: ThermalMonitor is a process-lifetime @Singleton — registered here so the
    // PowerManager listener is active for the full app session (Phase 5).
    @Inject lateinit var thermalMonitor: ThermalMonitor

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())   // T-02-02 gate: biometric/face-log data only in debug
            enableStrictMode()
        }
        // Guard for Robolectric test environments where Hilt injection is not triggered.
        if (::thermalMonitor.isInitialized) thermalMonitor.register()
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }
}
