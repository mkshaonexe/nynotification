package com.quietinbox.core.time

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An injectable clock. Production code must not call System.currentTimeMillis()
 * directly so tests can control time.
 */
interface Clock {
    fun now(): Long
    fun elapsedRealtime(): Long
}

@Singleton
class DefaultClock @Inject constructor() : Clock {
    override fun now(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
