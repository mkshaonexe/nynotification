package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationRepository(private val context: Context) {

    val database: NotificationDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            NotificationDatabase::class.java,
            "mynotification_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val dao get() = database.dao

    // ==========================================
    // INITIALIZATION & SEED DATA
    // ==========================================
    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedDatabaseIfNeeded()
        }
    }

    private suspend fun seedDatabaseIfNeeded() {
        // Seed default focus modes if none exist
        val modes = dao.getAllFocusModes()
        if (modes.isEmpty()) {
            val presetModes = listOf(
                FocusModeEntity(
                    name = "Study",
                    iconName = "school",
                    colorHex = "#3F51B5",
                    allowedPackages = "com.android.server.telecom,com.android.mms",
                    blockedPackages = "com.instagram.android,com.facebook.katana,com.google.android.youtube,org.telegram.messenger,com.whatsapp",
                    isPreset = true
                ),
                FocusModeEntity(
                    name = "Sleep",
                    iconName = "bedtime",
                    colorHex = "#9C27B0",
                    allowedPackages = "alarm", // simulated system alarm package
                    blockedPackages = "com.instagram.android,com.facebook.katana,com.google.android.youtube,com.slack,com.google.android.gm",
                    isPreset = true
                ),
                FocusModeEntity(
                    name = "Work",
                    iconName = "work",
                    colorHex = "#4CAF50",
                    allowedPackages = "com.slack,com.google.android.gm,com.microsoft.teams",
                    blockedPackages = "com.instagram.android,com.facebook.katana,com.google.android.youtube,com.spotify.music",
                    isPreset = true
                ),
                FocusModeEntity(
                    name = "Prayer",
                    iconName = "church",
                    colorHex = "#009688",
                    allowedPackages = "",
                    blockedPackages = "com.instagram.android,com.facebook.katana,com.google.android.youtube,com.whatsapp,com.slack,com.google.android.gm,com.android.server.telecom",
                    isPreset = true
                ),
                FocusModeEntity(
                    name = "Exam",
                    iconName = "description",
                    colorHex = "#F44336",
                    allowedPackages = "",
                    blockedPackages = "com.instagram.android,com.facebook.katana,com.google.android.youtube,com.whatsapp,com.telegram,com.slack,com.android.server.telecom",
                    isPreset = true
                ),
                FocusModeEntity(
                    name = "Gaming",
                    iconName = "sports_esports",
                    colorHex = "#FF9800",
                    allowedPackages = "org.telegram.messenger,com.whatsapp",
                    blockedPackages = "com.slack,com.google.android.gm,com.microsoft.teams",
                    isPreset = true
                )
            )
            for (m in presetModes) {
                dao.insertFocusMode(m)
            }
        }

        // Seed default apps if none exist
        val apps = dao.getAllBlockedApps()
        if (apps.isEmpty()) {
            val defaultApps = listOf(
                BlockedAppEntity(packageName = "com.instagram.android", appLabel = "Instagram", isBlocked = true, priority = "Normal"),
                BlockedAppEntity(packageName = "com.whatsapp", appLabel = "WhatsApp", isBlocked = false, priority = "High", maxPerHour = 15),
                BlockedAppEntity(packageName = "com.slack", appLabel = "Slack", isBlocked = true, priority = "Normal"),
                BlockedAppEntity(packageName = "com.google.android.gm", appLabel = "Gmail", isBlocked = false, priority = "Normal"),
                BlockedAppEntity(packageName = "com.google.android.youtube", appLabel = "YouTube", isBlocked = true, priority = "Low"),
                BlockedAppEntity(packageName = "com.microsoft.teams", appLabel = "Microsoft Teams", isBlocked = false, priority = "Normal"),
                BlockedAppEntity(packageName = "com.spotify.music", appLabel = "Spotify", isBlocked = false, priority = "Low"),
                BlockedAppEntity(packageName = "com.facebook.katana", appLabel = "Facebook", isBlocked = true, priority = "Low"),
                BlockedAppEntity(packageName = "com.linkedin.android", appLabel = "LinkedIn", isBlocked = false, priority = "Normal"),
                BlockedAppEntity(packageName = "com.twitter.android", appLabel = "Twitter / X", isBlocked = true, priority = "Normal"),
                BlockedAppEntity(packageName = "org.telegram.messenger", appLabel = "Telegram", isBlocked = false, priority = "Normal"),
                BlockedAppEntity(packageName = "com.android.server.telecom", appLabel = "Phone Calls", isBlocked = false, priority = "Critical"),
                BlockedAppEntity(packageName = "com.android.mms", appLabel = "SMS Messages", isBlocked = false, priority = "Critical")
            )
            dao.insertBlockedApps(defaultApps)

            // Seed some sample channel rules
            dao.insertChannelRule(ChannelRuleEntity(packageName = "com.instagram.android", channelId = "direct", channelName = "Direct Messages", isBlocked = true))
            dao.insertChannelRule(ChannelRuleEntity(packageName = "com.instagram.android", channelId = "likes", channelName = "Likes & Comments", isBlocked = true))
            dao.insertChannelRule(ChannelRuleEntity(packageName = "com.whatsapp", channelId = "chats", channelName = "Group Chats", isBlocked = false))
            dao.insertChannelRule(ChannelRuleEntity(packageName = "com.slack", channelId = "mentions", channelName = "Direct Mentions", isBlocked = false))
            dao.insertChannelRule(ChannelRuleEntity(packageName = "com.slack", channelId = "channels", channelName = "Channel Updates", isBlocked = true))

            // Seed some default keywords
            dao.insertKeyword(KeywordRuleEntity(keyword = "sale", isEnabled = true))
            dao.insertKeyword(KeywordRuleEntity(keyword = "promo", isEnabled = true))
            dao.insertKeyword(KeywordRuleEntity(keyword = "discount", isEnabled = true))
            dao.insertKeyword(KeywordRuleEntity(keyword = "offer", isEnabled = true))
            dao.insertKeyword(KeywordRuleEntity(keyword = "urgent", isEnabled = false))

            // Seed some initial blocked notifications so the inbox is beautifully pre-populated with data
            val now = System.currentTimeMillis()
            dao.insertSavedNotification(SavedNotificationEntity(
                packageName = "com.instagram.android",
                appLabel = "Instagram",
                title = "alex_vander posted a new photo",
                body = "Check out alex_vander's new post! 📸 'Sunset in Malibu!'",
                channelId = "posts",
                category = "Social",
                isRead = false,
                receivedAt = now - 15 * 60 * 1000 // 15 mins ago
            ))
            dao.insertSavedNotification(SavedNotificationEntity(
                packageName = "com.facebook.katana",
                appLabel = "Facebook",
                title = "Urgent Sale: 50% Off Everything!",
                body = "Don't miss out on our midnight sale, get massive discount on tech accessories now!",
                channelId = "promo",
                category = "Promo",
                isRead = false,
                receivedAt = now - 45 * 60 * 1000 // 45 mins ago
            ))
            dao.insertSavedNotification(SavedNotificationEntity(
                packageName = "com.whatsapp",
                appLabel = "WhatsApp",
                title = "Verification Code",
                body = "Your MyNotification verification code is: 582491. Do not share this OTP with anyone.",
                channelId = "chats",
                category = "OTP",
                isRead = false,
                isOtp = true,
                otpCode = "582491",
                receivedAt = now - 2 * 3600 * 1000 // 2 hours ago
            ))
            dao.insertSavedNotification(SavedNotificationEntity(
                packageName = "com.slack",
                appLabel = "Slack",
                title = "#production-announcements",
                body = "Deployment of v1.4.2 was successful. Critical response metrics are perfectly green.",
                channelId = "channels",
                category = "Work",
                isPinned = true,
                isRead = true,
                receivedAt = now - 12 * 3600 * 1000 // 12 hours ago
            ))

            // Log history
            dao.insertLog(NotificationLogEntity(packageName = "com.instagram.android", appLabel = "Instagram", title = "alex_vander posted a new photo", action = "BLOCKED", category = "Social", timestamp = now - 15 * 60 * 1000))
            dao.insertLog(NotificationLogEntity(packageName = "com.facebook.katana", appLabel = "Facebook", title = "Urgent Sale: 50% Off Everything!", action = "BLOCKED", category = "Promo", timestamp = now - 45 * 60 * 1000))
            dao.insertLog(NotificationLogEntity(packageName = "com.whatsapp", appLabel = "WhatsApp", title = "Verification Code", action = "SAVED", category = "OTP", timestamp = now - 2 * 3600 * 1000))
            dao.insertLog(NotificationLogEntity(packageName = "com.slack", appLabel = "Slack", title = "#production-announcements", action = "BLOCKED", category = "Work", timestamp = now - 12 * 3600 * 1000))
        }
    }

    // ==========================================
    // EXPOSED FLOWS & CRUD OPERATORS
    // ==========================================

    // Blocked Apps
    val allBlockedAppsFlow: Flow<List<BlockedAppEntity>> = dao.getAllBlockedAppsFlow()
    suspend fun updateBlockedApp(app: BlockedAppEntity) = dao.insertBlockedApp(app)
    suspend fun getBlockedApp(packageName: String): BlockedAppEntity? = dao.getBlockedApp(packageName)

    // Saved Notifications
    val allSavedNotificationsFlow: Flow<List<SavedNotificationEntity>> = dao.getAllSavedNotificationsFlow()
    val otpNotificationsFlow: Flow<List<SavedNotificationEntity>> = dao.getOtpNotificationsFlow()
    suspend fun insertSavedNotification(notification: SavedNotificationEntity) = dao.insertSavedNotification(notification)
    suspend fun deleteNotification(id: Long) = dao.deleteNotification(id)
    suspend fun markAsRead(id: Long) = dao.markAsRead(id)
    suspend fun markAllAsRead() = dao.markAllAsRead()
    suspend fun setPinned(id: Long, isPinned: Boolean) = dao.setPinned(id, isPinned)
    suspend fun deleteAllNotifications() = dao.deleteAllNotifications()
    suspend fun purgeOldNotifications(olderThanMs: Long) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        dao.purgeNotificationsOlderThan(cutoff)
    }

    // Focus Modes
    val allFocusModesFlow: Flow<List<FocusModeEntity>> = dao.getAllFocusModesFlow()
    suspend fun getActiveFocusMode(): FocusModeEntity? = dao.getActiveFocusMode()
    suspend fun insertFocusMode(mode: FocusModeEntity): Long = dao.insertFocusMode(mode)
    suspend fun activateFocusMode(modeId: Long) {
        dao.deactivateAllFocusModes()
        dao.setFocusModeActive(modeId, true)
    }
    suspend fun deactivateActiveFocusMode() {
        dao.deactivateAllFocusModes()
    }
    suspend fun deleteFocusMode(mode: FocusModeEntity) = dao.deleteFocusMode(mode)

    // Schedules
    val allSchedulesFlow: Flow<List<ScheduleEntity>> = dao.getAllSchedulesFlow()
    suspend fun insertSchedule(schedule: ScheduleEntity) = dao.insertSchedule(schedule)
    suspend fun deleteSchedule(schedule: ScheduleEntity) = dao.deleteSchedule(schedule)
    suspend fun getAllSchedules(): List<ScheduleEntity> = dao.getAllSchedules()

    // Keywords
    val allKeywordsFlow: Flow<List<KeywordRuleEntity>> = dao.getAllKeywordsFlow()
    suspend fun insertKeyword(keyword: KeywordRuleEntity) = dao.insertKeyword(keyword)
    suspend fun deleteKeyword(keyword: KeywordRuleEntity) = dao.deleteKeyword(keyword)

    // Logs & History
    val allLogsFlow: Flow<List<NotificationLogEntity>> = dao.getAllLogsFlow()
    fun getRecentHistoryLogsFlow(limit: Int): Flow<List<NotificationLogEntity>> = dao.getRecentHistoryLogsFlow(limit)
    suspend fun clearLogs() = dao.clearAllLogs()

    // Focus Sessions
    val allSessionsFlow: Flow<List<FocusSessionEntity>> = dao.getAllSessionsFlow()
    suspend fun logFocusSession(session: FocusSessionEntity) = dao.insertSession(session)

    // Channel Control
    fun getChannelRulesForAppFlow(packageName: String): Flow<List<ChannelRuleEntity>> = dao.getChannelRulesForAppFlow(packageName)
    suspend fun insertChannelRule(rule: ChannelRuleEntity) = dao.insertChannelRule(rule)

    // ==========================================
    // NOTIFICATION INTERCEPTION & SIMULATION ENGINE
    // ==========================================
    /**
     * Simulates an incoming notification being processed by the MyNotification Engine.
     * Evaluates constraints (Master Block, App Blocking, Focus Mode allowed/blocked, Whitelists, Priority, Keywords).
 * Returns true if the notification is ALLOWED (breaks through), and false if it is INTERCEPTED (blocked/saved).
     */
    suspend fun processSimulatedNotification(
        packageName: String,
        appLabel: String,
        title: String,
        body: String,
        channelId: String,
        isMasterBlockEnabled: Boolean
    ): SimulationResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Fetch metadata about the app from local database
        val appConfig = dao.getBlockedApp(packageName)
            ?: BlockedAppEntity(packageName = packageName, appLabel = appLabel)

        // 2. Local OTP Detector
        val isOtp = detectOtp(body)
        val otpCode = if (isOtp) extractOtp(body) else null
        val category = when {
            isOtp -> "OTP"
            body.contains("work", ignoreCase = true) || body.contains("project", ignoreCase = true) || body.contains("task", ignoreCase = true) || body.contains("meeting", ignoreCase = true) || body.contains("deadline", ignoreCase = true) -> "Work"
            body.contains("sale", ignoreCase = true) || body.contains("promo", ignoreCase = true) || body.contains("discount", ignoreCase = true) || body.contains("offer", ignoreCase = true) || body.contains("buy", ignoreCase = true) -> "Promo"
            channelId == "direct" || channelId == "chats" || body.contains("sent you", ignoreCase = true) || body.contains("mentioned", ignoreCase = true) -> "Social"
            else -> "General"
        }

        // 3. WHITING LIST CHECK (Phone emergency call, Alarm, and OTP override focus block)
        if (appConfig.priority == "Critical" || isOtp) {
            val log = NotificationLogEntity(packageName = packageName, appLabel = appLabel, title = title, action = "ALLOWED (Bypass)", category = category, timestamp = now)
            dao.insertLog(log)
            return@withContext SimulationResult(isAllowed = true, reason = "Critical bypass / OTP", category = category)
        }

        // 4. CHECK USER KEYWORDS BLOCK
        val globalKeywords = dao.getAllKeywords().filter { it.isEnabled && (it.packageName == null || it.packageName == packageName) }
        val triggeredKeyword = globalKeywords.firstOrNull { kw ->
            val matchPattern = kw.keyword.lowercase()
            if (kw.useWildcard) {
                body.lowercase().contains(matchPattern) || title.lowercase().contains(matchPattern)
            } else {
                body.lowercase().split("\\W+".toRegex()).contains(matchPattern) || title.lowercase().split("\\W+".toRegex()).contains(matchPattern)
            }
        }
        if (triggeredKeyword != null) {
            // Blocked by Keyword!
            val notificationEntity = SavedNotificationEntity(
                packageName = packageName, appLabel = appLabel, title = title, body = body, channelId = channelId, category = "Promo", isOtp = false, receivedAt = now
            )
            dao.insertSavedNotification(notificationEntity)
            dao.insertLog(NotificationLogEntity(packageName = packageName, appLabel = appLabel, title = title, action = "BLOCKED (Keyword: '${triggeredKeyword.keyword}')", category = "Promo", timestamp = now))
            return@withContext SimulationResult(isAllowed = false, reason = "Keyword Rule found: '${triggeredKeyword.keyword}'", category = "Promo")
        }

        // 5. CHECK FOCUS MODE LOGIC
        val activeFocusMode = dao.getActiveFocusMode()
        if (activeFocusMode != null) {
            val allowedPkgs = activeFocusMode.allowedPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val blockedPkgs = activeFocusMode.blockedPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val isSpecificallyAllowed = packageName in allowedPkgs
            val isSpecificallyBlocked = packageName in blockedPkgs

            // If focus mode has an explicit blocklist, block it if matches. If it has an allowlist, only allow if matches.
            val blockNotification = when {
                isSpecificallyBlocked -> true
                allowedPkgs.isNotEmpty() && !isSpecificallyAllowed -> true
                else -> false
            }

            if (blockNotification) {
                val notificationEntity = SavedNotificationEntity(
                    packageName = packageName, appLabel = appLabel, title = title, body = body, channelId = channelId, category = category, receivedAt = now
                )
                dao.insertSavedNotification(notificationEntity)
                dao.insertLog(NotificationLogEntity(packageName = packageName, appLabel = appLabel, title = title, action = "BLOCKED (${activeFocusMode.name} Mode)", category = category, timestamp = now))
                return@withContext SimulationResult(isAllowed = false, reason = "Blocked by active Focus: ${activeFocusMode.name}", category = category)
            }
        }

        // 6. CHECK MASTER BLOCK TOGGLE
        if (isMasterBlockEnabled) {
            val notificationEntity = SavedNotificationEntity(
                packageName = packageName, appLabel = appLabel, title = title, body = body, channelId = channelId, category = category, receivedAt = now
            )
            dao.insertSavedNotification(notificationEntity)
            dao.insertLog(NotificationLogEntity(packageName = packageName, appLabel = appLabel, title = title, action = "BLOCKED (Master Toggle)", category = category, timestamp = now))
            return@withContext SimulationResult(isAllowed = false, reason = "Master Block Mode active", category = category)
        }

        // 7. CHECK APP-SPECIFIC BLOCKING RULE
        if (appConfig.isBlocked) {
            val notificationEntity = SavedNotificationEntity(
                packageName = packageName, appLabel = appLabel, title = title, body = body, channelId = channelId, category = category, receivedAt = now
            )
            dao.insertSavedNotification(notificationEntity)
            dao.insertLog(NotificationLogEntity(packageName = packageName, appLabel = appLabel, title = title, action = "BLOCKED (App Blocker)", category = category, timestamp = now))
            return@withContext SimulationResult(isAllowed = false, reason = "Blocked in App settings", category = category)
        }

        // Default: ALLOWED
        dao.insertLog(NotificationLogEntity(packageName = packageName, appLabel = appLabel, title = title, action = "ALLOWED", category = category, timestamp = now))
        SimulationResult(isAllowed = true, reason = "No blocking rule triggered", category = category)
    }

    private fun detectOtp(body: String): Boolean {
        // Matches standard 4-8 digit numeric codes
        val regex = "\\b\\d{4,8}\\b".toRegex()
        val hasDigits = regex.containsMatchIn(body)
        val hasOtpKeyword = body.contains("otp", ignoreCase = true) ||
                body.contains("code", ignoreCase = true) ||
                body.contains("verification", ignoreCase = true) ||
                body.contains("verify", ignoreCase = true)
        return hasDigits && hasOtpKeyword
    }

    private fun extractOtp(body: String): String? {
        val regex = "\\b\\d{4,8}\\b".toRegex()
        return regex.find(body)?.value
    }
}

data class SimulationResult(
    val isAllowed: Boolean,
    val reason: String,
    val category: String
)
