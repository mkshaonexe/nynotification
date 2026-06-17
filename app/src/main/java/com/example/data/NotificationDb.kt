package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// ROOM ENTITIES
// ==========================================

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val isBlocked: Boolean = false,
    val priority: String = "Normal", // Critical, High, Normal, Low, Silent
    val maxPerHour: Int = 0, // 0 = no limit
    val cooldownMinutes: Int = 0, // 0 = disabled
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_notifications")
data class SavedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val body: String,
    val channelId: String = "general",
    val category: String = "Social", // Social, Promo, Urgent, OTP, Work, etc.
    val isRead: Boolean = false,
    val isPinned: Boolean = false,
    val isOtp: Boolean = false,
    val otpCode: String? = null,
    val receivedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_modes")
data class FocusModeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val allowedPackages: String = "", // Comma-separated
    val blockedPackages: String = "", // Comma-separated
    val isPreset: Boolean = false,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: String = "[1,2,3,4,5]", // JSON or Simple list: e.g. "1,2,3,4,5"
    val focusModeId: Long, // Linked focus mode
    val isEnabled: Boolean = true
)

@Entity(tableName = "keyword_rules")
data class KeywordRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val packageName: String? = null, // Null means global
    val useWildcard: Boolean = false,
    val isEnabled: Boolean = true
)

@Entity(tableName = "notification_log")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val action: String, // BLOCKED, SAVED, ALLOWED, DIGESTED
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val focusModeId: Long,
    val focusModeName: String,
    val startTime: Long,
    val endTime: Long,
    val notificationsBlocked: Int = 0,
    val wasCompleted: Boolean = true
)

@Entity(tableName = "channel_rules")
data class ChannelRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val channelId: String,
    val channelName: String,
    val userLabel: String? = null,
    val isBlocked: Boolean = false
)

// ==========================================
// DATA ACCESS OBJECTS (DAOs)
// ==========================================

@Dao
interface NotificationDao {
    // Blocked Apps
    @Query("SELECT * FROM blocked_apps ORDER BY appLabel ASC")
    fun getAllBlockedAppsFlow(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAllBlockedApps(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getBlockedApp(packageName: String): BlockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(app: BlockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApps(apps: List<BlockedAppEntity>)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteBlockedApp(packageName: String)

    // Saved Notifications
    @Query("SELECT * FROM saved_notifications ORDER BY receivedAt DESC")
    fun getAllSavedNotificationsFlow(): Flow<List<SavedNotificationEntity>>

    @Query("SELECT * FROM saved_notifications WHERE isOtp = 1 ORDER BY receivedAt DESC")
    fun getOtpNotificationsFlow(): Flow<List<SavedNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedNotification(notification: SavedNotificationEntity): Long

    @Query("UPDATE saved_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE saved_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("UPDATE saved_notifications SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("DELETE FROM saved_notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM saved_notifications")
    suspend fun deleteAllNotifications()

    @Query("DELETE FROM saved_notifications WHERE receivedAt < :timestamp AND isPinned = 0")
    suspend fun purgeNotificationsOlderThan(timestamp: Long)

    // Focus Modes
    @Query("SELECT * FROM focus_modes ORDER BY id ASC")
    fun getAllFocusModesFlow(): Flow<List<FocusModeEntity>>

    @Query("SELECT * FROM focus_modes")
    suspend fun getAllFocusModes(): List<FocusModeEntity>

    @Query("SELECT * FROM focus_modes WHERE id = :id LIMIT 1")
    suspend fun getFocusMode(id: Long): FocusModeEntity?

    @Query("SELECT * FROM focus_modes WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveFocusMode(): FocusModeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusMode(mode: FocusModeEntity): Long

    @Query("UPDATE focus_modes SET isActive = 0")
    suspend fun deactivateAllFocusModes()

    @Query("UPDATE focus_modes SET isActive = :isActive WHERE id = :id")
    suspend fun setFocusModeActive(id: Long, isActive: Boolean)

    @Delete
    suspend fun deleteFocusMode(mode: FocusModeEntity)

    // Schedules
    @Query("SELECT * FROM schedules ORDER BY id ASC")
    fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules")
    suspend fun getAllSchedules(): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    // Keyword Rules
    @Query("SELECT * FROM keyword_rules ORDER BY keyword ASC")
    fun getAllKeywordsFlow(): Flow<List<KeywordRuleEntity>>

    @Query("SELECT * FROM keyword_rules")
    suspend fun getAllKeywords(): List<KeywordRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyword(keyword: KeywordRuleEntity)

    @Delete
    suspend fun deleteKeyword(keyword: KeywordRuleEntity)

    // Notification Log
    @Query("SELECT * FROM notification_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistoryLogsFlow(limit: Int): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notification_log ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NotificationLogEntity)

    @Query("DELETE FROM notification_log")
    suspend fun clearAllLogs()

    // Focus Sessions
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    // Channel Rules
    @Query("SELECT * FROM channel_rules WHERE packageName = :packageName")
    fun getChannelRulesForAppFlow(packageName: String): Flow<List<ChannelRuleEntity>>

    @Query("SELECT * FROM channel_rules WHERE packageName = :packageName")
    suspend fun getChannelRulesForApp(packageName: String): List<ChannelRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelRule(rule: ChannelRuleEntity)
}

// ==========================================
// DATABASE SETUP
// ==========================================

@Database(
    entities = [
        BlockedAppEntity::class,
        SavedNotificationEntity::class,
        FocusModeEntity::class,
        ScheduleEntity::class,
        KeywordRuleEntity::class,
        NotificationLogEntity::class,
        FocusSessionEntity::class,
        ChannelRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NotificationDatabase : RoomDatabase() {
    abstract val dao: NotificationDao
}
