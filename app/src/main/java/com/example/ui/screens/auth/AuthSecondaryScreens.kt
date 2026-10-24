package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.LocationHelper
import com.example.model.BloodGroup
import com.example.model.DonorAvailability
import com.example.ui.theme.BloodRed
import com.example.ui.viewmodel.AuthViewModel

// ==========================================
// 3. EMAIL VERIFICATION SCREEN
// ==========================================

@Composable
fun EmailVerificationScreen(
    authViewModel: AuthViewModel,
    onVerifiedSuccess: () -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val userProfile by authViewModel.currentUserProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.auth_verify_email_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.auth_verify_email_subtitle,
                    userProfile?.email?.ifBlank { "your registered email" } ?: "your registered email"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    authViewModel.checkEmailVerification(onVerified = onVerifiedSuccess)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_check_verification"),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.auth_btn_verified), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { authViewModel.resendVerificationEmail() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_resend_verification"),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading && uiState.resendCooldownSeconds == 0
            ) {
                if (uiState.resendCooldownSeconds > 0) {
                    Text(stringResource(R.string.auth_btn_resend_cooldown, uiState.resendCooldownSeconds))
                } else {
                    Text(stringResource(R.string.auth_btn_resend))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = { authViewModel.signOut(onSignOut) },
                modifier = Modifier.testTag("btn_auth_signout")
            ) {
                Text("Sign Out / Switch Account", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ==========================================
// 4. FORGOT PASSWORD SCREEN
// ==========================================

@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Enter your registered email address and we will send you instructions to safely reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reset_email"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { authViewModel.sendPasswordReset(email) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_submit_reset"),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text("Send Reset Link", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_back_to_login"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Sign In")
            }
        }
    }
}

// ==========================================
// 5. PROFILE COMPLETION SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCompletionScreen(
    authViewModel: AuthViewModel,
    onCompletionSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val currentProfile by authViewModel.currentUserProfile.collectAsState()

    var name by remember(currentProfile?.uid) { mutableStateOf(currentProfile?.name ?: "") }
    var phone by remember(currentProfile?.uid) { mutableStateOf(currentProfile?.contactPhone ?: "") }
    var dob by remember(currentProfile?.uid) { mutableStateOf(currentProfile?.dateOfBirth ?: "") }
    var division by remember(currentProfile?.uid) { mutableStateOf(currentProfile?.location?.division?.ifBlank { "Dhaka" } ?: "Dhaka") }
    var district by remember(currentProfile?.uid) { mutableStateOf(currentProfile?.location?.district?.ifBlank { "Dhaka" } ?: "Dhaka") }
    var upazila by remember(currentProfile?.uid) { mutableStateOf(currentProfile?.location?.upazila ?: "") }
    var area by remember(currentProfile?.uid) { mutableStateOf(currentProfile?.location?.area ?: "") }
    var selectedBloodGroup by remember(currentProfile?.uid) { mutableStateOf(currentProfile?.bloodGroup ?: BloodGroup.O_POSITIVE) }
    var isDonor by remember(currentProfile?.uid) { mutableStateOf(currentProfile?.isDonor ?: true) }
    var isEmergencyOptIn by remember(currentProfile?.uid) { mutableStateOf(false) }

    var expandedDropdown by remember { mutableStateOf(false) }
    var expandedDivision by remember { mutableStateOf(false) }
    var expandedDistrict by remember { mutableStateOf(false) }

    val availableDistricts = remember(division) {
        LocationHelper.getDistrictsForDivision(division)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Complete Your Profile",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Please provide your blood group and location details to finish setting up your account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Phone *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth (DD/MM/YYYY)") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "${selectedBloodGroup.display} (${selectedBloodGroup.bengaliDisplay})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Blood Group *") },
                            leadingIcon = { Icon(Icons.Default.Bloodtype, contentDescription = null, tint = BloodRed) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            BloodGroup.entries.forEach { bg ->
                                DropdownMenuItem(
                                    text = { Text("${bg.display} - ${bg.bengaliDisplay}") },
                                    onClick = {
                                        selectedBloodGroup = bg
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedDivision,
                            onExpandedChange = { expandedDivision = !expandedDivision },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = division,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Division") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDivision) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDivision,
                                onDismissRequest = { expandedDivision = false }
                            ) {
                                LocationHelper.DIVISIONS.forEach { div ->
                                    DropdownMenuItem(
                                        text = { Text(div) },
                                        onClick = {
                                            division = div
                                            district = LocationHelper.getDistrictsForDivision(div).firstOrNull() ?: div
                                            expandedDivision = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedDistrict,
                            onExpandedChange = { expandedDistrict = !expandedDistrict },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = district,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("District") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDistrict) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDistrict,
                                onDismissRequest = { expandedDistrict = false }
                            ) {
                                availableDistricts.forEach { dist ->
                                    DropdownMenuItem(
                                        text = { Text(dist) },
                                        onClick = {
                                            district = dist
                                            expandedDistrict = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = upazila,
                            onValueChange = { upazila = it },
                            label = { Text("Upazila / Thana") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = area,
                            onValueChange = { area = it },
                            label = { Text("Area") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDonor,
                            onCheckedChange = { isDonor = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Register as Voluntary Blood Donor",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Be discoverable for blood requests in your district",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isDonor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isEmergencyOptIn,
                                onCheckedChange = { isEmergencyOptIn = it }
                            )
                            Text(
                                text = "Opt in to Emergency Push Alerts",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Button(
                        onClick = {
                            authViewModel.completeProfile(
                                name = name,
                                phone = phone,
                                bloodGroup = selectedBloodGroup,
                                dob = dob,
                                isDonor = isDonor,
                                donorAvailability = if (isDonor) DonorAvailability.AVAILABLE else DonorAvailability.UNAVAILABLE,
                                isEmergencyDonor = isDonor && isEmergencyOptIn,
                                division = division,
                                district = district,
                                upazila = upazila,
                                city = district,
                                area = area,
                                onSuccess = onCompletionSuccess
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_submit_profile_completion"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Save & Continue", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { authViewModel.signOut() },
                modifier = Modifier.testTag("btn_profile_completion_signout")
            ) {
                Text("Sign Out / Switch Account", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// 6. ACCOUNT DISABLED / DEACTIVATED SCREEN
// ==========================================

@Composable
fun AccountDisabledScreen(
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Account Deactivated",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your RoktoSetu account has been deactivated or disabled by an administrator in accordance with system safety and security policies.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Need Assistance?",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If you believe this is an error, please contact our support team at support@roktosetu.org or call the RoktoSetu helpline at +880 1900-ROKTO.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { authViewModel.signOut(onSignOut) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}
