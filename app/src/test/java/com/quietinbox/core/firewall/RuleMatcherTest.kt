package com.quietinbox.core.firewall

import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.MatchMode
import com.quietinbox.data.db.entity.RuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleMatcherTest {

    private lateinit var matcher: RuleMatcher

    @Before
    fun setup() {
        matcher = RuleMatcher()
    }

    private fun createNotification(
        packageName: String = "com.test.app",
        title: String? = null,
        body: String? = null,
        subText: String? = null,
        senderName: String? = null,
        senderDigits: String? = null
    ): CapturedNotification {
        return CapturedNotification(
            sbnKey = "0|com.test.app|1|tag|1000",
            packageName = packageName,
            appLabel = "Test App",
            title = title,
            body = body,
            subText = subText,
            senderName = senderName,
            senderDigits = senderDigits,
            channelId = "channel_1",
            androidCategory = null,
            importance = 3,
            postedAt = 1000L,
            isOngoing = false,
            isForegroundService = false,
            isGroupSummary = false,
            isClearable = true,
            hasProgress = false,
            groupKey = null
        )
    }

    // --- APP RULES ---

    @Test
    fun `app rule matches exact package name`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.APP, value = "com.whatsapp", enabled = true)
        val notif = createNotification(packageName = "com.whatsapp")
        assertTrue(matcher.matches(notif, rule))
    }

    @Test
    fun `app rule matches case-insensitively`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.APP, value = "com.WhatsApp", enabled = true)
        val notif = createNotification(packageName = "com.whatsapp")
        assertTrue(matcher.matches(notif, rule))
    }

    @Test
    fun `app rule does not match different package`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.APP, value = "com.whatsapp", enabled = true)
        val notif = createNotification(packageName = "com.telegram")
        assertFalse(matcher.matches(notif, rule))
    }

    // --- SENDER RULES (Names & Diacritics) ---

    @Test
    fun `sender rule matches exact sender name`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.SENDER, value = "Alice", enabled = true)
        val notif = createNotification(senderName = "Alice")
        assertTrue(matcher.matches(notif, rule))
    }

    @Test
    fun `sender rule matches substring in sender name`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.SENDER, value = "Mom", enabled = true)
        val notif = createNotification(senderName = "Mom ❤️")
        assertTrue(matcher.matches(notif, rule))
    }

    @Test
    fun `sender rule matches diacritic-insensitively`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.SENDER, value = "Rene", enabled = true)
        val notif = createNotification(senderName = "René")
        assertTrue(matcher.matches(notif, rule))

        val rule2 = AllowRuleEntity(id = 2, type = RuleType.SENDER, value = "Jérôme", enabled = true)
        val notif2 = createNotification(senderName = "Jerome Dupont")
        assertTrue(matcher.matches(notif2, rule2))
    }

    @Test
    fun `sender rule matches Bangla sender name`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.SENDER, value = "রাহিম", enabled = true)
        val notif = createNotification(senderName = "রাহিম আহমেদ")
        assertTrue(matcher.matches(notif, rule))

        val ruleMom = AllowRuleEntity(id = 2, type = RuleType.SENDER, value = "মা", enabled = true)
        val notifMom = createNotification(senderName = "মা ❤️")
        assertTrue(matcher.matches(notifMom, ruleMom))
    }

    @Test
    fun `sender rule matches title when senderName is null`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.SENDER, value = "Bank Support", enabled = true)
        val notif = createNotification(title = "Bank Support Alert", senderName = null)
        assertTrue(matcher.matches(notif, rule))
    }

    // --- SENDER RULES (Phone numbers & Digit Tail Matching) ---

    @Test
    fun `sender rule matches BD formatted phone number with 9-digit tail`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.SENDER, value = "+880 1709-093872", enabled = true)
        val notif = createNotification(senderDigits = "01709093872")
        assertTrue(matcher.matches(notif, rule))

        val notif2 = createNotification(senderDigits = "1709093872")
        assertTrue(matcher.matches(notif2, rule))

        val notif3 = createNotification(senderDigits = "8801709093872")
        assertTrue(matcher.matches(notif3, rule))
    }

    @Test
    fun `sender rule matches US phone number with different formatting`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.SENDER, value = "+1 (555) 123-4567", enabled = true)
        val notif = createNotification(senderDigits = "5551234567")
        assertTrue(matcher.matches(notif, rule))
    }

    @Test
    fun `sender rule matches partial digit query of 4 or more digits`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.SENDER, value = "093872", enabled = true)
        val notif = createNotification(senderDigits = "01709093872")
        assertTrue(matcher.matches(notif, rule))
    }

    @Test
    fun `sender rule does not match different digits`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.SENDER, value = "+880 1709-093872", enabled = true)
        val notif = createNotification(senderDigits = "01912345678")
        assertFalse(matcher.matches(notif, rule))
    }

    // --- WORD RULES (Whole-word & Contains) ---

    @Test
    fun `word rule matches whole word in body`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.WORD, value = "urgent", enabled = true)
        val notif = createNotification(body = "This is urgent! Please respond.")
        assertTrue(matcher.matches(notif, rule))
    }

    @Test
    fun `word rule does not match partial word in whole-word mode`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.WORD, value = "urgent", matchMode = MatchMode.EXACT, enabled = true)
        val notif = createNotification(body = "Please buy some detergent on your way.")
        assertFalse(matcher.matches(notif, rule))
    }

    @Test
    fun `word rule matches partial word when matchMode is CONTAINS`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.WORD, value = "urgent", matchMode = MatchMode.CONTAINS, enabled = true)
        val notif = createNotification(body = "Please buy some detergent on your way.")
        assertTrue(matcher.matches(notif, rule))
    }

    @Test
    fun `word rule matches Bangla whole word in title or body`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.WORD, value = "জরুরি", enabled = true)
        val notif = createNotification(title = "অফিস", body = "খুব জরুরি একটি মিটিং আছে।")
        assertTrue(matcher.matches(notif, rule))

        val ruleTaka = AllowRuleEntity(id = 2, type = RuleType.WORD, value = "টাকা", enabled = true)
        val notifTaka = createNotification(body = "বিকাশ অ্যাকাউন্টে টাকা জমা হয়েছে।")
        assertTrue(matcher.matches(notifTaka, ruleTaka))
    }

    @Test
    fun `word rule matches diacritic-insensitively`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.WORD, value = "café", enabled = true)
        val notif = createNotification(body = "Let's meet at the cafe today.")
        assertTrue(matcher.matches(notif, rule))
    }

    @Test
    fun `word rule matches in subText or senderName`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.WORD, value = "VIP", enabled = true)
        val notifSub = createNotification(subText = "VIP Client")
        assertTrue(matcher.matches(notifSub, rule))

        val notifSender = createNotification(senderName = "VIP Customer")
        assertTrue(matcher.matches(notifSender, rule))
    }

    // --- RULE LIST & DISABLED RULES ---

    @Test
    fun `disabled rule is ignored`() {
        val rule = AllowRuleEntity(id = 1, type = RuleType.APP, value = "com.whatsapp", enabled = false)
        val notif = createNotification(packageName = "com.whatsapp")
        assertFalse(matcher.matches(notif, rule))
    }

    @Test
    fun `findMatchingRule returns first matching enabled rule`() {
        val rule1 = AllowRuleEntity(id = 1, type = RuleType.APP, value = "com.telegram", enabled = true)
        val rule2 = AllowRuleEntity(id = 2, type = RuleType.WORD, value = "urgent", enabled = true)
        val rule3 = AllowRuleEntity(id = 3, type = RuleType.SENDER, value = "Mom", enabled = true)

        val notif = createNotification(
            packageName = "com.whatsapp",
            body = "This is urgent",
            senderName = "Mom"
        )

        val matched = matcher.findMatchingRule(notif, listOf(rule1, rule2, rule3))
        assertNotNull(matched)
        assertEquals(2L, matched?.id)
    }

    @Test
    fun `findMatchingRule returns null when no rules match`() {
        val rule1 = AllowRuleEntity(id = 1, type = RuleType.APP, value = "com.telegram", enabled = true)
        val notif = createNotification(packageName = "com.whatsapp", body = "Hello")

        val matched = matcher.findMatchingRule(notif, listOf(rule1))
        assertNull(matched)
    }
}
