package com.quietinbox.service.ingest

import com.quietinbox.core.model.CapturedNotification

interface IngestPipeline {
    /** @return the sbnKey if the caller should cancel this notification, else null. */
    suspend fun onPosted(n: CapturedNotification): String?
    suspend fun onRemoved(sbnKey: String, reason: Int)
    suspend fun backfill(active: List<CapturedNotification>)
}
