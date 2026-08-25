package com.quietinbox.core.signal

/**
 * Global tuning parameters for notification signal classification and spam mitigation.
 *
 * Nothing else in the codebase may hardcode these values.
 */
object SignalTuning {
    /** Coalescing window in milliseconds for burst notifications with the same SBN key (10 seconds). */
    const val BURST_WINDOW_MS: Long = 10_000L

    /** Maximum allowed event inserts per SBN key within a rolling one-hour window. */
    const val MAX_EVENTS_PER_KEY_PER_HOUR: Int = 60

    /** Maximum character count for notification body text before truncation during hashing and storage. */
    const val BODY_MAX_CHARS: Int = 4_000

    /** Window within which a group summary checks for existing non-summary child notifications (5 minutes). */
    const val GROUP_SUMMARY_WINDOW_MS: Long = 5 * 60 * 1000L

    /** Duration of one hour in milliseconds. */
    const val ONE_HOUR_MS: Long = 60 * 60 * 1000L
}
