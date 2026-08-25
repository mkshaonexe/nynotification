package com.quietinbox.core.firewall

import com.quietinbox.data.db.dao.StatsDao
import com.quietinbox.data.db.entity.FirewallDecisionEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Batched logger for firewall verdicts to prevent per-row disk I/O during notification storms.
 *
 * Writes to the `firewall_decisions` database table via [StatsDao].
 */
@Singleton
class FirewallDecisionLogger(
    private val statsDao: StatsDao,
    ioDispatcher: CoroutineDispatcher
) {
    @Inject
    constructor(statsDao: StatsDao) : this(statsDao, Dispatchers.IO)

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val queue = ConcurrentLinkedQueue<FirewallDecisionEntity>()
    private val flushMutex = Mutex()

    companion object {
        private const val BATCH_THRESHOLD = 20
    }

    /**
     * Enqueues a firewall decision for batched logging.
     *
     * @param decision The [FirewallDecisionEntity] to record.
     */
    fun log(decision: FirewallDecisionEntity) {
        queue.add(decision)
        if (queue.size >= BATCH_THRESHOLD) {
            flush()
        }
    }

    /**
     * Flushes all queued decisions to the database.
     */
    fun flush() {
        if (queue.isEmpty()) return

        scope.launch {
            flushMutex.withLock {
                val batch = mutableListOf<FirewallDecisionEntity>()
                while (true) {
                    val item = queue.poll() ?: break
                    batch.add(item)
                }

                if (batch.isNotEmpty()) {
                    try {
                        statsDao.insertFirewallDecisions(batch)
                    } catch (_: Exception) {
                        // Resilient: failure in logging shouldn't crash pipeline
                    }
                }
            }
        }
    }
}
