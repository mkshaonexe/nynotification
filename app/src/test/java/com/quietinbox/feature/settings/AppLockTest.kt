package com.quietinbox.feature.settings

import com.quietinbox.core.time.Clock
import com.quietinbox.feature.settings.util.AppLockManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockTest {

    private class FakeClock(var currentTimeMs: Long = 1000000L) : Clock {
        override fun now(): Long = currentTimeMs
        override fun elapsedRealtime(): Long = currentTimeMs
    }

    private lateinit var fakeClock: FakeClock
    private lateinit var appLockManager: AppLockManager

    @Before
    fun setup() {
        fakeClock = FakeClock()
        appLockManager = AppLockManager(fakeClock)
    }

    @Test
    fun testColdStartLocksWhenEnabled() {
        appLockManager.onColdStart(appLockEnabled = true)
        assertTrue(appLockManager.isLocked.value)
    }

    @Test
    fun testColdStartDoesNotLockWhenDisabled() {
        appLockManager.onColdStart(appLockEnabled = false)
        assertFalse(appLockManager.isLocked.value)
    }

    @Test
    fun testUnlockClearsLockState() {
        appLockManager.onColdStart(appLockEnabled = true)
        assertTrue(appLockManager.isLocked.value)

        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)
    }

    @Test
    fun testBackgroundUnder60SecondsDoesNotLock() {
        appLockManager.onColdStart(appLockEnabled = true)
        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)

        // App backgrounded at t = 1000000
        appLockManager.onAppBackgrounded()

        // Advance clock by 30 seconds
        fakeClock.currentTimeMs += 30_000L

        // Return to foreground
        appLockManager.onAppForegrounded(appLockEnabled = true)
        assertFalse("App should not lock if backgrounded for less than 60s", appLockManager.isLocked.value)
    }

    @Test
    fun testBackgroundOver60SecondsLocks() {
        appLockManager.onColdStart(appLockEnabled = true)
        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)

        // App backgrounded at t = 1000000
        appLockManager.onAppBackgrounded()

        // Advance clock by 61 seconds
        fakeClock.currentTimeMs += 61_000L

        // Return to foreground
        appLockManager.onAppForegrounded(appLockEnabled = true)
        assertTrue("App should lock if backgrounded for 60s or more", appLockManager.isLocked.value)
    }

    @Test
    fun testDisabledAppLockNeverLocksOnForeground() {
        appLockManager.onColdStart(appLockEnabled = false)
        assertFalse(appLockManager.isLocked.value)

        appLockManager.onAppBackgrounded()
        fakeClock.currentTimeMs += 120_000L // 2 minutes

        appLockManager.onAppForegrounded(appLockEnabled = false)
        assertFalse(appLockManager.isLocked.value)
    }
}
