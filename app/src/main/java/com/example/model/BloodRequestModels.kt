package com.example.model

enum class RequestUrgency(val display: String, val bengaliDisplay: String, val priorityScore: Int) {
    STANDARD("Standard", "সাধারণ", 1),
    HIGH("Urgent", "জরুরী", 2),
    CRITICAL_EMERGENCY("Critical Emergency", "অতি জরুরী (Emergency)", 3)
}

enum class RequestStatus(val display: String, val bengaliDisplay: String) {
    PENDING("Pending", "অপেক্ষমাণ"),
    DONOR_RESPONDED("Donor Responded", "রক্তদাতা সাড়া দিয়েছেন"),
    ACCEPTED("Donor Assigned", "রক্তদাতা নিশ্চিত"),
    FULFILLED("Fulfilled", "সম্পন্ন"),
    CANCELLED("Cancelled", "বাতিল"),
    EXPIRED("Expired", "মেয়াদোত্তীর্ণ")
}

/**
 * Blood Request document stored in `/blood_requests/{id}`.
 * Completely map-free: uses division, district, upazila, area, and hospitalName.
 */
data class BloodRequest(
    val id: String = "",
    val requesterUid: String = "",
    val requesterName: String = "",
    val patientName: String = "",
    val patientAge: Int = 0,
    val bloodGroup: BloodGroup = BloodGroup.O_POSITIVE,
    val unitsRequired: Int = 1,
    val unitsFulfilled: Int = 0,
    val hospitalName: String = "",
    val hospitalAddress: String = "",
    val division: String = "Dhaka",
    val district: String = "Dhaka",
    val upazila: String = "",
    val area: String = "",
    val city: String = "Dhaka",
    val urgency: RequestUrgency = RequestUrgency.HIGH,
    val status: RequestStatus = RequestStatus.PENDING,
    val contactPhone: String = "",
    val medicalReason: String = "",
    val requiredDateTime: Long = System.currentTimeMillis(),
    val acceptedDonorUid: String? = null,
    val acceptedDonorName: String? = null,
    val respondedDonorUids: List<String> = emptyList(),
    val isEmergencyBroadcast: Boolean = false,
    val broadcastDispatchedAt: Long? = null,
    val broadcastNotifiedCount: Int = 0,
    val adminNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 72 * 3600 * 1000L // 72 hours validity
) {
    fun toFirestoreMap(): Map<String, Any> {
        val effectiveDistrict = district.ifBlank { city.ifBlank { "Dhaka" } }
        val effectiveDivision = division.ifBlank { "Dhaka" }
        return mapOf<String, Any?>(
            "requesterUid" to requesterUid,
            "requesterName" to requesterName,
            "patientName" to patientName,
            "patientAge" to patientAge,
            "bloodGroup" to bloodGroup.name,
            "unitsRequired" to unitsRequired,
            "unitsFulfilled" to unitsFulfilled,
            "hospitalName" to hospitalName,
            "hospitalAddress" to hospitalAddress,
            "division" to effectiveDivision,
            "district" to effectiveDistrict,
            "upazila" to upazila,
            "area" to area,
            "area_lower" to area.trim().lowercase(),
            "city" to city.ifBlank { effectiveDistrict },
            "urgency" to urgency.name,
            "status" to status.name,
            "contactPhone" to contactPhone,
            "medicalReason" to medicalReason,
            "acceptedDonorUid" to acceptedDonorUid,
            "acceptedDonorName" to acceptedDonorName,
            "respondedDonorUids" to respondedDonorUids,
            "isEmergencyBroadcast" to isEmergencyBroadcast,
            "broadcastDispatchedAt" to broadcastDispatchedAt,
            "adminNotes" to adminNotes.ifBlank { null },
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "expiresAt" to expiresAt
        ).filterValues { it != null } as Map<String, Any>
    }
}
