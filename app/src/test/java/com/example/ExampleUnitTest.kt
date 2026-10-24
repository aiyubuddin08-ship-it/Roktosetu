package com.example

import com.example.model.DonorAvailability
import com.example.model.LocationInfo
import com.example.model.SystemAnnouncement
import com.example.model.UserProfile
import com.example.model.VerificationStatus
import com.example.ui.viewmodel.BloodViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun announcementTargetFilter_respectsStatusAudienceAndDistrict() {
        val donorInDhaka = UserProfile(
            uid = "u1",
            isDonor = true,
            donorAvailability = DonorAvailability.AVAILABLE,
            verificationStatus = VerificationStatus.VERIFIED,
            location = LocationInfo(division = "Dhaka", district = "Dhaka")
        )
        val nonDonorInSylhet = UserProfile(
            uid = "u2",
            isDonor = false,
            donorAvailability = DonorAvailability.UNAVAILABLE,
            verificationStatus = VerificationStatus.UNVERIFIED,
            location = LocationInfo(division = "Sylhet", district = "Sylhet")
        )

        val draftAnn = SystemAnnouncement(id = "a0", status = "DRAFT", targetAudience = "ALL_USERS")
        assertFalse(BloodViewModel.matchesAnnouncementTarget(draftAnn, donorInDhaka))

        val allUsersAnn = SystemAnnouncement(id = "a1", status = "PUBLISHED", targetAudience = "ALL_USERS")
        assertTrue(BloodViewModel.matchesAnnouncementTarget(allUsersAnn, donorInDhaka))
        assertTrue(BloodViewModel.matchesAnnouncementTarget(allUsersAnn, nonDonorInSylhet))

        val activeDonorsAnn = SystemAnnouncement(id = "a2", status = "PUBLISHED", targetAudience = "ACTIVE_DONORS")
        assertTrue(BloodViewModel.matchesAnnouncementTarget(activeDonorsAnn, donorInDhaka))
        assertFalse(BloodViewModel.matchesAnnouncementTarget(activeDonorsAnn, nonDonorInSylhet))

        val dhakaOnlyAnn = SystemAnnouncement(
            id = "a3",
            status = "PUBLISHED",
            targetAudience = "ALL_USERS",
            targetDistrict = "Dhaka"
        )
        assertTrue(BloodViewModel.matchesAnnouncementTarget(dhakaOnlyAnn, donorInDhaka))
        assertFalse(BloodViewModel.matchesAnnouncementTarget(dhakaOnlyAnn, nonDonorInSylhet))
    }
}

