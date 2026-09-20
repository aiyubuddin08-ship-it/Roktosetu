package com.example.model

/**
 * Urgency level for blood requests.
 */
enum class RequestUrgency(val display: String, val bengaliDisplay: String) {
    CRITICAL_EMERGENCY("Critical Emergency (ICU/OT)", "জরুরী (আইসিইউ/ওটি)"),
    HIGH("High Urgency (Today)", "জরুরী (আজকের মধ্যে)"),
    NORMAL("Normal / Scheduled", "সাধারণ / নির্ধারিত")
}

/**
 * Lifecycle status of a blood request.
 */
enum class RequestStatus(val display: String, val bengaliDisplay: String) {
    PENDING("Searching Donors", "রক্তদাতা খোঁজা হচ্ছে"),
    ACCEPTED("Donor Committed", "রক্তদাতা নিশ্চিত হয়েছে"),
    FULFILLED("Fulfilled / Donated", "সম্পন্ন হয়েছে"),
    CANCELLED("Cancelled", "বাতিল করা হয়েছে")
}

/**
 * Blood Request entity stored in Firestore 'blood_requests/{requestId}'.
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
    val city: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val urgency: RequestUrgency = RequestUrgency.HIGH,
    val requiredDateTime: Long = System.currentTimeMillis() + (6 * 60 * 60 * 1000), // Default 6 hrs
    val contactPhone: String = "",
    val alternativePhone: String = "",
    val medicalReason: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val acceptedDonorUid: String? = null,
    val acceptedDonorName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isEmergencyBroadcast: Boolean = false,
    val verifiedByOrgId: String? = null,
    val distanceKm: Double? = null
)
