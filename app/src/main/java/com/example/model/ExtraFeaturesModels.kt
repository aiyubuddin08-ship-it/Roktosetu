package com.example.model

enum class AppLanguage(val code: String, val displayName: String) {
    BANGLA("bn", "বাংলা"),
    ENGLISH("en", "English")
}

data class BloodBank(
    val id: String,
    val name: String,
    val nameBn: String,
    val organization: String,
    val address: String,
    val city: String,
    val phone: String,
    val emergencyHotline: String,
    val is24Hours: Boolean = true,
    val availableGroups: List<BloodGroup> = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE, BloodGroup.AB_POSITIVE),
    val verified: Boolean = true
)

data class AmbulanceService(
    val id: String,
    val title: String,
    val titleBn: String,
    val provider: String,
    val coverageArea: String,
    val phone: String,
    val type: String, // ICU, AC, Non-AC, Free
    val isAvailable24x7: Boolean = true
)

data class BloodDonationCamp(
    val id: String,
    val title: String,
    val titleBn: String,
    val organizer: String,
    val location: String,
    val date: String,
    val time: String,
    val contactPhone: String,
    val targetUnits: Int = 100,
    val registeredCount: Int = 0,
    val isUserRsvp: Boolean = false,
    val description: String = ""
)

data class DonationEligibility(
    val isEligible: Boolean,
    val daysRemaining: Long,
    val lastDonationDateString: String?,
    val nextEligibleDateString: String?,
    val totalDonations: Int,
    val healthTips: List<String>
)
