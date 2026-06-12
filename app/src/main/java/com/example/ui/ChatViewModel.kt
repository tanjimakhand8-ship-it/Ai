package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatDatabase
import com.example.data.ChatMessageEntity
import com.example.data.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    init {
        val database = ChatDatabase.getDatabase(application)
        val dao = database.chatMessageDao()
        repository = ChatRepository(dao, application)

        // Seed with welcome message if empty
        viewModelScope.launch {
            try {
                val currentList = repository.allMessages.first()
                if (currentList.isEmpty()) {
                    repository.insertSystemGreeting()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendMessage(text: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            repository.sendUserMessage(text)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
            // Automatically insert greeting after clearing
            repository.insertSystemGreeting()
        }
    }

    fun deleteMessage(id: Int) {
        viewModelScope.launch {
            repository.deleteMessageById(id)
        }
    }
}
