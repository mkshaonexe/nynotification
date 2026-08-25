package com.quietinbox.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt DI module for Phase 2 (Signal Engine).
 *
 * Note: SignalClassifier is bound to DefaultSignalClassifier in AppModule (Phase 0).
 */
@Module
@InstallIn(SingletonComponent::class)
object P2Module {
}
