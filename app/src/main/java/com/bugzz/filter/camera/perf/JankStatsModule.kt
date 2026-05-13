package com.bugzz.filter.camera.perf

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module skeleton for JankStats integration. **Plan 07-03 adds `@Provides` bodies.**
 *
 * Phase 7 D-01 (JankStats wire-in). Empty in Wave 0 — MainActivity is not yet wired.
 * Wave 2 fills @Provides for the application-scoped PerfReporter sink consumed by
 * MainActivity's `JankStats.createAndTrack(window) { ... }` callback.
 */
@Module
@InstallIn(SingletonComponent::class)
object JankStatsModule {
    // Plan 07-03 — @Provides methods land here
}
