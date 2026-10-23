package com.example.model

/**
 * Verified Donation record stored in Firestore 'donations/{donationId}'.
 * Direct client modification of verification or statistics is restricted.
 */
data class DonationRecord(
    val id: String = "",
    val donorUid: String = "",
    val donorName: String = "",
    val bloodGroup: BloodGroup = BloodGroup.O_POSITIVE,
    val donationDate: Long = System.currentTimeMillis(),
    val hospitalName: String = "",
    val division: String = "Dhaka",
    val district: String = "Dhaka",
    val upazila: String = "",
    val city: String = district,
    val unitsDonated: Int = 1,
    val requestId: String? = null,
    val patientName: String = "",
    val verifiedByOrgId: String? = null,
    val verifiedByOrgName: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val notes: String = "",
    val certificateId: String? = null,
    val certificateCode: String = certificateId ?: ""
)

/**
 * Donor milestone badges calculated by server logic.
 */
enum class DonorMilestoneBadge(
    val title: String,
    val minDonations: Int,
    val description: String,
    val bengaliTitle: String = title
) {
    FIRST_GIFT("First Step (প্রথম পদক্ষেপ)", 1, "Completed 1st successful blood donation", "প্রথম রক্তদান"),
    BRONZE_LIFESAVER("Bronze Lifesaver (ব্রোঞ্জ জীবনরক্ষক)", 3, "Completed 3 verified donations", "ব্রোঞ্জ জীবনরক্ষক"),
    SILVER_GUARDIAN("Silver Guardian (সিলভার অভিভাবক)", 5, "Completed 5 verified donations", "সিলভার অভিভাবক"),
    GOLD_CHAMPION("Gold Champion (গোল্ড চ্যাম্পিয়ন)", 10, "Completed 10 verified donations", "গোল্ড চ্যাম্পিয়ন"),
    PLATINUM_HERO("Platinum Hero (প্লাটিনাম হিরো)", 20, "Completed 20+ verified donations - Life Saver Legend", "প্লাটিনাম হিরো");

    companion object {
        fun getBadgeForCount(count: Int): DonorMilestoneBadge? {
            return entries.filter { count >= it.minDonations }.maxByOrNull { it.minDonations }
        }
    }
}
