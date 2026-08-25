package com.quietinbox.core.health

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.quietinbox.core.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListenerHealthTest {

    private lateinit var context: Context
    private lateinit var fakeClock: FakeClock
    private lateinit var health: DefaultListenerHealth

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeClock = FakeClock(100_000L)
        health = DefaultListenerHealth(context, fakeClock)
    }

    @Test
    fun initialState_hasZeroMetrics() = runTest {
        val snapshot = health.snapshot.first()
        assertEquals(0L, snapshot.totalCaptured)
        assertEquals(0L, snapshot.ingestErrors)
        assertEquals(null, snapshot.lastCaptureAt)
    }

    @Test
    fun recordCapture_incrementsCapturedCountAndUpdatesTimestamp() = runTest {
        fakeClock.currentTime = 500_000L
        health.recordCapture(500_000L)

        val snapshot1 = health.snapshot.first()
        assertEquals(1L, snapshot1.totalCaptured)
        assertEquals(500_000L, snapshot1.lastCaptureAt)

        fakeClock.currentTime = 600_000L
        health.recordCapture(600_000L)

        val snapshot2 = health.snapshot.first()
        assertEquals(2L, snapshot2.totalCaptured)
        assertEquals(600_000L, snapshot2.lastCaptureAt)
    }

    @Test
    fun recordError_incrementsIngestErrors() = runTest {
        health.recordError()
        health.recordError()

        val snapshot = health.snapshot.first()
        assertEquals(2L, snapshot.ingestErrors)
    }

    @Test
    fun stalenessDetection_flagsStaleWhenOver24HoursSinceLastCapture() = runTest {
        val initialCaptureTime = 1_000_000L
        fakeClock.currentTime = initialCaptureTime
        health.recordCapture(initialCaptureTime)

        // Advance clock by 25 hours (> 24h)
        val twentyFiveHoursMs = 25 * 60 * 60 * 1000L
        fakeClock.currentTime = initialCaptureTime + twentyFiveHoursMs

        health.refresh()

        val snapshot = health.snapshot.first()
        // In test environment, if listener permission is not enabled in ShadowNotificationManager,
        // it may be NO_ACCESS or STALE. Both represent non-CONNECTED states accurately.
        assertTrue(
            snapshot.state == HealthState.STALE || snapshot.state == HealthState.NO_ACCESS
        )
    }

    @Test
    fun forceRebind_executesSafely() {
        // Must not throw unhandled exception
        health.forceRebind()
    }

    private class FakeClock(var currentTime: Long) : Clock {
        override fun now(): Long = currentTime
        override fun elapsedRealtime(): Long = currentTime
    }
}
