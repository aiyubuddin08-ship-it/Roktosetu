package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.DonorAvailability
import com.example.model.LocationInfo
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.VerificationStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoktoSetuRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun `user can save own profile and public donor view but cannot read another users private profile`() {
        val aliceUid = signInTestUser("alice@roktosetu.org", "Pass1234!")

        val aliceProfile = UserProfile(
            uid = aliceUid,
            name = "Alice Rahman",
            email = "alice@roktosetu.org",
            bloodGroup = BloodGroup.O_POSITIVE,
            isDonor = true,
            donorAvailability = DonorAvailability.AVAILABLE,
            isEmergencyDonor = false,
            contactPhone = "01712345678",
            showPhonePublicly = false,
            location = LocationInfo(
                division = "Dhaka",
                district = "Dhaka",
                upazila = "Dhanmondi",
                city = "Dhaka",
                area = "Dhanmondi"
            ),
            verificationStatus = VerificationStatus.UNVERIFIED,
            role = UserRole.USER
        )

        awaitTask(firestore.collection("users").document(aliceUid).set(aliceProfile.toFirestoreMap()))

        val publicView = aliceProfile.toPublicDonorView()
        awaitTask(firestore.collection("public_donors").document(aliceUid).set(publicView.toFirestoreMap(isDonorFlag = true)))

        // Sign in as Bob
        val bobUid = signInTestUser("bob@roktosetu.org", "Pass1234!")
        assertTrue(bobUid != aliceUid)

        // Bob can read Alice's sanitized public_donors document
        val publicSnap = awaitTask(firestore.collection("public_donors").document(aliceUid).get())
        assertTrue(publicSnap.exists())
        assertEquals("", publicSnap.getString("contactPhone"))

        // Bob CANNOT read Alice's private /users/{aliceUid} document
        try {
            awaitTask(firestore.collection("users").document(aliceUid).get())
            fail("Expected PERMISSION_DENIED when reading another user's private profile")
        } catch (e: Exception) {
            val firestoreEx = e as? FirebaseFirestoreException ?: e.cause as? FirebaseFirestoreException
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun `blood request enforces valid creation and status transitions`() {
        val requesterUid = signInTestUser("requester@roktosetu.org", "Pass1234!")

        val req = BloodRequest(
            id = "req_test_1",
            requesterUid = requesterUid,
            requesterName = "Requester User",
            patientName = "Patient One",
            patientAge = 35,
            bloodGroup = BloodGroup.B_POSITIVE,
            unitsRequired = 2,
            hospitalName = "DMCH",
            hospitalAddress = "Shahbagh",
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Shahbagh",
            area = "Shahbagh",
            city = "Dhaka",
            urgency = RequestUrgency.HIGH,
            status = RequestStatus.PENDING,
            contactPhone = "01711000000",
            medicalReason = "Surgery"
        )

        val reqMap = mapOf(
            "id" to req.id,
            "requesterUid" to req.requesterUid,
            "requesterName" to req.requesterName,
            "patientName" to req.patientName,
            "patientAge" to req.patientAge,
            "bloodGroup" to req.bloodGroup.name,
            "unitsRequired" to req.unitsRequired,
            "hospitalName" to req.hospitalName,
            "hospitalAddress" to req.hospitalAddress,
            "division" to req.division,
            "district" to req.district,
            "upazila" to req.upazila,
            "area" to req.area,
            "city" to req.city,
            "urgency" to req.urgency.name,
            "status" to req.status.name,
            "contactPhone" to req.contactPhone,
            "medicalReason" to req.medicalReason,
            "acceptedDonorUid" to null,
            "acceptedDonorName" to null,
            "createdAt" to req.createdAt,
            "expiresAt" to req.expiresAt,
            "isEmergencyBroadcast" to req.isEmergencyBroadcast
        )

        awaitTask(firestore.collection("blood_requests").document(req.id).set(reqMap))

        // Switch to donor user and accept the request
        val donorUid = signInTestUser("donor@roktosetu.org", "Pass1234!")
        awaitTask(
            firestore.collection("blood_requests").document(req.id).update(
                mapOf(
                    "status" to RequestStatus.DONOR_RESPONDED.name,
                    "acceptedDonorUid" to donorUid,
                    "acceptedDonorName" to "Donor Hero"
                )
            )
        )

        val updatedSnap = awaitTask(firestore.collection("blood_requests").document(req.id).get())
        assertEquals("DONOR_RESPONDED", updatedSnap.getString("status"))
        assertEquals(donorUid, updatedSnap.getString("acceptedDonorUid"))
    }

    @Test
    fun `user can submit valid report against another target but cannot self report or read others reports`() {
        val aliceUid = signInTestUser("alice.reporter@roktosetu.org", "Pass1234!")
        val bobUid = "target_user_bob_99"

        val validReport = mapOf(
            "id" to "rep_test_1",
            "reporterUid" to aliceUid,
            "reporterEmail" to "alice.reporter@roktosetu.org",
            "targetId" to bobUid,
            "targetType" to "USER",
            "category" to "FAKE_REQUEST",
            "details" to "এই ব্যবহারকারী ভুয়া রক্তের অনুরোধ পোস্ট করেছেন বলে সন্দেহ হচ্ছে।",
            "status" to "OPEN",
            "createdAt" to System.currentTimeMillis()
        )

        awaitTask(firestore.collection("reports").document("rep_test_1").set(validReport))

        // Self-report must fail
        val selfReport = validReport.toMutableMap().apply {
            put("id", "rep_self_1")
            put("targetId", aliceUid)
        }
        try {
            awaitTask(firestore.collection("reports").document("rep_self_1").set(selfReport))
            fail("Expected PERMISSION_DENIED when reporting self")
        } catch (e: Exception) {
            val firestoreEx = e as? FirebaseFirestoreException ?: e.cause as? FirebaseFirestoreException
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }

        // Another user cannot read Alice's report
        signInTestUser("charlie.other@roktosetu.org", "Pass1234!")
        try {
            awaitTask(firestore.collection("reports").document("rep_test_1").get())
            fail("Expected PERMISSION_DENIED when reading another user's report")
        } catch (e: Exception) {
            val firestoreEx = e as? FirebaseFirestoreException ?: e.cause as? FirebaseFirestoreException
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }
}

