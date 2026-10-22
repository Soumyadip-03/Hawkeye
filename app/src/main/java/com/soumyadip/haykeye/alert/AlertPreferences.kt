package com.soumyadip.haykeye.alert

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri

class AlertPreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun getRingtoneUri(context: Context): Uri {
        val uriString = prefs.getString(KEY_RINGTONE_URI, null)
        if (!uriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriString)
                if (isUriValid(context, uri)) {
                    return uri
                }
            } catch (_: Exception) {
                // Fallback on invalid URI
            }
        }
        return getDefaultRingtoneUri()
    }

    fun saveRingtoneUri(uri: Uri?) {
        prefs.edit().apply {
            if (uri != null) {
                putString(KEY_RINGTONE_URI, uri.toString())
            } else {
                remove(KEY_RINGTONE_URI)
            }
            apply()
        }
    }

    private fun isUriValid(context: Context, uri: Uri): Boolean {
        return try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone != null
        } catch (_: Exception) {
            false
        }
    }

    fun getDefaultRingtoneUri(): Uri {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: Uri.EMPTY
    }

    companion object {
        private const val PREFS_NAME = "haykeye_alert_prefs"
        private const val KEY_RINGTONE_URI = "alert_ringtone_uri"
    }
}
