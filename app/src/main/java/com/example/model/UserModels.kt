package com.example.model

import com.example.domain.LocationHelper

/**
 * Role-based Access Control (RBAC) levels enforced by server custom claims & Firestore `/admins` collection.
 */
enum class UserRole {
    USER,
    VERIFIED_DONOR,
    ORG_ADMIN,
    ORGANIZATION_ADMIN,
    ADMIN,
    SUPER_ADMIN
}

/**
 * Donor availability status controlled by the donor or updated after a verified donation.
 */
enum class DonorAvailability(val displayEn: String, val displayBn: String) {
    AVAILABLE("Available", "রক্তদানে প্রস্তুত"),
    RESTING("Resting (Post-Donation)", "বিশ্রামে (রক্তদান পরবর্তী)"),
    EMERGENCY_ONLY("Emergency Only", "শুধুমাত্র জরুরী প্রয়োজনে"),
    UNAVAILABLE("Unavailable", "বর্তমানে অনুপস্থিত")
}

/**
 * Verification status of a donor or organization.
 * Never assigned automatically at registration; requires institutional or admin verification.
 */
enum class VerificationStatus(val labelEn: String, val labelBn: String, val descriptionEn: String) {
    UNVERIFIED(
        "Unverified",
        "অযাচাইকৃত",
        "Self-registered account. Identity and donation history have not yet been verified by an authorized hospital or administrator."
    ),
    PENDING(
        "Verification Pending",
        "যাচাইয়ের অপেক্ষায়",
        "Verification documents submitted and awaiting review by an authorized administrator."
    ),
    PENDING_REVIEW(
        "Verification Pending",
        "যাচাইয়ের অপেক্ষায়",
        "Verification documents submitted and awaiting review by an authorized administrator."
    ),
    VERIFIED(
        "Verified Donor",
        "যাচাইকৃত রক্তদাতা",
        "Identity or prior blood donation record verified by an authorized hospital, blood bank, or RoktoSetu administrator."
    ),
    REJECTED(
        "Verification Declined",
        "যাচাই বাতিল",
        "Submitted verification evidence was incomplete or could not be validated."
    )
}

/**
 * Administrative text-based location model (Division, District, Upazila, Area).
 * Map coordinates (latitude/longitude) have been completely removed for privacy and simplicity.
 */
data class LocationInfo(
    val division: String = "Dhaka",
    val district: String = "Dhaka",
    val upazila: String = "",
    val area: String = "",
    val city: String = "Dhaka"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "division" to division.ifBlank { "Dhaka" },
        "district" to district.ifBlank { city.ifBlank { "Dhaka" } },
        "upazila" to upazila,
        "area" to area,
        "city" to city.ifBlank { district.ifBlank { "Dhaka" } }
    )
}

/**
 * Account status for administrative moderation.
 */
enum class AccountStatus(val labelEn: String, val labelBn: String) {
    ACTIVE("Active", "সক্রিয়"),
    SUSPENDED("Suspended", "স্থগিত")
}

