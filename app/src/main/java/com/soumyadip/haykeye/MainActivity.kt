package com.soumyadip.haykeye

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.soumyadip.haykeye.alert.AlertPreferences
import com.soumyadip.haykeye.ui.NotebookScreen
import com.soumyadip.haykeye.ui.NotebookViewModel
import com.soumyadip.haykeye.ui.SettingsScreen
import com.soumyadip.haykeye.ui.theme.HaykeyeTheme
import com.soumyadip.haykeye.ui.theme.ThemePreferences

enum class Screen {
    NOTEBOOK,
    SETTINGS,
}

class MainActivity : ComponentActivity() {

    private val notebookViewModel: NotebookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themePrefs = remember { ThemePreferences(context) }
            var currentTheme by remember { mutableStateOf(themePrefs.getTheme()) }

            HaykeyeTheme(appTheme = currentTheme) {
                val lifecycleOwner = LocalLifecycleOwner.current
                var currentScreen by remember { mutableStateOf(Screen.NOTEBOOK) }

                var isAccessGranted by remember {
                    mutableStateOf(isNotificationListenerEnabled(context))
                }

                var hasPostNotificationPermission by remember {
                    mutableStateOf(checkPostNotificationPermission(context))
                }

                val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { isGranted ->
                    hasPostNotificationPermission = isGranted
                }

                val ringtonePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                        }
                        val alertPrefs = AlertPreferences(context)
                        alertPrefs.saveRingtoneUri(uri)
                    }
                }

                val launchRingtonePicker = {
                    val alertPrefs = AlertPreferences(context)
                    val currentUri = alertPrefs.getRingtoneUri(context)
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alert Ringtone")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, alertPrefs.getDefaultRingtoneUri())
                    }
                    ringtonePickerLauncher.launch(intent)
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isAccessGranted = isNotificationListenerEnabled(context)
                            hasPostNotificationPermission = checkPostNotificationPermission(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        Screen.NOTEBOOK -> {
                            NotebookScreen(
                                isAccessGranted = isAccessGranted,
                                hasPostNotificationPermission = hasPostNotificationPermission,
                                onOpenNotificationAccessSettings = {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    startActivity(intent)
                                },
                                onRequestPostNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onOpenSettingsScreen = { currentScreen = Screen.SETTINGS },
                                viewModel = notebookViewModel,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            )
                        }
                        Screen.SETTINGS -> {
                            BackHandler {
                                currentScreen = Screen.NOTEBOOK
                            }
                            SettingsScreen(
                                currentTheme = currentTheme,
                                onThemeChanged = { newTheme ->
                                    currentTheme = newTheme
                                    themePrefs.saveTheme(newTheme)
                                },
                                onNavigateBack = { currentScreen = Screen.NOTEBOOK },
                                onSelectRingtone = launchRingtonePicker,
                                onClearAllMessages = { notebookViewModel.deleteAllMessages() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
    return enabledPackages.contains(context.packageName)
}

private fun checkPostNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
