package com.quietinbox.data.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.quietinbox.core.firewall.ActiveSchedule
import com.quietinbox.core.firewall.ScheduleEvaluator
import com.quietinbox.core.time.Clock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class SchedulePauseWorkerTest {

    private class FakeScheduleEvaluator : ScheduleEvaluator {
        var evaluatedTimes = mutableListOf<Long>()
        override suspend fun activeAt(nowEpochMs: Long): ActiveSchedule? {
            evaluatedTimes.add(nowEpochMs)
            return null
        }
    }

    private class FakeClock(val timeMs: Long = 5000L) : Clock {
        override fun now(): Long = timeMs
        override fun elapsedRealtime(): Long = timeMs
    }

    @Test
    fun doWork_queriesScheduleEvaluator_returnsSuccess() = runTest {
        val mockContext = mock(Context::class.java)
        val mockParams = mock(WorkerParameters::class.java)
        val fakeEvaluator = FakeScheduleEvaluator()
        val fakeClock = FakeClock(12345L)

        val worker = SchedulePauseWorker(
            appContext = mockContext,
            workerParams = mockParams,
            scheduleEvaluator = fakeEvaluator,
            clock = fakeClock
        )

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, fakeEvaluator.evaluatedTimes.size)
        assertEquals(12345L, fakeEvaluator.evaluatedTimes.first())
    }
}
