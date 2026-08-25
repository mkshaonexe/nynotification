package com.quietinbox.data.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsDataStoreTest {

    private lateinit var context: Context
    private lateinit var dataStore: SettingsDataStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dataStore = SettingsDataStore(context)
    }

    @Test
    fun defaultSettingsValuesAreCorrect() = runTest {
        dataStore.settings.test {
            val initial = awaitItem()
            assertFalse(initial.quietModeEnabled)
            assertEquals(0L, initial.pausedUntilEpochMs)
            assertTrue(initial.leaveSystemAndMediaAlone)
            assertTrue(initial.otpAlwaysBreaksThrough)
            assertEquals(90, initial.retentionDays)
            assertFalse(initial.showTransportInInbox)
            assertFalse(initial.appLockEnabled)
            assertFalse(initial.onboardingCompleted)
            assertEquals("SYSTEM", initial.themeMode)
            assertFalse(initial.dynamicColorEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateQuietModeAndRetentionDays() = runTest {
        dataStore.setQuietModeEnabled(true)
        dataStore.setRetentionDays(365)
        dataStore.setOnboardingCompleted(true)

        dataStore.settings.test {
            val updated = awaitItem()
            assertTrue(updated.quietModeEnabled)
            assertEquals(365, updated.retentionDays)
            assertTrue(updated.onboardingCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
