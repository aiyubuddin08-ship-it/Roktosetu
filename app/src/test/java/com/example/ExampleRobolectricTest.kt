package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.data.firebase.RoktoSetuMessagingService
import com.example.domain.BloodCompatibilityHelper
import com.example.model.BloodGroup
import com.example.model.SystemMetrics
import com.example.model.UserProfile
import com.example.ui.viewmodel.ExtraFeaturesViewModel
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RoktoSetu", appName)
    }

    @Test
    fun `ensureNotificationChannel registers high-importance emergency alert channel`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        RoktoSetuMessagingService.ensureNotificationChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(RoktoSetuMessagingService.CHANNEL_ID_EMERGENCY)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    @Test
    fun `bengali localization resources resolve for FCM and verification strings`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("bn"))
        }
        val bnContext = baseContext.createConfigurationContext(config)
        val bnChannelName = bnContext.getString(R.string.fcm_channel_emergency_name)
        assertTrue(bnChannelName.contains("জরুরি"))
        val bnPickFile = bnContext.getString(R.string.verification_upload_pick_file)
        assertTrue(bnPickFile.contains("ল্যাব রিপোর্ট"))
        val bnSaved = bnContext.getString(R.string.donor_card_saved_success)
        assertTrue(bnSaved.contains("ডোনার কার্ড"))
    }

    @Test
    fun `RBC blood compatibility matrix enforces universal donor and recipient clinical rules`() {
        // O- can donate to every blood group
        BloodGroup.entries.forEach { recipient ->
            assertTrue(
                "O- should be compatible with ${recipient.display}",
                BloodCompatibilityHelper.isCompatible(BloodGroup.O_NEGATIVE, recipient)
            )
        }
        // AB+ can receive from every blood group
        BloodGroup.entries.forEach { donor ->
            assertTrue(
                "${donor.display} should be compatible with AB+",
                BloodCompatibilityHelper.isCompatible(donor, BloodGroup.AB_POSITIVE)
            )
        }
        // A+ cannot donate to O+ or B+
        assertFalse(BloodCompatibilityHelper.isCompatible(BloodGroup.A_POSITIVE, BloodGroup.O_POSITIVE))
        assertFalse(BloodCompatibilityHelper.isCompatible(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE))
    }

    @Test
    fun `ExtraFeaturesViewModel calculates 90-day eligibility and formats offline emergency SMS`() {
        val vm = ExtraFeaturesViewModel()
        val recentDonor = UserProfile(
            uid = "u_recent",
            name = "Nadia Islam",
            bloodGroup = BloodGroup.B_NEGATIVE,
            lastDonationDate = System.currentTimeMillis() - (15L * 24L * 3600L * 1000L),
            donationCount = 3
        )
        val eligibility = vm.calculateEligibility(recentDonor)
        assertFalse(eligibility.isEligible)
        assertTrue(eligibility.daysRemaining in 70..76)
        assertEquals(3, eligibility.totalDonations)

        val sms = vm.createEmergencySmsBody(
            patientBloodGroup = "O-",
            patientName = "Rahim Uddin",
            hospital = "DMCH",
            city = "Dhaka",
            units = 2,
            contact = "01711223344"
        )
        assertTrue(sms.contains("Blood Group: O-"))
        assertTrue(sms.contains("Rahim Uddin (2 Bag/s)"))
        assertTrue(sms.contains("DMCH, Dhaka"))
    }

    @Test
    fun `SystemMetrics data model preserves live telemetry counters`() {
        val metrics = SystemMetrics(
            totalRegisteredUsers = 42,
            activeDonors = 31,
            emergencyDonorsOnline = 14,
            pendingRequests = 5,
            fulfilledRequests = 19,
            verifiedOrganizations = 4,
            totalDonationsCompleted = 68,
            serverStatus = "Operational",
            appCheckStatus = "Initialized"
        )
        assertEquals(42, metrics.totalRegisteredUsers)
        assertEquals(31, metrics.activeDonors)
        assertEquals(68, metrics.totalDonationsCompleted)
        assertEquals("Operational", metrics.serverStatus)
    }
}

