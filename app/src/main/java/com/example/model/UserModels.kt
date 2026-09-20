package com.example.model

/**
 * Verification state for users and registered donors.
 */
enum class VerificationStatus {
    UNVERIFIED,
    PENDING_REVIEW,
    VERIFIED,
    REJECTED
}

/**
 * System roles for Role-Based Access Control (RBAC).
 */
enum class UserRole {
    USER,
    ORGANIZATION_ADMIN,
    ADMIN
}

/**
 * Donor availability status.
 */
enum class DonorAvailability {
    AVAILABLE,
    RESTING,
    UNAVAILABLE,
    EMERGENCY_ONLY
}

/**
 * Approximate location representation to protect exact private home addresses.
 */
data class LocationInfo(
    val city: String = "",
    val district: String = "",
    val area: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isApproximate: Boolean = true
)

/**
 * Main User Profile entity stored in Firestore 'users/{uid}'.
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bloodGroup: BloodGroup = BloodGroup.O_POSITIVE,
    val isDonor: Boolean = false,
    val donorAvailability: DonorAvailability = DonorAvailability.AVAILABLE,
    val isEmergencyDonor: Boolean = true,
    val contactPhone: String = "",
    val showPhonePublicly: Boolean = true,
    val location: LocationInfo = LocationInfo(),
    val dateOfBirthString: String = "",
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val role: UserRole = UserRole.USER,
    val donationCount: Int = 0,
    val lastDonationDate: Long? = null,
    val profilePhotoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val organizationId: String? = null,
    val isAccountActive: Boolean = true
) {
    /**
     * Checks if the donor is medically eligible to donate (minimum 90 days / 3 months gap).
     */
    fun isEligibleToDonate(): Boolean {
        if (!isDonor) return false
        val lastDate = lastDonationDate ?: return true
        val ninetyDaysMs = 90L * 24L * 60L * 60L * 1000L
        return System.currentTimeMillis() - lastDate >= ninetyDaysMs
    }

    /**
     * Calculates days remaining until next eligible donation date.
     */
    fun daysUntilNextEligible(): Long {
        val lastDate = lastDonationDate ?: return 0
        val ninetyDaysMs = 90L * 24L * 60L * 60L * 1000L
        val diff = (lastDate + ninetyDaysMs) - System.currentTimeMillis()
        return if (diff > 0) diff / (24L * 60L * 60L * 1000L) + 1 else 0
    }

    /**
     * Sanitized public view preventing leakage of private identifiers or private contact details.
     */
    fun toPublicDonorView(): PublicDonorView {
        return PublicDonorView(
            uid = uid,
            name = name,
            bloodGroup = bloodGroup,
            availability = if (isEligibleToDonate()) donorAvailability else DonorAvailability.RESTING,
            isEmergencyDonor = isEmergencyDonor,
            generalLocation = "${location.area.ifEmpty { location.district }}, ${location.city}",
            latitude = location.latitude,
            longitude = location.longitude,
            verificationStatus = verificationStatus,
            donationCount = donationCount,
            lastDonationDate = lastDonationDate,
            contactPhone = if (showPhonePublicly) contactPhone else ""
        )
    }
}

/**
 * Privacy-safe public donor entity visible to other searchers.
 */
data class PublicDonorView(
    val uid: String = "",
    val name: String = "",
    val bloodGroup: BloodGroup = BloodGroup.O_POSITIVE,
    val availability: DonorAvailability = DonorAvailability.AVAILABLE,
    val isEmergencyDonor: Boolean = true,
    val generalLocation: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val donationCount: Int = 0,
    val lastDonationDate: Long? = null,
    val contactPhone: String = "",
    val distanceKm: Double? = null
)
