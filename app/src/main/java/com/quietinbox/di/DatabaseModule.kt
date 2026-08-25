package com.quietinbox.di

import android.content.Context
import androidx.room.Room
import com.quietinbox.data.db.QuietDatabase
import com.quietinbox.data.db.dao.AppCacheDao
import com.quietinbox.data.db.dao.NotificationDao
import com.quietinbox.data.db.dao.RuleDao
import com.quietinbox.data.db.dao.ScheduleDao
import com.quietinbox.data.db.dao.StatsDao
import com.quietinbox.feature.rules.data.RulesUiDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuietDatabase {
        return Room.databaseBuilder(
            context,
            QuietDatabase::class.java,
            QuietDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideNotificationDao(db: QuietDatabase): NotificationDao = db.notificationDao()

    @Provides
    fun provideRuleDao(db: QuietDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideScheduleDao(db: QuietDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    fun provideStatsDao(db: QuietDatabase): StatsDao = db.statsDao()

    @Provides
    fun provideAppCacheDao(db: QuietDatabase): AppCacheDao = db.appCacheDao()

    @Provides
    fun provideRulesUiDao(db: QuietDatabase): RulesUiDao = db.rulesUiDao()
}