/**
 * Full User Profile stored in `/users/{uid}` (strictly private to the user and admins).
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bloodGroup: BloodGroup = BloodGroup.O_POSITIVE,
    val dateOfBirth: String = "",
    val isDonor: Boolean = true,
    val donorAvailability: DonorAvailability = DonorAvailability.AVAILABLE,
    val lastDonationDate: Long? = null, // Epoch millis
    val isEmergencyDonor: Boolean = false,
    val contactPhone: String = "",
    val showPhonePublicly: Boolean = false,
    val location: LocationInfo = LocationInfo(),
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val role: UserRole = UserRole.USER,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val suspensionReason: String? = null,
    val organizationId: String? = null,
    val donationCount: Int = 0,
    val badges: List<String> = emptyList(),
    val photoUrl: String = "",
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks whether the minimum 90-day recovery interval since the last recorded donation has elapsed
     * AND the donor has marked themselves available.
     * NOTE: Passing the 90-day interval is only a preliminary recovery check; final medical eligibility
     * must always be determined by a qualified physician or blood bank prior to donation.
     */
    fun isEligibleToDonate(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (accountStatus == AccountStatus.SUSPENDED) return false
        if (!isDonor || donorAvailability == DonorAvailability.UNAVAILABLE || donorAvailability == DonorAvailability.RESTING) {
            return false
        }
        val lastDate = lastDonationDate ?: return true
        val daysSinceLast = (currentTimeMillis - lastDate) / (1000L * 60 * 60 * 24)
        return daysSinceLast >= 90
    }

    /**
     * Days remaining in the standard 90-day post-donation recovery interval.
     */
    fun daysUntilNextEligible(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val lastDate = lastDonationDate ?: return 0L
        val daysSinceLast = (currentTimeMillis - lastDate) / (1000L * 60 * 60 * 24)
        return (90L - daysSinceLast).coerceAtLeast(0L)
    }

    /**
     * Produces a sanitized public donor view that strips private email, DOB, FCM token, and hides
     * the contact phone unless `showPhonePublicly` is explicitly enabled by the donor.
     */
    fun toPublicDonorView(): PublicDonorView {
        val effectiveDivision = location.division.ifBlank { LocationHelper.inferDivisionForDistrict(location.district.ifBlank { location.city }) }
        val effectiveDistrict = location.district.ifBlank { location.city.ifBlank { "Dhaka" } }
        val effectiveUpazila = location.upazila
        val effectiveArea = location.area
        val effectiveAvailability = when {
            accountStatus == AccountStatus.SUSPENDED -> DonorAvailability.UNAVAILABLE
            !isEligibleToDonate() && lastDonationDate != null && daysUntilNextEligible() > 0 -> DonorAvailability.RESTING
            else -> donorAvailability
        }

        return PublicDonorView(
            uid = uid,
            name = name,
            bloodGroup = bloodGroup,
            availability = effectiveAvailability,
            division = effectiveDivision,
            district = effectiveDistrict,
            upazila = effectiveUpazila,
            area = effectiveArea,
            city = effectiveDistrict,
            generalLocation = LocationHelper.formatGeneralLocation(
                division = effectiveDivision,
                district = effectiveDistrict,
                upazila = effectiveUpazila,
                area = effectiveArea
            ),
            isEmergencyDonor = isEmergencyDonor && accountStatus == AccountStatus.ACTIVE,
            contactPhone = if (showPhonePublicly && accountStatus == AccountStatus.ACTIVE) contactPhone else "",
            verificationStatus = verificationStatus,
            donationCount = donationCount,
            lastDonationDate = lastDonationDate
        )
    }

    fun toFirestoreMap(): Map<String, Any> {
        val effectiveDivision = location.division.ifBlank { LocationHelper.inferDivisionForDistrict(location.district.ifBlank { location.city }) }
        val effectiveDistrict = location.district.ifBlank { location.city.ifBlank { "Dhaka" } }
        return mapOf<String, Any?>(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "bloodGroup" to bloodGroup.name,
            "dateOfBirth" to dateOfBirth,
            "isDonor" to isDonor,
            "donorAvailability" to donorAvailability.name,
            "lastDonationDate" to lastDonationDate,
            "isEmergencyDonor" to isEmergencyDonor,
            "contactPhone" to contactPhone,
            "showPhonePublicly" to showPhonePublicly,
            "division" to effectiveDivision,
            "district" to effectiveDistrict,
            "upazila" to location.upazila,
            "area" to location.area,
            "area_lower" to location.area.trim().lowercase(),
            "city" to location.city.ifBlank { effectiveDistrict },
            "location" to location.copy(
                division = effectiveDivision,
                district = effectiveDistrict,
                city = location.city.ifBlank { effectiveDistrict }
            ).toMap(),
            "verificationStatus" to verificationStatus.name,
            "role" to role.name,
            "accountStatus" to accountStatus.name,
            "suspensionReason" to suspensionReason,
            "organizationId" to organizationId,
            "donationCount" to donationCount,
            "badges" to badges,
            "photoUrl" to photoUrl,
            "fcmToken" to fcmToken,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        ).filterValues { it != null } as Map<String, Any>
    }
}

/**
 * Sanitized Public Donor Profile stored in `/public_donors/{uid}` for privacy-preserving search.
 */
data class PublicDonorView(
    val uid: String = "",
    val name: String = "",
    val bloodGroup: BloodGroup = BloodGroup.O_POSITIVE,
    val availability: DonorAvailability = DonorAvailability.AVAILABLE,
    val division: String = "Dhaka",
    val district: String = "Dhaka",
    val upazila: String = "",
    val area: String = "",
    val city: String = district,
    val generalLocation: String = "",
    val isEmergencyDonor: Boolean = false,
    val contactPhone: String = "", // Empty unless user explicitly opted into showPhonePublicly
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val donationCount: Int = 0,
    val lastDonationDate: Long? = null
) {
    fun isRecoveryPeriodComplete(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val last = lastDonationDate ?: return true
        val daysSince = (currentTimeMillis - last) / (1000L * 60 * 60 * 24)
        return daysSince >= 90
    }

    fun daysRemainingInRecovery(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val last = lastDonationDate ?: return 0L
        val daysSince = (currentTimeMillis - last) / (1000L * 60 * 60 * 24)
        return (90L - daysSince).coerceAtLeast(0L)
    }

    fun toFirestoreMap(isDonorFlag: Boolean = true): Map<String, Any> {
        return mapOf<String, Any?>(
            "uid" to uid,
            "name" to name,
            "bloodGroup" to bloodGroup.name,
            "availability" to availability.name,
            "donorAvailability" to availability.name,
            "division" to division.ifBlank { "Dhaka" },
            "district" to district.ifBlank { "Dhaka" },
            "upazila" to upazila,
            "area" to area,
            "area_lower" to area.trim().lowercase(),
            "generalLocation" to generalLocation.ifBlank {
                LocationHelper.formatGeneralLocation(division, district, upazila, area)
            },
            "isDonor" to isDonorFlag,
            "isEmergencyDonor" to isEmergencyDonor,
            "contactPhone" to contactPhone,
            "showPhonePublicly" to contactPhone.isNotBlank(),
            "verificationStatus" to verificationStatus.name,
            "donationCount" to donationCount,
            "lastDonationDate" to lastDonationDate,
            "updatedAt" to System.currentTimeMillis()
        ).filterValues { it != null } as Map<String, Any>
    }
}
