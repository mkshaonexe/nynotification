package com.quietinbox.service.ingest

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationParserTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createSbn(
        key: String = "0|com.example.chat|100|tag|1000",
        packageName: String = "com.example.chat",
        notification: Notification,
        postTime: Long = 1000L,
        isOngoing: Boolean = false,
        isClearable: Boolean = true,
        groupKey: String? = "chat_group"
    ): StatusBarNotification {
        return StatusBarNotification(
            packageName,
            "com.example.chat",
            100,
            "tag",
            1000,
            0,
            0,
            notification,
            android.os.Process.myUserHandle(),
            postTime
        )
    }

    @Test
    fun parse_basicNotification_extractsAllStandardFields() {
        val notification = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle("Alice")
            .setContentText("Hey, are you free?")
            .setSubText("Work")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .build()

        val sbn = createSbn(notification = notification)
        val captured = NotificationParser.parse(sbn, appLabel = "Chat App")

        assertEquals("0|com.example.chat|100|tag|1000", captured.sbnKey)
        assertEquals("com.example.chat", captured.packageName)
        assertEquals("Chat App", captured.appLabel)
        assertEquals("Alice", captured.title)
        assertEquals("Hey, are you free?", captured.body)
        assertEquals("Work", captured.subText)
        assertEquals("Alice", captured.senderName)
        assertEquals("test_channel", captured.channelId)
        assertEquals(Notification.CATEGORY_MESSAGE, captured.androidCategory)
        assertEquals(1000L, captured.postedAt)
        assertFalse(captured.isOngoing)
        assertFalse(captured.isForegroundService)
        assertFalse(captured.isGroupSummary)
        assertFalse(captured.hasProgress)
    }

    @Test
    fun parse_bigTextStyle_extractsBigTextOverText() {
        val notification = NotificationCompat.Builder(context, "news_channel")
            .setContentTitle("Breaking News")
            .setContentText("Short preview")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Long extended breaking news full body article text"))
            .build()

        val sbn = createSbn(notification = notification)
        val captured = NotificationParser.parse(sbn)

        assertEquals("Breaking News", captured.title)
        assertEquals("Long extended breaking news full body article text", captured.body)
    }

    @Test
    fun parse_inboxStyle_joinsLinesWithNewlines() {
        val notification = NotificationCompat.Builder(context, "email_channel")
            .setContentTitle("3 New Emails")
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine("Email 1 from Bob")
                    .addLine("Email 2 from Charlie")
                    .addLine("Email 3 from Dave")
            )
            .build()

        val sbn = createSbn(notification = notification)
        val captured = NotificationParser.parse(sbn)

        assertEquals("3 New Emails", captured.title)
        assertEquals("Email 1 from Bob\nEmail 2 from Charlie\nEmail 3 from Dave", captured.body)
    }

    @Test
    fun parse_phoneSender_extractsNormalizedDigits() {
        val notification = NotificationCompat.Builder(context, "sms_channel")
            .setContentTitle("+880 1709-093872")
            .setContentText("Your OTP code is 492810")
            .build()

        val sbn = createSbn(notification = notification)
        val captured = NotificationParser.parse(sbn)

        assertEquals("+880 1709-093872", captured.title)
        assertEquals("Your OTP code is 492810", captured.body)
        assertEquals("+880 1709-093872", captured.senderName)
        assertEquals("709093872", captured.senderDigits)
    }

    @Test
    fun parse_progressNotification_flagsHasProgressTrue() {
        val notification = NotificationCompat.Builder(context, "download_channel")
            .setContentTitle("Downloading file")
            .setProgress(100, 45, false)
            .build()

        val sbn = createSbn(notification = notification)
        val captured = NotificationParser.parse(sbn)

        assertTrue(captured.hasProgress)
    }

    @Test
    fun parse_ongoingAndForegroundServiceFlags_extractedAccurately() {
        val notification = NotificationCompat.Builder(context, "service_channel")
            .setContentTitle("Music Playing")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .build()
        notification.flags = notification.flags or Notification.FLAG_FOREGROUND_SERVICE or Notification.FLAG_ONGOING_EVENT

        val sbn = createSbn(notification = notification, isOngoing = true)
        val captured = NotificationParser.parse(sbn)

        assertTrue(captured.isOngoing)
        assertTrue(captured.isForegroundService)
    }

    @Test
    fun parse_groupSummaryNotification_flagsGroupSummaryTrue() {
        val notification = NotificationCompat.Builder(context, "chat_channel")
            .setContentTitle("Group Chat")
            .setGroup("family_chat")
            .setGroupSummary(true)
            .build()

        val sbn = createSbn(notification = notification)
        val captured = NotificationParser.parse(sbn)

        assertTrue(captured.isGroupSummary)
    }
}
