package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseSecurityHelper
import com.example.data.firebase.Resource
import com.example.data.repository.RoktoSetuRepository
import com.example.data.repository.RoktoSetuRepository.Companion.toBloodRequest
import com.example.data.repository.RoktoSetuRepository.Companion.toPublicDonorView
import com.example.data.repository.RoktoSetuRepository.Companion.toSystemAnnouncement
import com.example.domain.BloodCompatibilityHelper
import com.example.domain.LocationHelper
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.DonationRecord
import com.example.model.DonorAvailability
import com.example.model.PublicDonorView
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.model.SystemAnnouncement
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.VerificationStatus
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class BloodUiState(
    val requests: List<BloodRequest> = emptyList(),
    val donors: List<PublicDonorView> = emptyList(),
    val myDonations: List<DonationRecord> = emptyList(),
    val announcements: List<SystemAnnouncement> = emptyList(),
    val dismissedAnnouncementIds: Set<String> = emptySet(),
    val selectedBloodGroupFilter: BloodGroup? = null,
    val includeCompatibleDonors: Boolean = true,
    val onlyAvailableDonors: Boolean = false,
    val selectedDivision: String = "",
    val selectedDistrict: String = "",
    val selectedUpazila: String = "",
    val areaSearchQuery: String = "",
    val hasMoreDonors: Boolean = false,
    val hasMoreRequests: Boolean = false,
    val isLoadingMoreDonors: Boolean = false,
    val isLoadingMoreRequests: Boolean = false,
    val isOffline: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BloodViewModel(
    private val repository: RoktoSetuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BloodUiState())
    val uiState: StateFlow<BloodUiState> = _uiState.asStateFlow()

    val isOnline: StateFlow<Boolean> = repository.isOnline

    private var requestsJob: Job? = null
    private var donorsJob: Job? = null
    private var donationsJob: Job? = null
    private var announcementsJob: Job? = null
    private var rawAnnouncements: List<SystemAnnouncement> = emptyList()

    private var lastDonorSnapshot: DocumentSnapshot? = null
    private var lastRequestSnapshot: DocumentSnapshot? = null

    init {
        viewModelScope.launch {
            repository.getDismissedAnnouncementIds().collect { dismissedIds ->
                _uiState.value = _uiState.value.copy(dismissedAnnouncementIds = dismissedIds)
                refreshFilteredAnnouncements()
            }
        }
        viewModelScope.launch {
            var wasOffline = false
            repository.isOnline.collect { online ->
                _uiState.value = _uiState.value.copy(isOffline = !online)
                if (online && wasOffline) {
                    retryLoad()
                }
                wasOffline = !online
            }
        }
        viewModelScope.launch {
            repository.currentUserProfile.collect {
                loadRequests()
                loadDonors()
                loadMyDonations()
                loadAnnouncements()
                refreshFilteredAnnouncements()
            }
        }
    }

    fun setBloodGroupFilter(bloodGroup: BloodGroup?) {
        lastDonorSnapshot = null
        lastRequestSnapshot = null
        _uiState.value = _uiState.value.copy(selectedBloodGroupFilter = bloodGroup)
        loadRequests()
        loadDonors()
    }

    fun setIncludeCompatibleDonors(includeCompatible: Boolean) {
        lastDonorSnapshot = null
        _uiState.value = _uiState.value.copy(includeCompatibleDonors = includeCompatible)
        loadDonors()
    }

    fun setOnlyAvailableDonors(onlyAvailable: Boolean) {
        lastDonorSnapshot = null
        _uiState.value = _uiState.value.copy(onlyAvailableDonors = onlyAvailable)
        loadDonors()
    }

    fun setDivisionFilter(division: String) {
        lastDonorSnapshot = null
        lastRequestSnapshot = null
        val nextDivision = if (division == "All") "" else division
        _uiState.value = _uiState.value.copy(
            selectedDivision = nextDivision,
            selectedDistrict = "",
            selectedUpazila = ""
        )
        loadRequests()
        loadDonors()
    }

    fun setDistrictFilter(district: String) {
        lastDonorSnapshot = null
        lastRequestSnapshot = null
        val nextDistrict = if (district == "All") "" else district
        _uiState.value = _uiState.value.copy(
            selectedDistrict = nextDistrict,
            selectedUpazila = ""
        )
        loadRequests()
        loadDonors()
    }

    fun setUpazilaFilter(upazila: String) {
        lastDonorSnapshot = null
        lastRequestSnapshot = null
        val nextUpazila = if (upazila == "All") "" else upazila
        _uiState.value = _uiState.value.copy(selectedUpazila = nextUpazila)
        loadRequests()
        loadDonors()
    }

    fun setAreaSearchQuery(area: String) {
        lastDonorSnapshot = null
        lastRequestSnapshot = null
        _uiState.value = _uiState.value.copy(areaSearchQuery = area)
        loadRequests()
        loadDonors()
    }

    fun clearFilters() {
        lastDonorSnapshot = null
        lastRequestSnapshot = null
        _uiState.value = _uiState.value.copy(
            selectedBloodGroupFilter = null,
            includeCompatibleDonors = true,
            onlyAvailableDonors = false,
            selectedDivision = "",
            selectedDistrict = "",
            selectedUpazila = "",
            areaSearchQuery = ""
        )
        loadRequests()
        loadDonors()
    }

    fun retryLoad() {
        lastDonorSnapshot = null
        lastRequestSnapshot = null
        _uiState.value = _uiState.value.copy(errorMessage = null)
        loadRequests()
        loadDonors()
        loadMyDonations()
        loadAnnouncements()
    }

    fun loadMoreDonors() {
        val state = _uiState.value
        if (state.isLoadingMoreDonors || !state.hasMoreDonors) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMoreDonors = true)
            when (
                val res = repository.fetchPublicDonorsPage(
                    filterBloodGroup = state.selectedBloodGroupFilter,
                    includeCompatibleForRecipient = state.includeCompatibleDonors,
                    divisionFilter = state.selectedDivision.ifBlank { null },
                    districtFilter = state.selectedDistrict.ifBlank { null },
                    upazilaFilter = state.selectedUpazila.ifBlank { null },
                    areaQuery = state.areaSearchQuery.ifBlank { null },
                    onlyAvailable = state.onlyAvailableDonors,
                    lastSnapshot = lastDonorSnapshot,
                    pageSize = 25L
                )
            ) {
                is Resource.Success -> {
                    lastDonorSnapshot = res.data.lastSnapshot
                    val existingIds = _uiState.value.donors.map { it.uid }.toSet()
                    val merged = _uiState.value.donors + res.data.items.filterNot { it.uid in existingIds }
                    _uiState.value = _uiState.value.copy(
                        donors = merged,
                        hasMoreDonors = res.data.hasMore,
                        isLoadingMoreDonors = false
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMoreDonors = false,
                        errorMessage = res.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun loadMoreRequests() {
        val state = _uiState.value
        if (state.isLoadingMoreRequests || !state.hasMoreRequests) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMoreRequests = true)
            when (
                val res = repository.fetchBloodRequestsPage(
                    filterBloodGroup = state.selectedBloodGroupFilter,
                    divisionFilter = state.selectedDivision.ifBlank { null },
                    districtFilter = state.selectedDistrict.ifBlank { null },
                    upazilaFilter = state.selectedUpazila.ifBlank { null },
                    areaQuery = state.areaSearchQuery.ifBlank { null },
                    lastSnapshot = lastRequestSnapshot,
                    pageSize = 25L
                )
            ) {
                is Resource.Success -> {
                    lastRequestSnapshot = res.data.lastSnapshot
                    val existingIds = _uiState.value.requests.map { it.id }.toSet()
                    val merged = _uiState.value.requests + res.data.items.filterNot { it.id in existingIds }
                    _uiState.value = _uiState.value.copy(
                        requests = merged,
                        hasMoreRequests = res.data.hasMore,
                        isLoadingMoreRequests = false
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMoreRequests = false,
                        errorMessage = res.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun loadAnnouncements() {
        announcementsJob?.cancel()
        announcementsJob = viewModelScope.launch {
            repository.getPublishedAnnouncements().collect { list ->
                if (list.isNotEmpty()) {
                    rawAnnouncements = list
                    repository.cacheAnnouncementsLocally(list)
                    refreshFilteredAnnouncements()
                } else {
                    val cached = repository.getCachedAnnouncements()
                        ?.firstOrNull()
                        ?.map { it.toSystemAnnouncement() }
                        .orEmpty()
                    rawAnnouncements = if (cached.isNotEmpty()) cached else emptyList()
                    refreshFilteredAnnouncements()
                }
            }
        }
    }

    fun dismissAnnouncement(announcementId: String) {
        if (announcementId.isBlank()) return
        val updatedDismissed = _uiState.value.dismissedAnnouncementIds + announcementId
        _uiState.value = _uiState.value.copy(dismissedAnnouncementIds = updatedDismissed)
        refreshFilteredAnnouncements()
        viewModelScope.launch {
            repository.dismissAnnouncement(announcementId)
        }
    }

    private fun refreshFilteredAnnouncements() {
        val profile = repository.currentUserProfile.value
        val dismissed = _uiState.value.dismissedAnnouncementIds
        val filtered = rawAnnouncements
            .asSequence()
            .filter { it.status == "PUBLISHED" && it.id !in dismissed }
            .filter { matchesAnnouncementTarget(it, profile) }
            .sortedByDescending { it.publishedAt ?: it.createdAt }
            .take(3)
            .toList()
        _uiState.value = _uiState.value.copy(announcements = filtered)
    }

    companion object {
        fun matchesAnnouncementTarget(
            announcement: SystemAnnouncement,
            userProfile: UserProfile?
        ): Boolean {
            if (announcement.status != "PUBLISHED") return false

            val audienceMatch = when (announcement.targetAudience.uppercase()) {
                "", "ALL_USERS", "ALL" -> true
                "ACTIVE_DONORS" -> userProfile != null &&
                    userProfile.isDonor &&
                    userProfile.donorAvailability != DonorAvailability.UNAVAILABLE
                "DONORS", "ONLY_DONORS" -> userProfile != null && userProfile.isDonor
                "VERIFIED_DONORS" -> userProfile != null &&
                    userProfile.isDonor &&
                    (userProfile.verificationStatus == VerificationStatus.VERIFIED ||
                        userProfile.role == UserRole.VERIFIED_DONOR)
                "ORGANIZATIONS" -> userProfile != null &&
                    (!userProfile.organizationId.isNullOrBlank() ||
                        userProfile.role == UserRole.ORG_ADMIN ||
                        userProfile.role == UserRole.ORGANIZATION_ADMIN)
                else -> true
            }
            if (!audienceMatch) return false

            val targetDiv = announcement.targetDivision.trim()
            if (targetDiv.isNotBlank() && !targetDiv.equals("All", ignoreCase = true)) {
                val profile = userProfile ?: return false
                val userDiv = profile.location.division.ifBlank {
                    LocationHelper.inferDivisionForDistrict(
                        profile.location.district.ifBlank { profile.location.city }
                    )
                }
                if (!userDiv.equals(targetDiv, ignoreCase = true)) return false
            }

            val targetDist = announcement.targetDistrict.trim()
            if (targetDist.isNotBlank() && !targetDist.equals("All", ignoreCase = true)) {
                val profile = userProfile ?: return false
                val userDist = profile.location.district.ifBlank { profile.location.city }
                if (!userDist.equals(targetDist, ignoreCase = true)) return false
            }

            return true
        }
    }

    fun loadRequests() {
        requestsJob?.cancel()
        requestsJob = viewModelScope.launch {
            val state = _uiState.value
            repository.getBloodRequests(
                filterBloodGroup = state.selectedBloodGroupFilter,
                divisionFilter = state.selectedDivision.ifBlank { null },
                districtFilter = state.selectedDistrict.ifBlank { null },
                upazilaFilter = state.selectedUpazila.ifBlank { null },
                areaQuery = state.areaSearchQuery.ifBlank { null },
                onError = { msg ->
                    _uiState.value = _uiState.value.copy(errorMessage = msg)
                }
            ).collect { list ->
                if (list.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        requests = list,
                        hasMoreRequests = list.size >= 25
                    )
                    repository.cacheRequestsLocally(list)
                } else {
                    val cached = repository.getCachedRequests()?.firstOrNull()?.map { it.toBloodRequest() }.orEmpty()
                    val filteredCached = cached.filter { req ->
                        (state.selectedBloodGroupFilter == null || req.bloodGroup == state.selectedBloodGroupFilter) &&
                            LocationHelper.matchesLocationFilter(
                                itemDivision = req.division,
                                itemDistrict = req.district.ifBlank { req.city },
                                itemUpazila = req.upazila,
                                itemArea = "${req.area} ${req.hospitalName} ${req.hospitalAddress}",
                                filterDivision = state.selectedDivision,
                                filterDistrict = state.selectedDistrict,
                                filterUpazila = state.selectedUpazila,
                                filterAreaQuery = state.areaSearchQuery
                            )
                    }
                    _uiState.value = _uiState.value.copy(
                        requests = if ((!repository.isAuthenticated || _uiState.value.isOffline) && filteredCached.isNotEmpty()) {
                            filteredCached
                        } else {
                            list
                        },
                        hasMoreRequests = false
                    )
                }
            }
        }
    }

    fun loadDonors() {
        donorsJob?.cancel()
        donorsJob = viewModelScope.launch {
            val state = _uiState.value
            repository.getDonors(
                filterBloodGroup = state.selectedBloodGroupFilter,
                includeCompatible = state.includeCompatibleDonors,
                divisionFilter = state.selectedDivision.ifBlank { null },
                districtFilter = state.selectedDistrict.ifBlank { null },
                upazilaFilter = state.selectedUpazila.ifBlank { null },
                areaFilter = state.areaSearchQuery.ifBlank { null },
                onlyAvailable = state.onlyAvailableDonors,
                onError = { msg ->
                    _uiState.value = _uiState.value.copy(errorMessage = msg)
                }
            ).collect { list ->
                if (list.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        donors = list,
                        hasMoreDonors = list.size >= 25
                    )
                    repository.cacheDonorsLocally(list)
                } else {
                    val cached = repository.getCachedDonors()?.firstOrNull()?.map { it.toPublicDonorView() }.orEmpty()
                    val filteredCached = cached.filter { donor ->
                        (state.selectedBloodGroupFilter == null || donor.bloodGroup == state.selectedBloodGroupFilter) &&
                            (!state.onlyAvailableDonors || donor.availability == DonorAvailability.AVAILABLE) &&
                            LocationHelper.matchesLocationFilter(
                                itemDivision = donor.division,
                                itemDistrict = donor.district.ifBlank { donor.city },
                                itemUpazila = donor.upazila,
                                itemArea = donor.area.ifBlank { donor.generalLocation },
                                filterDivision = state.selectedDivision,
                                filterDistrict = state.selectedDistrict,
                                filterUpazila = state.selectedUpazila,
                                filterAreaQuery = state.areaSearchQuery
                            )
                    }
                    _uiState.value = _uiState.value.copy(
                        donors = if ((!repository.isAuthenticated || _uiState.value.isOffline) && filteredCached.isNotEmpty()) {
                            filteredCached
                        } else {
                            list
                        },
                        hasMoreDonors = false
                    )
                }
            }
        }
    }

    fun loadMyDonations() {
        donationsJob?.cancel()
        donationsJob = viewModelScope.launch {
            val uid = repository.currentUserId ?: return@launch
            repository.getUserDonations(
                donorUid = uid,
                onError = { msg ->
                    _uiState.value = _uiState.value.copy(errorMessage = msg)
                }
            ).collect { donations ->
                _uiState.value = _uiState.value.copy(myDonations = donations)
            }
        }
    }

    fun createRequest(
        patientName: String,
        patientAge: Int,
        bloodGroup: BloodGroup,
        unitsRequired: Int,
        hospitalName: String,
        hospitalAddress: String,
        division: String = "Dhaka",
        district: String = "Dhaka",
        upazila: String = "",
        city: String = district,
        area: String = hospitalAddress,
        urgency: RequestUrgency,
        contactPhone: String,
        medicalReason: String,
        isEmergencyBroadcast: Boolean,
        onSuccess: (String) -> Unit
    ) {
        if (patientName.isBlank() || hospitalName.isBlank() || contactPhone.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "রোগীর নাম, হাসপাতালের নাম এবং যোগাযোগের নম্বর দিন")
            return
        }
        if (!FirebaseSecurityHelper.isValidPhoneNumber(contactPhone.trim())) {
            _uiState.value = _uiState.value.copy(errorMessage = "অনুগ্রহ করে সঠিক বাংলাদেশি মোবাইল নম্বর দিন (যেমন 017XXXXXXXX)")
            return
        }
        if (unitsRequired !in 1..20) {
            _uiState.value = _uiState.value.copy(errorMessage = "রক্তের ব্যাগের সংখ্যা ১ থেকে ২০-এর মধ্যে হতে হবে")
            return
        }

        val uid = repository.currentUserId
        if (uid.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "রক্তের অনুরোধ পোস্ট করতে অনুগ্রহ করে লগইন করুন")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val profile = repository.currentUserProfile.value
            val resolvedDistrict = district.trim().ifEmpty { city.trim().ifEmpty { "Dhaka" } }
            val resolvedDivision = division.trim().ifEmpty { "Dhaka" }
            val resolvedArea = area.trim().ifEmpty { hospitalAddress.trim() }
            val request = BloodRequest(
                requesterUid = uid,
                requesterName = profile?.name ?: "নিবন্ধিত ব্যবহারকারী",
                patientName = patientName.trim(),
                patientAge = patientAge.coerceIn(0, 120),
                bloodGroup = bloodGroup,
                unitsRequired = unitsRequired,
                hospitalName = hospitalName.trim(),
                hospitalAddress = hospitalAddress.trim(),
                division = resolvedDivision,
                district = resolvedDistrict,
                upazila = upazila.trim(),
                area = resolvedArea,
                city = resolvedDistrict,
                urgency = urgency,
                contactPhone = contactPhone.trim(),
                medicalReason = medicalReason.trim(),
                isEmergencyBroadcast = isEmergencyBroadcast
            )

            when (val res = repository.createBloodRequest(request)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "রক্তের অনুরোধ প্রকাশিত হয়েছে! $resolvedDistrict জেলার উপযুক্ত রক্তদাতাদের জানানো হয়েছে।"
                    )
                    onSuccess(res.data)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun acceptRequest(requestId: String, onSuccess: () -> Unit) {
        val user = repository.currentUserProfile.value
        if (user == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "রক্তের অনুরোধ গ্রহণ করতে অনুগ্রহ করে লগইন করুন এবং প্রোফাইল সম্পূর্ণ করুন।")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.acceptBloodRequest(requestId, user.uid, user.name, user.bloodGroup)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "রক্তদানের অঙ্গীকার করার জন্য ধন্যবাদ! অনুরোধকারীকে জানানো হয়েছে।"
                    )
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateRequestStatus(requestId: String, status: RequestStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.updateRequestStatus(requestId, status)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "অনুরোধের অবস্থা '${status.bengaliDisplay}' হিসেবে হালনাগাদ করা হয়েছে"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun getCompatibleDonorsForRecipient(
        recipientBlood: BloodGroup,
        includeCompatibleAlternatives: Boolean = true,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null
    ): List<PublicDonorView> {
        val compatibleGroups = if (includeCompatibleAlternatives) {
            BloodCompatibilityHelper.getCompatibleDonorGroups(recipientBlood)
        } else {
            listOf(recipientBlood)
        }
        return _uiState.value.donors
            .filter { donor ->
                compatibleGroups.contains(donor.bloodGroup) &&
                    donor.availability != DonorAvailability.UNAVAILABLE &&
                    LocationHelper.matchesLocationFilter(
                        itemDivision = donor.division,
                        itemDistrict = donor.district.ifBlank { donor.city },
                        itemUpazila = donor.upazila,
                        itemArea = donor.area.ifBlank { donor.generalLocation },
                        filterDivision = divisionFilter,
                        filterDistrict = districtFilter,
                        filterUpazila = upazilaFilter,
                        filterAreaQuery = areaQuery
                    )
            }
            .sortedWith(
                compareBy<PublicDonorView> { it.bloodGroup != recipientBlood }
                    .thenBy { it.availability != DonorAvailability.AVAILABLE }
            )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

class BloodViewModelFactory(private val repository: RoktoSetuRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BloodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BloodViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
