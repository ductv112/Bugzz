package com.bugzz.filter.camera.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Named
import javax.inject.Singleton

/**
 * Camera / detector / render DI module (Phase 2).
 *
 * Provides the two single-thread executors declared in D-18:
 *   - cameraExecutor — MlKitAnalyzer analyze callback, ImageCapture callback, VideoRecordEvent listener
 *   - renderExecutor — OverlayEffect.onDrawListener (wrapped by OverlayEffectBuilder's HandlerThread)
 *
 * CameraController, FaceDetectorClient, OverlayEffectBuilder, DebugOverlayRenderer each declare
 * their own @Singleton with constructor @Inject — no explicit @Provides needed for them.
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    @Named("cameraExecutor")
    fun provideCameraExecutor(): Executor =
        Executors.newSingleThreadExecutor { r -> Thread(r, "BugzzCameraExecutor") }

    @Provides
    @Singleton
    @Named("renderExecutor")
    fun provideRenderExecutor(): Executor =
        Executors.newSingleThreadExecutor { r -> Thread(r, "BugzzRenderExecutor") }
}
