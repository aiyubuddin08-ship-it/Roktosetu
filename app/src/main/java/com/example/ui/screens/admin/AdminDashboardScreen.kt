package com.example.ui.screens.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MetricsLoadState
import com.example.model.SystemMetrics
import com.example.ui.theme.BloodGold
import com.example.ui.theme.BloodSuccessContainer
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel? = null,
    onNavigateToSecurity: () -> Unit = {}
) {
    val uiState by adminViewModel.uiState.collectAsState()
    val authUiState = authViewModel?.uiState?.collectAsState()?.value
    val currentUserProfile = authViewModel?.currentUserProfile?.collectAsState()?.value
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedModuleTab by remember { mutableStateOf(AdminModuleTab.OVERVIEW) }

    // Handle back press when inside a sub-module tab to return to Overview first
    BackHandler(enabled = uiState.isAdmin && selectedModuleTab != AdminModuleTab.OVERVIEW) {
        selectedModuleTab = AdminModuleTab.OVERVIEW
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearMessage()
        }
    }

    LaunchedEffect(authUiState?.errorMessage) {
        authUiState?.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    LaunchedEffect(authUiState?.successMessage) {
        authUiState?.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    val totalPendingAlerts =
        uiState.metrics.securityAlertsCount + uiState.metrics.openReportsCount + uiState.metrics.pendingVerifications

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "রক্তসেতু — সুপার অ্যাডমিন",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        if (uiState.isAdmin) {
                            Text(
                                text = "কেন্দ্রীয় নিয়ন্ত্রণ ও তদারকি কেন্দ্র",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isAdmin && selectedModuleTab != AdminModuleTab.OVERVIEW) {
                                selectedModuleTab = AdminModuleTab.OVERVIEW
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("btn_admin_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    if (uiState.isAdmin) {
                        IconButton(
                            onClick = { selectedModuleTab = AdminModuleTab.COMPLAINTS },
                            modifier = Modifier.testTag("btn_admin_alerts_bell")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (totalPendingAlerts > 0) {
                                        Badge(containerColor = EmergencyRed, contentColor = Color.White) {
                                            Text(if (totalPendingAlerts > 99) "99+" else totalPendingAlerts.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "সতর্কতা ও বিজ্ঞপ্তি")
                            }
                        }
                        IconButton(
                            onClick = { adminViewModel.checkAdminAndLoad(forceRefreshToken = true, notifyUserOnCheck = false) },
                            enabled = !uiState.isLoading,
                            modifier = Modifier.testTag("btn_admin_refresh_data")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "ডেটা রিফ্রেশ করুন")
                        }
                        if (authViewModel != null) {
                            TextButton(
                                onClick = {
                                    authViewModel.signOut()
                                    adminViewModel.checkAdminAndLoad(forceRefreshToken = true, notifyUserOnCheck = false)
                                },
                                modifier = Modifier.testTag("btn_admin_sign_out")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "সাইন আউট",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "লগ-আউট",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val verification = uiState.adminVerification

        if (!uiState.isAdmin) {
            val statusMessage = when {
                verification == null -> "প্রশাসনিক অনুমতি এখনো যাচাই করা হয়নি।"
                !verification.isAuthenticated -> "অ্যাডমিন প্যানেল ব্যবহারের জন্য প্রথমে আপনার অনুমোদিত অ্যাকাউন্টে লগইন করুন।"
                else -> "এই অ্যাকাউন্টে সুপার অ্যাডমিন অনুমোদন পাওয়া যায়নি।"
            }
            val accountEmail = verification?.email?.takeIf { it.isNotBlank() } ?: (currentUserProfile?.email ?: "")

            RestrictedAdminAccessView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                isChecking = uiState.isLoading,
                verificationMessage = statusMessage,
                userEmail = accountEmail,
                isSignedIn = verification?.isAuthenticated == true,
                hasCustomClaim = verification?.hasAdminCustomClaim == true,
                hasRegistryRecord = verification?.hasAdminRegistryDoc == true,
                lastCheckedAt = verification?.checkedAt ?: 0L,
                onReverifyAccess = {
                    adminViewModel.checkAdminAndLoad(forceRefreshToken = true, notifyUserOnCheck = true)
                },
                onReturnHome = onNavigateBack
            )
            return@Scaffold
        }

        val adminDisplayName = currentUserProfile?.name?.takeIf { it.isNotBlank() }
            ?: verification?.displayName?.takeIf { it.isNotBlank() }
            ?: "অনুমোদিত প্রশাসক"

        val adminEmail = currentUserProfile?.email?.takeIf { it.isNotBlank() }
            ?: verification?.email
            ?: ""

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Dashboard Identity & Status Header Card
            AdminIdentityHeaderCard(
                adminName = adminDisplayName,
                adminEmail = adminEmail,
                isVerifiedSuperAdmin = verification?.isVerifiedAdmin == true,
                lastRefreshedAt = uiState.metrics.lastRefreshedAt ?: (verification?.checkedAt ?: 0L),
                alertCount = uiState.metrics.securityAlertsCount,
                pendingReportsCount = uiState.metrics.openReportsCount,
                onSecurityClick = onNavigateToSecurity,
                onComplaintsClick = { selectedModuleTab = AdminModuleTab.COMPLAINTS }
            )

            // 2. Module Navigation Strip
            AdminModuleNavigationBar(
                selectedTab = selectedModuleTab,
                onSelectTab = { tab -> selectedModuleTab = tab },
                onNavigateToSecurity = onNavigateToSecurity
            )

            // 3. Active Module Content
            when (selectedModuleTab) {
                AdminModuleTab.OVERVIEW -> {
                    // Live Statistics Section
                    AdminLiveStatisticsSection(
                        metrics = uiState.metrics,
                        onRefresh = {
                            adminViewModel.checkAdminAndLoad(forceRefreshToken = true, notifyUserOnCheck = false)
                        },
                        onNavigateTab = { selectedModuleTab = it },
                        onNavigateToSecurity = onNavigateToSecurity
                    )

                    // Quick Actions Section (10 Bengali Quick Actions)
                    AdminQuickActionsSection(
                        onSelectTab = { selectedModuleTab = it },
                        onNavigateToSecurity = onNavigateToSecurity
                    )

                    // Recent Pending Verifications Preview
                    if (uiState.verificationTasks.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "যাচাইয়ের অপেক্ষায় থাকা আবেদন (${uiState.verificationTasks.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    TextButton(onClick = { selectedModuleTab = AdminModuleTab.VERIFICATIONS }) {
                                        Text("সব দেখুন")
                                    }
                                }
                                uiState.verificationTasks.take(3).forEach { task ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.name,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${task.type} • জমা: ${formatAdminTimestamp(task.submittedAt)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { selectedModuleTab = AdminModuleTab.VERIFICATIONS }
                                        ) {
                                            Text("পর্যালোচনা", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Recent Audit Activity Preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "সাম্প্রতিক প্রশাসনিক কার্যক্রম",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                TextButton(onClick = { selectedModuleTab = AdminModuleTab.AUDIT_LOGS }) {
                                    Text("সম্পূর্ণ ইতিহাস")
                                }
                            }
                            if (uiState.auditLogs.isEmpty()) {
                                Text(
                                    text = "এখনো কোনো প্রশাসনিক কার্যক্রম রেকর্ড হয়নি।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                uiState.auditLogs.take(4).forEach { log ->
                                    val detailSummary = log.details.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                                        .ifBlank { "${log.targetType} (${log.targetId})" }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = log.action,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Text(
                                                text = detailSummary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = formatAdminTimestamp(log.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AdminModuleTab.USERS_DONORS -> {
                    AdminUsersDonorsModule(adminState = uiState, adminViewModel = adminViewModel)
                }

                AdminModuleTab.BLOOD_REQUESTS -> {
                    AdminBloodRequestsModule(adminState = uiState, adminViewModel = adminViewModel)
                }

                AdminModuleTab.EMERGENCY_BROADCAST -> {
                    AdminEmergencyBroadcastModule(adminState = uiState, adminViewModel = adminViewModel)
                }

                AdminModuleTab.VERIFICATIONS -> {
                    AdminVerificationsModule(adminState = uiState, adminViewModel = adminViewModel)
                }

                AdminModuleTab.ORGANIZATIONS -> {
                    AdminOrganizationsModule(adminState = uiState, adminViewModel = adminViewModel)
                }

                AdminModuleTab.BLOOD_BANKS_CAMPS -> {
                    AdminBloodBanksCampsModule(adminState = uiState, adminViewModel = adminViewModel)
                }

                AdminModuleTab.NOTIFICATIONS -> {
                    AdminNotificationsModule(adminState = uiState, adminViewModel = adminViewModel)
                }

                AdminModuleTab.COMPLAINTS -> {
                    AdminComplaintsModule(adminState = uiState, adminViewModel = adminViewModel)
                }

                AdminModuleTab.SYSTEM_SETTINGS -> {
                    AdminSystemSettingsModule(
                        adminState = uiState,
                        adminViewModel = adminViewModel
                    )
                }

                AdminModuleTab.AUDIT_LOGS -> {
                    AdminAuditLogsModule(auditLogs = uiState.auditLogs)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RestrictedAdminAccessView(
    modifier: Modifier = Modifier,
    isChecking: Boolean,
    verificationMessage: String,
    userEmail: String,
    isSignedIn: Boolean,
    hasCustomClaim: Boolean,
    hasRegistryRecord: Boolean,
    lastCheckedAt: Long,
    onReverifyAccess: () -> Unit,
    onReturnHome: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "প্রবেশাধিকার সীমাবদ্ধ",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "প্রশাসনিক প্রবেশাধিকার সীমাবদ্ধ",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "এই বিভাগটি শুধু অনুমোদিত প্রশাসকদের জন্য",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "অনুমতি যাচাইয়ের বর্তমান অবস্থা",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = verificationMessage.ifBlank { "অ্যাডমিন অনুমোদন নিশ্চিত করা যায়নি।" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                if (userEmail.isNotBlank()) {
                    Text(
                        text = "অ্যাকাউন্ট: $userEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "সেশন অবস্থা: ${if (isSignedIn) "সাইন-ইন সক্রিয়" else "সাইন-ইন করা নেই"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "অ্যাডমিন অনুমতি: ${if (hasCustomClaim) "নিশ্চিত ✔" else "পাওয়া যায়নি"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "প্রশাসক নিবন্ধন তালিকা: ${if (hasRegistryRecord) "নিবন্ধিত" else "নিবন্ধিত নয়"}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (lastCheckedAt > 0L) {
                    Text(
                        text = "সর্বশেষ যাচাই: ${formatAdminTimestamp(lastCheckedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onReverifyAccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_admin_refresh_access"),
            shape = RoundedCornerShape(12.dp),
            enabled = !isChecking
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("অনুমতি যাচাই করা হচ্ছে...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("পুনরায় অনুমতি যাচাই করুন", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onReturnHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_admin_return_home"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("হোমে ফিরে যান", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AdminIdentityHeaderCard(
    adminName: String,
    adminEmail: String,
    isVerifiedSuperAdmin: Boolean,
    lastRefreshedAt: Long,
    alertCount: Int,
    pendingReportsCount: Int,
    onSecurityClick: () -> Unit,
    onComplaintsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = adminName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        if (adminEmail.isNotBlank()) {
                            Text(
                                text = adminEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isVerifiedSuperAdmin) {
                    Surface(
                        color = BloodSuccessContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BloodSuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "সুপার অ্যাডমিন (যাচাইকৃত)",
                                color = BloodSuccessGreen,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lastRefreshedAt > 0L) {
                        "সর্বশেষ তথ্য হালনাগাদ: ${formatAdminTimestamp(lastRefreshedAt)}"
                    } else {
                        "তথ্য হালনাগাদ করা হচ্ছে..."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (alertCount > 0) {
                        Surface(
                            color = EmergencyRedContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onSecurityClick() }
                        ) {
                            Text(
                                text = "নিরাপত্তা সতর্কতা: $alertCount",
                                color = EmergencyRed,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (pendingReportsCount > 0) {
                        Surface(
                            color = BloodGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onComplaintsClick() }
                        ) {
                            Text(
                                text = "নতুন অভিযোগ: $pendingReportsCount",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminModuleNavigationBar(
    selectedTab: AdminModuleTab,
    onSelectTab: (AdminModuleTab) -> Unit,
    onNavigateToSecurity: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdminModuleTab.entries.forEach { tab ->
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                label = { Text(tab.titleBn, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
            )
        }
        FilterChip(
            selected = false,
            onClick = onNavigateToSecurity,
            label = { Text("নিরাপত্তা ও গোপনীয়তা") },
            modifier = Modifier.testTag("admin_tab_security_center")
        )
    }
}

@Composable
private fun AdminLiveStatisticsSection(
    metrics: SystemMetrics,
    onRefresh: () -> Unit,
    onNavigateTab: (AdminModuleTab) -> Unit,
    onNavigateToSecurity: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "সরাসরি পরিসংখ্যান ও বর্তমান অবস্থা",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = when (metrics.loadState) {
                            MetricsLoadState.IDLE, MetricsLoadState.LOADING -> "ডেটাবেস থেকে তথ্য লোড হচ্ছে..."
                            MetricsLoadState.LOADED -> "অনুমোদিত ডেটাবেস থেকে প্রাপ্ত প্রকৃত সংখ্যা"
                            MetricsLoadState.PERMISSION_DENIED -> "তথ্য পড়ার অনুমতি পাওয়া যায়নি"
                            MetricsLoadState.ERROR -> metrics.errorMessage ?: "তথ্য লোড করতে সমস্যা হয়েছে"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (metrics.loadState) {
                    MetricsLoadState.LOADED -> {
                        Surface(color = BloodSuccessContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "যাচাইকৃত তথ্য",
                                color = BloodSuccessGreen,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    MetricsLoadState.IDLE, MetricsLoadState.LOADING -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    MetricsLoadState.PERMISSION_DENIED, MetricsLoadState.ERROR -> {
                        TextButton(onClick = onRefresh) {
                            Text("পুনরায় চেষ্টা করুন")
                        }
                    }
                }
            }

            if (metrics.loadState == MetricsLoadState.PERMISSION_DENIED || metrics.loadState == MetricsLoadState.ERROR) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EmergencyRedContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = metrics.errorMessage
                                ?: "পরিসংখ্যান লোড করা সম্ভব হয়নি। অনুগ্রহ করে সংযোগ ও অনুমতি পরীক্ষা করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmergencyRed
                        )
                    }
                }
            } else {
                val isLoading = metrics.loadState == MetricsLoadState.LOADING || metrics.loadState == MetricsLoadState.IDLE

                // Row 1
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveStatCard(
                        titleBn = "মোট নিবন্ধিত ব্যবহারকারী",
                        count = metrics.totalRegisteredUsers,
                        isLoading = isLoading,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AdminModuleTab.USERS_DONORS) }
                    )
                    LiveStatCard(
                        titleBn = "সক্রিয় রক্তদাতা",
                        count = metrics.activeDonors,
                        isLoading = isLoading,
                        accentColor = BloodSuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AdminModuleTab.USERS_DONORS) }
                    )
                }

                // Row 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveStatCard(
                        titleBn = "যাচাইয়ের অপেক্ষায়",
                        count = metrics.pendingVerifications,
                        isLoading = isLoading,
                        accentColor = BloodGold,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AdminModuleTab.VERIFICATIONS) }
                    )
                    LiveStatCard(
                        titleBn = "চলমান রক্তের অনুরোধ",
                        count = metrics.pendingRequests,
                        isLoading = isLoading,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AdminModuleTab.BLOOD_REQUESTS) }
                    )
                }

                // Row 3
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveStatCard(
                        titleBn = "জরুরি রক্তের অনুরোধ",
                        count = metrics.emergencyRequests,
                        isLoading = isLoading,
                        accentColor = EmergencyRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AdminModuleTab.EMERGENCY_BROADCAST) }
                    )
                    LiveStatCard(
                        titleBn = "সম্পন্ন রক্তদান",
                        count = metrics.totalDonationsCompleted,
                        isLoading = isLoading,
                        accentColor = BloodSuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AdminModuleTab.VERIFICATIONS) }
                    )
                }

                // Row 4
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveStatCard(
                        titleBn = "মেয়াদোত্তীর্ণ অনুরোধ",
                        count = metrics.expiredRequests,
                        isLoading = isLoading,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AdminModuleTab.BLOOD_REQUESTS) }
                    )
                    LiveStatCard(
                        titleBn = "যাচাইয়ের অপেক্ষায় থাকা প্রতিষ্ঠান",
                        count = metrics.pendingOrganizations,
                        isLoading = isLoading,
                        accentColor = BloodGold,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AdminModuleTab.ORGANIZATIONS) }
                    )
                }

                // Row 5
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveStatCard(
                        titleBn = "নতুন অভিযোগ",
                        count = metrics.openReportsCount,
                        isLoading = isLoading,
                        accentColor = EmergencyRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AdminModuleTab.COMPLAINTS) }
                    )
                    LiveStatCard(
                        titleBn = "নিরাপত্তা সতর্কতা",
                        count = metrics.securityAlertsCount,
                        isLoading = isLoading,
                        accentColor = if (metrics.securityAlertsCount > 0) EmergencyRed else BloodSuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSecurity
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStatCard(
    titleBn: String,
    count: Int,
    isLoading: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isLoading) "..." else count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            )
            Text(
                text = titleBn,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private data class QuickActionItem(
    val labelBn: String,
    val subtitleBn: String,
    val icon: ImageVector,
    val color: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
private fun AdminQuickActionsSection(
    onSelectTab: (AdminModuleTab) -> Unit,
    onNavigateToSecurity: () -> Unit
) {
    val actions = listOf(
        QuickActionItem(
            labelBn = "ব্যবহারকারী পরিচালনা",
            subtitleBn = "রক্তদাতা ও প্রোফাইল নিয়ন্ত্রণ",
            icon = Icons.Default.People,
            color = MaterialTheme.colorScheme.primary,
            tag = "qa_users",
            onClick = { onSelectTab(AdminModuleTab.USERS_DONORS) }
        ),
        QuickActionItem(
            labelBn = "রক্তদাতা যাচাই",
            subtitleBn = "আবেদন ও সম্পন্ন রক্তদান অনুমোদন",
            icon = Icons.Default.VerifiedUser,
            color = BloodSuccessGreen,
            tag = "qa_donor_verify",
            onClick = { onSelectTab(AdminModuleTab.VERIFICATIONS) }
        ),
        QuickActionItem(
            labelBn = "রক্তের অনুরোধ পরিচালনা",
            subtitleBn = "জরুরি ও চলমান অনুরোধ তদারকি",
            icon = Icons.Default.Bloodtype,
            color = EmergencyRed,
            tag = "qa_requests",
            onClick = { onSelectTab(AdminModuleTab.BLOOD_REQUESTS) }
        ),
        QuickActionItem(
            labelBn = "জরুরি বিজ্ঞপ্তি",
            subtitleBn = "উপযুক্ত রক্তদাতাদের তাৎক্ষণিক বার্তা",
            icon = Icons.Default.Campaign,
            color = EmergencyRed,
            tag = "qa_emergency_broadcast",
            onClick = { onSelectTab(AdminModuleTab.EMERGENCY_BROADCAST) }
        ),
        QuickActionItem(
            labelBn = "প্রতিষ্ঠান যাচাই",
            subtitleBn = "হাসপাতাল, ব্লাড ব্যাংক ও সংগঠন",
            icon = Icons.Default.AccountBalance,
            color = BloodGold,
            tag = "qa_organizations",
            onClick = { onSelectTab(AdminModuleTab.ORGANIZATIONS) }
        ),
        QuickActionItem(
            labelBn = "ব্লাড ব্যাংক ও ক্যাম্প",
            subtitleBn = "ব্লাড ব্যাংক, অ্যাম্বুলেন্স ও ক্যাম্প নিয়ন্ত্রণ",
            icon = Icons.Default.Bloodtype,
            color = MaterialTheme.colorScheme.primary,
            tag = "qa_blood_banks_camps",
            onClick = { onSelectTab(AdminModuleTab.BLOOD_BANKS_CAMPS) }
        ),
        QuickActionItem(
            labelBn = "নোটিফিকেশন পাঠানো",
            subtitleBn = "সাধারণ ও জেলাভিত্তিক ঘোষণা",
            icon = Icons.Default.NotificationsActive,
            color = MaterialTheme.colorScheme.primary,
            tag = "qa_notifications",
            onClick = { onSelectTab(AdminModuleTab.NOTIFICATIONS) }
        ),
        QuickActionItem(
            labelBn = "অভিযোগ পর্যালোচনা",
            subtitleBn = "ব্যবহারকারীর রিপোর্ট ও ব্যবস্থা গ্রহণ",
            icon = Icons.Default.ReportProblem,
            color = EmergencyRed,
            tag = "qa_complaints",
            onClick = { onSelectTab(AdminModuleTab.COMPLAINTS) }
        ),
        QuickActionItem(
            labelBn = "সিস্টেম সেটিংস",
            subtitleBn = "ফিচার ফ্ল্যাগ, ব্যাকআপ ও বিশ্লেষণ",
            icon = Icons.Default.Settings,
            color = MaterialTheme.colorScheme.secondary,
            tag = "qa_settings",
            onClick = { onSelectTab(AdminModuleTab.SYSTEM_SETTINGS) }
        ),
        QuickActionItem(
            labelBn = "নিরাপত্তা ও গোপনীয়তা",
            subtitleBn = "ডেটাবেস সুরক্ষা ও সতর্কতা কেন্দ্র",
            icon = Icons.Default.Security,
            color = BloodSuccessGreen,
            tag = "qa_security",
            onClick = onNavigateToSecurity
        ),
        QuickActionItem(
            labelBn = "অ্যাডমিন কার্যক্রমের ইতিহাস",
            subtitleBn = "অপরিবর্তনীয় প্রশাসনিক লগ",
            icon = Icons.Default.History,
            color = MaterialTheme.colorScheme.primary,
            tag = "qa_audit_logs",
            onClick = { onSelectTab(AdminModuleTab.AUDIT_LOGS) }
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "দ্রুত নিয়ন্ত্রণ ও পরিচালনা",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            actions.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { item.onClick() }
                                .testTag(item.tag)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(item.color.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.labelBn,
                                        tint = item.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.labelBn,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.subtitleBn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
