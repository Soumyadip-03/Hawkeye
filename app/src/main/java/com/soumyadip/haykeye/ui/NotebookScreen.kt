package com.soumyadip.haykeye.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soumyadip.haykeye.classifier.ClassifierConfigRepository
import com.soumyadip.haykeye.data.UrgentMessage
import com.soumyadip.haykeye.navigation.MessageLauncher
import com.soumyadip.haykeye.ui.theme.StatusGreen
import com.soumyadip.haykeye.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ProviderTab(val displayName: String) {
    ALL("All"),
    WHATSAPP("WhatsApp"),
    GMAIL("Gmail"),
    MESSAGES("Messages");

    fun matches(messageProvider: String): Boolean {
        return when (this) {
            ALL -> true
            WHATSAPP -> ((messageProvider == "WhatsApp") || (messageProvider == "WhatsApp Business"))
            GMAIL -> messageProvider == "Gmail"
            MESSAGES -> messageProvider == "Google Messages"
        }
    }
}

@Composable
fun NotebookScreen(
    isAccessGranted: Boolean,
    hasPostNotificationPermission: Boolean,
    onOpenNotificationAccessSettings: () -> Unit,
    onRequestPostNotificationPermission: () -> Unit,
    onOpenSettingsScreen: () -> Unit,
    viewModel: NotebookViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configRepo = remember { ClassifierConfigRepository(context) }
    var config by remember { mutableStateOf(configRepo.getConfig()) }

    val allMessages by viewModel.messages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedTab by remember { mutableStateOf(ProviderTab.ALL) }
    var showClearReadDialog by remember { mutableStateOf(false) }

    val filteredMessages = remember(allMessages, selectedTab, searchQuery) {
        allMessages.filter { msg ->
            val matchesTab = selectedTab.matches(msg.provider)
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                msg.senderName.lowercase().contains(q) ||
                        (msg.groupName?.lowercase()?.contains(q) == true) ||
                        msg.preview.lowercase().contains(q) ||
                        msg.fullText.lowercase().contains(q)
            }
            matchesTab && matchesSearch
        }
    }

    if (showClearReadDialog) {
        AlertDialog(
            onDismissRequest = { showClearReadDialog = false },
            title = { Text("Clear All Read Messages?") },
            text = { Text("This will permanently remove all read messages from your notebook.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearReadMessages()
                        showClearReadDialog = false
                    },
                ) {
                    Text("Clear Read", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearReadDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        HeaderBar(
            isAccessGranted = isAccessGranted,
            isListeningEnabled = config.isListeningEnabled,
            hasPostNotificationPermission = hasPostNotificationPermission,
            onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
            onRequestPostNotificationPermission = onRequestPostNotificationPermission,
            onOpenSettingsScreen = onOpenSettingsScreen,
            onClearReadClicked = { showClearReadDialog = true },
            onToggleListeningEnabled = {
                val updated = config.copy(isListeningEnabled = !config.isListeningEnabled)
                config = updated
                configRepo.saveConfig(updated)
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search sender, group, text...") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Text("✖", fontSize = 12.sp)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProviderTabRow(
            selectedTab = selectedTab,
            onTabSelected = { tab -> selectedTab = tab },
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredMessages.isEmpty()) {
            EmptyState(
                selectedTab = selectedTab,
                searchQuery = searchQuery,
                isAccessGranted = isAccessGranted,
                onOpenSettings = onOpenNotificationAccessSettings,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = filteredMessages,
                    key = { msg -> msg.id },
                ) { message ->
                    MessageCard(
                        message = message,
                        onToggleRead = { viewModel.toggleReadStatus(message) },
                        onDelete = { viewModel.deleteMessage(message) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderTabRow(
    selectedTab: ProviderTab,
    onTabSelected: (ProviderTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProviderTab.entries.forEach { tab ->
            val isSelected = (tab == selectedTab)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.displayName,
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

@Composable
private fun HeaderBar(
    isAccessGranted: Boolean,
    isListeningEnabled: Boolean,
    hasPostNotificationPermission: Boolean,
    onOpenNotificationAccessSettings: () -> Unit,
    onRequestPostNotificationPermission: () -> Unit,
    onOpenSettingsScreen: () -> Unit,
    onClearReadClicked: () -> Unit,
    onToggleListeningEnabled: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(
                    text = "Haykeye",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    text = "Encrypted Local Notebook",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onClearReadClicked,
                    modifier = Modifier.size(36.dp),
                ) {
                    Text(text = "🧹", fontSize = 16.sp)
                }

                IconButton(
                    onClick = onOpenSettingsScreen,
                    modifier = Modifier.size(36.dp),
                ) {
                    Text(text = "⚙️", fontSize = 16.sp)
                }

                val statusText = when {
                    !isAccessGranted -> "Off"
                    !isListeningEnabled -> "Paused"
                    else -> "Active"
                }

                val statusColor = when {
                    !isAccessGranted || !isListeningEnabled -> StatusWarning
                    else -> StatusGreen
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        statusColor.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.clickable {
                        if (!isAccessGranted) {
                            onOpenNotificationAccessSettings()
                        } else {
                            onToggleListeningEnabled()
                        }
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = statusColor,
                                    shape = CircleShape,
                                ),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        if (!isAccessGranted) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Enable permission to capture messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenNotificationAccessSettings,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(text = "Enable", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (!hasPostNotificationPermission) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Notification permission needed for alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRequestPostNotificationPermission,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(text = "Grant", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: UrgentMessage,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(value = false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, if (message.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = message.provider,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }

                    if (!message.isRead) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(
                        onClick = onToggleRead,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = if (message.isRead) "Unread" else "Read",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Text(
                            text = "🗑️",
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message.senderName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (!message.groupName.isNullOrBlank()) {
                Text(
                    text = "Group: ${message.groupName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isExpanded) message.fullText else message.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )

            if ((!isExpanded) && (message.fullText.length > message.preview.length)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to expand",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        MessageLauncher.launchSourceApp(context, message)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Open ${message.provider}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    selectedTab: ProviderTab,
    searchQuery: String,
    isAccessGranted: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messageText = when {
        searchQuery.isNotBlank() -> "No messages found matching '$searchQuery'."
        !isAccessGranted -> "Notification Access is required so Haykeye can receive incoming messages."
        selectedTab == ProviderTab.ALL -> "Haykeye is actively listening. Incoming urgent messages will appear here."
        selectedTab == ProviderTab.WHATSAPP -> "No WhatsApp or WhatsApp Business messages captured yet."
        selectedTab == ProviderTab.GMAIL -> "No Gmail messages captured yet."
        selectedTab == ProviderTab.MESSAGES -> "No Google Messages captured yet."
        else -> "No messages available for this filter."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = if (searchQuery.isNotBlank()) "🔍" else "📬", fontSize = 32.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isNotBlank()) "No Matching Search Results" else if (selectedTab == ProviderTab.ALL) "No Messages Captured Yet" else "No ${selectedTab.displayName} Messages",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = messageText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (!isAccessGranted) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onOpenSettings) {
                Text(text = "Enable Notification Access")
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
