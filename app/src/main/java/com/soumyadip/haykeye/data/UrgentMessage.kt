package com.soumyadip.haykeye.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "urgent_messages",
    indices = [Index(value = ["dedupKey"], unique = true)],
)
data class UrgentMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val provider: String,
    val senderName: String,
    val groupName: String? = null,
    val preview: String,
    val fullText: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val wasMentioned: Boolean = false,
    val dedupKey: String = "$provider|${groupName ?: ""}|$senderName|$timestamp|$fullText",
)
