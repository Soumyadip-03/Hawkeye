package com.soumyadip.haykeye.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soumyadip.haykeye.data.AppDatabase
import com.soumyadip.haykeye.data.UrgentMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotebookViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).urgentMessageDao()

    val messages: StateFlow<List<UrgentMessage>> = dao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleReadStatus(message: UrgentMessage) {
        viewModelScope.launch {
            dao.markRead(message.id, !message.isRead)
        }
    }

    fun deleteMessage(message: UrgentMessage) {
        viewModelScope.launch {
            dao.deleteById(message.id)
        }
    }

    fun restoreMessage(message: UrgentMessage) {
        viewModelScope.launch {
            dao.insertMessage(message)
        }
    }

    fun clearReadMessages() {
        viewModelScope.launch {
            dao.deleteReadMessages()
        }
    }

    fun deleteAllMessages() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }
}
