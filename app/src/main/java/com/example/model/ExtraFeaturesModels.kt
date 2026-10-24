package com.example.model

enum class AppLanguage(val code: String, val displayName: String) {
    BANGLA("bn", "বাংলা"),
    ENGLISH("en", "English")
}

data class BloodBank(
    val id: String = "",
    val name: String = "",
    val nameBn: String = "",
    val organization: String = "",
    val address: String = "",
    val city: String = "Dhaka",
    val district: String = city.ifBlank { "Dhaka" },
    val phone: String = "",
    val emergencyHotline: String = "",
    val is24Hours: Boolean = true,
    val availableGroups: List<BloodGroup> = listOf(
        BloodGroup.A_POSITIVE,
        BloodGroup.B_POSITIVE,
        BloodGroup.O_POSITIVE,
        BloodGroup.AB_POSITIVE
    ),
    val verified: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> {
        val effectiveDistrict = district.ifBlank { city.ifBlank { "Dhaka" } }
        val effectiveCity = city.ifBlank { effectiveDistrict }
        return mapOf(
            "id" to id,
            "name" to name.trim(),
            "nameBn" to nameBn.trim().ifBlank { name.trim() },
            "organization" to organization.trim(),
            "address" to address.trim(),
            "city" to effectiveCity,
            "district" to effectiveDistrict,
            "phone" to phone.trim(),
            "contactPhone" to phone.trim(),
            "emergencyHotline" to emergencyHotline.trim().ifBlank { phone.trim() },
            "is24Hours" to is24Hours,
            "availableGroups" to availableGroups.map { it.name },
            "verified" to verified,
            "isActive" to isActive,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}

data class AmbulanceService(
    val id: String = "",
    val title: String = "",
    val titleBn: String = "",
    val provider: String = "",
    val coverageArea: String = "",
    val district: String = "Dhaka",
    val city: String = district.ifBlank { "Dhaka" },
    val phone: String = "",
    val type: String = "AC", // ICU, AC, Non-AC, Free
    val isAvailable24x7: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> {
        val effectiveDistrict = district.ifBlank { city.ifBlank { "Dhaka" } }
        val effectiveCity = city.ifBlank { effectiveDistrict }
        return mapOf(
            "id" to id,
            "title" to title.trim(),
            "titleBn" to titleBn.trim().ifBlank { title.trim() },
            "provider" to provider.trim(),
            "providerName" to provider.trim().ifBlank { title.trim() },
            "coverageArea" to coverageArea.trim().ifBlank { effectiveDistrict },
            "district" to effectiveDistrict,
            "city" to effectiveCity,
            "phone" to phone.trim(),
            "type" to type.trim().ifBlank { "AC" },
            "isAvailable24x7" to isAvailable24x7,
            "isActive" to isActive,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}

data class BloodDonationCamp(
    val id: String = "",
    val title: String = "",
    val titleBn: String = "",
    val organizer: String = "",
    val location: String = "",
    val district: String = "Dhaka",
    val city: String = district.ifBlank { "Dhaka" },
    val date: String = "",
    val time: String = "",
    val contactPhone: String = "",
    val targetUnits: Int = 100,
    val registeredCount: Int = 0,
    val isUserRsvp: Boolean = false,
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> {
        val effectiveDistrict = district.ifBlank { city.ifBlank { "Dhaka" } }
        val effectiveCity = city.ifBlank { effectiveDistrict }
        return mapOf(
            "id" to id,
            "title" to title.trim(),
            "titleBn" to titleBn.trim().ifBlank { title.trim() },
            "organizer" to organizer.trim(),
            "location" to location.trim(),
            "district" to effectiveDistrict,
            "city" to effectiveCity,
            "date" to date.trim(),
            "time" to time.trim(),
            "contactPhone" to contactPhone.trim(),
            "targetUnits" to targetUnits,
            "registeredCount" to registeredCount,
            "description" to description.trim(),
            "isActive" to isActive,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}

data class CampRsvpRecord(
    val uid: String = "",
    val campId: String = "",
    val userName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class DonationEligibility(
    val isEligible: Boolean,
    val daysRemaining: Long,
    val lastDonationDateString: String?,
    val nextEligibleDateString: String?,
    val totalDonations: Int,
    val healthTips: List<String>
)
