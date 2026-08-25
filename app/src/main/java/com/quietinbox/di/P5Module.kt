package com.quietinbox.di

import com.quietinbox.data.db.QuietDatabase
import com.quietinbox.feature.home.data.HomeStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module owned by Phase 5.
 * Provides the [HomeStatsDao] instance from the Room database.
 */
@Module
@InstallIn(SingletonComponent::class)
object P5Module {

    @Provides
    @Singleton
    fun provideHomeStatsDao(database: QuietDatabase): HomeStatsDao {
        return database.homeStatsDao()
    }
}
