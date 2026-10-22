package com.soumyadip.haykeye.classifier

data class ClassificationResult(
    val isUrgent: Boolean,
    val matchedSignal: UrgencySignal? = null,
    val matchedReason: String? = null,
) {
    companion object {
        val NOT_URGENT = ClassificationResult(isUrgent = false)
    }
}
