package com.quietinbox.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt dependency injection module for Phase 3 (Firewall & Rule Matching).
 */
@Module
@InstallIn(SingletonComponent::class)
object P3Module {
    // Additional internal bindings for Phase 3 if needed
}
