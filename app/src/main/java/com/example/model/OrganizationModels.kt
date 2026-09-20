package com.example.model

/**
 * Verified Blood Donation Organization/Club entity in 'organizations/{orgId}'.
 */
data class Organization(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val registrationNumber: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val headquartersCity: String = "",
    val coverImageUrl: String = "",
    val logoUrl: String = "",
    val adminUids: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val memberCount: Int = 0,
    val activeCampaignsCount: Int = 0,
    val totalBagsCollected: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Blood donation camp / awareness campaign in 'organizations/{orgId}/campaigns/{campId}'.
 */
data class Campaign(
    val id: String = "",
    val organizationId: String = "",
    val organizationName: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val city: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000),
    val targetBags: Int = 50,
    val collectedBags: Int = 0,
    val isActive: Boolean = true,
    val contactPhone: String = ""
)
