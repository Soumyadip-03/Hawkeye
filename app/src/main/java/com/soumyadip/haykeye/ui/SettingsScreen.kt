package com.soumyadip.haykeye.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soumyadip.haykeye.classifier.ClassifierConfigRepository
import com.soumyadip.haykeye.listener.NotificationListener
import com.soumyadip.haykeye.navigation.ContactMappingRepository
import com.soumyadip.haykeye.receiver.BatteryOptimizationHelper
import com.soumyadip.haykeye.retention.DatabaseSizeCalculator
import com.soumyadip.haykeye.retention.RetentionManager
import com.soumyadip.haykeye.ui.theme.AppTheme
import com.soumyadip.haykeye.ui.theme.StatusGreen
import com.soumyadip.haykeye.ui.theme.StatusWarning
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeChanged: (AppTheme) -> Unit,
    onNavigateBack: () -> Unit,
    onSelectRingtone: () -> Unit,
    onClearAllMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configRepo = remember { ClassifierConfigRepository(context) }
    val contactRepo = remember { ContactMappingRepository(context) }

    var config by remember { mutableStateOf(configRepo.getConfig()) }
    var dbSizeText by remember { mutableStateOf("Calculating...") }
    var showClearAllDialog by remember { mutableStateOf(false) }

    var customKeywordsText by remember { mutableStateOf(config.customKeywords.joinToString(", ")) }
    var heuristicsEnabled by remember { mutableStateOf(config.enableTextHeuristics) }
    var selectedRetentionDays by remember { mutableIntStateOf(config.retentionDays) }

    var newSenderName by remember { mutableStateOf("") }
    var newPhoneNumber by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        dbSizeText = DatabaseSizeCalculator.calculateFormattedSize(context)
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Delete All Messages?") },
            text = { Text("This will permanently erase all saved urgent messages from your encrypted database. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllMessages()
                        showClearAllDialog = false
                        coroutineScope.launch {
                            dbSizeText = DatabaseSizeCalculator.calculateFormattedSize(context)
                        }
                    },
                ) {
                    Text("Delete All", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onNavigateBack, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text(text = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "App Appearance & Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toggle visual theme:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ThemeSegmentedCapsule(
                            currentTheme = currentTheme,
                            onThemeSelected = onThemeChanged,
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "System Health & OEM Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val isListenerActive = NotificationListener.isListenerConnected
                        val isBatteryIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)

                        Text(
                            text = if (isListenerActive) "Notification Listener: Connected" else "Notification Listener: Disconnected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isListenerActive) StatusGreen else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )

                        Text(
                            text = if (isBatteryIgnored) "Battery Optimization: Unrestricted" else "Battery Optimization: Optimized (May delay alerts)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isBatteryIgnored) StatusGreen else StatusWarning,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "realme UI / ColorOS Reliability Instructions:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "1. Enable 'Auto-launch' for Haykeye in Settings ➔ Apps ➔ Auto-launch.\n" +
                                    "2. Set Battery Usage to 'Allow background activity'.\n" +
                                    "3. Lock Haykeye in Recent Apps overview screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val intent = BatteryOptimizationHelper.openBatterySettingsIntent(context)
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Opening battery settings failed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = CircleShape,
                            ) {
                                Text("Battery Settings", style = MaterialTheme.typography.labelSmall)
                            }

                            if (!isListenerActive) {
                                OutlinedButton(
                                    onClick = {
                                        NotificationListener.requestReconnect(context)
                                    },
                                    shape = CircleShape,
                                ) {
                                    Text("Reconnect Listener", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Database Security & Storage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Status: Encrypted at Rest (SQLCipher 256-bit AES)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Storage Used: $dbSizeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showClearAllDialog = true },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        ) {
                            Text(
                                text = "Clear All Messages",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Alert Ringtone",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        OutlinedButton(
                            onClick = onSelectRingtone,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = "🔔 Ringtone",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Urgency Keywords & Text Heuristics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customKeywordsText,
                            onValueChange = {
                                customKeywordsText = it
                                val newKeywords = it.split(",").map { kw -> kw.trim() }.filter { kw -> kw.isNotBlank() }.toSet()
                                config = config.copy(customKeywords = newKeywords)
                                configRepo.saveConfig(config)
                            },
                            label = { Text("Custom Keywords (comma separated)") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Text Heuristics", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Detect !! exclamations & 70%+ uppercase text",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = heuristicsEnabled,
                                onCheckedChange = { enabled ->
                                    heuristicsEnabled = enabled
                                    config = config.copy(enableTextHeuristics = enabled)
                                    configRepo.saveConfig(config)
                                },
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Auto-Retention Cleanup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Automatically delete messages older than the selected retention period via WorkManager.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val retentionOptions = listOf(7, 30, 60, 90, 0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            retentionOptions.forEach { days ->
                                val label = if (days == 0) "Never" else "${days}d"
                                val isSelected = (selectedRetentionDays == days)
                                OutlinedButton(
                                    onClick = {
                                        selectedRetentionDays = days
                                        config = config.copy(retentionDays = days)
                                        configRepo.saveConfig(config)
                                        if (days > 0) {
                                            RetentionManager.scheduleCleanupWorker(context)
                                        } else {
                                            RetentionManager.cancelCleanupWorker(context)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(4.dp),
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Phase 10 Contact Phone Mappings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Map sender display names to verified phone numbers so tapping Open launches WhatsApp chat directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newSenderName,
                            onValueChange = { newSenderName = it },
                            label = { Text("Sender Name (e.g. John)") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newPhoneNumber,
                            onValueChange = { newPhoneNumber = it },
                            label = { Text("Phone Number (e.g. +919876543210)") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (newSenderName.isNotBlank() && newPhoneNumber.isNotBlank()) {
                                    contactRepo.saveContactMapping(newSenderName, newPhoneNumber)
                                    newSenderName = ""
                                    newPhoneNumber = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text("Save Mapping")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSegmentedCapsule(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        AppTheme.DARK to "Dark",
        AppTheme.LIGHT to "Light",
        AppTheme.SYSTEM to "System",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { (theme, label) ->
                val isSelected = (currentTheme == theme)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onThemeSelected(theme) },
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
