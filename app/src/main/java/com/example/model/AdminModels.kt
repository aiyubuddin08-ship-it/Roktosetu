package com.example.model

/**
 * System audit log for sensitive operations stored in 'audit_logs/{logId}'.
 */
data class AuditLogEntry(
    val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val ipAddressOrClient: String = "Android-Client",
    val details: Map<String, String> = emptyMap()
)

/**
 * System metrics and real-time live stats.
 */
data class SystemMetrics(
    val totalRegisteredUsers: Int = 0,
    val activeDonors: Int = 0,
    val emergencyDonorsOnline: Int = 0,
    val pendingRequests: Int = 0,
    val fulfilledRequests: Int = 0,
    val verifiedOrganizations: Int = 0,
    val totalDonationsCompleted: Int = 0,
    val serverStatus: String = "Operational",
    val appCheckStatus: String = "Enforced"
)

/**
 * Verification task queue item for admins.
 */
data class VerificationTask(
    val id: String = "",
    val targetId: String = "",
    val type: String = "DONOR", // "DONOR", "ORGANIZATION", "DONATION_RECORD"
    val name: String = "",
    val bloodGroup: BloodGroup? = null,
    val submittedAt: Long = System.currentTimeMillis(),
    val documents: List<String> = emptyList(),
    val status: VerificationStatus = VerificationStatus.PENDING_REVIEW
)
