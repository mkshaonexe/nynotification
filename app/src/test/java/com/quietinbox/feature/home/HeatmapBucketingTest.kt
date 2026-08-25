package com.quietinbox.feature.home

import com.quietinbox.feature.home.domain.HeatmapBucketer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HeatmapBucketingTest {

    @Test
    fun `p90 calculation on empty or zero list returns zero`() {
        assertEquals(0.0, HeatmapBucketer.calculateP90(emptyList()), 0.001)
        assertEquals(0.0, HeatmapBucketer.calculateP90(listOf(0, 0, 0)), 0.001)
    }

    @Test
    fun `p90 calculation on uniform distribution`() {
        val counts = (1..100).toList()
        val p90 = HeatmapBucketer.calculateP90(counts)
        assertEquals(90.0, p90, 0.001)
    }

    @Test
    fun `mapLevel maps counts to 5 distinct levels`() {
        val p90 = 100.0

        // Level 0: 0 or negative
        assertEquals(0, HeatmapBucketer.mapLevel(0, p90))
        assertEquals(0, HeatmapBucketer.mapLevel(-5, p90))

        // Level 1: 1 .. 25
        assertEquals(1, HeatmapBucketer.mapLevel(1, p90))
        assertEquals(1, HeatmapBucketer.mapLevel(25, p90))

        // Level 2: 26 .. 50
        assertEquals(2, HeatmapBucketer.mapLevel(26, p90))
        assertEquals(2, HeatmapBucketer.mapLevel(50, p90))

        // Level 3: 51 .. 75
        assertEquals(3, HeatmapBucketer.mapLevel(51, p90))
        assertEquals(3, HeatmapBucketer.mapLevel(75, p90))

        // Level 4: 76 .. 100+
        assertEquals(4, HeatmapBucketer.mapLevel(76, p90))
        assertEquals(4, HeatmapBucketer.mapLevel(100, p90))
        assertEquals(4, HeatmapBucketer.mapLevel(500, p90))
    }

    @Test
    fun `buildHeatmap constructs 26 weeks and 7 day rows`() {
        val today = LocalDate.of(2026, 8, 26) // Wednesday
        val dayCounts = mapOf(
            20260824 to 10,
            20260825 to 25,
            20260826 to 50
        )

        val heatmap = HeatmapBucketer.buildHeatmap(
            dayCounts = dayCounts,
            today = today,
            weeks = 26
        )

        assertEquals(26, heatmap.weeks.size)

        // All previous weeks should have 7 non-null days
        for (w in 0 until 25) {
            assertEquals(7, heatmap.weeks[w].days.size)
            heatmap.weeks[w].days.forEach { cell ->
                assertNotNull("Past cell should not be null", cell)
            }
        }

        // The current week (Wednesday today) should have Mon, Tue, Wed non-null, and Thu, Fri, Sat, Sun null
        val currentWeek = heatmap.weeks.last()
        assertEquals(7, currentWeek.days.size)
        assertNotNull(currentWeek.days[0]) // Monday
        assertNotNull(currentWeek.days[1]) // Tuesday
        assertNotNull(currentWeek.days[2]) // Wednesday (Today)
        assertNull(currentWeek.days[3])    // Thursday (Future)
        assertNull(currentWeek.days[4])    // Friday (Future)
        assertNull(currentWeek.days[5])    // Saturday (Future)
        assertNull(currentWeek.days[6])    // Sunday (Future)

        // Verify content description of today's cell
        val todayCell = currentWeek.days[2]
        assertNotNull(todayCell)
        assertTrue(todayCell!!.contentDescription.contains("50 silenced"))
    }
}
