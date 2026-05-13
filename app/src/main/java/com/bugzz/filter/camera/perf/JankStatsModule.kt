package com.bugzz.filter.camera.perf

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Phase 7 D-01 — placeholder Hilt module for the perf/ package.
 *
 * **Empty by design (Plan 07-03 audit).** Both [PerfReporter] and
 * [DetectionLatencyRecorder] are `@Singleton` classes with `@Inject constructor()`, so Hilt
 * synthesises their bindings without needing `@Provides` here. This module exists as a
 * documentation anchor — future additions (e.g. a tagged Timber tree, an alternate
 * FrameDataSource binding, or a per-build-flavor PerfReporter substitute) land here.
 *
 * Audit note (Plan 07-03): if no `@Provides` are added by end of v1, this file may be
 * deleted. Until then it remains as a small, idiomatic perf/ landing pad.
 */
@Module
@InstallIn(SingletonComponent::class)
object JankStatsModule {
    // Plan 07-03 audit — no @Provides needed; PerfReporter + DetectionLatencyRecorder are
    // both @Singleton @Inject constructor() self-bound. Add @Provides here if a future plan
    // needs a non-constructor-binding (e.g. test substitute, qualifier-tagged variant).
}
