package com.quietinbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quietinbox.data.prefs.SettingsDataStore
import com.quietinbox.navigation.QuietNavHost
import com.quietinbox.ui.theme.QuietInboxTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by settingsDataStore.settings.collectAsState(initial = null)
            val currentSettings = settings

            val isDark = when (currentSettings?.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            val dynamicColor = currentSettings?.dynamicColorEnabled ?: false

            QuietInboxTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColor
            ) {
                QuietNavHost(
                    onboardingCompleted = currentSettings?.onboardingCompleted ?: false
                )
            }
        }
    }
}
