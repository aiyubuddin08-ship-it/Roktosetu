package com.example.model

/**
 * 1-to-1 secure conversation entity stored in 'conversations/{convId}'.
 * Participants array is strictly validated by Firestore rules: request.auth.uid in resource.data.participants
 */
data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(), // [uid1, uid2]
    val participantNames: Map<String, String> = emptyMap(),
    val participantBloodGroups: Map<String, String> = emptyMap(),
    val relatedRequestId: String? = null,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val lastMessageSenderUid: String = ""
)

/**
 * Chat message entity stored in 'conversations/{convId}/messages/{msgId}'.
 * senderUid MUST match request.auth.uid to prevent impersonation.
 */
data class ChatMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
