package com.quietinbox.core.firewall

import com.quietinbox.core.model.CapturedNotification
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OtpDetectionTest {

    private lateinit var detector: OtpDetector

    @Before
    fun setup() {
        detector = OtpDetector()
    }

    private fun createNotification(
        title: String? = null,
        body: String? = null,
        subText: String? = null,
        channelId: String? = null,
        appLabel: String = "Test App"
    ): CapturedNotification {
        return CapturedNotification(
            sbnKey = "0|com.test.app|1|tag|1000",
            packageName = "com.test.app",
            appLabel = appLabel,
            title = title,
            body = body,
            subText = subText,
            senderName = null,
            senderDigits = null,
            channelId = channelId,
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

    // --- POSITIVE CASES (Standard body with code token + OTP keyword) ---

    @Test
    fun `detects English verification code in body`() {
        val notif = createNotification(
            title = "Google",
            body = "Your Google verification code is 482910."
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects English OTP in body`() {
        val notif = createNotification(
            title = "Bank Login",
            body = "Use OTP 583921 to authenticate your transaction."
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects English security PIN in body`() {
        val notif = createNotification(
            title = "Account Alert",
            body = "Your security PIN is 9842. Do not share it."
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects English 2FA auth code in body`() {
        val notif = createNotification(
            title = "GitHub",
            body = "Your 2FA auth code is 394812"
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects English one-time passcode in body`() {
        val notif = createNotification(
            title = "Service",
            body = "Your one-time passcode is 773829. Valid for 5 minutes."
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects Bangla verification code in body`() {
        val notif = createNotification(
            title = "bKash",
            body = "আপনার বিকাশ ভেরিফিকেশন কোড হলো 392019"
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects Bangla OTP in body`() {
        val notif = createNotification(
            title = "Nagad",
            body = "আপনার ওটিপি 8492, কাউকে বলবেন না।"
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects Bangla PIN in body`() {
        val notif = createNotification(
            title = "Bank",
            body = "আপনার সিকিউরিটি পিন 9382"
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects OTP when keyword is in title and token is in body`() {
        val notif = createNotification(
            title = "Bank Security Code",
            body = "4920 is for your login request"
        )
        assertTrue(detector.isOtp(notif))
    }

    // --- ANDROID 15+ REDACTED BODY CASES ---

    @Test
    fun `detects redacted body when title indicates verification code`() {
        val notif = createNotification(
            title = "Verification Code",
            body = "[Hidden content]"
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects redacted body when title is OTP and body is null`() {
        val notif = createNotification(
            title = "Your OTP",
            body = null
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects redacted body when channel indicates OTP`() {
        val notif = createNotification(
            title = "Security Alert",
            body = null,
            channelId = "otp_notifications"
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects redacted body when subText indicates security code`() {
        val notif = createNotification(
            title = "Banking App",
            subText = "Security Code",
            body = null
        )
        assertTrue(detector.isOtp(notif))
    }

    @Test
    fun `detects redacted body for Bangla title`() {
        val notif = createNotification(
            title = "বিকাশ ওটিপি",
            body = null
        )
        assertTrue(detector.isOtp(notif))
    }

    // --- NEGATIVE CASES ---

    @Test
    fun `does not detect notification with numbers but no OTP keyword`() {
        val notif = createNotification(
            title = "Amazon",
            body = "Your package with order #123456 has shipped."
        )
        assertFalse(detector.isOtp(notif))
    }

    @Test
    fun `does not detect notification with OTP keyword but no digit token`() {
        val notif = createNotification(
            title = "Welcome",
            body = "Please verify your email address by clicking this link."
        )
        assertFalse(detector.isOtp(notif))
    }

    @Test
    fun `does not detect 11-digit phone number without 4-8 digit standalone token`() {
        val notif = createNotification(
            title = "Support",
            body = "Call 01709093872 for customer inquiries."
        )
        assertFalse(detector.isOtp(notif))
    }

    @Test
    fun `does not detect 2-digit number`() {
        val notif = createNotification(
            title = "Chat",
            body = "You have 42 new messages."
        )
        assertFalse(detector.isOtp(notif))
    }

    @Test
    fun `does not detect empty or blank notification`() {
        val notif = createNotification(title = "", body = "")
        assertFalse(detector.isOtp(notif))
    }
}
