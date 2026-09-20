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
    val city: String = "",
    val unitsDonated: Int = 1,
    val requestId: String? = null,
    val patientName: String = "",
    val verifiedByOrgId: String? = null,
    val verifiedByOrgName: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val notes: String = "",
    val certificateId: String? = null
)

/**
 * Donor milestone badges calculated by server logic.
 */
enum class DonorMilestoneBadge(val title: String, val minDonations: Int, val description: String) {
    FIRST_GIFT("First Step (প্রথম পদক্ষেপ)", 1, "Completed 1st successful blood donation"),
    BRONZE_LIFESAVER("Bronze Lifesaver (ব্রোঞ্জ জীবনরক্ষক)", 3, "Completed 3 verified donations"),
    SILVER_GUARDIAN("Silver Guardian (সিলভার অভিভাবক)", 5, "Completed 5 verified donations"),
    GOLD_CHAMPION("Gold Champion (গোল্ড চ্যাম্পিয়ন)", 10, "Completed 10 verified donations"),
    PLATINUM_HERO("Platinum Hero (প্লাটিনাম হিরো)", 20, "Completed 20+ verified donations - Life Saver Legend");

    companion object {
        fun getBadgeForCount(count: Int): DonorMilestoneBadge? {
            return entries.filter { count >= it.minDonations }.maxByOrNull { it.minDonations }
        }
    }
}
