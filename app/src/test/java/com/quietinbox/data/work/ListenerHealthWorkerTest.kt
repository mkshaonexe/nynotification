package com.quietinbox.data.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.quietinbox.core.health.HealthSnapshot
import com.quietinbox.core.health.HealthState
import com.quietinbox.core.health.ListenerHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeListenerHealth(initialState: HealthState = HealthState.CONNECTED) : ListenerHealth {
    private val _snapshot = MutableStateFlow(
        HealthSnapshot(
            state = initialState,
            lastCaptureAt = null,
            totalCaptured = 0L,
            ingestErrors = 0L
        )
    )
    override val snapshot: StateFlow<HealthSnapshot> = _snapshot.asStateFlow()

    var refreshCalled = false
    var forceRebindCalled = false

    fun setState(state: HealthState) {
        _snapshot.value = _snapshot.value.copy(state = state)
    }

    override fun refresh() {
        refreshCalled = true
    }

    override fun forceRebind() {
        forceRebindCalled = true
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ListenerHealthWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `worker triggers forceRebind when listener is disconnected or has no access`() = runBlocking {
        val fakeHealth = FakeListenerHealth(HealthState.NO_ACCESS)
        val worker = TestListenableWorkerBuilder<ListenerHealthWorker>(context).build()
        val customWorker = ListenerHealthWorker(
            appContext = context,
            workerParams = worker.workerParameters,
            listenerHealth = fakeHealth
        )

        val result = customWorker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(fakeHealth.refreshCalled)
        assertTrue(fakeHealth.forceRebindCalled)
    }

    @Test
    fun `worker does not trigger forceRebind when listener is connected`() = runBlocking {
        val fakeHealth = FakeListenerHealth(HealthState.CONNECTED)
        val worker = TestListenableWorkerBuilder<ListenerHealthWorker>(context).build()
        val customWorker = ListenerHealthWorker(
            appContext = context,
            workerParams = worker.workerParameters,
            listenerHealth = fakeHealth
        )

        val result = customWorker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(fakeHealth.refreshCalled)
        assertFalse(fakeHealth.forceRebindCalled)
    }
}
