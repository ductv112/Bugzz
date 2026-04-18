package com.bugzz.filter.camera.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Phase 2+ will add @Provides functions here.
    // Empty module is valid — Hilt's graph just has no app-scoped bindings in Phase 1.
}
