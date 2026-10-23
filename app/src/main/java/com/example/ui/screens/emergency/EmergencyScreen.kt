package com.example.ui.screens.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BroadcastOnPersonal
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.BloodCompatibilityHelper
import com.example.domain.LocationHelper
import com.example.model.BloodGroup
import com.example.ui.components.AuthRequiredDialog
import com.example.ui.components.PublicDonorItemCard
import com.example.ui.theme.BloodSuccessContainer
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BloodViewModel
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    authViewModel: AuthViewModel,
    bloodViewModel: BloodViewModel,
    chatViewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChatConversation: (String, String, String) -> Unit,
    onNavigateToCreateRequest: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val bloodState by bloodViewModel.uiState.collectAsState()

    var selectedPatientBlood by remember { mutableStateOf(BloodGroup.O_POSITIVE) }
    var includeCompatibleAlternatives by remember { mutableStateOf(true) }
    var selectedDivision by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf("") }
    var selectedUpazila by remember { mutableStateOf("") }
    var areaQuery by remember { mutableStateOf("") }

    var divisionExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var upazilaExpanded by remember { mutableStateOf(false) }

    var showAuthPromptDialog by remember { mutableStateOf(false) }
    var authPromptMessage by remember { mutableStateOf("এই সুবিধাটি ব্যবহার করতে অনুগ্রহ করে লগইন করুন।") }

    val availableDistricts = remember(selectedDivision) {
        if (selectedDivision.isBlank()) LocationHelper.ALL_DISTRICTS
        else LocationHelper.getDistrictsForDivision(selectedDivision)
    }

    val availableUpazilas = remember(selectedDistrict) {
        if (selectedDistrict.isBlank()) emptyList()
        else LocationHelper.getUpazilasForDistrict(selectedDistrict)
    }

    val compatibleDonors = bloodViewModel.getCompatibleDonorsForRecipient(
        recipientBlood = selectedPatientBlood,
        includeCompatibleAlternatives = includeCompatibleAlternatives,
        divisionFilter = selectedDivision.ifBlank { null },
        districtFilter = selectedDistrict.ifBlank { null },
        upazilaFilter = selectedUpazila.ifBlank { null },
        areaQuery = areaQuery.ifBlank { null }
    )

    val snackbarHostState = remember { SnackbarHostState() }

    if (showAuthPromptDialog) {
        AuthRequiredDialog(
            title = "সাইন ইন প্রয়োজন",
            message = authPromptMessage,
            onDismiss = { showAuthPromptDialog = false },
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToRegister = onNavigateToRegister
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Emergency Blood Finder",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "জরুরী রক্তদাতা অনুসন্ধান (বিভাগ ও জেলাভিত্তিক)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmergencyRed
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { bloodViewModel.retryLoad() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                // Emergency Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmergencyRedContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = null,
                                tint = EmergencyRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Instant RBC Transfusion Matcher",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmergencyRed
                                )
                            )
                        }
                        Text(
                            text = "Select the patient's blood group and hospital location (Division, District, Upazila, Area) to find exact and medically compatible donors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Step 1: Select Patient Blood Group
            item {
                Text(
                    text = "1. Patient Blood Group (রোগীর রক্তের গ্রুপ):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BloodGroup.entries) { bg ->
                        FilterChip(
                            selected = selectedPatientBlood == bg,
                            onClick = { selectedPatientBlood = bg },
                            label = {
                                Text(
                                    text = "${bg.display} (${bg.bengaliDisplay})",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmergencyRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_patient_blood_${bg.name}")
                        )
                    }
                }
            }

            // Compatible Groups Display & Exact vs Compatible Toggle
            item {
                val compatibleTypes = BloodCompatibilityHelper.getCompatibleDonorGroups(selectedPatientBlood)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Compatible Donor Groups:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = !includeCompatibleAlternatives,
                                    onClick = { includeCompatibleAlternatives = false },
                                    label = { Text("Exact Only", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = includeCompatibleAlternatives,
                                    onClick = { includeCompatibleAlternatives = true },
                                    label = { Text("All Compatible", fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            compatibleTypes.forEach { bg ->
                                val isExact = bg == selectedPatientBlood
                                Surface(
                                    color = if (isExact) EmergencyRed else BloodSuccessContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isExact) "${bg.display} (Exact)" else "${bg.display} (Compatible)",
                                        color = if (isExact) Color.White else BloodSuccessGreen,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = BloodCompatibilityHelper.CLINICAL_DISCLAIMER_EN,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Step 2: Filter by Division, District, Upazila, Area
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "2. Location Filter (বিভাগ, জেলা, উপজেলা ও এলাকা)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (selectedDivision.isNotBlank() || selectedDistrict.isNotBlank() || selectedUpazila.isNotBlank() || areaQuery.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        selectedDivision = ""
                                        selectedDistrict = ""
                                        selectedUpazila = ""
                                        areaQuery = ""
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Clear", fontSize = 11.sp)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = divisionExpanded,
                                onExpandedChange = { divisionExpanded = !divisionExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedDivision.ifBlank { "All Divisions" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Division") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = divisionExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = divisionExpanded,
                                    onDismissRequest = { divisionExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Divisions") },
                                        onClick = {
                                            selectedDivision = ""
                                            selectedDistrict = ""
                                            selectedUpazila = ""
                                            divisionExpanded = false
                                        }
                                    )
                                    LocationHelper.DIVISIONS.forEach { div ->
                                        DropdownMenuItem(
                                            text = { Text(div) },
                                            onClick = {
                                                selectedDivision = div
                                                selectedDistrict = ""
                                                selectedUpazila = ""
                                                divisionExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = districtExpanded,
                                onExpandedChange = { districtExpanded = !districtExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedDistrict.ifBlank { "All Districts" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("District") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = districtExpanded,
                                    onDismissRequest = { districtExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Districts") },
                                        onClick = {
                                            selectedDistrict = ""
                                            selectedUpazila = ""
                                            districtExpanded = false
                                        }
                                    )
                                    availableDistricts.forEach { dist ->
                                        DropdownMenuItem(
                                            text = { Text(dist) },
                                            onClick = {
                                                selectedDistrict = dist
                                                selectedUpazila = ""
                                                districtExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (availableUpazilas.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = upazilaExpanded,
                                onExpandedChange = { upazilaExpanded = !upazilaExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedUpazila.ifBlank { "All Upazilas / Thanas" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Upazila / Thana") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upazilaExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = upazilaExpanded,
                                    onDismissRequest = { upazilaExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Upazilas / Thanas") },
                                        onClick = {
                                            selectedUpazila = ""
                                            upazilaExpanded = false
                                        }
                                    )
                                    availableUpazilas.forEach { upz ->
                                        DropdownMenuItem(
                                            text = { Text(upz) },
                                            onClick = {
                                                selectedUpazila = upz
                                                upazilaExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = areaQuery,
                            onValueChange = { areaQuery = it },
                            label = { Text("Area / Hospital Locality (এলাকা বা হাসপাতালের অবস্থান)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_emergency_area"),
                            singleLine = true
                        )
                    }
                }
            }

            // Emergency Broadcast Button
            item {
                Button(
                    onClick = {
                        if (currentUserProfile != null) {
                            onNavigateToCreateRequest()
                        } else {
                            authPromptMessage = "জরুরী রক্তের আবেদন প্রকাশ বা ব্রডকাস্ট পাঠাতে অনুগ্রহ করে সাইন ইন করুন।"
                            showAuthPromptDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_broadcast_emergency")
                ) {
                    Icon(Icons.Default.BroadcastOnPersonal, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Post & Broadcast Emergency Blood Request",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Results Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Compatible Donors (${compatibleDonors.size} Found)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = BloodSuccessGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Privacy Protected", style = MaterialTheme.typography.labelSmall, color = BloodSuccessGreen)
                    }
                }
            }

            // Donors List
            if (compatibleDonors.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No matching donors found for this filter",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Try selecting a broader location (District/Division) or post an Emergency Blood Request to notify donors.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(compatibleDonors) { donor ->
                    PublicDonorItemCard(
                        donor = donor,
                        recipientBloodGroup = selectedPatientBlood,
                        onCallClick = {
                            if (donor.contactPhone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${donor.contactPhone}"))
                                context.startActivity(intent)
                            }
                        },
                        onChatClick = {
                            if (currentUserProfile != null) {
                                chatViewModel.startChatWithDonor(
                                    donorUid = donor.uid,
                                    donorName = donor.name,
                                    donorBloodGroup = donor.bloodGroup.display,
                                    onReady = { convId ->
                                        onNavigateToChatConversation(convId, donor.name, donor.bloodGroup.display)
                                    }
                                )
                            } else {
                                authPromptMessage = "রক্তদাতার সাথে নিরাপদ ১-টু-১ চ্যাট শুরু করতে অনুগ্রহ করে সাইন ইন করুন।"
                                showAuthPromptDialog = true
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
