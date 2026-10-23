package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.Resource
import com.example.data.repository.RoktoSetuRepository
import com.example.model.ChatMessage
import com.example.model.Conversation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val activeConversationId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val repository: RoktoSetuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var conversationsJob: Job? = null
    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            repository.currentUserProfile.collect { profile ->
                if (profile != null) {
                    loadConversations()
                } else {
                    conversationsJob?.cancel()
                    messagesJob?.cancel()
                    _uiState.value = ChatUiState()
                }
            }
        }
    }

    fun loadConversations() {
        val uid = repository.currentUserId ?: return
        conversationsJob?.cancel()
        conversationsJob = viewModelScope.launch {
            repository.getConversations(
                userUid = uid,
                onError = { msg ->
                    _uiState.value = _uiState.value.copy(errorMessage = msg)
                }
            ).collect { list ->
                _uiState.value = _uiState.value.copy(conversations = list)
            }
        }
    }

    fun openConversation(convId: String) {
        _uiState.value = _uiState.value.copy(activeConversationId = convId)
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessages(
                conversationId = convId,
                onError = { msg ->
                    _uiState.value = _uiState.value.copy(errorMessage = msg)
                }
            ).collect { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }
    }

    fun sendMessage(convId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val uid = repository.currentUserId
        if (uid.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "বার্তা পাঠাতে অনুগ্রহ করে সাইন-ইন করুন।")
            return
        }
        val name = repository.currentUserProfile.value?.name ?: "User"
        viewModelScope.launch {
            when (val res = repository.sendMessage(convId, uid, name, trimmed)) {
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    fun startChatWithDonor(
        donorUid: String,
        donorName: String,
        donorBloodGroup: String,
        requestId: String? = null,
        onReady: (String) -> Unit
    ) {
        val myProfile = repository.currentUserProfile.value
        if (myProfile == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please sign in to start a conversation.")
            return
        }
        if (myProfile.uid == donorUid) {
            _uiState.value = _uiState.value.copy(errorMessage = "You cannot start a conversation with yourself.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.getOrCreateConversation(
                myUid = myProfile.uid,
                myName = myProfile.name,
                myBloodGroup = myProfile.bloodGroup.display,
                otherUid = donorUid,
                otherName = donorName,
                otherBloodGroup = donorBloodGroup,
                requestId = requestId
            )) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onReady(res.data)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class ChatViewModelFactory(private val repository: RoktoSetuRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
