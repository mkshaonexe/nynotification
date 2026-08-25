package com.quietinbox.di

import com.quietinbox.data.db.QuietDatabase
import com.quietinbox.feature.settings.data.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Phase 9 (Settings, Storage, Retention, Export, App Lock, Privacy) dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object P9Module {

    @Provides
    @Singleton
    fun provideSettingsDao(db: QuietDatabase): SettingsDao = db.settingsDao()
}
