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
    val outcome: String = "SUCCESS",
    val details: Map<String, String> = emptyMap()
)

/**
 * Data loading / query state for live metrics so the dashboard never fabricates numbers
 * and distinguishes loading, zero, permission denied, and query error states.
 */
enum class MetricsLoadState {
    IDLE,
    LOADING,
    LOADED,
    PERMISSION_DENIED,
    ERROR
}

/**
 * System metrics and real-time live stats backed by actual Firestore queries.
 */
data class SystemMetrics(
    val totalRegisteredUsers: Int = 0,
    val activeDonors: Int = 0,
    val emergencyDonorsOnline: Int = 0,
    val pendingVerifications: Int = 0,
    val pendingRequests: Int = 0,
    val emergencyRequests: Int = 0,
    val fulfilledRequests: Int = 0,
    val expiredRequests: Int = 0,
    val verifiedOrganizations: Int = 0,
    val pendingOrganizations: Int = 0,
    val totalDonationsCompleted: Int = 0,
    val openReportsCount: Int = 0,
    val securityAlertsCount: Int = 0,
    val serverStatus: String = "যাচাইকৃত সংযোগ",
    val appCheckStatus: String = "সক্রিয়",
    val loadState: MetricsLoadState = MetricsLoadState.IDLE,
    val lastRefreshedAt: Long? = null,
    val errorMessage: String? = null
)

/**
 * Verification task queue item for admins stored in '/verification_requests/{taskId}'.
 */
data class VerificationTask(
    val id: String = "",
    val targetId: String = "",
    val applicantUid: String = targetId,
    val type: String = "DONOR", // "DONOR", "DONOR_CARD", "ORGANIZATION", "DONATION_RECORD"
    val name: String = "",
    val bloodGroup: BloodGroup? = null,
    val submittedAt: Long = System.currentTimeMillis(),
    val documents: List<String> = emptyList(),
    val status: VerificationStatus = VerificationStatus.PENDING_REVIEW,
    val previousStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val rejectionReason: String = "",
    val reviewedByUid: String = "",
    val reviewedByEmail: String = "",
    val reviewedAt: Long? = null,
    val notes: String = ""
)

/**
 * User complaint / abuse report stored in '/reports/{reportId}'.
 */
data class UserReport(
    val id: String = "",
    val reporterUid: String = "",
    val reporterName: String = "",
    val targetUid: String = "",
    val targetName: String = "",
    val targetType: String = "USER", // "USER", "BLOOD_REQUEST", "ORGANIZATION", "CHAT"
    val category: String = "MISUSE", // "FAKE_REQUEST", "HARASSMENT", "FINANCIAL_DEMAND", "WRONG_INFO", "MISUSE"
    val reasonBn: String = "",
    val details: String = "",
    val severity: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val status: String = "OPEN", // "OPEN", "IN_REVIEW", "RESOLVED", "DISMISSED"
    val resolutionNotes: String = "",
    val actionTaken: String = "",
    val reviewedByUid: String = "",
    val reviewedByEmail: String = "",
    val reviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * System-wide or targeted announcement stored in '/announcements/{announcementId}'.
 */
data class SystemAnnouncement(
    val id: String = "",
    val titleBn: String = "",
    val bodyBn: String = "",
    val category: String = "GENERAL", // "GENERAL", "EMERGENCY", "MAINTENANCE", "CAMPAIGN"
    val targetAudience: String = "ALL_USERS", // "ALL_USERS", "ACTIVE_DONORS", "VERIFIED_DONORS", "ORGANIZATIONS"
    val targetDivision: String = "", // Empty means all divisions
    val targetDistrict: String = "", // Empty means all districts
    val status: String = "DRAFT", // "DRAFT", "PUBLISHED"
    val createdByUid: String = "",
    val createdByEmail: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null,
    val recipientCount: Int = 0
)

/**
 * System configuration and feature flags stored in '/system_config/general' and '/system_config/emergency_broadcast'.
 */
data class SystemConfigSettings(
    val maintenanceMode: Boolean = false,
    val maintenanceMessageBn: String = "সিস্টেম রক্ষণাবেক্ষণের কাজ চলছে। জরুরি সেবা শীঘ্রই স্বাভাবিক হবে।",
    val registrationEnabled: Boolean = true,
    val donorRegistrationEnabled: Boolean = true,
    val emergencyBroadcastArmed: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val requestExpiryHours: Int = 72,
    val maxRequestsPerUserPerDay: Int = 3,
    val announcementBannerBn: String = "",
    val updatedAt: Long? = null,
    val updatedByUid: String = "",
    val updatedByEmail: String = ""
)

/**
 * Security alert item stored in '/security_alerts/{alertId}' or derived from real runtime checks.
 */
data class SecurityAlertItem(
    val id: String = "",
    val alertType: String = "",
    val severity: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val detectedAt: Long = System.currentTimeMillis(),
    val departmentBn: String = "অ্যাকাউন্ট ও অনুমতি",
    val summaryBn: String = "",
    val recommendedActionBn: String = "",
    val reviewStatus: String = "OPEN", // "OPEN", "REVIEWED"
    val resolutionNotes: String = "",
    val reviewedByUid: String = "",
    val reviewedByEmail: String = "",
    val reviewedAt: Long? = null
)

/**
 * Readable database protection check card model for Section 3 of Security & Privacy Center.
 */
data class DatabaseProtectionCheck(
    val id: String,
    val titleBn: String,
    val protectedDataBn: String,
    val accessSummaryBn: String,
    val isVerified: Boolean,
    val statusLabelBn: String,
    val lastCheckedAt: Long,
    val issueDetectedBn: String? = null,
    val recommendedFixBn: String? = null
)

/**
 * Overall verified security status for the Security & Privacy Center.
 */
enum class VerifiedSecurityState(val labelBn: String) {
    PROTECTED("সুরক্ষিত"),
    WARNING_NEEDED("সতর্কতা প্রয়োজন"),
    RISK_DETECTED("ঝুঁকি শনাক্ত হয়েছে"),
    UNVERIFIED("যাচাই করা যায়নি")
}
