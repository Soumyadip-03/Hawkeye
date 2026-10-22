package com.soumyadip.haykeye.navigation

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.soumyadip.haykeye.data.UrgentMessage

enum class LaunchResult {
    EXACT_NOTIFICATION_ACTION,
    MEDIA_VIEWER_ACTION,
    MAPPED_CHAT_DEEP_LINK,
    CONTACT_OR_COMPOSE_DEEP_LINK,
    APP_FALLBACK,
    APP_NOT_INSTALLED,
    FAILED,
}

object MessageLauncher {

    private const val TAG = "HaykeyeMessageLauncher"

    const val PACKAGE_WHATSAPP = "com.whatsapp"
    const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    const val PACKAGE_GMAIL = "com.google.android.gm"
    const val PACKAGE_MESSAGES = "com.google.android.apps.messaging"

    fun getTargetPackageName(provider: String): String? {
        return when (provider) {
            "WhatsApp" -> PACKAGE_WHATSAPP
            "WhatsApp Business" -> PACKAGE_WHATSAPP_BUSINESS
            "Gmail" -> PACKAGE_GMAIL
            "Google Messages" -> PACKAGE_MESSAGES
            else -> null
        }
    }

    fun launchSourceApp(context: Context, message: UrgentMessage): LaunchResult {
        // Priority 1: Original Notification Action (Active Notification ContentIntent)
        val pendingIntent = NotificationActionRegistry.getPendingIntent(message)
        if (pendingIntent != null) {
            val isMediaAction = isMediaViewerIntent(pendingIntent)
            try {
                pendingIntent.send(context, 0, null)
                val result = if (isMediaAction) LaunchResult.MEDIA_VIEWER_ACTION else LaunchResult.EXACT_NOTIFICATION_ACTION
                Log.d(TAG, "[${message.provider}] Opened destination via active notification PendingIntent (Result: $result).")
                return result
            } catch (_: PendingIntent.CanceledException) {
                Log.w(TAG, "[${message.provider}] Active notification action was canceled/expired. Falling back.")
                NotificationActionRegistry.unregisterAction(message)
            } catch (e: Exception) {
                Log.w(TAG, "[${message.provider}] Failed to send PendingIntent: ${e.message}. Falling back.")
            }
        }

        // Priority 2: Provider-Specific Contact / Mapped Deep Link
        // 2a. Check if user mapped senderName to a verified phone number
        val contactRepo = ContactMappingRepository(context)
        val mappedPhoneNumber = contactRepo.getPhoneNumberForSender(message.senderName)
        if (!mappedPhoneNumber.isNullOrBlank()) {
            val mappedIntent = createPhoneChatIntent(context, message.provider, mappedPhoneNumber)
            if (mappedIntent != null) {
                try {
                    mappedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(mappedIntent)
                    Log.d(
                        TAG,
                        "[${message.provider}] Opened chat conversation via user-mapped phone number for '${message.senderName}'.",
                    )
                    return LaunchResult.MAPPED_CHAT_DEEP_LINK
                } catch (e: Exception) {
                    Log.w(TAG, "[${message.provider}] Failed to launch mapped phone intent: ${e.message}")
                }
            }
        }

        // 2b. Check if senderName itself is a verified phone number or email address
        val deepLinkIntent = tryProviderDeepLink(context, message)
        if (deepLinkIntent != null) {
            try {
                deepLinkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(deepLinkIntent)
                Log.d(TAG, "[${message.provider}] Opened contact chat / compose screen via deep link.")
                return LaunchResult.CONTACT_OR_COMPOSE_DEEP_LINK
            } catch (e: Exception) {
                Log.w(TAG, "[${message.provider}] Failed to launch deep link: ${e.message}. Falling back.")
            }
        }

        // Priority 3: Safe App-Level Fallback
        val packageName = getTargetPackageName(message.provider)
            ?: return LaunchResult.FAILED

        val pm = context.packageManager
        var launchIntent = pm.getLaunchIntentForPackage(packageName)

        if (launchIntent == null) {
            val fallbackIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
            if (isIntentResolvable(context, fallbackIntent)) {
                launchIntent = fallbackIntent
            }
        }

        if (launchIntent == null) {
            showToastOnMainThread(context, "${message.provider} is not installed on this device")
            return LaunchResult.APP_NOT_INSTALLED
        }

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(launchIntent)
            Log.d(TAG, "[${message.provider}] Opened provider app home screen as fallback.")
            LaunchResult.APP_FALLBACK
        } catch (_: ActivityNotFoundException) {
            showToastOnMainThread(context, "Unable to open ${message.provider}")
            LaunchResult.FAILED
        } catch (e: Exception) {
            showToastOnMainThread(context, "Error opening ${message.provider}: ${e.message}")
            LaunchResult.FAILED
        }
    }

    private fun isMediaViewerIntent(pendingIntent: PendingIntent): Boolean {
        return try {
            val intentString = pendingIntent.toString()
            intentString.contains("Media", ignoreCase = true) ||
                    intentString.contains("Gallery", ignoreCase = true) ||
                    intentString.contains("Photo", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    private fun createPhoneChatIntent(context: Context, provider: String, rawPhone: String): Intent? {
        val targetPackage = getTargetPackageName(provider) ?: return null
        val cleanNumber = rawPhone.replace("+", "").replace(" ", "").replace("-", "")

        return when (provider) {
            "WhatsApp", "WhatsApp Business" -> {
                val nativeWaUri = Uri.parse("whatsapp://send?phone=$cleanNumber")
                val nativeIntent = Intent(Intent.ACTION_VIEW, nativeWaUri).apply {
                    setPackage(targetPackage)
                }
                if (isIntentResolvable(context, nativeIntent)) nativeIntent else null
            }
            "Google Messages" -> {
                val smsUri = Uri.parse("smsto:$cleanNumber")
                val smsIntent = Intent(Intent.ACTION_VIEW, smsUri).apply {
                    setPackage(targetPackage)
                }
                if (isIntentResolvable(context, smsIntent)) smsIntent else null
            }
            else -> null
        }
    }

    private fun tryProviderDeepLink(context: Context, message: UrgentMessage): Intent? {
        val sender = message.senderName.trim()
        val targetPackage = getTargetPackageName(message.provider) ?: return null

        when (message.provider) {
            "Google Messages" -> {
                if (isPhoneNumber(sender)) {
                    val smsUri = Uri.parse("smsto:$sender")
                    val intent = Intent(Intent.ACTION_VIEW, smsUri).apply {
                        setPackage(targetPackage)
                    }
                    if (isIntentResolvable(context, intent)) return intent
                }
            }
            "WhatsApp", "WhatsApp Business" -> {
                if (isPhoneNumber(sender)) {
                    val cleanNumber = sender.replace("+", "").replace(" ", "").replace("-", "")

                    // Try 1: Native WhatsApp custom URI scheme
                    val nativeWaUri = Uri.parse("whatsapp://send?phone=$cleanNumber")
                    val nativeIntent = Intent(Intent.ACTION_VIEW, nativeWaUri).apply {
                        setPackage(targetPackage)
                    }
                    if (isIntentResolvable(context, nativeIntent)) return nativeIntent

                    // Try 2: Web API deep link format
                    val waUri = Uri.parse("https://wa.me/$cleanNumber")
                    val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                        setPackage(targetPackage)
                    }
                    if (isIntentResolvable(context, waIntent)) return waIntent
                }
            }
            "Gmail" -> {
                if (isEmailAddress(sender)) {
                    val mailUri = Uri.parse("mailto:$sender")
                    val intent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                        setPackage(targetPackage)
                    }
                    if (isIntentResolvable(context, intent)) return intent
                }
            }
        }
        return null
    }

    private fun isPhoneNumber(text: String): Boolean {
        val clean = text.replace(" ", "").replace("-", "")
        return clean.matches(Regex("""^\+?[0-9]{10,15}$"""))
    }

    private fun isEmailAddress(text: String): Boolean {
        return text.matches(Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"""))
    }

    private fun isIntentResolvable(context: Context, intent: Intent): Boolean {
        return context.packageManager.resolveActivity(intent, 0) != null
    }

    private fun showToastOnMainThread(context: Context, text: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                // Ignore toast errors on non-UI test environments
            }
        }
    }
}
