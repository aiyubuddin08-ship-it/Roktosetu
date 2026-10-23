package com.example.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.LocationHelper
import com.example.model.BloodGroup
import com.example.model.DonorAvailability
import com.example.model.DonorMilestoneBadge
import com.example.model.VerificationStatus
import com.example.ui.components.BloodGroupBadge
import com.example.ui.theme.BloodBurgundy
import com.example.ui.theme.BloodGold
import com.example.ui.theme.BloodRed
import com.example.ui.theme.BloodSuccessContainer
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BloodViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToOrganizations: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onSignedOut: () -> Unit
) {
    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authUiState.successMessage) {
        authUiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }
    LaunchedEffect(authUiState.errorMessage) {
        authUiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (currentUserProfile != null) "My Profile & Donor ID" else "Guest Profile (অতিথি মোড)",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentUserProfile != null) "ব্যক্তিগত প্রোফাইল ও ডিজিটাল ডোনার কার্ড" else "রক্তসেতু প্ল্যাটফর্ম",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSecurity) {
                        Icon(Icons.Default.Security, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val profile = currentUserProfile

        if (profile == null) {
            // Guest Mode Profile Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Guest Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_guest_profile_hero"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(BloodRed, BloodBurgundy, Color(0xFF4A0E17))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "রক্তসেতু • ROKTOSETU",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BloodGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp
                                    )
                                    Text(
                                        text = "Digital Donor Card Preview",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                                BloodGroupBadge(bloodGroup = BloodGroup.O_POSITIVE, sizeInDp = 50, isHighlighted = true)
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Guest Lifesaver (অতিথি)",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Sign in to unlock your official verified donor ID card and badges",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Call to Action Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "সাইন ইন বা একাউন্ট খুলুন",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "রক্তসেতুর সমস্ত সুবিধা (রক্তের রিকোয়েস্ট পোস্ট করা, রক্তদাতা হিসেবে সাড়া দেওয়া, চ্যাট করা ও ডোনেশন হিস্ট্রি ট্র্যাকিং) পেতে লগইন করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = onNavigateToLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_guest_profile_login"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("লগইন করুন (Sign In)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        OutlinedButton(
                            onClick = onNavigateToRegister,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_guest_profile_register"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("নতুন ডোনার একাউন্ট তৈরি করুন (Register)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Quick Links
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToHistory() }
                                .padding(16.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = BloodGold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("View Milestone Badges Showcase", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSecurity() }
                                .padding(16.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Security & Privacy Guidelines", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            val formattedLoc = LocationHelper.formatGeneralLocation(
                division = profile.location.division,
                district = profile.location.district,
                upazila = profile.location.upazila,
                area = profile.location.area,
                city = profile.location.city
            )

            // Digital Blood Donor Card (Hero Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_digital_donor_id"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(BloodRed, BloodBurgundy, Color(0xFF4A0E17))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "রক্তসেতু • ROKTOSETU",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BloodGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "Official Blood Donor ID",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                            BloodGroupBadge(bloodGroup = profile.bloodGroup, sizeInDp = 54, isHighlighted = true)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedLoc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Verified Donations", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text("${profile.donationCount} Bags Given", style = MaterialTheme.typography.titleSmall.copy(color = BloodGold, fontWeight = FontWeight.Bold))
                            }

                            Surface(
                                color = if (profile.verificationStatus == VerificationStatus.VERIFIED) BloodSuccessGreen else BloodGold,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = profile.verificationStatus.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Verification Explanation & Submission Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About Donor Verification (${profile.verificationStatus.name})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "The VERIFIED badge indicates that a RoktoSetu administrator or partner blood bank has reviewed your blood grouping report. It does NOT replace clinical crossmatching before a blood transfusion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profile.verificationStatus == VerificationStatus.UNVERIFIED ||
                        profile.verificationStatus == VerificationStatus.REJECTED
                    ) {
                        OutlinedButton(
                            onClick = { showVerificationDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_request_donor_verification")
                        ) {
                            Text("Submit Blood Group Report for Admin Verification", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Stats & History Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToHistory() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = BloodGold, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Donation History & Badges", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("View verified medical records & milestone certificates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Settings & Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Donor Availability & Privacy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    Text(
                        text = "Select your current availability status:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val options = listOf(
                            DonorAvailability.AVAILABLE to "Available (উপলব্ধ)",
                            DonorAvailability.RESTING to "Resting (বিশ্রাম)",
                            DonorAvailability.EMERGENCY_ONLY to "Emergency Only",
                            DonorAvailability.UNAVAILABLE to "Unavailable (অনুপলব্ধ)"
                        )
                        items(options) { (status, label) ->
                            FilterChip(
                                selected = profile.donorAvailability == status,
                                onClick = { authViewModel.setDonorAvailability(status) },
                                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (status == DonorAvailability.AVAILABLE) BloodSuccessGreen else MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("chip_avail_${status.name}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Phone Publicly in Directory", fontWeight = FontWeight.SemiBold)
                            Text("When off, requesters can only contact you via secure in-app chat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = profile.showPhonePublicly,
                            onCheckedChange = {
                                authViewModel.updateProfile(profile.copy(showPhonePublicly = it))
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Emergency High-Priority Alerts", fontWeight = FontWeight.SemiBold)
                            Text("Receive push notifications for critical matching patients in your district", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = profile.isEmergencyDonor,
                            onCheckedChange = {
                                authViewModel.updateProfile(profile.copy(isEmergencyDonor = it))
                            }
                        )
                    }
                }
            }

            // Account Actions
            OutlinedButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Contact & Location Info")
            }

            Button(
                onClick = {
                    authViewModel.signOut(onSignedOut)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out Securely")
            }

            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth().testTag("btn_delete_account")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete My Account & Data", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showVerificationDialog) {
            val context = LocalContext.current
            var docRef by remember { mutableStateOf("Lab_Blood_Group_Report.pdf") }
            var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    selectedFileUri = uri
                    docRef = uri.lastPathSegment ?: "Uploaded_Lab_Report"
                }
            }

            AlertDialog(
                onDismissRequest = { showVerificationDialog = false },
                title = { Text("Request Donor Verification", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Upload your pathology lab blood grouping report (PDF, JPG, PNG, or WEBP up to 10 MB) or provide a document reference for administrator review.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth().testTag("btn_pick_verification_file")
                        ) {
                            Text(
                                if (selectedFileUri != null) "Selected: $docRef"
                                else "Choose Report File (PDF / Image)"
                            )
                        }
                        OutlinedTextField(
                            value = docRef,
                            onValueChange = { docRef = it },
                            label = { Text("Document Reference / Lab Report") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedFileUri != null || docRef.isNotBlank()) {
                                authViewModel.uploadAndSubmitVerificationRequest(
                                    context = context,
                                    fileUri = selectedFileUri,
                                    fallbackDocumentRef = docRef.trim()
                                )
                                showVerificationDialog = false
                            }
                        }
                    ) {
                        Text("Upload & Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVerificationDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Account Confirmation Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Are you sure you want to permanently delete your RoktoSetu account? Your donor profile, public listing, notifications, and cached records will be removed.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            authViewModel.deleteAccount(onDeleted = onSignedOut)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Permanently Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Re-authentication Dialog when Firebase Auth requires recent login before account deletion
        if (authUiState.requiresReauthForDelete) {
            var reauthPass by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { authViewModel.dismissReauthDialog() },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.auth_reauth_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.auth_reauth_message),
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = reauthPass,
                            onValueChange = { reauthPass = it },
                            label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.auth_reauth_password_label)) },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("input_reauth_password")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (reauthPass.isNotBlank()) {
                                authViewModel.deleteAccount(reauthPassword = reauthPass, onDeleted = onSignedOut)
                            }
                        },
                        enabled = reauthPass.isNotBlank() && !authUiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("btn_confirm_reauth_delete")
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.auth_reauth_confirm_btn))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { authViewModel.dismissReauthDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Edit Profile Dialog
        if (showEditDialog) {
            var newName by remember { mutableStateOf(profile.name) }
            var newPhone by remember { mutableStateOf(profile.contactPhone) }
            var newDivision by remember { mutableStateOf(profile.location.division.ifBlank { "Dhaka" }) }
            var newDistrict by remember { mutableStateOf(profile.location.district.ifBlank { profile.location.city.ifBlank { "Dhaka" } }) }
            var newUpazila by remember { mutableStateOf(profile.location.upazila) }
            var newArea by remember { mutableStateOf(profile.location.area) }

            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Profile Details", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("Contact Phone") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newDivision,
                            onValueChange = { newDivision = it },
                            label = { Text("Division (বিভাগ)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newDistrict,
                            onValueChange = { newDistrict = it },
                            label = { Text("District (জেলা)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newUpazila,
                            onValueChange = { newUpazila = it },
                            label = { Text("Upazila / Thana (উপজেলা/থানা)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newArea,
                            onValueChange = { newArea = it },
                            label = { Text("Area / Locality (এলাকা)") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updated = profile.copy(
                                name = newName.trim(),
                                contactPhone = newPhone.trim(),
                                location = profile.location.copy(
                                    division = newDivision.trim().ifBlank { "Dhaka" },
                                    district = newDistrict.trim().ifBlank { "Dhaka" },
                                    upazila = newUpazila.trim(),
                                    city = newDistrict.trim().ifBlank { "Dhaka" },
                                    area = newArea.trim()
                                )
                            )
                            authViewModel.updateProfile(updated)
                            showEditDialog = false
                        }
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationHistoryScreen(
    bloodViewModel: BloodViewModel,
    onNavigateBack: () -> Unit
) {
    val bloodState by bloodViewModel.uiState.collectAsState()
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Donation History & Badges", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Milestone Badges Showcase
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Milestone Badges (সম্মাননা ব্যাজ)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(DonorMilestoneBadge.entries) { badge ->
                                Card(
                                    modifier = Modifier.width(110.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (bloodState.myDonations.size >= badge.minDonations)
                                            BloodSuccessContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MilitaryTech,
                                            contentDescription = null,
                                            tint = if (bloodState.myDonations.size >= badge.minDonations) BloodGold else Color.Gray,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(badge.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                        Text("${badge.minDonations}+ Bags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Verified Medical Donations Record (${bloodState.myDonations.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (bloodState.myDonations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = BloodRed, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No donation records yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "When you donate blood at a partner hospital or NGO, your record will be securely verified and logged here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(bloodState.myDonations) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BloodGroupBadge(bloodGroup = record.bloodGroup, sizeInDp = 46)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.hospitalName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Date: ${sdf.format(Date(record.donationDate))} • ${record.district.ifBlank { record.city }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Verified by: ${record.verifiedByOrgName}", style = MaterialTheme.typography.labelSmall, color = BloodSuccessGreen, fontWeight = FontWeight.SemiBold)
                            }
                            Surface(color = BloodSuccessContainer, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "${record.unitsDonated} Bag",
                                    color = BloodSuccessGreen,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
