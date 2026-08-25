package com.quietinbox.feature.home.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

/**
 * Represents a single cell in the activity heatmap.
 */
data class HeatmapCell(
    val date: LocalDate,
    val dayInt: Int,
    val count: Int,
    val level: Int,
    val contentDescription: String
)

/**
 * Represents a week column in the activity heatmap (containing up to 7 day cells, Monday to Sunday).
 */
data class HeatmapWeek(
    val days: List<HeatmapCell?>
)

/**
 * Result data class for the computed heatmap.
 */
data class HeatmapData(
    val weeks: List<HeatmapWeek>,
    val p90: Double
)

/**
 * Logic for computing 5-level intensity bucketing from 90th percentile data and structuring
 * the 7x26 activity heatmap.
 */
object HeatmapBucketer {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())

    /**
     * Calculates the 90th percentile from a list of integer counts.
     */
    fun calculateP90(counts: List<Int>): Double {
        val nonZero = counts.filter { it > 0 }.sorted()
        if (nonZero.isEmpty()) return 0.0
        val index = ceil(0.90 * nonZero.size).toInt() - 1
        val safeIndex = index.coerceIn(0, nonZero.size - 1)
        return nonZero[safeIndex].toDouble()
    }

    /**
     * Maps a count to an intensity level from 0 to 4 based on the 90th percentile.
     */
    fun mapLevel(count: Int, p90: Double): Int {
        if (count <= 0) return 0
        if (p90 <= 0.0) return 1

        val q1 = p90 * 0.25
        val q2 = p90 * 0.50
        val q3 = p90 * 0.75

        return when {
            count <= q1 -> 1
            count <= q2 -> 2
            count <= q3 -> 3
            else -> 4
        }
    }

    /**
     * Builds the heatmap grid ending at [today] for the specified number of [weeks].
     *
     * @param dayCounts Map of day integer (YYYYMMDD) to silenced count.
     * @param today Reference date for the end of the grid.
     * @param weeks Number of weeks (columns) to generate (default 26).
     */
    fun buildHeatmap(
        dayCounts: Map<Int, Int>,
        today: LocalDate = LocalDate.now(),
        weeks: Int = 26
    ): HeatmapData {
        val p90 = calculateP90(dayCounts.values.toList())

        // Find Monday of the week `weeks - 1` weeks ago
        val currentMonday = today.with(DayOfWeek.MONDAY)
        val startMonday = currentMonday.minusWeeks((weeks - 1).toLong())

        val resultWeeks = mutableListOf<HeatmapWeek>()

        var weekStart = startMonday
        for (w in 0 until weeks) {
            val weekDays = mutableListOf<HeatmapCell?>()
            for (dayOffset in 0..6) {
                val cellDate = weekStart.plusDays(dayOffset.toLong())
                if (cellDate.isAfter(today)) {
                    // Future day in the current week
                    weekDays.add(null)
                } else {
                    val dayInt = StreakCalculator.toDayInt(cellDate)
                    val count = dayCounts[dayInt] ?: 0
                    val level = mapLevel(count, p90)
                    val formattedDate = cellDate.format(dateFormatter)
                    val description = "$formattedDate, $count silenced"

                    weekDays.add(
                        HeatmapCell(
                            date = cellDate,
                            dayInt = dayInt,
                            count = count,
                            level = level,
                            contentDescription = description
                        )
                    )
                }
            }
            resultWeeks.add(HeatmapWeek(days = weekDays))
            weekStart = weekStart.plusWeeks(1)
        }

        return HeatmapData(weeks = resultWeeks, p90 = p90)
    }
}
