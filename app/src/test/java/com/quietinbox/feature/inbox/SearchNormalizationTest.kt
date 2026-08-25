package com.quietinbox.feature.inbox

import com.quietinbox.feature.inbox.util.InboxSearchHelper
import com.quietinbox.feature.inbox.util.InboxTimeHelper
import com.quietinbox.feature.inbox.util.OtpHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Tests for phone number normalization, digit search queries, and OTP extraction.
 */
class SearchNormalizationTest {

    @Test
    fun testNormalizeToDigits() {
        assertEquals("8801709093872", InboxSearchHelper.normalizeToDigits("+880 1709-093872"))
        assertEquals("01709093872", InboxSearchHelper.normalizeToDigits("01709093872"))
        assertEquals("1709093872", InboxSearchHelper.normalizeToDigits("1709093872"))
        assertEquals("1234567890", InboxSearchHelper.normalizeToDigits("(123) 456-7890"))
        assertNull(InboxSearchHelper.normalizeToDigits(""))
        assertNull(InboxSearchHelper.normalizeToDigits("   "))
        assertNull(InboxSearchHelper.normalizeToDigits("no digits here"))
    }

    @Test
    fun testExtractDigitsQuery() {
        // Less than 4 digits returns null
        assertNull(InboxSearchHelper.extractDigitsQuery("12"))
        assertNull(InboxSearchHelper.extractDigitsQuery("123"))
        assertNull(InboxSearchHelper.extractDigitsQuery("abc 12"))

        // 4 or more digits returns stripped digits
        assertEquals("1234", InboxSearchHelper.extractDigitsQuery("1234"))
        assertEquals("1709093872", InboxSearchHelper.extractDigitsQuery("1709093872"))
        assertEquals("8801709093872", InboxSearchHelper.extractDigitsQuery("+880 1709-093872"))
    }

    @Test
    fun testPhoneFoundByDigits() {
        val targetPhone = "+880 1709-093872"

        // Required by spec: found by 1709093872
        assertTrue(InboxSearchHelper.matchesDigits(targetPhone, "1709093872"))

        // Required by spec: found by 093872
        assertTrue(InboxSearchHelper.matchesDigits(targetPhone, "093872"))

        // Found by full formatted input
        assertTrue(InboxSearchHelper.matchesDigits(targetPhone, "+880 1709-093872"))

        // Found by full digits
        assertTrue(InboxSearchHelper.matchesDigits(targetPhone, "8801709093872"))

        // Unrelated number does not match
        assertFalse(InboxSearchHelper.matchesDigits(targetPhone, "99988877"))
    }

    @Test
    fun testOtpCodeDetection() {
        // English variants
        assertEquals("492102", OtpHelper.extractOtpCode("Bank Auth", "Your OTP is 492102. Valid for 5 minutes."))
        assertEquals("8392", OtpHelper.extractOtpCode("Verification", "Use verification code 8392 to verify your login."))
        assertEquals("501928", OtpHelper.extractOtpCode("Security Alert", "Your Google verification code is 501928"))
        assertEquals("1234", OtpHelper.extractOtpCode("PIN Reset", "Your temp pin is 1234."))

        // Bengali variants
        assertEquals("593021", OtpHelper.extractOtpCode("নিরাপত্তা", "আপনার ওটিপি কোড হলো 593021"))
        assertEquals("9482", OtpHelper.extractOtpCode("ভেরিফিকেশন", "আপনার পিন কোড 9482"))

        // Non-OTP numbers should return null
        assertNull(OtpHelper.extractOtpCode("Delivery", "Order 123456 has been shipped!"))
        assertNull(OtpHelper.extractOtpCode("Meeting", "Let's meet at 1000 AM at room 405"))
        assertNull(OtpHelper.extractOtpCode(null, null))
    }

    @Test
    fun testInboxTimeFormatting() {
        val zoneId = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 8, 26)
        val todayEpochDay = today.toEpochDay()

        val todayMs = today.atStartOfDay(zoneId).toInstant().toEpochMilli() + 3600000 // 1 AM
        val yesterdayMs = today.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() + 3600000
        val olderMs = today.minusDays(5).atStartOfDay(zoneId).toInstant().toEpochMilli() + 3600000

        assertEquals("Today", InboxTimeHelper.formatDayHeader(todayMs, todayEpochDay, zoneId, Locale.ENGLISH))
        assertEquals("Yesterday", InboxTimeHelper.formatDayHeader(yesterdayMs, todayEpochDay, zoneId, Locale.ENGLISH))
        assertEquals("21 Aug", InboxTimeHelper.formatDayHeader(olderMs, todayEpochDay, zoneId, Locale.ENGLISH))

        // Test session formatting
        val sessionStart = today.atTime(22, 17).atZone(zoneId).toInstant().toEpochMilli()
        val sessionEnd = today.atTime(22, 27).atZone(zoneId).toInstant().toEpochMilli()
        val sessionDesc = InboxTimeHelper.formatSessionDescription(
            firstSeenAt = sessionStart,
            lastSeenAt = sessionEnd,
            endedAt = sessionEnd,
            updateCount = 612,
            wasRateLimited = false,
            zoneId = zoneId
        )
        assertTrue(sessionDesc.contains("10 min"))
        assertTrue(sessionDesc.contains("612 updates"))
    }
}
