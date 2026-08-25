package com.quietinbox.core.signal

import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass

sealed interface IngestDecision {
    data class Insert(val signalClass: SignalClass) : IngestDecision
    data class UpdateExisting(val rowId: Long, val bumpCount: Boolean) : IngestDecision
    data class Drop(val reason: String) : IngestDecision
}

interface SignalClassifier {
    fun classify(n: CapturedNotification): SignalClass
    fun contentHash(n: CapturedNotification): String
    /** Decides insert / update / drop given what is already stored for this key. */
    suspend fun decide(n: CapturedNotification): IngestDecision
}
