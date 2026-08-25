package com.quietinbox.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.NotificationEntity
import com.quietinbox.data.db.entity.ScheduleEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QuietDatabaseTest {

    private lateinit var db: QuietDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, QuietDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndQueryNotification() = runTest {
        val notification = NotificationEntity(
            sbnKey = "0|com.whatsapp|1|null|10001",
            packageName = "com.whatsapp",
            appLabel = "WhatsApp",
            title = "Test User",
            body = "Hello from Quiet Inbox",
            subText = null,
            senderName = "Test User",
            senderDigits = null,
            channelId = "messages",
            androidCategory = "CATEGORY_MESSAGE",
            importance = 3,
            signalClass = "ALERT",
            contentHash = "test_hash",
            firstSeenAt = 1000L,
            lastSeenAt = 1000L
        )

        val id = db.notificationDao().insert(notification)
        assertTrue(id > 0)

        val found = db.notificationDao().findByKey("0|com.whatsapp|1|null|10001")
        assertNotNull(found)
        assertEquals("WhatsApp", found?.appLabel)
        assertEquals("Hello from Quiet Inbox", found?.body)
    }

    @Test
    fun ruleDaoInsertAndQuery() = runTest {
        val rule = AllowRuleEntity(
            type = "SENDER",
            value = "Mom",
            matchMode = "CONTAINS",
            enabled = true,
            createdAt = 1000L
        )

        val id = db.ruleDao().insertRule(rule)
        assertTrue(id > 0)

        val enabledRules = db.ruleDao().getEnabledRules()
        assertEquals(1, enabledRules.size)
        assertEquals("Mom", enabledRules[0].value)
    }

    @Test
    fun scheduleDaoInsertAndQuery() = runTest {
        val schedule = ScheduleEntity(
            name = "Sleep",
            startMinute = 22 * 60,
            endMinute = 7 * 60,
            daysMask = 127,
            policy = "QUIET",
            enabled = true,
            createdAt = 1000L
        )

        val id = db.scheduleDao().insertSchedule(schedule)
        assertTrue(id > 0)

        val enabled = db.scheduleDao().getEnabledSchedules()
        assertEquals(1, enabled.size)
        assertEquals("Sleep", enabled[0].name)
    }

    @Test
    fun statsDaoInsertAndQuery() = runTest {
        val stat = DailyStatEntity(
            day = 20260826,
            captured = 100,
            silenced = 80,
            allowed = 20,
            quietMinutes = 480,
            distinctApps = 12
        )

        db.statsDao().insertOrReplace(stat)
        val read = db.statsDao().getStats(20260826)
        assertNotNull(read)
        assertEquals(80, read?.silenced)
    }
}
