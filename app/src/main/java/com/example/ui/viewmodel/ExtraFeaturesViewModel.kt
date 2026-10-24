package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.DefaultDirectorySeedData
import com.example.data.firebase.Resource
import com.example.data.repository.RoktoSetuRepository
import com.example.data.repository.RoktoSetuRepository.Companion.toAmbulanceService
import com.example.data.repository.RoktoSetuRepository.Companion.toBloodBank
import com.example.data.repository.RoktoSetuRepository.Companion.toBloodDonationCamp
import com.example.model.AmbulanceService
import com.example.model.AppLanguage
import com.example.model.BloodBank
import com.example.model.BloodDonationCamp
import com.example.model.Campaign
import com.example.model.DonationEligibility
import com.example.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

class ExtraFeaturesViewModel(
    private val repository: RoktoSetuRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtraFeaturesUiState())
    val uiState: StateFlow<ExtraFeaturesUiState> = _uiState.asStateFlow()

    private var banksJob: Job? = null
    private var ambulancesJob: Job? = null
    private var campsJob: Job? = null
    private var campaignsJob: Job? = null

    private var remoteCampsList: List<BloodDonationCamp> = emptyList()
    private var orgCampaignsList: List<BloodDonationCamp> = emptyList()

    init {
        if (repository == null) {
            _uiState.update {
                it.copy(
                    bloodBanks = DefaultDirectorySeedData.defaultBloodBanks,
                    ambulances = DefaultDirectorySeedData.defaultAmbulances,
                    camps = DefaultDirectorySeedData.defaultCamps
                )
            }
        } else {
            observeDirectoryStreams()
        }
    }

    private fun observeDirectoryStreams() {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.currentUserProfile.collect {
                loadBloodBanks()
                loadAmbulances()
                loadBloodCampsAndCampaigns()
            }
        }
    }

    private fun loadBloodBanks() {
        val repo = repository ?: return
        banksJob?.cancel()
        banksJob = viewModelScope.launch {
            repo.getBloodBanks(includeInactive = false).collect { remoteBanks ->
                if (remoteBanks.isNotEmpty()) {
                    _uiState.update { it.copy(bloodBanks = remoteBanks) }
                    repo.cacheBloodBanksLocally(remoteBanks)
                } else {
                    val cached = repo.getCachedBloodBanks()
                        ?.firstOrNull()
                        ?.map { it.toBloodBank() }
                        ?.filter { it.isActive }
                        .orEmpty()
                    val fallback = if (cached.isNotEmpty()) cached else DefaultDirectorySeedData.defaultBloodBanks
                    _uiState.update { it.copy(bloodBanks = fallback) }
                }
            }
        }
    }

    private fun loadAmbulances() {
        val repo = repository ?: return
        ambulancesJob?.cancel()
        ambulancesJob = viewModelScope.launch {
            repo.getAmbulances(includeInactive = false).collect { remoteAmbulances ->
                if (remoteAmbulances.isNotEmpty()) {
                    _uiState.update { it.copy(ambulances = remoteAmbulances) }
                    repo.cacheAmbulancesLocally(remoteAmbulances)
                } else {
                    val cached = repo.getCachedAmbulances()
                        ?.firstOrNull()
                        ?.map { it.toAmbulanceService() }
                        ?.filter { it.isActive }
                        .orEmpty()
                    val fallback = if (cached.isNotEmpty()) cached else DefaultDirectorySeedData.defaultAmbulances
                    _uiState.update { it.copy(ambulances = fallback) }
                }
            }
        }
    }

    private fun loadBloodCampsAndCampaigns() {
        val repo = repository ?: return
        campsJob?.cancel()
        campaignsJob?.cancel()

        campsJob = viewModelScope.launch {
            repo.getBloodCamps(includeInactive = false).collect { camps ->
                if (camps.isNotEmpty()) {
                    remoteCampsList = camps
                    repo.cacheBloodCampsLocally(camps)
                } else {
                    val cached = repo.getCachedBloodCamps()
                        ?.firstOrNull()
                        ?.map { it.toBloodDonationCamp() }
                        ?.filter { it.isActive }
                        .orEmpty()
                    remoteCampsList = if (cached.isNotEmpty()) cached else DefaultDirectorySeedData.defaultCamps
                }
                mergeCampsState()
            }
        }

        campaignsJob = viewModelScope.launch {
            repo.getCampaigns().collect { remoteCampaigns ->
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                orgCampaignsList = remoteCampaigns.map { c ->
                    BloodDonationCamp(
                        id = c.id,
                        title = c.title,
                        titleBn = c.title,
                        organizer = c.organizationName.ifBlank { "রক্তদান সংগঠন" },
                        location = if (c.city.isNotBlank()) "${c.location}, ${c.city}" else c.location,
                        district = c.city.ifBlank { "Dhaka" },
                        date = if (c.startDate > 0L) sdf.format(Date(c.startDate)) else "আসন্ন",
                        time = "09:00 AM - 05:00 PM",
                        contactPhone = c.contactPhone,
                        targetUnits = c.targetBags,
                        registeredCount = c.collectedBags,
                        description = c.description
                    )
                }
                mergeCampsState()
            }
        }
    }

    private fun mergeCampsState() {
        val primaryIds = remoteCampsList.map { it.id }.toSet()
        val combined = remoteCampsList + orgCampaignsList.filterNot { it.id in primaryIds }
        _uiState.update { it.copy(camps = combined) }
    }

    fun setLanguage(lang: AppLanguage) {
        _uiState.update { it.copy(language = lang) }
    }

    fun toggleLanguage() {
        val nextLang = if (_uiState.value.language == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
        _uiState.update { it.copy(language = nextLang) }
    }

    fun toggleCampRsvp(campId: String) {
        val currentCamp = _uiState.value.camps.find { it.id == campId } ?: return
        val currentlyRsvped = currentCamp.isUserRsvp
        val repo = repository
        if (repo == null) {
            _uiState.update { current ->
                val updated = current.camps.map { camp ->
                    if (camp.id == campId) {
                        val next = !camp.isUserRsvp
                        val nextCount = if (next) camp.registeredCount + 1 else (camp.registeredCount - 1).coerceAtLeast(0)
                        camp.copy(isUserRsvp = next, registeredCount = nextCount)
                    } else camp
                }
                current.copy(
                    camps = updated,
                    statusMessage = if (!currentlyRsvped) "রক্তদান ক্যাম্পে আপনার অংশগ্রহণ নিশ্চিত হয়েছে!" else "অংশগ্রহণ বাতিল করা হয়েছে।"
                )
            }
            return
        }

        // Optimistic UI update + persistent Firestore RSVP
        _uiState.update { current ->
            val updated = current.camps.map { camp ->
                if (camp.id == campId) {
                    val next = !currentlyRsvped
                    val nextCount = if (next) camp.registeredCount + 1 else (camp.registeredCount - 1).coerceAtLeast(0)
                    camp.copy(isUserRsvp = next, registeredCount = nextCount)
                } else camp
            }
            current.copy(camps = updated)
        }

        viewModelScope.launch {
            when (val res = repo.toggleCampRsvp(campId, currentlyRsvped)) {
                is Resource.Success -> {
                    val msg = if (res.data) {
                        if (_uiState.value.language == AppLanguage.BANGLA) {
                            "রক্তদান ক্যাম্পে আপনার অংশগ্রহণ স্থায়ীভাবে সংরক্ষিত হয়েছে!"
                        } else {
                            "RSVP confirmed and saved!"
                        }
                    } else {
                        if (_uiState.value.language == AppLanguage.BANGLA) {
                            "ক্যাম্পে অংশগ্রহণ বাতিল করা হয়েছে।"
                        } else {
                            "RSVP removed."
                        }
                    }
                    _uiState.update { it.copy(statusMessage = msg) }
                }
                is Resource.Error -> {
                    // Revert optimistic change on failure
                    _uiState.update { current ->
                        val reverted = current.camps.map { camp ->
                            if (camp.id == campId) {
                                camp.copy(isUserRsvp = currentlyRsvped, registeredCount = currentCamp.registeredCount)
                            } else camp
                        }
                        current.copy(camps = reverted, statusMessage = res.message)
                    }
                }
                is Resource.Loading -> {}
            }
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
        val campId = "camp_${System.currentTimeMillis()}"
        val repo = repository
        val district = repo?.currentUserProfile?.value?.location?.district ?: "Dhaka"
        val newCamp = BloodDonationCamp(
            id = campId,
            title = titleBn,
            titleBn = titleBn,
            organizer = organizer,
            location = location,
            district = district,
            date = date,
            time = time,
            contactPhone = phone,
            targetUnits = targetUnits,
            registeredCount = 1,
            isUserRsvp = true,
            description = description
        )
        remoteCampsList = listOf(newCamp) + remoteCampsList
        _uiState.update { current ->
            current.copy(
                camps = listOf(newCamp) + current.camps,
                statusMessage = if (current.language == AppLanguage.BANGLA) "নতুন রক্তদান ক্যাম্প সফলভাবে যোগ করা হয়েছে!" else "New Blood Camp published successfully!"
            )
        }
        if (repo != null) {
            viewModelScope.launch {
                val profile = repo.currentUserProfile.value
                val orgId = profile?.organizationId ?: repo.currentUserId ?: ""
                if (orgId.isNotBlank()) {
                    repo.createCampaign(
                        Campaign(
                            id = campId,
                            organizationId = orgId,
                            organizationName = organizer,
                            title = titleBn,
                            description = description,
                            location = location,
                            city = district,
                            startDate = System.currentTimeMillis(),
                            endDate = System.currentTimeMillis() + 7 * 86400000L,
                            targetBags = targetUnits.coerceAtLeast(1),
                            collectedBags = 1,
                            contactPhone = phone,
                            isActive = true
                        )
                    )
                }
            }
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

        val requiredIntervalMs = TimeUnit.DAYS.toMillis(90)
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

class ExtraFeaturesViewModelFactory(
    private val repository: RoktoSetuRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExtraFeaturesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExtraFeaturesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
