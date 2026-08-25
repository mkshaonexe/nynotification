package com.quietinbox.feature.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietinbox.core.time.Clock
import com.quietinbox.data.prefs.AppSettings
import com.quietinbox.data.prefs.SettingsDataStore
import com.quietinbox.data.work.DigestWorker
import com.quietinbox.data.work.WorkScheduler
import com.quietinbox.feature.settings.data.ExportManager
import com.quietinbox.feature.settings.data.SettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val settingsDao: SettingsDao,
    private val exportManager: ExportManager,
    private val workScheduler: WorkScheduler,
    private val clock: Clock
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    val notificationCount: StateFlow<Int> = settingsDao.observeNotificationCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val starredCount: StateFlow<Int> = settingsDao.observeStarredCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val oldestTimestamp: StateFlow<Long?> = settingsDao.observeOldestTimestamp()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val lastCaptureTimestamp: StateFlow<Long?> = settingsDao.observeNewestTimestamp()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _dbSizeBytes = MutableStateFlow(0L)
    val dbSizeBytes: StateFlow<Long> = _dbSizeBytes.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

    private val digestPrefs = context.getSharedPreferences(DigestWorker.PREFS_NAME, Context.MODE_PRIVATE)
    private val _isDigestEnabled = MutableStateFlow(digestPrefs.getBoolean(DigestWorker.KEY_DIGEST_ENABLED, false))
    val isDigestEnabled: StateFlow<Boolean> = _isDigestEnabled.asStateFlow()

    init {
        refreshDbSize()
    }

    fun refreshDbSize() {
        viewModelScope.launch {
            val dbFile = context.getDatabasePath("quiet_inbox.db")
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

            var totalSize = 0L
            if (dbFile.exists()) totalSize += dbFile.length()
            if (walFile.exists()) totalSize += walFile.length()
            if (shmFile.exists()) totalSize += shmFile.length()

            _dbSizeBytes.value = totalSize
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDynamicColorEnabled(enabled)
        }
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch {
            settingsDataStore.setRetentionDays(days)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAppLockEnabled(enabled)
        }
    }

    fun setDigestEnabled(enabled: Boolean) {
        _isDigestEnabled.value = enabled
        digestPrefs.edit().putBoolean(DigestWorker.KEY_DIGEST_ENABLED, enabled).apply()
        if (enabled) {
            workScheduler.scheduleDigestWorker()
        } else {
            workScheduler.cancelDigestWorker()
        }
    }

    fun rerunOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(false)
            onComplete()
        }
    }

    fun deleteAllHistory(onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsDao.deleteAllNotifications()
            settingsDao.deleteAllFirewallDecisions()
            settingsDao.deleteAllDailyStats()
            refreshDbSize()
            onComplete()
        }
    }

    fun exportJson(contentResolver: ContentResolver, uri: Uri, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            val result = exportManager.exportJsonToUri(contentResolver, uri)
            _isExporting.value = false
            result.onSuccess { count ->
                _exportResult.value = "Exported $count notifications as JSON"
                onSuccess(count)
            }.onFailure { error ->
                val errorMsg = error.localizedMessage ?: "Unknown export error"
                _exportResult.value = "Export failed: $errorMsg"
                onError(errorMsg)
            }
        }
    }

    fun exportCsv(contentResolver: ContentResolver, uri: Uri, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            val result = exportManager.exportCsvToUri(contentResolver, uri)
            _isExporting.value = false
            result.onSuccess { count ->
                _exportResult.value = "Exported $count notifications as CSV"
                onSuccess(count)
            }.onFailure { error ->
                val errorMsg = error.localizedMessage ?: "Unknown export error"
                _exportResult.value = "Export failed: $errorMsg"
                onError(errorMsg)
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(java.util.Locale.US, "%.2f GB", gb)
    }
}
