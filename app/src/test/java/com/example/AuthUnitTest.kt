package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.FirebaseAuthService
import com.example.data.firebase.FirestoreService
import com.example.data.repository.RoktoSetuRepository
import com.example.model.AuthState
import com.example.model.BloodGroup
import com.example.model.DonorAvailability
import com.example.model.LocationInfo
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.VerificationStatus
import com.example.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthUnitTest : FirestoreEmulatorTestBase() {

    private lateinit var authService: FirebaseAuthService
    private lateinit var repository: RoktoSetuRepository
    private lateinit var authViewModel: AuthViewModel

    @Before
    override fun setUp() {
        super.setUp()
        authService = FirebaseAuthService(auth = auth, firestore = firestore, prefs = null)
        repository = RoktoSetuRepository(
            authService = authService,
            firestoreService = FirestoreService(firestore),
            localDb = null
        )
        authViewModel = AuthViewModel(repository)
    }

    @Test
    fun `test real AuthViewModel validateRegistrationInput passes valid inputs`() {
        val result = authViewModel.validateRegistrationInput(
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            pass = "StrongPass123",
            confirmPass = "StrongPass123",
            phone = "01712345678",
            termsAccepted = true
        )
        assertTrue(result.isValid)
        assertNull(result.nameError)
        assertNull(result.emailError)
        assertNull(result.phoneError)
        assertNull(result.passwordError)
        assertNull(result.confirmPasswordError)
        assertNull(result.termsError)
    }

    @Test
    fun `test real FirebaseAuthService and AuthViewModel password policy alignment`() {
        // Short password (< 8 chars)
        val shortResult = authViewModel.validateRegistrationInput(
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            pass = "Pass1",
            confirmPass = "Pass1",
            phone = "01712345678",
            termsAccepted = true
        )
        assertFalse(shortResult.isValid)
        assertEquals(FirebaseAuthService.validatePasswordRules("Pass1"), shortResult.passwordError)

        // No uppercase
        val noUpperResult = authViewModel.validateRegistrationInput(
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            pass = "pass12345",
            confirmPass = "pass12345",
            phone = "01712345678",
            termsAccepted = true
        )
        assertFalse(noUpperResult.isValid)
        assertEquals(FirebaseAuthService.validatePasswordRules("pass12345"), noUpperResult.passwordError)

        // No lowercase
        val noLowerResult = authViewModel.validateRegistrationInput(
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            pass = "PASS12345",
            confirmPass = "PASS12345",
            phone = "01712345678",
            termsAccepted = true
        )
        assertFalse(noLowerResult.isValid)
        assertEquals(FirebaseAuthService.validatePasswordRules("PASS12345"), noLowerResult.passwordError)

        // No number
        val noDigitResult = authViewModel.validateRegistrationInput(
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            pass = "PasswordOnly",
            confirmPass = "PasswordOnly",
            phone = "01712345678",
            termsAccepted = true
        )
        assertFalse(noDigitResult.isValid)
        assertEquals(FirebaseAuthService.validatePasswordRules("PasswordOnly"), noDigitResult.passwordError)

        // Valid password
        assertNull(FirebaseAuthService.validatePasswordRules("SecurePass2026"))
    }

    @Test
    fun `test real AuthViewModel password mismatch fails validation`() {
        val result = authViewModel.validateRegistrationInput(
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            pass = "StrongPass123",
            confirmPass = "DifferentPass123",
            phone = "01712345678",
            termsAccepted = true
        )
        assertFalse(result.isValid)
        assertEquals("Passwords do not match", result.confirmPasswordError)
    }

    @Test
    fun `test real AuthViewModel invalid email fails validation`() {
        val result = authViewModel.validateRegistrationInput(
            name = "Tanvir Ahmed",
            email = "invalid-email-address",
            pass = "StrongPass123",
            confirmPass = "StrongPass123",
            phone = "01712345678",
            termsAccepted = true
        )
        assertFalse(result.isValid)
        assertNotNull(result.emailError)
    }

    @Test
    fun `test real AuthViewModel unaccepted terms fails validation`() {
        val result = authViewModel.validateRegistrationInput(
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            pass = "StrongPass123",
            confirmPass = "StrongPass123",
            phone = "01712345678",
            termsAccepted = false
        )
        assertFalse(result.isValid)
        assertNotNull(result.termsError)
    }

    @Test
    fun `test FirebaseAuthService session relies strictly on FirebaseAuth currentUser`() {
        authService.signOut()
        assertNull(authService.currentUserId)
        assertFalse(authService.isAuthenticated)
        assertEquals(AuthState.Unauthenticated, authService.authState.value)
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

        val hundredDaysAgo = System.currentTimeMillis() - (100L * 24L * 60L * 60L * 1000L)
        val profileOldDonor = UserProfile(
            uid = "user_456",
            isDonor = true,
            lastDonationDate = hundredDaysAgo
        )
        assertTrue(profileOldDonor.isEligibleToDonate())

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
            showPhonePublicly = false,
            location = LocationInfo(
                division = "Dhaka",
                district = "Dhaka",
                upazila = "Dhanmondi",
                city = "Dhaka",
                area = "Dhanmondi"
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
        assertEquals("", publicView.contactPhone)
    }

    @Test
    fun `test recent login required detection and new UserProfile defaults`() {
        val recentEx = FirebaseAuthRecentLoginRequiredException("ERROR_REQUIRES_RECENT_LOGIN", "Requires recent authentication")
        assertTrue(FirebaseAuthService.isRecentLoginRequired(recentEx))

        val defaultProfile = UserProfile(
            uid = "user_regular_01",
            name = "Regular User",
            email = "user@example.com"
        )
        assertEquals(UserRole.USER, defaultProfile.role)
        assertEquals(VerificationStatus.UNVERIFIED, defaultProfile.verificationStatus)
        assertFalse(defaultProfile.isEmergencyDonor)
        assertEquals(0, defaultProfile.donationCount)
    }
}
