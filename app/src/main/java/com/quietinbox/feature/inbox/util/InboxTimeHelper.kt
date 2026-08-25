package com.quietinbox.feature.inbox.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Time and date formatting utilities for Inbox day headers, relative timestamps, and session spans.
 */
object InboxTimeHelper {

    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val DAY_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val FULL_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())

    /**
     * Converts epoch milliseconds to local epoch day (days since 1970-01-01).
     */
    fun toEpochDay(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate().toEpochDay()
    }

    /**
     * Formats a day header string: "Today", "Yesterday", or "d MMM" (e.g. "24 Aug").
     */
    fun formatDayHeader(
        epochMs: Long,
        todayEpochDay: Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val date = Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
        val itemEpochDay = date.toEpochDay()

        return when (itemEpochDay) {
            todayEpochDay -> "Today"
            todayEpochDay - 1 -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("d MMM", locale))
        }
    }

    /**
     * Converts a yyyymmdd Int (e.g., 20260826) into start and end epoch milliseconds in the given time zone.
     */
    fun dayNumberToEpochRange(
        dayNumber: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<Long, Long> {
        val year = dayNumber / 10000
        val month = (dayNumber % 10000) / 100
        val day = dayNumber % 100
        val localDate = LocalDate.of(year, month, day)
        val startEpochMs = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endEpochMs = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        return Pair(startEpochMs, endEpochMs)
    }

    /**
     * Formats a relative timestamp for ALERT rows (e.g. "Just now", "5m", "2h", or "10:17 PM").
     */
    fun formatRelativeTime(
        epochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val diffMs = (nowEpochMs - epochMs).coerceAtLeast(0)
        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHours = diffMin / 60
        val diffDays = diffHours / 24

        return when {
            diffSec < 60 -> "Just now"
            diffMin < 60 -> "${diffMin}m"
            diffHours < 24 -> "${diffHours}h"
            diffDays < 7 -> "${diffDays}d"
            else -> Instant.ofEpochMilli(epochMs).atZone(zoneId).format(TIME_FORMATTER)
        }
    }

    /**
     * Formats session timestamp range and duration for ONGOING/PROGRESS/TRANSPORT/SERVICE rows.
     * Example: "10:17 PM – 10:27 PM · 10 min · 612 updates" or "10:17 PM – Now · 5 min · 1 update"
     */
    fun formatSessionDescription(
        firstSeenAt: Long,
        lastSeenAt: Long,
        endedAt: Long?,
        updateCount: Int,
        wasRateLimited: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val startTimeStr = Instant.ofEpochMilli(firstSeenAt).atZone(zoneId).format(TIME_FORMATTER)
        val isEnded = endedAt != null
        val endTimeStr = if (isEnded) {
            Instant.ofEpochMilli(endedAt!!).atZone(zoneId).format(TIME_FORMATTER)
        } else {
            "Now"
        }

        val effectiveEndMs = endedAt ?: lastSeenAt
        val durationMs = (effectiveEndMs - firstSeenAt).coerceAtLeast(0)
        val durationMinutes = ChronoUnit.MINUTES.between(
            Instant.ofEpochMilli(firstSeenAt),
            Instant.ofEpochMilli(effectiveEndMs)
        ).coerceAtLeast(1)

        val durationStr = if (durationMinutes >= 60) {
            val hours = durationMinutes / 60
            val remainingMinutes = durationMinutes % 60
            if (remainingMinutes > 0) "${hours}h ${remainingMinutes}m" else "${hours}h"
        } else {
            "${durationMinutes} min"
        }

        val updateCopy = if (updateCount <= 1) "1 update" else "$updateCount updates"
        val rateLimitedSuffix = if (wasRateLimited) " (rate limited)" else ""

        return "$startTimeStr – $endTimeStr · $durationStr · $updateCopy$rateLimitedSuffix"
    }

    /**
     * Formats a full date and time string for the detail sheet.
     */
    fun formatFullDateTime(
        epochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return Instant.ofEpochMilli(epochMs).atZone(zoneId).format(FULL_DATE_TIME_FORMATTER)
    }
}
