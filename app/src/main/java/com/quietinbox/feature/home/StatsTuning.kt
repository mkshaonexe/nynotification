package com.quietinbox.feature.home

/**
 * Tuning parameters and constants for statistical calculations and estimates.
 * Owned by Phase 5.
 */
object StatsTuning {
    /**
     * Default seconds estimated for refocusing after an interruption.
     */
    const val SECONDS_PER_INTERRUPTION: Int = 8

    /**
     * User selectable refocus options in seconds.
     */
    val REFOCUS_OPTIONS: List<Int> = listOf(4, 8, 15)

    /**
     * Threshold of silenced notifications required before showing the focus reclaimed card.
     */
    const val FOCUS_RECLAIMED_MIN_SILENCED: Int = 50

    /**
     * Minimum minutes of active Quiet Mode in a calendar day to qualify as a "quiet day".
     * (4 hours = 240 minutes).
     */
    const val QUIET_DAY_MINIMUM_MINUTES: Int = 240

    /**
     * Default number of weeks displayed in the activity heatmap (7 rows x 26 columns).
     */
    const val HEATMAP_WEEKS: Int = 26
}
