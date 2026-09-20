package com.example.model

/**
 * Types of system notifications.
 */
enum class NotificationType {
    EMERGENCY_BROADCAST,
    REQUEST_ACCEPTED,
    DONATION_VERIFIED,
    STATUS_UPDATE,
    ORGANIZATION_ALERT,
    SYSTEM_ANNOUNCEMENT
}

/**
 * Push & In-app Notification entity stored in 'users/{uid}/notifications/{notifId}'.
 * Protected so normal users cannot inject arbitrary notifications into another user's subcollection.
 */
data class AppNotification(
    val id: String = "",
    val recipientUid: String = "",
    val title: String = "",
    val body: String = "",
    val type: NotificationType = NotificationType.STATUS_UPDATE,
    val relatedEntityId: String? = null,
    val bloodGroup: BloodGroup? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
