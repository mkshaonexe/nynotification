package com.quietinbox.feature.permissions

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.quietinbox.core.health.HealthState
import com.quietinbox.data.work.FakeListenerHealth
import com.quietinbox.feature.onboarding.FakeTestClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PermissionsHealthViewModelTest {

    private lateinit var context: Context
    private lateinit var fakeHealth: FakeListenerHealth
    private lateinit var clock: FakeTestClock
    private lateinit var viewModel: PermissionsHealthViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeHealth = FakeListenerHealth(HealthState.CONNECTED)
        clock = FakeTestClock(currentEpochMs = 1_000_000L)
        viewModel = PermissionsHealthViewModel(context, fakeHealth, clock)
    }

    @Test
    fun `reconnectListener calls forceRebind and refresh on ListenerHealth`() {
        viewModel.reconnectListener()
        assertTrue(fakeHealth.forceRebindCalled)
        assertTrue(fakeHealth.refreshCalled)
    }

    @Test
    fun `dismissOemNotice updates uiState`() {
        assertFalse(viewModel.uiState.value.isOemNoticeDismissed)
        viewModel.dismissOemNotice()
        assertTrue(viewModel.uiState.value.isOemNoticeDismissed)
    }

    @Test
    fun `show and hide autostart dialog toggles state`() {
        assertFalse(viewModel.uiState.value.showAutostartDialog)
        viewModel.showAutostartDialog()
        assertTrue(viewModel.uiState.value.showAutostartDialog)
        viewModel.hideAutostartDialog()
        assertFalse(viewModel.uiState.value.showAutostartDialog)
    }

    @Test
    fun `formatLastCapture formats timestamps accurately`() {
        assertEquals("Never", viewModel.formatLastCapture(null))
        assertEquals("Never", viewModel.formatLastCapture(0L))

        // Just now (within 60s)
        clock.currentEpochMs = 100_000L
        assertEquals("Just now", viewModel.formatLastCapture(90_000L))

        // 5 minutes ago
        clock.currentEpochMs = 100_000L + (5 * 60 * 1000L)
        assertEquals("5m ago", viewModel.formatLastCapture(100_000L))

        // 2 hours ago
        clock.currentEpochMs = 100_000L + (2 * 3600 * 1000L)
        assertEquals("2h ago", viewModel.formatLastCapture(100_000L))

        // 1 day ago (Yesterday)
        clock.currentEpochMs = 100_000L + (25 * 3600 * 1000L)
        assertEquals("Yesterday", viewModel.formatLastCapture(100_000L))

        // 5 days ago
        clock.currentEpochMs = 100_000L + (5 * 86400 * 1000L)
        assertEquals("5d ago", viewModel.formatLastCapture(100_000L))
    }
}
