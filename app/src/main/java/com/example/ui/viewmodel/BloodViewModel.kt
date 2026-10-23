package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.Resource
import com.example.data.repository.RoktoSetuRepository
import com.example.data.repository.RoktoSetuRepository.Companion.toBloodRequest
import com.example.data.repository.RoktoSetuRepository.Companion.toPublicDonorView
import com.example.domain.BloodCompatibilityHelper
import com.example.domain.LocationHelper
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.DonationRecord
import com.example.model.DonorAvailability
import com.example.model.PublicDonorView
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
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
    val selectedBloodGroupFilter: BloodGroup? = null,
    val includeCompatibleDonors: Boolean = true,
    val onlyAvailableDonors: Boolean = false,
    val selectedDivision: String = "",
    val selectedDistrict: String = "",
    val selectedUpazila: String = "",
    val areaSearchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BloodViewModel(
    private val repository: RoktoSetuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BloodUiState())
    val uiState: StateFlow<BloodUiState> = _uiState.asStateFlow()

    private var requestsJob: Job? = null
    private var donorsJob: Job? = null
    private var donationsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.currentUserProfile.collect {
                loadRequests()
                loadDonors()
                loadMyDonations()
            }
        }
    }

    fun setBloodGroupFilter(bloodGroup: BloodGroup?) {
        _uiState.value = _uiState.value.copy(selectedBloodGroupFilter = bloodGroup)
        loadRequests()
        loadDonors()
    }

    fun setIncludeCompatibleDonors(includeCompatible: Boolean) {
        _uiState.value = _uiState.value.copy(includeCompatibleDonors = includeCompatible)
        loadDonors()
    }

    fun setOnlyAvailableDonors(onlyAvailable: Boolean) {
        _uiState.value = _uiState.value.copy(onlyAvailableDonors = onlyAvailable)
        loadDonors()
    }

    fun setDivisionFilter(division: String) {
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
        val nextDistrict = if (district == "All") "" else district
        _uiState.value = _uiState.value.copy(
            selectedDistrict = nextDistrict,
            selectedUpazila = ""
        )
        loadRequests()
        loadDonors()
    }

    fun setUpazilaFilter(upazila: String) {
        val nextUpazila = if (upazila == "All") "" else upazila
        _uiState.value = _uiState.value.copy(selectedUpazila = nextUpazila)
        loadRequests()
        loadDonors()
    }

    fun setAreaSearchQuery(area: String) {
        _uiState.value = _uiState.value.copy(areaSearchQuery = area)
        loadRequests()
        loadDonors()
    }

    fun clearFilters() {
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
        _uiState.value = _uiState.value.copy(errorMessage = null)
        loadRequests()
        loadDonors()
        loadMyDonations()
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
                    _uiState.value = _uiState.value.copy(requests = list)
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
                        requests = if (!repository.isAuthenticated && filteredCached.isNotEmpty()) filteredCached else list
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
                    _uiState.value = _uiState.value.copy(donors = list)
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
                        donors = if (!repository.isAuthenticated && filteredCached.isNotEmpty()) filteredCached else list
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
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter patient name, hospital, and contact number")
            return
        }
        if (unitsRequired !in 1..20) {
            _uiState.value = _uiState.value.copy(errorMessage = "Units required must be between 1 and 20 bags")
            return
        }

        val uid = repository.currentUserId
        if (uid.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "You must be signed in to post a blood request")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val profile = repository.currentUserProfile.value
            val resolvedDistrict = district.trim().ifEmpty { city.trim().ifEmpty { "Dhaka" } }
            val resolvedDivision = division.trim().ifEmpty { "Dhaka" }
            val request = BloodRequest(
                requesterUid = uid,
                requesterName = profile?.name ?: "Verified User",
                patientName = patientName.trim(),
                patientAge = patientAge.coerceIn(0, 120),
                bloodGroup = bloodGroup,
                unitsRequired = unitsRequired,
                hospitalName = hospitalName.trim(),
                hospitalAddress = hospitalAddress.trim(),
                division = resolvedDivision,
                district = resolvedDistrict,
                upazila = upazila.trim(),
                area = area.trim().ifEmpty { hospitalAddress.trim() },
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
                        successMessage = "Blood request posted! Compatible donors in $resolvedDistrict have been notified."
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
            _uiState.value = _uiState.value.copy(errorMessage = "Please sign in and complete your profile to accept blood requests.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.acceptBloodRequest(requestId, user.uid, user.name, user.bloodGroup)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Thank you for committing to donate! The requester has been notified."
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
                        successMessage = "Request status updated to ${status.display}"
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
                compareBy<PublicDonorView> { it.bloodGroup != recipientBlood } // Exact match first
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
