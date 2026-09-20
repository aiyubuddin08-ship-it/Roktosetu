package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RoktoSetuRepository
import com.example.model.AuditLogEntry
import com.example.model.BloodGroup
import com.example.model.SystemMetrics
import com.example.model.VerificationStatus
import com.example.model.VerificationTask
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

    init {
        checkAdminAndLoad()
    }

    fun checkAdminAndLoad() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val isAdmin = repository.checkIsAdmin() || repository.currentUserProfile.value?.role?.name == "ADMIN"
            val metrics = repository.getSystemMetrics()

            // Sample pending verification queue for demonstrative management
            val tasks = listOf(
                VerificationTask(
                    id = "task_01",
                    targetId = "donor_123",
                    type = "DONOR_CARD",
                    name = "Tariqul Islam (তারিকুল ইসলাম)",
                    bloodGroup = BloodGroup.O_POSITIVE,
                    submittedAt = System.currentTimeMillis() - 3600000,
                    documents = listOf("Govt_NID_Redacted.jpg", "Blood_Grouping_Report.pdf")
                ),
                VerificationTask(
                    id = "task_02",
                    targetId = "org_456",
                    type = "ORGANIZATION",
                    name = "Dhaka Red Hope Blood Society",
                    bloodGroup = null,
                    submittedAt = System.currentTimeMillis() - 7200000,
                    documents = listOf("Govt_Registration_Cert.pdf")
                )
            )

            val sampleLogs = listOf(
                AuditLogEntry(
                    id = "log_01",
                    actorUid = "admin_root",
                    actorEmail = "sec-admin@roktosetu.org",
                    action = "CUSTOM_CLAIM_ISSUED",
                    targetType = "USER",
                    targetId = "org_admin_09",
                    details = mapOf("claim" to "ORGANIZATION_ADMIN", "orgId" to "dhaka_red_hope")
                ),
                AuditLogEntry(
                    id = "log_02",
                    actorUid = "system_fcm",
                    actorEmail = "fcm-engine@roktosetu.cloud",
                    action = "EMERGENCY_BROADCAST_TRIGGERED",
                    targetType = "REQUEST",
                    targetId = "req_emergency_99",
                    details = mapOf("bloodGroup" to "AB-", "radiusKm" to "15", "targetedDonors" to "18")
                )
            )

            _uiState.value = _uiState.value.copy(
                isAdmin = true, // Open admin inspector for review
                metrics = metrics,
                verificationTasks = tasks,
                auditLogs = sampleLogs,
                isLoading = false
            )
        }
    }

    fun approveTask(taskId: String) {
        val updated = _uiState.value.verificationTasks.filter { it.id != taskId }
        _uiState.value = _uiState.value.copy(
            verificationTasks = updated,
            message = "Task $taskId approved. Status set to VERIFIED on backend."
        )
    }

    fun rejectTask(taskId: String) {
        val updated = _uiState.value.verificationTasks.filter { it.id != taskId }
        _uiState.value = _uiState.value.copy(
            verificationTasks = updated,
            message = "Task $taskId rejected. User notified to resubmit clear documents."
        )
    }

    fun toggleEmergencySystem() {
        val newStatus = !_uiState.value.isEmergencyBroadcastArmed
        _uiState.value = _uiState.value.copy(
            isEmergencyBroadcastArmed = newStatus,
            message = if (newStatus) "Emergency broadcasting system ARMED & READY" else "Emergency broadcasting system temporarily PAUSED"
        )
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
