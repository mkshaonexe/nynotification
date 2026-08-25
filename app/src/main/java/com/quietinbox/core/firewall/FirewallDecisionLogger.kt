package com.quietinbox.core.firewall

import com.quietinbox.data.db.dao.RuleDao
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
 * Writes to the `firewall_decisions` database table via [RuleDao].
 */
@Singleton
class FirewallDecisionLogger @Inject constructor(
    private val ruleDao: RuleDao,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

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
        queue.offer(decision)
        if (queue.size >= BATCH_THRESHOLD) {
            scope.launch {
                flush()
            }
        }
    }

    /**
     * Flushes all currently buffered decisions to the database.
     */
    suspend fun flush() {
        flushMutex.withLock {
            if (queue.isEmpty()) return
            val batch = mutableListOf<FirewallDecisionEntity>()
            while (true) {
                val item = queue.poll() ?: break
                batch.add(item)
            }
            if (batch.isNotEmpty()) {
                runCatching {
                    ruleDao.insertDecisions(batch)
                }
            }
        }
    }
}
