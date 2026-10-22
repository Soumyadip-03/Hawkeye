package com.soumyadip.haykeye.classifier

data class ClassifierConfig(
    val vipSenders: Map<String, Set<String>> = emptyMap(),
    val vipGroups: Map<String, Set<String>> = emptyMap(),
    val customKeywords: Set<String> = emptySet(),
    val enableTextHeuristics: Boolean = true,
)
