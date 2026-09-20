package com.example

import com.example.model.AuthState
import com.example.model.BloodGroup
import com.example.model.DonorAvailability
import com.example.model.LocationInfo
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthUnitTest {

    // Helper for password validation logic
    private fun validatePassword(pass: String): String? {
        if (pass.length < 8) return "Password must be at least 8 characters"
        if (!pass.any { it.isUpperCase() }) return "Password must contain at least one uppercase letter"
        if (!pass.any { it.isLowerCase() }) return "Password must contain at least one lowercase letter"
        if (!pass.any { it.isDigit() }) return "Password must contain at least one number"
        return null
    }

    private fun validateRegistration(
        name: String,
        email: String,
        pass: String,
        confirmPass: String,
        phone: String,
        termsAccepted: Boolean
    ): Boolean {
        if (name.trim().length < 2) return false
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) return false
        if (phone.trim().length < 8) return false
        if (validatePassword(pass) != null) return false
        if (pass != confirmPass) return false
        if (!termsAccepted) return false
        return true
    }

    @Test
    fun `test valid registration inputs pass validation`() {
        val result = validateRegistration(
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            pass = "StrongPass123",
            confirmPass = "StrongPass123",
            phone = "01712345678",
            termsAccepted = true
        )
        assertTrue(result)
    }

    @Test
    fun `test password rules enforcement`() {
        // Short password
        assertEquals("Password must be at least 8 characters", validatePassword("Pass1"))

        // No uppercase
        assertEquals("Password must contain at least one uppercase letter", validatePassword("pass12345"))

        // No lowercase
        assertEquals("Password must contain at least one lowercase letter", validatePassword("PASS12345"))

        // No number
        assertEquals("Password must contain at least one number", validatePassword("PasswordOnly"))

        // Valid password
        assertNull(validatePassword("SecurePass2026"))
    }

    @Test
    fun `test password mismatch fails validation`() {
        val result = validateRegistration(
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            pass = "StrongPass123",
            confirmPass = "DifferentPass123",
            phone = "01712345678",
            termsAccepted = true
        )
        assertFalse(result)
    }

    @Test
    fun `test invalid email fails validation`() {
        val result = validateRegistration(
            name = "Tanvir Ahmed",
            email = "invalid-email-address",
            pass = "StrongPass123",
            confirmPass = "StrongPass123",
            phone = "01712345678",
            termsAccepted = true
        )
        assertFalse(result)
    }

    @Test
    fun `test unaccepted terms fails validation`() {
        val result = validateRegistration(
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            pass = "StrongPass123",
            confirmPass = "StrongPass123",
            phone = "01712345678",
            termsAccepted = false
        )
        assertFalse(result)
    }

    @Test
    fun `test donor medical donation eligibility calculation`() {
        val profileNewDonor = UserProfile(
            uid = "user_123",
            isDonor = true,
            lastDonationDate = null
        )
        assertTrue(profileNewDonor.isEligibleToDonate())
        assertEquals(0L, profileNewDonor.daysUntilNextEligible())

        // Donated 100 days ago -> eligible
        val hundredDaysAgo = System.currentTimeMillis() - (100L * 24L * 60L * 60L * 1000L)
        val profileOldDonor = UserProfile(
            uid = "user_456",
            isDonor = true,
            lastDonationDate = hundredDaysAgo
        )
        assertTrue(profileOldDonor.isEligibleToDonate())

        // Donated 30 days ago -> resting (not eligible)
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
        val profileResting = UserProfile(
            uid = "user_789",
            isDonor = true,
            lastDonationDate = thirtyDaysAgo
        )
        assertFalse(profileResting.isEligibleToDonate())
        assertTrue(profileResting.daysUntilNextEligible() > 50)
    }

    @Test
    fun `test public donor view privacy sanitization`() {
        val privateProfile = UserProfile(
            uid = "u_secure_99",
            name = "Dr. Hasan",
            email = "hasan.private@gmail.com",
            bloodGroup = BloodGroup.A_POSITIVE,
            isDonor = true,
            donorAvailability = DonorAvailability.AVAILABLE,
            isEmergencyDonor = true,
            contactPhone = "01811223344",
            showPhonePublicly = false, // User hid phone from public
            location = LocationInfo(
                city = "Dhaka",
                district = "Dhaka",
                area = "Dhanmondi",
                latitude = 23.7465,
                longitude = 90.3760
            ),
            verificationStatus = VerificationStatus.VERIFIED,
            role = UserRole.USER,
            donationCount = 5
        )

        val publicView = privateProfile.toPublicDonorView()

        assertEquals("u_secure_99", publicView.uid)
        assertEquals("Dr. Hasan", publicView.name)
        assertEquals(BloodGroup.A_POSITIVE, publicView.bloodGroup)
        assertEquals(5, publicView.donationCount)
        assertEquals(VerificationStatus.VERIFIED, publicView.verificationStatus)
        // Phone must be stripped because showPhonePublicly is false
        assertEquals("", publicView.contactPhone)
    }

    @Test
    fun `test AuthState sealed interface instances`() {
        val states: List<AuthState> = listOf(
            AuthState.Loading,
            AuthState.Unauthenticated,
            AuthState.Authenticated,
            AuthState.EmailVerificationRequired,
            AuthState.ProfileCompletionRequired,
            AuthState.AccountDisabled,
            AuthState.Error("Network error")
        )
        assertEquals(7, states.size)
    }
}
