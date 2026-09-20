package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.BloodCompatibilityHelper
import com.example.model.BloodGroup
import com.example.model.DonorAvailability
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.ui.components.AuthRequiredDialog
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.BloodRequestItemCard
import com.example.ui.components.RoktoTopAppBar
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WifiOff
import com.example.model.AppLanguage
import com.example.ui.theme.BloodGold
import com.example.ui.theme.BloodRed
import com.example.ui.theme.BloodSuccessContainer
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BloodViewModel
import com.example.ui.viewmodel.ExtraFeaturesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    bloodViewModel: BloodViewModel,
    extraViewModel: ExtraFeaturesViewModel,
    onNavigateToCreateRequest: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToDonors: () -> Unit,
    onNavigateToOrganizations: () -> Unit,
    onNavigateToDonationHistory: () -> Unit,
    onNavigateToRequestDetails: (String) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToBloodBanks: () -> Unit,
    onNavigateToCamps: () -> Unit,
    onNavigateToDonorCard: () -> Unit,
    onNavigateToOfflineSms: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val bloodState by bloodViewModel.uiState.collectAsState()
    val extraState by extraViewModel.uiState.collectAsState()
    val isBangla = extraState.language == AppLanguage.BANGLA
    val eligibility = extraViewModel.calculateEligibility(currentUserProfile)

    val snackbarHostState = remember { SnackbarHostState() }

    var testRecipientBlood by remember { mutableStateOf<BloodGroup?>(null) }
    var showAuthPromptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bloodState.successMessage) {
        bloodState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            bloodViewModel.clearMessages()
        }
    }

    if (showAuthPromptDialog) {
        AuthRequiredDialog(
            title = if (isBangla) "রক্তের আবেদন করতে সাইন ইন করুন" else "Sign In to Request Blood",
            message = if (isBangla) "রক্তের নতুন আবেদন পোস্ট করতে বা দাতাদের সরাসরি নোটিফিকেশন পাঠাতে অনুগ্রহ করে লগইন করুন বা বিনামূল্যে রেজিস্ট্রেশন করুন।" else "Please sign in or register for free to create blood requests and notify donors.",
            onDismiss = { showAuthPromptDialog = false },
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToRegister = onNavigateToRegister
        )
    }

    Scaffold(
        topBar = {
            RoktoTopAppBar(
                title = "RoktoSetu",
                onNavigateToAdmin = onNavigateToAdmin,
                onNavigateToSecurity = onNavigateToSecurity,
                onNavigateToChat = onNavigateToChat,
                isGuest = currentUserProfile == null,
                onNavigateToAuth = onNavigateToLogin,
                currentLanguageName = if (isBangla) "বাংলা" else "EN",
                onToggleLanguage = { extraViewModel.toggleLanguage() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentUserProfile != null) {
                        onNavigateToCreateRequest()
                    } else {
                        showAuthPromptDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_create_request")
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Post Request")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Request Blood", fontWeight = FontWeight.Bold)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                // Emergency Quick CTA Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToEmergency() }
                        .testTag("card_emergency_cta"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmergencyRed)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emergency Blood Finder",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "জরুরী রক্ত অনুসন্ধান • Find compatible donors nearby instantly",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Button(
                            onClick = onNavigateToEmergency,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("FIND NOW", color = EmergencyRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Guest Welcome Card (Shown if unauthenticated)
            if (currentUserProfile == null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_guest_welcome"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "স্বাগতম রক্তসেতুতে (Welcome to RoktoSetu)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "অতিথি মোডে ব্রাউজ করছেন • Guest Access",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "আপনি সমস্ত রক্তের রিকোয়েস্ট ও রক্তদাতাদের তথ্য উন্মুক্তভাবে দেখতে পারছেন। রক্ত চাইতে বা স্বেচ্ছায় দান করতে সাইন ইন করুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToLogin,
                                    modifier = Modifier.weight(1f).testTag("btn_guest_card_login"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("লগইন (Sign In)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = onNavigateToRegister,
                                    modifier = Modifier.weight(1f).testTag("btn_guest_card_register"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("রেজিস্ট্রেশন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // User Donor Status Bar
            item {
                currentUserProfile?.let { profile ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (profile.isDonor && profile.isEligibleToDonate())
                                BloodSuccessContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BloodGroupBadge(bloodGroup = profile.bloodGroup, sizeInDp = 48)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                val statusDesc = if (!profile.isDonor) {
                                    "Registered as Requester / Non-donor"
                                } else if (!profile.isEligibleToDonate()) {
                                    "Resting (${profile.daysUntilNextEligible()} days left for next donation)"
                                } else if (profile.donorAvailability == DonorAvailability.AVAILABLE) {
                                    "Ready to donate blood • Available"
                                } else {
                                    "Temporarily paused"
                                }
                                Text(
                                    text = statusDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (profile.isDonor) {
                                Switch(
                                    checked = profile.donorAvailability == DonorAvailability.AVAILABLE,
                                    onCheckedChange = { isChecked ->
                                        authViewModel.toggleDonorAvailability(isChecked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BloodSuccessGreen,
                                        checkedTrackColor = BloodSuccessContainer
                                    ),
                                    modifier = Modifier.testTag("switch_donor_status")
                                )
                            }
                        }
                    }
                }
            }

            // Smart Donation Eligibility & Countdown Tracker Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_donation_countdown"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (eligibility.isEligible)
                            BloodSuccessContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (eligibility.isEligible) BloodSuccessGreen else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isBangla) "রক্তদান যোগ্যতা ও কাউন্টডাউন" else "Donation Eligibility Status",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (eligibility.isEligible)
                                            (if (isBangla) "আপনি এখন রক্তদানের জন্য প্রস্তুত!" else "Eligible to donate blood right now!")
                                        else
                                            (if (isBangla) "পুনরায় রক্তদানের জন্য অপেক্ষা করুন" else "Resting period active"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (eligibility.isEligible) BloodSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (eligibility.isEligible) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BloodSuccessGreen)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("READY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("${eligibility.daysRemaining} DAYS LEFT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(if (isBangla) "সর্বশেষ রক্তদান:" else "Last Donated:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(eligibility.lastDonationDateString ?: "N/A", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (isBangla) "পরবর্তী রক্তদানের তারিখ:" else "Next Eligible Date:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(eligibility.nextEligibleDateString ?: "N/A", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Quick Hub Icons - Row 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionTile(
                        icon = Icons.Default.People,
                        label = "Donors",
                        subLabel = if (isBangla) "রক্তদাতা" else "Donors",
                        onClick = onNavigateToDonors
                    )
                    QuickActionTile(
                        icon = Icons.Default.LocalHospital,
                        label = "Banks",
                        subLabel = if (isBangla) "ব্লাড ব্যাংক" else "Banks",
                        onClick = onNavigateToBloodBanks
                    )
                    QuickActionTile(
                        icon = Icons.Default.CalendarMonth,
                        label = "Camps",
                        subLabel = if (isBangla) "ক্যাম্প" else "Camps",
                        onClick = onNavigateToCamps
                    )
                    QuickActionTile(
                        icon = Icons.Default.QrCode,
                        label = "Card",
                        subLabel = if (isBangla) "স্মার্ট কার্ড" else "ID Card",
                        onClick = onNavigateToDonorCard
                    )
                }
            }

            // Quick Hub Icons - Row 2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionTile(
                        icon = Icons.Default.WifiOff,
                        label = "Offline SMS",
                        subLabel = if (isBangla) "অফলাইন" else "SMS Alert",
                        onClick = onNavigateToOfflineSms
                    )
                    QuickActionTile(
                        icon = Icons.Default.CorporateFare,
                        label = "NGOs",
                        subLabel = if (isBangla) "সংস্থা" else "NGOs",
                        onClick = onNavigateToOrganizations
                    )
                    QuickActionTile(
                        icon = Icons.Default.History,
                        label = "History",
                        subLabel = if (isBangla) "ইতিহাস" else "History",
                        onClick = onNavigateToDonationHistory
                    )
                    QuickActionTile(
                        icon = Icons.Default.Security,
                        label = "Security",
                        subLabel = if (isBangla) "নিরাপত্তা" else "Security",
                        onClick = onNavigateToSecurity
                    )
                }
            }

            // Blood Compatibility Quick Checker
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Compatibility Matrix (রক্তের সামঞ্জস্য)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = "Tap a patient blood type to check compatible donors:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(BloodGroup.entries) { bg ->
                                FilterChip(
                                    selected = testRecipientBlood == bg,
                                    onClick = {
                                        testRecipientBlood = if (testRecipientBlood == bg) null else bg
                                    },
                                    label = { Text(bg.display, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        testRecipientBlood?.let { rec ->
                            val compatible = BloodCompatibilityHelper.getCompatibleDonorGroups(rec)
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Patient ${rec.display} can safely receive from:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        compatible.forEach { comp ->
                                            Surface(
                                                color = BloodSuccessContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = comp.display,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BloodSuccessGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Live Blood Requests Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Blood Requests (চলমান রক্তের আবেদন)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${bloodState.requests.size} Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Blood Requests Feed
            if (bloodState.requests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No active requests right now",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "All patients in your area have received assistance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(bloodState.requests) { req ->
                    BloodRequestItemCard(
                        request = req,
                        onAcceptClick = {
                            bloodViewModel.acceptRequest(req.id) {}
                        },
                        onCardClick = {
                            onNavigateToRequestDetails(req.id)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun QuickActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
