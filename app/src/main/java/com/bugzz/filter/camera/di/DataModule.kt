package com.bugzz.filter.camera.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Data-layer Hilt module (D-26 / Plan 04-05).
 *
 * [FilterPrefsRepository] uses the `@Inject constructor(@ApplicationContext context)` pattern
 * (Phase 2 STATE #14 constructor-split) — no explicit `@Provides` needed here. Hilt can
 * auto-bind @Singleton @Inject classes when given a `@ApplicationContext Context` binding,
 * which is always present in [SingletonComponent].
 *
 * This module acts as an anchor: future data-layer providers (billing, analytics, remote config)
 * will be added here without touching [CameraModule].
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule // FilterPrefsRepository uses @Inject + @ApplicationContext — no @Provides needed yet.
