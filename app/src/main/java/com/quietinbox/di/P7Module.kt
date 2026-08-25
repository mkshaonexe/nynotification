package com.quietinbox.di

import com.quietinbox.data.db.QuietDatabase
import com.quietinbox.feature.schedules.data.SchedulesUiDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module for Phase 7 (Schedules).
 */
@Module
@InstallIn(SingletonComponent::class)
object P7Module {

    @Provides
    @Singleton
    fun provideSchedulesUiDao(database: QuietDatabase): SchedulesUiDao {
        return database.schedulesUiDao()
    }
}
