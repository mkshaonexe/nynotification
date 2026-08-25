package com.quietinbox.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quietinbox.data.db.dao.AppCacheDao
import com.quietinbox.data.db.dao.NotificationDao
import com.quietinbox.data.db.dao.RuleDao
import com.quietinbox.data.db.dao.ScheduleDao
import com.quietinbox.data.db.dao.StatsDao
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.AppCacheEntity
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.FirewallDecisionEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import com.quietinbox.data.db.entity.NotificationEntity
import com.quietinbox.data.db.entity.NotificationFtsEntity
import com.quietinbox.data.db.entity.ScheduleAppEntity
import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.feature.home.data.HomeStatsDao
import com.quietinbox.feature.inbox.data.InboxDao
import com.quietinbox.feature.rules.data.RulesUiDao
import com.quietinbox.feature.schedules.data.SchedulesUiDao
import com.quietinbox.feature.settings.data.SettingsDao

@Database(
    entities = [
        NotificationEntity::class,
        NotificationFtsEntity::class,
        FirewallDecisionEntity::class,
        AllowRuleEntity::class,
        MutedAppEntity::class,
        ScheduleEntity::class,
        ScheduleAppEntity::class,
        DailyStatEntity::class,
        AppCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class QuietDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao
    abstract fun ruleDao(): RuleDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun statsDao(): StatsDao
    abstract fun appCacheDao(): AppCacheDao

    // Empty feature DAOs declared up front for Wave-2 phases
    abstract fun homeStatsDao(): HomeStatsDao
    abstract fun inboxDao(): InboxDao
    abstract fun rulesUiDao(): RulesUiDao
    abstract fun schedulesUiDao(): SchedulesUiDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "quiet_inbox.db"
    }
}
