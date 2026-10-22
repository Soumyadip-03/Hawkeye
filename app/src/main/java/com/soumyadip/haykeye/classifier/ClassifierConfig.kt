package com.soumyadip.haykeye.classifier

enum class GroupNoisePolicy(val displayName: String) {
    MONITOR_NORMALLY("Monitor Normally"),
    ALERT_ON_MENTION_ONLY("Alert on @Mention Only"),
    IGNORE_ENTIRELY("Ignore Group Entirely");

    companion object {
        fun fromName(name: String?): GroupNoisePolicy {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: MONITOR_NORMALLY
        }
    }
}

data class ClassifierConfig(
    val vipSenders: Map<String, Set<String>> = emptyMap(),
    val vipGroups: Map<String, Set<String>> = emptyMap(),
    val groupNoisePolicies: Map<String, GroupNoisePolicy> = emptyMap(),
    val customKeywords: Set<String> = emptySet(),
    val enableTextHeuristics: Boolean = true,
    val retentionDays: Int = 60,
    val isListeningEnabled: Boolean = true,
)
