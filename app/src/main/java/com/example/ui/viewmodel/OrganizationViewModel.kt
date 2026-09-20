package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RoktoSetuRepository
import com.example.model.Campaign
import com.example.model.Organization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrgUiState(
    val organizations: List<Organization> = emptyList(),
    val campaigns: List<Campaign> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCity: String = ""
)

class OrganizationViewModel(
    private val repository: RoktoSetuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrgUiState())
    val uiState: StateFlow<OrgUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            repository.getOrganizations().collect { orgs ->
                _uiState.value = _uiState.value.copy(organizations = orgs)
            }
        }
        viewModelScope.launch {
            repository.getCampaigns().collect { camps ->
                _uiState.value = _uiState.value.copy(campaigns = camps)
            }
        }
    }
}

class OrgViewModelFactory(private val repository: RoktoSetuRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrganizationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrganizationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
