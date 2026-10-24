package com.example.ui.screens.security

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.FirebaseSecurityHelper
import com.example.model.AccountStatus
import com.example.model.SecurityAlertItem
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.ReportSubmissionDialog
import com.example.ui.screens.admin.formatAdminTimestamp
import com.example.ui.theme.BloodSuccessContainer
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun SecurityCenterScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    onNavigateToAdminDashboard: () -> Unit = {}
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    SecurityCenterScreen(
        authUiState = authUiState,
        authViewModel = authViewModel,
        adminViewModel = adminViewModel,
        onBackClick = onNavigateBack,
        onLoggedOut = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(
    authUiState: AuthUiState,
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    onBackClick: () -> Unit,
    onLoggedOut: () -> Unit = {}
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val adminState by adminViewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUserProfile.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val isVerifiedSuperAdmin = adminState.isAdmin ||
        currentUser?.role == UserRole.SUPER_ADMIN ||
        currentUser?.role == UserRole.ORGANIZATION_ADMIN

    LaunchedEffect(currentUser?.uid, currentUser?.role) {
        if (currentUser != null && (currentUser?.role == UserRole.SUPER_ADMIN || currentUser?.role == UserRole.ORGANIZATION_ADMIN)) {
            adminViewModel.checkAdminAndLoad(forceRefreshToken = true)
        } else {
            adminViewModel.runSecurityCenterChecks()
        }
    }

    LaunchedEffect(authUiState.successMessage, authUiState.errorMessage, adminState.message) {
        val msg = authUiState.successMessage ?: authUiState.errorMessage ?: adminState.message
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            authViewModel.clearMessages()
            adminViewModel.clearMessage()
        }
    }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var alertToReview by remember { mutableStateOf<SecurityAlertItem?>(null) }
    var showTechnicalDetails by remember { mutableStateOf(false) }

    val isNotificationPermissionGranted = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val isAppCheckActive = FirebaseSecurityHelper.isAppCheckInitialized()
    val lastCheckMs = adminState.lastSecurityCheckAt?.takeIf { it > 0L } ?: System.currentTimeMillis()
    val protectionChecks = adminState.databaseProtectionChecks.ifEmpty {
        defaultDatabaseProtections(lastCheckMs)
    }
    val openSecurityAlerts = adminState.securityAlerts.count { it.reviewStatus == "OPEN" }
    val isEmailVerified = authViewModel.isEmailVerified

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "নিরাপত্তা ও গোপনীয়তা কেন্দ্র",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "অ্যাকাউন্ট সুরক্ষা, গোপনীয়তা ও নিরাপদ রক্তদান",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("security_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            authViewModel.checkEmailVerification()
                            adminViewModel.runSecurityCenterChecks()
                        },
                        modifier = Modifier.testTag("security_refresh_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "পুনরায় যাচাই করুন")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. Overall Security Summary Banner
            item {
                OverallSecuritySummaryBanner(
                    user = currentUser,
                    isEmailVerified = isEmailVerified,
                    openAlertsCount = openSecurityAlerts,
                    lastDiagnosticsTimestamp = lastCheckMs
                )
            }

            // 2. Account Security Section (আমার অ্যাকাউন্টের নিরাপত্তা)
            item {
                AccountSecuritySectionCard(
                    user = currentUser,
                    isEmailVerified = isEmailVerified,
                    onVerifyEmailClick = { authViewModel.resendVerificationEmail() },
                    onRefreshEmailStatusClick = { authViewModel.checkEmailVerification() },
                    onChangePasswordClick = { showChangePasswordDialog = true },
                    onSendPasswordResetEmailClick = {
                        val userEmail = currentUser?.email.orEmpty()
                        if (userEmail.isNotBlank()) {
                            authViewModel.sendPasswordReset(userEmail)
                        }
                    },
                    onLogoutClick = { showLogoutConfirmDialog = true },
                    onDeleteAccountClick = { showDeleteAccountDialog = true }
                )
            }

            // 3. Privacy Switches Section (গোপনীয়তা সুইচ)
            currentUser?.let { activeProfile ->
                item {
                    PrivacySwitchesSectionCard(
                        profile = activeProfile,
                        onUpdatePrivacy = { showPhonePublicly, isDonor, isEmergencyDonor, availability ->
                            authViewModel.updateProfile(
                                activeProfile.copy(
                                    showPhonePublicly = showPhonePublicly,
                                    isDonor = isDonor,
                                    isEmergencyDonor = isEmergencyDonor,
                                    donorAvailability = availability
                                )
                            )
                        }
                    )
                }
            }

            // 4. Database Protection (ডেটাবেস সুরক্ষা — simple 1-2 line reassurance for regular users)
            item {
                DatabaseProtectionSectionCard(
                    checks = protectionChecks,
                    isVerifiedSuperAdmin = isVerifiedSuperAdmin
                )
            }

            // 5. How Your Data Is Used (আপনার তথ্য কীভাবে ব্যবহৃত হয়)
            item {
                HowYourDataIsUsedCard()
            }

            // 6. Safe Blood Search & Donation Tips (নিরাপদে রক্ত খোঁজা/দেওয়ার টিপস)
            item {
                SafeBloodTipsCard()
            }

            // 7. User Report Submission & Own Reports List (রিপোর্ট করুন ও নিজের রিপোর্ট তালিকা)
            item {
                UserReportingSectionCard(
                    myReports = authUiState.myReports,
                    onOpenReportDialog = { showReportDialog = true }
                )
            }

            // 8. Help, Contact & Privacy Policy (সাহায্য ও যোগাযোগ, গোপনীয়তা নীতি ও শর্ত)
            item {
                HelpAndPrivacyPolicyCard()
            }

            // 9. Super Admin Sections (Alerts, Audit History, Jargon-free Diagnostics)
            if (isVerifiedSuperAdmin) {
                item {
                    SecurityAlertsSectionCard(
                        alerts = adminState.securityAlerts,
                        isVerifiedSuperAdmin = true,
                        onMarkReviewed = { alert -> alertToReview = alert }
                    )
                }

                item {
                    SecurityActivityHistoryCard(
                        auditLogs = adminState.auditLogs,
                        isVerifiedSuperAdmin = true
                    )
                }

                item {
                    SuperAdminTechnicalDiagnosticsCard(
                        isAppCheckActive = isAppCheckActive,
                        isNotificationGranted = isNotificationPermissionGranted,
                        hasCustomClaim = adminState.adminVerification?.hasAdminCustomClaim == true,
                        hasRegistryDoc = adminState.adminVerification?.hasAdminRegistryDoc == true,
                        protectionChecks = protectionChecks,
                        lastCheckedAt = lastCheckMs,
                        expanded = showTechnicalDetails,
                        onToggleExpand = { showTechnicalDetails = !showTechnicalDetails },
                        onRunProbe = { adminViewModel.runSecurityCenterChecks() }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showReportDialog) {
        ReportSubmissionDialog(
            currentUserUid = currentUser?.uid,
            initialTargetUid = "",
            initialTargetName = "",
            initialTargetType = "USER",
            onDismiss = { showReportDialog = false },
            onSubmit = { targetUid, targetName, targetType, category, reasonBn, details ->
                authViewModel.submitUserReport(
                    targetUid = targetUid,
                    targetName = targetName,
                    targetType = targetType,
                    category = category,
                    reasonBn = reasonBn,
                    details = details,
                    onResult = { success, _ ->
                        if (success) showReportDialog = false
                    }
                )
            }
        )
    }

    if (showChangePasswordDialog) {
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("পাসওয়ার্ড পরিবর্তন করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "নিরাপত্তার স্বার্থে আপনার বর্তমান পাসওয়ার্ড এবং নতুন শক্তিশালী পাসওয়ার্ড প্রদান করুন।",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("বর্তমান পাসওয়ার্ড *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("নতুন পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর, সংখ্যা ও চিহ্ন) *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("নতুন পাসওয়ার্ড পুনরায় লিখুন *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.changePassword(
                            currentPass = currentPass,
                            newPass = newPass,
                            confirmPass = confirmPass,
                            onSuccess = { showChangePasswordDialog = false }
                        )
                    },
                    enabled = currentPass.isNotBlank() && newPass.length >= 8 && confirmPass.isNotBlank()
                ) {
                    Text("পাসওয়ার্ড পরিবর্তন করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("সেশন থেকে লগআউট নিশ্চিতকরণ", fontWeight = FontWeight.Bold) },
            text = {
                Text("আপনি কি নিশ্চিতভাবে বর্তমান ডিভাইস থেকে লগআউট করতে চান?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        authViewModel.signOut(onSignedOut = onLoggedOut)
                    }
                ) {
                    Text("লগআউট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        var currentPassword by remember { mutableStateOf("") }
        var confirmPhrase by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    text = "স্থায়ীভাবে অ্যাকাউন্ট মুছে ফেলুন",
                    fontWeight = FontWeight.Bold,
                    color = EmergencyRed
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "সতর্কবার্তা: অ্যাকাউন্ট মুছে ফেললে আপনার প্রোফাইল স্থায়ীভাবে মুছে যাবে। নিশ্চিত করতে আপনার বর্তমান পাসওয়ার্ড এবং নিচের বক্সে DELETE লিখুন।",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("বর্তমান পাসওয়ার্ড *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPhrase,
                        onValueChange = { confirmPhrase = it },
                        label = { Text("নিশ্চিত করতে DELETE লিখুন *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.deleteAccount(
                            reauthPassword = currentPassword,
                            onDeleted = {
                                showDeleteAccountDialog = false
                                onLoggedOut()
                            }
                        )
                    },
                    enabled = currentPassword.isNotBlank() && confirmPhrase.trim().equals("DELETE", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("স্থায়ীভাবে মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    alertToReview?.let { targetAlert ->
        var resolutionNote by remember { mutableStateOf("সুপার অ্যাডমিন কর্তৃক পর্যালোচিত ও সমাধানকৃত") }
        AlertDialog(
            onDismissRequest = { alertToReview = null },
            title = { Text("নিরাপত্তা সতর্কতা পর্যালোচনা", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(targetAlert.summaryBn, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = resolutionNote,
                        onValueChange = { resolutionNote = it },
                        label = { Text("পর্যালোচনা ও গৃহীত পদক্ষেপের নোট") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.reviewSecurityAlert(targetAlert, resolutionNote)
                        alertToReview = null
                    }
                ) {
                    Text("পর্যালোচিত হিসেবে সংরক্ষণ")
                }
            },
            dismissButton = {
                TextButton(onClick = { alertToReview = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun OverallSecuritySummaryBanner(
    user: UserProfile?,
    isEmailVerified: Boolean,
    openAlertsCount: Int,
    lastDiagnosticsTimestamp: Long
) {
    val isHealthy = user != null && user.accountStatus == AccountStatus.ACTIVE && openAlertsCount == 0
    val statusTitle = when {
        user == null -> "নিরাপত্তা অবস্থা: লগইন প্রয়োজন"
        user.accountStatus != AccountStatus.ACTIVE -> "সতর্কতা প্রয়োজন: অ্যাকাউন্ট সীমাবদ্ধ"
        !isEmailVerified -> "পর্যালোচনাধীন: ইমেইল যাচাই বাকি আছে"
        openAlertsCount > 0 -> "সতর্কতা প্রয়োজন: $openAlertsCount টি অমীমাংসিত নিরাপত্তা সতর্কতা"
        else -> "সার্বিক নিরাপত্তা অবস্থা: সুরক্ষিত"
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("security_summary_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHealthy) BloodSuccessContainer else EmergencyRedContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isHealthy) BloodSuccessGreen else EmergencyRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHealthy) Icons.Default.VerifiedUser else Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isHealthy) BloodSuccessGreen else EmergencyRed
                    )
                    Text(
                        text = "আপনার অ্যাকাউন্ট, ব্যক্তিগত তথ্য ও যোগাযোগ নিরাপত্তা ব্যবস্থার মাধ্যমে সুরক্ষিত।",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text(
                text = "শেষ নিরাপত্তা পরীক্ষা: ${formatAdminTimestamp(lastDiagnosticsTimestamp)}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun AccountSecuritySectionCard(
    user: UserProfile?,
    isEmailVerified: Boolean,
    onVerifyEmailClick: () -> Unit,
    onRefreshEmailStatusClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onSendPasswordResetEmailClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("account_security_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "আমার অ্যাকাউন্টের নিরাপত্তা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (user == null) {
                Text(
                    text = "অ্যাকাউন্টের বিস্তারিত নিরাপত্তা তথ্য দেখতে অনুগ্রহ করে লগইন করুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SecurityInfoRow(label = "পূর্ণ নাম", value = user.name.ifBlank { "অনির্ধারিত" })
                SecurityInfoRow(label = "ইমেইল ঠিকানা", value = user.email.ifBlank { "প্রদান করা হয়নি" })
                SecurityInfoRow(
                    label = "ইমেইল যাচাই অবস্থা",
                    value = if (isEmailVerified) "যাচাইকৃত ✔" else "অযাচাইকৃত (যাচাই প্রয়োজন)",
                    highlightColor = if (isEmailVerified) BloodSuccessGreen else EmergencyRed
                )
                SecurityInfoRow(
                    label = "অ্যাকাউন্টের অবস্থা",
                    value = if (user.accountStatus == AccountStatus.ACTIVE) "সক্রিয়" else "স্থগিত / সীমাবদ্ধ",
                    highlightColor = if (user.accountStatus == AccountStatus.ACTIVE) BloodSuccessGreen else EmergencyRed
                )

                if (!isEmailVerified && user.email.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onVerifyEmailClick,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ভেরিফিকেশন ইমেইল পাঠান")
                        }
                        OutlinedButton(
                            onClick = onRefreshEmailStatusClick,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("স্ট্যাটাস যাচাই")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onChangePasswordClick,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("btn_change_password")
                    ) {
                        Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পাসওয়ার্ড পরিবর্তন")
                    }
                    OutlinedButton(
                        onClick = onSendPasswordResetEmailClick,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("btn_reset_password_email")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রিসেট ইমেইল")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("btn_security_logout")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("লগআউট করুন")
                    }
                    OutlinedButton(
                        onClick = onDeleteAccountClick,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("btn_delete_account"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("অ্যাকাউন্ট মুছুন")
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityInfoRow(
    label: String,
    value: String,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.55f)
        )
    }
}
