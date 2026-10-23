package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.Resource
import com.example.data.repository.RoktoSetuRepository
import com.example.model.AuditLogEntry
import com.example.model.SystemMetrics
import com.example.model.UserRole
import com.example.model.VerificationTask
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val metrics: SystemMetrics = SystemMetrics(),
    val verificationTasks: List<VerificationTask> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val isAdmin: Boolean = false,
    val isEmergencyBroadcastArmed: Boolean = true,
    val isLoading: Boolean = false,
    val message: String? = null
)

class AdminViewModel(
    private val repository: RoktoSetuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null
    private var logsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.currentUserProfile.collect {
                checkAdminAndLoad()
            }
        }
    }

    fun checkAdminAndLoad() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val profile = repository.currentUserProfile.value
            val isRoleAdmin = profile?.role == UserRole.SUPER_ADMIN || profile?.role == UserRole.ORGANIZATION_ADMIN
            val isClaimOrDocAdmin = repository.checkIsAdmin()
            val hasAdminAccess = isRoleAdmin || isClaimOrDocAdmin

            if (!hasAdminAccess) {
                tasksJob?.cancel()
                logsJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    isAdmin = false,
                    verificationTasks = emptyList(),
                    auditLogs = emptyList(),
                    isLoading = false
                )
                return@launch
            }

            val metrics = repository.getSystemMetrics()
            val broadcastArmed = repository.getEmergencyBroadcastArmed()
            _uiState.value = _uiState.value.copy(
                isAdmin = true,
                metrics = metrics,
                isEmergencyBroadcastArmed = broadcastArmed,
                isLoading = false
            )

            tasksJob?.cancel()
            tasksJob = viewModelScope.launch {
                repository.getPendingVerificationTasks(
                    onError = { msg ->
                        _uiState.value = _uiState.value.copy(message = msg)
                    }
                ).collect { tasks ->
                    _uiState.value = _uiState.value.copy(verificationTasks = tasks)
                }
            }

            logsJob?.cancel()
            logsJob = viewModelScope.launch {
                repository.getAuditLogs(
                    onError = { msg ->
                        _uiState.value = _uiState.value.copy(message = msg)
                    }
                ).collect { logs ->
                    _uiState.value = _uiState.value.copy(auditLogs = logs)
                }
            }
        }
    }

    fun approveTask(taskId: String) {
        val task = _uiState.value.verificationTasks.firstOrNull { it.id == taskId } ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.reviewVerificationTask(task, approve = true)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Verification task for ${task.name} approved and recorded in audit trail."
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = res.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun rejectTask(taskId: String, reason: String = "Submitted verification documents were incomplete or unclear.") {
        val task = _uiState.value.verificationTasks.firstOrNull { it.id == taskId } ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.reviewVerificationTask(task, approve = false, reason = reason)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Verification task for ${task.name} rejected."
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = res.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun toggleEmergencySystem() {
        if (!_uiState.value.isAdmin) {
            _uiState.value = _uiState.value.copy(message = "Unauthorized: Administrator privileges required.")
            return
        }
        val newStatus = !_uiState.value.isEmergencyBroadcastArmed
        viewModelScope.launch {
            when (val res = repository.setEmergencyBroadcastArmed(newStatus)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isEmergencyBroadcastArmed = res.data,
                        message = if (res.data) {
                            "Emergency broadcasting system ARMED & READY"
                        } else {
                            "Emergency broadcasting system temporarily PAUSED"
                        }
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

class AdminViewModelFactory(private val repository: RoktoSetuRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
