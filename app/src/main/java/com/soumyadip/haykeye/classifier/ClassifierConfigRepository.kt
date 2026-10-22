package com.soumyadip.haykeye.classifier

import android.content.Context
import android.content.SharedPreferences

class ClassifierConfigRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun getConfig(): ClassifierConfig {
        val keywordsString = prefs.getString(KEY_CUSTOM_KEYWORDS, "") ?: ""
        val customKeywords = if (keywordsString.isBlank()) {
            emptySet()
        } else {
            keywordsString.split(",").asSequence().map { it.trim() }.filter { it.isNotBlank() }.toSet()
        }
        val enableHeuristics = prefs.getBoolean(KEY_ENABLE_HEURISTICS, true)
        val retentionDays = prefs.getInt(KEY_RETENTION_DAYS, 60)
        val isListeningEnabled = prefs.getBoolean(KEY_LISTENING_ENABLED, true)

        val vipSenders = loadMap(KEY_VIP_SENDERS)
        val vipGroups = loadMap(KEY_VIP_GROUPS)
        val groupNoisePolicies = loadGroupNoisePolicies()

        return ClassifierConfig(
            vipSenders = vipSenders,
            vipGroups = vipGroups,
            groupNoisePolicies = groupNoisePolicies,
            customKeywords = customKeywords,
            enableTextHeuristics = enableHeuristics,
            retentionDays = retentionDays,
            isListeningEnabled = isListeningEnabled,
        )
    }

    fun saveConfig(config: ClassifierConfig) {
        prefs.edit().apply {
            putString(KEY_CUSTOM_KEYWORDS, config.customKeywords.joinToString(","))
            putBoolean(KEY_ENABLE_HEURISTICS, config.enableTextHeuristics)
            putInt(KEY_RETENTION_DAYS, config.retentionDays)
            putBoolean(KEY_LISTENING_ENABLED, config.isListeningEnabled)
            saveMap(KEY_VIP_SENDERS, config.vipSenders)
            saveMap(KEY_VIP_GROUPS, config.vipGroups)
            saveGroupNoisePolicies(config.groupNoisePolicies)
            apply()
        }
    }

    private fun loadMap(prefKeyPrefix: String): Map<String, Set<String>> {
        val map = mutableMapOf<String, Set<String>>()
        val allEntries = prefs.all
        for ((key, value) in allEntries) {
            if ((key.startsWith("$prefKeyPrefix:")) && (value is String)) {
                val provider = key.removePrefix("$prefKeyPrefix:")
                if (value.isNotBlank()) {
                    map[provider] = value.split(",").asSequence().map { it.trim() }.filter { it.isNotBlank() }.toSet()
                }
            }
        }
        return map
    }

    private fun SharedPreferences.Editor.saveMap(prefKeyPrefix: String, map: Map<String, Set<String>>) {
        for ((provider, set) in map) {
            putString("$prefKeyPrefix:$provider", set.joinToString(","))
        }
    }

    private fun loadGroupNoisePolicies(): Map<String, GroupNoisePolicy> {
        val map = mutableMapOf<String, GroupNoisePolicy>()
        val allEntries = prefs.all
        for ((key, value) in allEntries) {
            if ((key.startsWith("$KEY_GROUP_POLICY:")) && (value is String)) {
                val groupKey = key.removePrefix("$KEY_GROUP_POLICY:")
                map[groupKey] = GroupNoisePolicy.fromName(value)
            }
        }
        return map
    }

    private fun SharedPreferences.Editor.saveGroupNoisePolicies(policies: Map<String, GroupNoisePolicy>) {
        for ((groupKey, policy) in policies) {
            putString("$KEY_GROUP_POLICY:$groupKey", policy.name)
        }
    }

    companion object {
        private const val PREFS_NAME = "haykeye_classifier_prefs"
        private const val KEY_CUSTOM_KEYWORDS = "custom_keywords"
        private const val KEY_ENABLE_HEURISTICS = "enable_heuristics"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_LISTENING_ENABLED = "listening_enabled"
        private const val KEY_VIP_SENDERS = "vip_senders"
        private const val KEY_VIP_GROUPS = "vip_groups"
        private const val KEY_GROUP_POLICY = "group_policy"
    }
}
