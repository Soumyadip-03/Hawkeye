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
            keywordsString.split(",").asSequence().map { it.trim() }.toSet()
        }
        val enableHeuristics = prefs.getBoolean(KEY_ENABLE_HEURISTICS, true)

        val vipSenders = loadMap(KEY_VIP_SENDERS)
        val vipGroups = loadMap(KEY_VIP_GROUPS)

        return ClassifierConfig(
            vipSenders = vipSenders,
            vipGroups = vipGroups,
            customKeywords = customKeywords,
            enableTextHeuristics = enableHeuristics,
        )
    }

    fun saveConfig(config: ClassifierConfig) {
        prefs.edit().apply {
            putString(KEY_CUSTOM_KEYWORDS, config.customKeywords.joinToString(","))
            putBoolean(KEY_ENABLE_HEURISTICS, config.enableTextHeuristics)
            saveMap(KEY_VIP_SENDERS, config.vipSenders)
            saveMap(KEY_VIP_GROUPS, config.vipGroups)
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
                    map[provider] = value.split(",").asSequence().map { it.trim() }.toSet()
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

    companion object {
        private const val PREFS_NAME = "haykeye_classifier_prefs"
        private const val KEY_CUSTOM_KEYWORDS = "custom_keywords"
        private const val KEY_ENABLE_HEURISTICS = "enable_heuristics"
        private const val KEY_VIP_SENDERS = "vip_senders"
        private const val KEY_VIP_GROUPS = "vip_groups"
    }
}
