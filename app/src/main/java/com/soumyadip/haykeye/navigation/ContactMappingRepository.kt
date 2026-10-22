package com.soumyadip.haykeye.navigation

import android.content.Context
import android.content.SharedPreferences

class ContactMappingRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun getPhoneNumberForSender(senderName: String): String? {
        if (senderName.isBlank()) return null
        val cleanSender = senderName.trim().lowercase()
        return prefs.getString("$KEY_PREFIX_PHONE$cleanSender", null)
    }

    fun saveContactMapping(senderName: String, phoneNumber: String) {
        if (senderName.isNotBlank() && phoneNumber.isNotBlank()) {
            val cleanSender = senderName.trim().lowercase()
            val cleanPhone = phoneNumber.trim()
            prefs.edit().putString("$KEY_PREFIX_PHONE$cleanSender", cleanPhone).apply()
        }
    }

    fun removeContactMapping(senderName: String) {
        if (senderName.isNotBlank()) {
            val cleanSender = senderName.trim().lowercase()
            prefs.edit().remove("$KEY_PREFIX_PHONE$cleanSender").apply()
        }
    }

    fun clearAllMappings() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "haykeye_contact_mappings"
        private const val KEY_PREFIX_PHONE = "phone_map_"
    }
}
