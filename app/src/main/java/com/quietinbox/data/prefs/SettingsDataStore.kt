package com.quietinbox.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "quiet_inbox_settings")

data class AppSettings(
    val quietModeEnabled: Boolean = false,
    val pausedUntilEpochMs: Long = 0L,
    val leaveSystemAndMediaAlone: Boolean = true,
    val otpAlwaysBreaksThrough: Boolean = true,
    val retentionDays: Int = 90,
    val showTransportInInbox: Boolean = false,
    val appLockEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val themeMode: String = "SYSTEM", // SYSTEM | DARK | LIGHT
    val dynamicColorEnabled: Boolean = false,
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    private object PreferencesKeys {
        val QUIET_MODE_ENABLED = booleanPreferencesKey("quiet_mode_enabled")
        val PAUSED_UNTIL_EPOCH_MS = longPreferencesKey("paused_until_epoch_ms")
        val LEAVE_SYSTEM_AND_MEDIA_ALONE = booleanPreferencesKey("leave_system_and_media_alone")
        val OTP_ALWAYS_BREAKS_THROUGH = booleanPreferencesKey("otp_always_breaks_through")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
        val SHOW_TRANSPORT_IN_INBOX = booleanPreferencesKey("show_transport_in_inbox")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            quietModeEnabled = preferences[PreferencesKeys.QUIET_MODE_ENABLED] ?: false,
            pausedUntilEpochMs = preferences[PreferencesKeys.PAUSED_UNTIL_EPOCH_MS] ?: 0L,
            leaveSystemAndMediaAlone = preferences[PreferencesKeys.LEAVE_SYSTEM_AND_MEDIA_ALONE] ?: true,
            otpAlwaysBreaksThrough = preferences[PreferencesKeys.OTP_ALWAYS_BREAKS_THROUGH] ?: true,
            retentionDays = preferences[PreferencesKeys.RETENTION_DAYS] ?: 90,
            showTransportInInbox = preferences[PreferencesKeys.SHOW_TRANSPORT_IN_INBOX] ?: false,
            appLockEnabled = preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false,
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            dynamicColorEnabled = preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] ?: false,
        )
    }

    suspend fun setQuietModeEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.QUIET_MODE_ENABLED] = enabled }
    }

    suspend fun setPausedUntilEpochMs(epochMs: Long) {
        dataStore.edit { it[PreferencesKeys.PAUSED_UNTIL_EPOCH_MS] = epochMs }
    }

    suspend fun setLeaveSystemAndMediaAlone(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.LEAVE_SYSTEM_AND_MEDIA_ALONE] = enabled }
    }

    suspend fun setOtpAlwaysBreaksThrough(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.OTP_ALWAYS_BREAKS_THROUGH] = enabled }
    }

    suspend fun setRetentionDays(days: Int) {
        dataStore.edit { it[PreferencesKeys.RETENTION_DAYS] = days }
    }

    suspend fun setShowTransportInInbox(show: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_TRANSPORT_IN_INBOX] = show }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DYNAMIC_COLOR_ENABLED] = enabled }
    }
}
