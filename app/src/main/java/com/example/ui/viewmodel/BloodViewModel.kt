package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.Resource
import com.example.data.repository.RoktoSetuRepository
import com.example.domain.BloodCompatibilityHelper
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.DonationRecord
import com.example.model.PublicDonorView
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BloodUiState(
    val requests: List<BloodRequest> = emptyList(),
    val donors: List<PublicDonorView> = emptyList(),
    val myDonations: List<DonationRecord> = emptyList(),
    val selectedBloodGroupFilter: BloodGroup? = null,
    val selectedCityFilter: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val userLat: Double = 23.8103, // Default Dhaka coordinates for demonstration
    val userLon: Double = 90.4125
)

class BloodViewModel(
    private val repository: RoktoSetuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BloodUiState())
    val uiState: StateFlow<BloodUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
        loadDonors()
        loadMyDonations()
    }

    fun setLocation(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(userLat = lat, userLon = lon)
        loadDonors()
    }

    fun setBloodGroupFilter(bloodGroup: BloodGroup?) {
        _uiState.value = _uiState.value.copy(selectedBloodGroupFilter = bloodGroup)
        loadRequests()
        loadDonors()
    }

    fun setCityFilter(city: String) {
        _uiState.value = _uiState.value.copy(selectedCityFilter = city)
        loadDonors()
    }

    fun loadRequests() {
        viewModelScope.launch {
            repository.getBloodRequests(
                filterBloodGroup = _uiState.value.selectedBloodGroupFilter
            ).collect { list ->
                _uiState.value = _uiState.value.copy(requests = list)
            }
        }
    }

    fun loadDonors() {
        viewModelScope.launch {
            repository.getDonors(
                filterBloodGroup = _uiState.value.selectedBloodGroupFilter,
                cityFilter = _uiState.value.selectedCityFilter.ifBlank { null },
                userLat = _uiState.value.userLat,
                userLon = _uiState.value.userLon
            ).collect { list ->
                _uiState.value = _uiState.value.copy(donors = list)
                repository.cacheDonorsLocally(list)
            }
        }
    }

    fun loadMyDonations() {
        viewModelScope.launch {
            val uid = repository.currentUserId ?: return@launch
            repository.getUserDonations(uid).collect { donations ->
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
        city: String,
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

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val profile = repository.currentUserProfile.value
            val request = BloodRequest(
                requesterUid = repository.currentUserId ?: "anon",
                requesterName = profile?.name ?: "Blood Requester",
                patientName = patientName.trim(),
                patientAge = patientAge,
                bloodGroup = bloodGroup,
                unitsRequired = unitsRequired,
                hospitalName = hospitalName.trim(),
                hospitalAddress = hospitalAddress.trim(),
                city = city.trim().ifEmpty { "Dhaka" },
                urgency = urgency,
                contactPhone = contactPhone.trim(),
                medicalReason = medicalReason.trim(),
                isEmergencyBroadcast = isEmergencyBroadcast
            )

            when (val res = repository.createBloodRequest(request)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Blood request posted! Compatible donors in the area are notified."
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
        val user = repository.currentUserProfile.value ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.acceptBloodRequest(requestId, user.uid, user.name)) {
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

    fun getCompatibleDonorsForRecipient(recipientBlood: BloodGroup): List<PublicDonorView> {
        val compatibleGroups = BloodCompatibilityHelper.getCompatibleDonorGroups(recipientBlood)
        return _uiState.value.donors.filter { compatibleGroups.contains(it.bloodGroup) }
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
