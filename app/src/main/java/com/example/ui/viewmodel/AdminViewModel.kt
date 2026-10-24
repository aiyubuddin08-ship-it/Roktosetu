package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AdminVerificationResult
import com.example.data.firebase.Resource
import com.example.data.repository.RoktoSetuRepository
import com.example.model.AccountStatus
import com.example.model.AmbulanceService
import com.example.model.AuditLogEntry
import com.example.model.BloodBank
import com.example.model.BloodDonationCamp
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.DatabaseProtectionCheck
import com.example.model.DonationRecord
import com.example.model.DonorAvailability
import com.example.model.MetricsLoadState
import com.example.model.Organization
import com.example.model.RequestStatus
import com.example.model.SecurityAlertItem
import com.example.model.SystemAnnouncement
import com.example.model.SystemConfigSettings
import com.example.model.SystemMetrics
import com.example.model.UserProfile
import com.example.model.UserReport
import com.example.model.VerificationStatus
import com.example.model.VerificationTask
import com.example.model.VerifiedSecurityState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val metrics: SystemMetrics = SystemMetrics(loadState = MetricsLoadState.IDLE),
    val adminVerification: AdminVerificationResult? = null,
    val verificationTasks: List<VerificationTask> = emptyList(),
    val allVerificationTasks: List<VerificationTask> = emptyList(),
    val allUsers: List<UserProfile> = emptyList(),
    val allBloodRequests: List<BloodRequest> = emptyList(),
    val allOrganizations: List<Organization> = emptyList(),
    val allDonations: List<DonationRecord> = emptyList(),
    val bloodBanks: List<BloodBank> = emptyList(),
    val ambulances: List<AmbulanceService> = emptyList(),
    val bloodCamps: List<BloodDonationCamp> = emptyList(),
    val reports: List<UserReport> = emptyList(),
    val announcements: List<SystemAnnouncement> = emptyList(),
    val systemConfig: SystemConfigSettings = SystemConfigSettings(),
    val securityAlerts: List<SecurityAlertItem> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val adminRegistry: List<Map<String, Any>> = emptyList(),
    val databaseProtectionChecks: List<DatabaseProtectionCheck> = emptyList(),
    val verifiedSecurityState: VerifiedSecurityState = VerifiedSecurityState.UNVERIFIED,
    val lastSecurityCheckAt: Long? = null,
    val failedAdminCheckAttempts: Int = 0,
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
    private var allTasksJob: Job? = null
    private var logsJob: Job? = null
    private var usersJob: Job? = null
    private var requestsJob: Job? = null
    private var orgsJob: Job? = null
    private var donationsJob: Job? = null
    private var bloodBanksJob: Job? = null
    private var ambulancesJob: Job? = null
    private var bloodCampsJob: Job? = null
    private var reportsJob: Job? = null
    private var announcementsJob: Job? = null
    private var securityAlertsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.currentUserProfile.collect {
                checkAdminAndLoad(forceRefreshToken = true)
            }
        }
    }

    private fun cancelAdminListeners() {
        tasksJob?.cancel()
        allTasksJob?.cancel()
        logsJob?.cancel()
        usersJob?.cancel()
        requestsJob?.cancel()
        orgsJob?.cancel()
        donationsJob?.cancel()
        bloodBanksJob?.cancel()
        ambulancesJob?.cancel()
        bloodCampsJob?.cancel()
        reportsJob?.cancel()
        announcementsJob?.cancel()
        securityAlertsJob?.cancel()
    }

    fun checkAdminAndLoad(forceRefreshToken: Boolean = true, notifyUserOnCheck: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                metrics = _uiState.value.metrics.copy(loadState = MetricsLoadState.LOADING)
            )
            val verification = repository.verifyAdminStatusDetailed(forceRefreshToken)
            val hasAdminAccess = verification.isVerifiedAdmin
            val dbChecks = repository.runDatabaseProtectionChecks(hasAdminAccess)

            if (!hasAdminAccess) {
                cancelAdminListeners()
                val newFailedCount = if (notifyUserOnCheck) {
                    _uiState.value.failedAdminCheckAttempts + 1
                } else {
                    _uiState.value.failedAdminCheckAttempts
                }
                val secState = if (verification.isAuthenticated) {
                    VerifiedSecurityState.PROTECTED
                } else {
                    VerifiedSecurityState.UNVERIFIED
                }
                _uiState.value = _uiState.value.copy(
                    isAdmin = false,
                    adminVerification = verification,
                    verificationTasks = emptyList(),
                    allVerificationTasks = emptyList(),
                    allUsers = emptyList(),
                    allBloodRequests = emptyList(),
                    allOrganizations = emptyList(),
                    allDonations = emptyList(),
                    bloodBanks = emptyList(),
                    ambulances = emptyList(),
                    bloodCamps = emptyList(),
                    reports = emptyList(),
                    announcements = emptyList(),
                    securityAlerts = emptyList(),
                    auditLogs = emptyList(),
                    adminRegistry = emptyList(),
                    databaseProtectionChecks = dbChecks,
                    verifiedSecurityState = secState,
                    lastSecurityCheckAt = verification.checkedAt,
                    failedAdminCheckAttempts = newFailedCount,
                    isLoading = false,
                    message = if (notifyUserOnCheck) {
                        if (!verification.isAuthenticated) {
                            "প্রশাসনিক অনুমতি যাচাই করতে প্রথমে আপনার অ্যাকাউন্টে লগইন করুন।"
                        } else {
                            "এই অ্যাকাউন্টের জন্য সুপার অ্যাডমিন অনুমতি পাওয়া যায়নি।"
                        }
                    } else {
                        _uiState.value.message
                    }
                )
                return@launch
            }

            val metrics = repository.getSystemMetrics()
            val sysConfig = repository.getSystemConfigSettings()
            val adminReg = repository.getAdminRegistryList()

            val computedSecState = when {
                metrics.securityAlertsCount > 0 -> VerifiedSecurityState.WARNING_NEEDED
                dbChecks.all { it.isVerified } -> VerifiedSecurityState.PROTECTED
                else -> VerifiedSecurityState.WARNING_NEEDED
            }

            _uiState.value = _uiState.value.copy(
                isAdmin = true,
                adminVerification = verification,
                metrics = metrics,
                systemConfig = sysConfig,
                isEmergencyBroadcastArmed = sysConfig.emergencyBroadcastArmed,
                adminRegistry = adminReg,
                databaseProtectionChecks = dbChecks,
                verifiedSecurityState = computedSecState,
                lastSecurityCheckAt = verification.checkedAt,
                isLoading = false,
                message = if (notifyUserOnCheck) {
                    "সুপার অ্যাডমিন অনুমতি সফলভাবে যাচাই করা হয়েছে।"
                } else {
                    _uiState.value.message
                }
            )

            startAdminStreams()
        }
    }

    fun reverifyAdminAccess() {
        checkAdminAndLoad(forceRefreshToken = true, notifyUserOnCheck = true)
    }

    fun runSecurityCenterChecks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val verification = repository.verifyAdminStatusDetailed(forceRefreshToken = true)
            val dbChecks = repository.runDatabaseProtectionChecks(verification.isVerifiedAdmin)
            val openAlertCount = _uiState.value.securityAlerts.count { it.reviewStatus == "OPEN" }
            val secState = when {
                !verification.isAuthenticated -> VerifiedSecurityState.UNVERIFIED
                openAlertCount > 0 -> VerifiedSecurityState.WARNING_NEEDED
                dbChecks.any { !it.isVerified && it.id != "admin_action_protection" } -> VerifiedSecurityState.WARNING_NEEDED
                else -> VerifiedSecurityState.PROTECTED
            }
            _uiState.value = _uiState.value.copy(
                adminVerification = verification,
                isAdmin = verification.isVerifiedAdmin,
                databaseProtectionChecks = dbChecks,
                verifiedSecurityState = secState,
                lastSecurityCheckAt = System.currentTimeMillis(),
                isLoading = false,
                message = "নিরাপত্তা ও অনুমতি পরীক্ষা সম্পন্ন হয়েছে।"
            )
        }
    }

    private fun startAdminStreams() {
        cancelAdminListeners()

        tasksJob = viewModelScope.launch {
            repository.getPendingVerificationTasks().collect { tasks ->
                _uiState.value = _uiState.value.copy(verificationTasks = tasks)
            }
        }

        allTasksJob = viewModelScope.launch {
            repository.getAllVerificationTasks().collect { allTasks ->
                _uiState.value = _uiState.value.copy(allVerificationTasks = allTasks)
            }
        }

        logsJob = viewModelScope.launch {
            repository.getAuditLogs().collect { logs ->
                _uiState.value = _uiState.value.copy(auditLogs = logs)
            }
        }

        usersJob = viewModelScope.launch {
            repository.getAllUsersForAdmin().collect { users ->
                _uiState.value = _uiState.value.copy(allUsers = users)
            }
        }

        requestsJob = viewModelScope.launch {
            repository.getAllBloodRequestsForAdmin().collect { requests ->
                _uiState.value = _uiState.value.copy(allBloodRequests = requests)
            }
        }

        orgsJob = viewModelScope.launch {
            repository.getOrganizations().collect { orgs ->
                _uiState.value = _uiState.value.copy(allOrganizations = orgs)
            }
        }

        donationsJob = viewModelScope.launch {
            repository.getAllDonationsForAdmin().collect { donations ->
                _uiState.value = _uiState.value.copy(allDonations = donations)
            }
        }

        bloodBanksJob = viewModelScope.launch {
            repository.getBloodBanks(includeInactive = true).collect { banks ->
                _uiState.value = _uiState.value.copy(bloodBanks = banks)
            }
        }

        ambulancesJob = viewModelScope.launch {
            repository.getAmbulances(includeInactive = true).collect { list ->
                _uiState.value = _uiState.value.copy(ambulances = list)
            }
        }

        bloodCampsJob = viewModelScope.launch {
            repository.getBloodCamps(includeInactive = true).collect { list ->
                _uiState.value = _uiState.value.copy(bloodCamps = list)
            }
        }

        reportsJob = viewModelScope.launch {
            repository.getReportsForAdmin().collect { reports ->
                _uiState.value = _uiState.value.copy(reports = reports)
            }
        }

        announcementsJob = viewModelScope.launch {
            repository.getAnnouncementsForAdmin().collect { list ->
                _uiState.value = _uiState.value.copy(announcements = list)
            }
        }

        securityAlertsJob = viewModelScope.launch {
            repository.getSecurityAlertsForAdmin().collect { alerts ->
                val openCount = alerts.count { it.reviewStatus == "OPEN" }
                val currentSecState = if (openCount > 0) {
                    VerifiedSecurityState.WARNING_NEEDED
                } else if (_uiState.value.adminVerification?.isAuthenticated == true) {
                    VerifiedSecurityState.PROTECTED
                } else {
                    VerifiedSecurityState.UNVERIFIED
                }
                _uiState.value = _uiState.value.copy(
                    securityAlerts = alerts,
                    verifiedSecurityState = currentSecState
                )
            }
        }
    }

    fun refreshMetrics() {
        if (!_uiState.value.isAdmin) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                metrics = _uiState.value.metrics.copy(loadState = MetricsLoadState.LOADING)
            )
            val metrics = repository.getSystemMetrics()
            val sysConfig = repository.getSystemConfigSettings()
            val adminReg = repository.getAdminRegistryList()
            _uiState.value = _uiState.value.copy(
                metrics = metrics,
                systemConfig = sysConfig,
                isEmergencyBroadcastArmed = sysConfig.emergencyBroadcastArmed,
                adminRegistry = adminReg,
                isLoading = false
            )
        }
    }

    fun approveTask(taskId: String, notes: String = "") {
        val task = (_uiState.value.verificationTasks + _uiState.value.allVerificationTasks)
            .firstOrNull { it.id == taskId } ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.reviewVerificationTask(task, approve = true, notes = notes)) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = "${task.name}-এর যাচাই আবেদন অনুমোদিত হয়েছে এবং অডিট লগে সংরক্ষিত হয়েছে।"
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

    fun rejectTask(taskId: String, reason: String = "প্রদানকৃত নথিপত্র অসম্পূর্ণ বা অস্পষ্ট।") {
        val task = (_uiState.value.verificationTasks + _uiState.value.allVerificationTasks)
            .firstOrNull { it.id == taskId } ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.reviewVerificationTask(task, approve = false, reason = reason)) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = "${task.name}-এর যাচাই আবেদন বাতিল করা হয়েছে।"
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

    fun updateUserAccountStatus(targetUser: UserProfile, newStatus: AccountStatus, reason: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.updateUserAccountStatusAsAdmin(targetUser, newStatus, reason)) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = if (newStatus == AccountStatus.SUSPENDED) {
                            "${targetUser.name}-এর অ্যাকাউন্ট স্থগিত করা হয়েছে।"
                        } else {
                            "${targetUser.name}-এর অ্যাকাউন্ট পুনরায় সক্রিয় করা হয়েছে।"
                        }
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateDonorStatus(
        targetUser: UserProfile,
        newAvailability: DonorAvailability,
        newVerificationStatus: VerificationStatus,
        notes: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.updateDonorStatusAsAdmin(targetUser, newAvailability, newVerificationStatus, notes)) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = "${targetUser.name}-এর রক্তদাতা তথ্য হালনাগাদ করা হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateBloodRequestStatus(request: BloodRequest, newStatus: RequestStatus, adminNotes: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.updateBloodRequestAsAdmin(request, newStatus, adminNotes)) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = "রক্তের অনুরোধের অবস্থা '${newStatus.bengaliDisplay}' হিসেবে হালনাগাদ করা হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun expireStaleRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.expireStaleRequestsAsAdmin()) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = if (res.data > 0) {
                            "${res.data}টি মেয়াদোত্তীর্ণ রক্তের অনুরোধ নিষ্পত্তি করা হয়েছে।"
                        } else {
                            "বর্তমানে কোনো মেয়াদোত্তীর্ণ অপেক্ষমাণ অনুরোধ পাওয়া যায়নি।"
                        }
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dispatchEmergencyBroadcast(request: BloodRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.dispatchEmergencyBroadcastAsAdmin(request)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "জরুরি বিজ্ঞপ্তি সফলভাবে ${res.data} জন উপযুক্ত রক্তদাতার কাছে পাঠানো হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun toggleEmergencySystem() {
        if (!_uiState.value.isAdmin) {
            _uiState.value = _uiState.value.copy(message = "অনুমতি নেই: শুধুমাত্র অনুমোদিত প্রশাসকের জন্য প্রযোজ্য।")
            return
        }
        val newStatus = !_uiState.value.isEmergencyBroadcastArmed
        viewModelScope.launch {
            when (val res = repository.setEmergencyBroadcastArmed(newStatus)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isEmergencyBroadcastArmed = res.data,
                        systemConfig = _uiState.value.systemConfig.copy(emergencyBroadcastArmed = res.data),
                        message = if (res.data) {
                            "জরুরি রক্তের বিজ্ঞপ্তি সম্প্রচার ব্যবস্থা সক্রিয় করা হয়েছে।"
                        } else {
                            "জরুরি রক্তের বিজ্ঞপ্তি সম্প্রচার সাময়িকভাবে বিরত করা হয়েছে।"
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

    fun verifyOrganizationDirect(org: Organization, verified: Boolean, reason: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.verifyOrganizationDirectAsAdmin(org, verified, reason)) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = if (verified) {
                            "${org.name} প্রতিষ্ঠানটি যাচাইকৃত হিসেবে অনুমোদিত হয়েছে।"
                        } else {
                            "${org.name} প্রতিষ্ঠানের যাচাই অনুমোদন প্রত্যাহার করা হয়েছে।"
                        }
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun verifyDonationRecordAsAdmin(
        donorUid: String,
        donorName: String,
        bloodGroup: BloodGroup,
        hospitalName: String,
        division: String,
        district: String,
        upazila: String,
        requestId: String? = null,
        unitsDonated: Int = 1
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            val adminUid = repository.currentUserId ?: "admin"
            val record = DonationRecord(
                donorUid = donorUid.trim(),
                donorName = donorName.trim(),
                bloodGroup = bloodGroup,
                hospitalName = hospitalName.trim(),
                division = division.trim().ifBlank { "Dhaka" },
                district = district.trim().ifBlank { "Dhaka" },
                upazila = upazila.trim(),
                city = district.trim().ifBlank { "Dhaka" },
                requestId = requestId?.trim()?.ifBlank { null },
                unitsDonated = unitsDonated.coerceIn(1, 5),
                verifiedByOrgId = adminUid,
                verifiedByOrgName = "রক্তসেতু সুপার অ্যাডমিন"
            )
            when (val res = repository.verifyAndRecordDonation(record)) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = "${donorName}-এর রক্তদান সনদ যাচাই ও রেকর্ড সম্পন্ন হয়েছে (সনদ কোড: ${res.data.certificateCode})।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    // --- Phase 4: Admin Directory Management (Blood Banks, Ambulances, Blood Camps) ---

    fun saveBloodBank(bank: BloodBank) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.saveBloodBankAsAdmin(bank)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "ব্লাড ব্যাংকের তথ্য সফলভাবে সংরক্ষিত হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteBloodBank(bankId: String, bankName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.deleteBloodBankAsAdmin(bankId, bankName)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "ব্লাড ব্যাংকের তথ্য মুছে ফেলা হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun saveAmbulance(ambulance: AmbulanceService) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.saveAmbulanceAsAdmin(ambulance)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "অ্যাম্বুলেন্স সেবার তথ্য সফলভাবে সংরক্ষিত হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteAmbulance(ambulanceId: String, title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.deleteAmbulanceAsAdmin(ambulanceId, title)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "অ্যাম্বুলেন্স সেবার তথ্য মুছে ফেলা হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun saveBloodCamp(camp: BloodDonationCamp) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.saveBloodCampAsAdmin(camp)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "রক্তদান ক্যাম্পের তথ্য সফলভাবে সংরক্ষিত হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteBloodCamp(campId: String, title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.deleteBloodCampAsAdmin(campId, title)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "রক্তদান ক্যাম্পের তথ্য মুছে ফেলা হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun importDefaultDirectoryData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.importDefaultDirectoryDataAsAdmin()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = if (res.data > 0) {
                            "${res.data}টি ডিফল্ট ব্লাড ব্যাংক, অ্যাম্বুলেন্স ও ক্যাম্পের তথ্য সফলভাবে আমদানি করা হয়েছে।"
                        } else {
                            "ডিফল্ট তথ্যগুলো ইতিমধ্যে ডাটাবেসে সংরক্ষিত আছে (কোনো ডুপ্লিকেট তৈরি হয়নি)।"
                        }
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    // --- Phase 3: Complaints / Reports ---

    fun reviewReport(
        report: UserReport,
        newStatus: String,
        resolutionNotes: String,
        actionTaken: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.reviewReportAsAdmin(report, newStatus, resolutionNotes, actionTaken)) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = "অভিযোগের পর্যালোচনা সংরক্ষিত হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun submitSampleOrAdminReport(
        targetUid: String,
        targetName: String,
        targetType: String,
        category: String,
        reasonBn: String,
        details: String,
        severity: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.submitUserReport(targetUid, targetName, targetType, category, reasonBn, details, severity)) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = "অভিযোগ সফলভাবে নথিভুক্ত করা হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun saveOrPublishAnnouncement(
        titleBn: String,
        bodyBn: String,
        category: String,
        targetAudience: String,
        targetDivision: String,
        targetDistrict: String,
        publishNow: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (
                val res = repository.saveOrPublishAnnouncementAsAdmin(
                    titleBn, bodyBn, category, targetAudience, targetDivision, targetDistrict, publishNow
                )
            ) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = if (publishNow) {
                            "বিজ্ঞপ্তি প্রকাশিত হয়েছে এবং ${res.data} জন ব্যবহারকারীর কাছে নোটিফিকেশন পাঠানো হয়েছে।"
                        } else {
                            "বিজ্ঞপ্তি খসড়া হিসেবে সংরক্ষিত হয়েছে।"
                        }
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateSystemConfig(settings: SystemConfigSettings) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.updateSystemConfigSettingsAsAdmin(settings)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        systemConfig = res.data,
                        isEmergencyBroadcastArmed = res.data.emergencyBroadcastArmed,
                        message = "সিস্টেম কনফিগারেশন সফলভাবে সংরক্ষিত হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun reviewSecurityAlert(alert: SecurityAlertItem, resolutionNotes: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            when (val res = repository.reviewSecurityAlertAsAdmin(alert, resolutionNotes)) {
                is Resource.Success -> {
                    val metrics = repository.getSystemMetrics()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        metrics = metrics,
                        message = "নিরাপত্তা সতর্কতা পর্যালোচনা হিসেবে চিহ্নিত ও অডিট লগে সংরক্ষিত হয়েছে।"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = res.message)
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
