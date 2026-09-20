package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AmbulanceService
import com.example.model.AppLanguage
import com.example.model.BloodBank
import com.example.model.BloodDonationCamp
import com.example.model.BloodGroup
import com.example.model.DonationEligibility
import com.example.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ExtraFeaturesUiState(
    val language: AppLanguage = AppLanguage.BANGLA,
    val bloodBanks: List<BloodBank> = emptyList(),
    val ambulances: List<AmbulanceService> = emptyList(),
    val camps: List<BloodDonationCamp> = emptyList(),
    val searchQuery: String = "",
    val selectedCity: String = "All",
    val statusMessage: String? = null
)

class ExtraFeaturesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ExtraFeaturesUiState())
    val uiState: StateFlow<ExtraFeaturesUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val initialBloodBanks = listOf(
            BloodBank(
                id = "bb-1",
                name = "Quantum Foundation Blood Lab",
                nameBn = "কোয়ান্টাম ফাউন্ডেশন ব্লাড ল্যাব",
                organization = "Quantum Foundation",
                address = "31/V Shilpacharya Zainul Abedin Sarak, Shantinagar",
                city = "Dhaka",
                phone = "+8801714010869",
                emergencyHotline = "+88029351969",
                is24Hours = true,
                availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE, BloodGroup.AB_POSITIVE, BloodGroup.O_NEGATIVE)
            ),
            BloodBank(
                id = "bb-2",
                name = "Bangladesh Red Crescent Society Blood Center",
                nameBn = "বাংলাদেশ রেড ক্রিসেন্ট সোসাইটি রক্ত কেন্দ্র",
                organization = "BDRCS",
                address = "7/5 Aurangzeb Road, Mohammadpur",
                city = "Dhaka",
                phone = "+8801811458524",
                emergencyHotline = "+88029116563",
                is24Hours = true,
                availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE, BloodGroup.B_POSITIVE, BloodGroup.B_NEGATIVE, BloodGroup.O_POSITIVE)
            ),
            BloodBank(
                id = "bb-3",
                name = "Sandhani Central Blood Bank - DMCH",
                nameBn = "সন্ধানী কেন্দ্রীয় রক্ত পরিসঞ্চালন কেন্দ্র - ডিএমসিএইচ",
                organization = "Sandhani",
                address = "Dhaka Medical College Hospital, Secretariate Rd",
                city = "Dhaka",
                phone = "+8801711223344",
                emergencyHotline = "+880255165088",
                is24Hours = true,
                availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.AB_POSITIVE, BloodGroup.O_POSITIVE, BloodGroup.AB_NEGATIVE)
            ),
            BloodBank(
                id = "bb-4",
                name = "Badhan Blood Transfusion Center",
                nameBn = "বাঁধন রক্ত পরিসঞ্চালন কেন্দ্র",
                organization = "Badhan (DU)",
                address = "TSC, University of Dhaka",
                city = "Dhaka",
                phone = "+8801534982674",
                emergencyHotline = "+88029668690",
                is24Hours = true,
                availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE, BloodGroup.AB_POSITIVE)
            ),
            BloodBank(
                id = "bb-5",
                name = "Chittagong Medical College Blood Bank",
                nameBn = "চট্টগ্রাম মেডিকেল কলেজ ব্লাড ব্যাংক",
                organization = "CMCH",
                address = "57 K.B. Fazlul Kader Rd, Chattogram",
                city = "Chattogram",
                phone = "+8801819554433",
                emergencyHotline = "+88031619400",
                is24Hours = true,
                availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE)
            ),
            BloodBank(
                id = "bb-6",
                name = "Rajshahi Medical College Blood Bank",
                nameBn = "রাজশাহী মেডিকেল কলেজ ব্লাড ব্যাংক",
                organization = "RMCH",
                address = "Laxmipur, Rajshahi",
                city = "Rajshahi",
                phone = "+8801712998877",
                emergencyHotline = "+880721772150",
                is24Hours = true,
                availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE, BloodGroup.AB_POSITIVE)
            ),
            BloodBank(
                id = "bb-7",
                name = "Sylhet MAG Osmani Medical Blood Bank",
                nameBn = "সিলেট এমএজি ওসমানী মেডিকেল ব্লাড ব্যাংক",
                organization = "SOMCH",
                address = "Medical Road, Sylhet",
                city = "Sylhet",
                phone = "+8801713445566",
                emergencyHotline = "+880821713667",
                is24Hours = true,
                availableGroups = listOf(BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.O_POSITIVE)
            )
        )

        val initialAmbulances = listOf(
            AmbulanceService(
                id = "amb-1",
                title = "National Emergency Ambulance (৯৯৯)",
                titleBn = "জাতীয় জরুরী অ্যাম্বুলেন্স সেবা",
                provider = "Bangladesh Police & Fire Service",
                coverageArea = "Nationwide (সারাদেশ)",
                phone = "999",
                type = "Government Emergency",
                isAvailable24x7 = true
            ),
            AmbulanceService(
                id = "amb-2",
                title = "Red Crescent Emergency Ambulance",
                titleBn = "রেড ক্রিসেন্ট ইমার্জেন্সি অ্যাম্বুলেন্স",
                provider = "BDRCS",
                coverageArea = "Dhaka & Major Divisions",
                phone = "+8801811458524",
                type = "ICU & Life Support",
                isAvailable24x7 = true
            ),
            AmbulanceService(
                id = "amb-3",
                title = "Alif 24/7 ICU Ambulance Service",
                titleBn = "আলিফ ২৪/৭ আইসিইউ অ্যাম্বুলেন্স",
                provider = "Alif Medical Transport",
                coverageArea = "Dhaka Metropolitan & All Over Bangladesh",
                phone = "+8801713205555",
                type = "AC / Freezing / ICU",
                isAvailable24x7 = true
            ),
            AmbulanceService(
                id = "amb-4",
                title = "Health Batayon Health & Ambulance Hotline (১৬২৬৩)",
                titleBn = "স্বাস্থ্য বাতায়ন অ্যাম্বুলেন্স ও ডাক্তার সহায়তা",
                provider = "Ministry of Health & Family Welfare",
                coverageArea = "Nationwide Toll-Free",
                phone = "16263",
                type = "Govt Telehealth & Referral",
                isAvailable24x7 = true
            ),
            AmbulanceService(
                id = "amb-5",
                title = "Shastya Seba 24/7 Ambulance Network",
                titleBn = "স্বাস্থ্য সেবা ২৪/৭ অ্যাম্বুলেন্স নেটওয়ার্ক",
                provider = "Shastya Seba Ltd",
                coverageArea = "Chattogram, Sylhet, Rajshahi",
                phone = "+8801911123456",
                type = "Cardiac ICU & General AC",
                isAvailable24x7 = true
            )
        )

        val initialCamps = listOf(
            BloodDonationCamp(
                id = "camp-1",
                title = "Dhaka University Campus Voluntary Blood Drive",
                titleBn = "ঢাকা বিশ্ববিদ্যালয় স্বেচ্ছায় রক্তদান ক্যাম্প ২০২৬",
                organizer = "Badhan DU Central Committee",
                location = "TSC Auditorium, Dhaka University",
                date = "25 Sep 2026",
                time = "09:00 AM - 05:00 PM",
                contactPhone = "+8801534982674",
                targetUnits = 200,
                registeredCount = 84,
                description = "Join us in saving lives at the largest campus-wide blood drive. Free blood group testing for all visitors."
            ),
            BloodDonationCamp(
                id = "camp-2",
                title = "Sandhani Youth Blood Donation Camp",
                titleBn = "সন্ধানী যুব রক্তদান ও থ্যালাসেমিয়া সচেতনতা কর্মসূচি",
                organizer = "Sandhani & Red Crescent",
                location = "Dhanmondi Lake Amphitheatre, Dhaka",
                date = "28 Sep 2026",
                time = "10:00 AM - 06:00 PM",
                contactPhone = "+8801711223344",
                targetUnits = 150,
                registeredCount = 42,
                description = "Voluntary blood donation campaign focused on helping thalassemia children and cancer patients."
            ),
            BloodDonationCamp(
                id = "camp-3",
                title = "Chattogram City Life Saver Campaign",
                titleBn = "চট্টগ্রাম সিটি লাইফ সেভার রক্তদান উৎসব",
                organizer = "Chattogram Blood Donors Club",
                location = "MA Aziz Stadium Gate 2, Chattogram",
                date = "02 Oct 2026",
                time = "08:30 AM - 04:30 PM",
                contactPhone = "+8801819554433",
                targetUnits = 120,
                registeredCount = 59,
                description = "Citywide mega donation drive with donor gift packs, certificates, and complimentary health checkup."
            )
        )

        _uiState.update {
            it.copy(
                bloodBanks = initialBloodBanks,
                ambulances = initialAmbulances,
                camps = initialCamps
            )
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _uiState.update { it.copy(language = lang) }
    }

    fun toggleLanguage() {
        val nextLang = if (_uiState.value.language == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
        _uiState.update { it.copy(language = nextLang) }
    }

    fun toggleCampRsvp(campId: String) {
        _uiState.update { current ->
            val updatedCamps = current.camps.map { camp ->
                if (camp.id == campId) {
                    val newRsvp = !camp.isUserRsvp
                    val newCount = if (newRsvp) camp.registeredCount + 1 else (camp.registeredCount - 1).coerceAtLeast(0)
                    camp.copy(isUserRsvp = newRsvp, registeredCount = newCount)
                } else camp
            }
            val target = updatedCamps.find { it.id == campId }
            val msg = if (target?.isUserRsvp == true) {
                if (current.language == AppLanguage.BANGLA) "রক্তদান ক্যাম্পে আপনার অংশগ্রহণ নিশ্চিত হয়েছে!" else "RSVP confirmed! See you at the camp."
            } else {
                if (current.language == AppLanguage.BANGLA) "অংশগ্রহণ বাতিল করা হয়েছে।" else "RSVP removed."
            }
            current.copy(camps = updatedCamps, statusMessage = msg)
        }
    }

    fun addNewCamp(
        titleBn: String,
        organizer: String,
        location: String,
        date: String,
        time: String,
        phone: String,
        targetUnits: Int,
        description: String
    ) {
        val newCamp = BloodDonationCamp(
            id = "camp-${System.currentTimeMillis()}",
            title = titleBn,
            titleBn = titleBn,
            organizer = organizer,
            location = location,
            date = date,
            time = time,
            contactPhone = phone,
            targetUnits = targetUnits,
            registeredCount = 1,
            isUserRsvp = true,
            description = description
        )
        _uiState.update { current ->
            current.copy(
                camps = listOf(newCamp) + current.camps,
                statusMessage = if (current.language == AppLanguage.BANGLA) "নতুন রক্তদান ক্যাম্প সফলভাবে যোগ করা হয়েছে!" else "New Blood Camp published successfully!"
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun calculateEligibility(userProfile: UserProfile?): DonationEligibility {
        val lastDateMs = userProfile?.lastDonationDate
        val totalDonations = userProfile?.donationCount ?: 0

        val healthTips = listOf(
            "রক্তদানের পূর্বে পর্যাপ্ত পানি পান করুন (কমপক্ষে ৫০০ মিলি)।",
            "রক্তদানের আগের রাতে কমপক্ষে ৬-৮ ঘণ্টা ভালো ঘুম নিশ্চিত করুন।",
            "রক্তদানের আগে পুষ্টিকর হালকা খাবার গ্রহণ করুন, খালি পেটে রক্তদান করবেন না।",
            "রক্তদানের পর ১০-১৫ মিনিট বিশ্রাম নিন এবং ভারী কাজ পরিহার করুন।"
        )

        if (lastDateMs == null || lastDateMs <= 0L) {
            return DonationEligibility(
                isEligible = true,
                daysRemaining = 0,
                lastDonationDateString = "এখনও কোনো তথ্য নেই",
                nextEligibleDateString = "আজই রক্তদান করতে পারেন",
                totalDonations = totalDonations,
                healthTips = healthTips
            )
        }

        val requiredIntervalMs = TimeUnit.DAYS.toMillis(90) // 90 days interval
        val nextEligibleDateMs = lastDateMs + requiredIntervalMs
        val currentMs = System.currentTimeMillis()

        val isEligible = currentMs >= nextEligibleDateMs
        val remainingMs = (nextEligibleDateMs - currentMs).coerceAtLeast(0L)
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(remainingMs)

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val lastFormatted = sdf.format(Date(lastDateMs))
        val nextFormatted = sdf.format(Date(nextEligibleDateMs))

        return DonationEligibility(
            isEligible = isEligible,
            daysRemaining = daysRemaining,
            lastDonationDateString = lastFormatted,
            nextEligibleDateString = if (isEligible) "এখনই উপযুক্ত" else nextFormatted,
            totalDonations = totalDonations,
            healthTips = healthTips
        )
    }

    fun createEmergencySmsBody(
        patientBloodGroup: String,
        patientName: String,
        hospital: String,
        city: String,
        units: Int,
        contact: String
    ): String {
        return "URGENT BLOOD NEEDED!\n" +
                "Blood Group: $patientBloodGroup\n" +
                "Patient: $patientName ($units Bag/s)\n" +
                "Hospital: $hospital, $city\n" +
                "Emergency Contact: $contact\n" +
                "Sent via RoktoSetu App. Please call immediately if you or someone you know can donate!"
    }
}
