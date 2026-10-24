package com.example.ui.screens.donors

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.model.PublicDonorView
import com.example.ui.components.AuthRequiredDialog
import com.example.ui.components.PublicDonorItemCard
import com.example.ui.components.ReportSubmissionDialog
import com.example.ui.components.RoktoTopAppBar
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BloodViewModel
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorDirectoryScreen(
    authViewModel: AuthViewModel,
    bloodViewModel: BloodViewModel,
    chatViewModel: ChatViewModel,
    onNavigateToChatConversation: (String, String, String) -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val bloodState by bloodViewModel.uiState.collectAsState()

    var nameOrAreaQuery by remember { mutableStateOf("") }
    var showAuthPromptDialog by remember { mutableStateOf(false) }
    var donorToReport by remember { mutableStateOf<PublicDonorView?>(null) }

    var divisionExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var upazilaExpanded by remember { mutableStateOf(false) }

    val availableDistricts = remember(bloodState.selectedDivision) {
        if (bloodState.selectedDivision.isBlank()) {
            LocationHelper.ALL_DISTRICTS
        } else {
            LocationHelper.getDistrictsForDivision(bloodState.selectedDivision)
        }
    }

    val availableUpazilas = remember(bloodState.selectedDistrict) {
        if (bloodState.selectedDistrict.isBlank()) {
            emptyList()
        } else {
            LocationHelper.getUpazilasForDistrict(bloodState.selectedDistrict)
        }
    }

    val filteredDonors = bloodState.donors.filter { donor ->
        nameOrAreaQuery.isBlank() ||
            donor.name.contains(nameOrAreaQuery, ignoreCase = true) ||
            donor.generalLocation.contains(nameOrAreaQuery, ignoreCase = true) ||
            donor.area.contains(nameOrAreaQuery, ignoreCase = true) ||
            donor.upazila.contains(nameOrAreaQuery, ignoreCase = true) ||
            donor.district.contains(nameOrAreaQuery, ignoreCase = true)
    }

    if (showAuthPromptDialog) {
        AuthRequiredDialog(
            title = "সাইন ইন প্রয়োজন",
            message = "রক্তদাতার সাথে যোগাযোগ বা অভিযোগ জানাতে অনুগ্রহ করে আপনার অ্যাকাউন্টে লগইন করুন।",
            onDismiss = { showAuthPromptDialog = false },
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToRegister = onNavigateToRegister
        )
    }

    donorToReport?.let { targetDonor ->
        ReportSubmissionDialog(
            currentUserUid = currentUserProfile?.uid,
            initialTargetUid = targetDonor.uid,
            initialTargetName = "${targetDonor.name} (${targetDonor.bloodGroup.display})",
            initialTargetType = "DONOR",
            targetOwnerUid = targetDonor.uid,
            lockTarget = true,
            onDismiss = { donorToReport = null },
            onSubmit = { tUid, tName, tType, cat, reasonBn, details ->
                donorToReport = null
                authViewModel.submitUserReport(
                    targetUid = tUid,
                    targetName = tName,
                    targetType = tType,
                    category = cat,
                    reasonBn = reasonBn,
                    details = details
                ) { _, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RoktoTopAppBar(
                title = "Donor Directory (রক্তদাতা তালিকা)",
                onNavigateToSecurity = onNavigateToSecurity,
                onNavigateToChat = onNavigateToChat,
                isGuest = currentUserProfile == null,
                onNavigateToAuth = onNavigateToLogin
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nameOrAreaQuery,
                    onValueChange = {
                        nameOrAreaQuery = it
                        bloodViewModel.setAreaSearchQuery(it)
                    },
                    placeholder = { Text("রক্তদাতার নাম, এলাকা বা উপজেলা দিয়ে খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (nameOrAreaQuery.isNotBlank()) {
                            IconButton(onClick = {
                                nameOrAreaQuery = ""
                                bloodViewModel.setAreaSearchQuery("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "মুছুন")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_donors"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                Text(
                    text = "Recipient / Required Blood Group (প্রয়োজনীয় রক্তের গ্রুপ):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = bloodState.selectedBloodGroupFilter == null,
                            onClick = { bloodViewModel.setBloodGroupFilter(null) },
                            label = { Text("All Groups", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_donor_bg_all")
                        )
                    }
                    items(BloodGroup.entries) { bg ->
                        FilterChip(
                            selected = bloodState.selectedBloodGroupFilter == bg,
                            onClick = {
                                val next = if (bloodState.selectedBloodGroupFilter == bg) null else bg
                                bloodViewModel.setBloodGroupFilter(next)
                            },
                            label = { Text("${bg.display} (${bg.bengaliDisplay})", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_donor_bg_${bg.name}")
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !bloodState.includeCompatibleDonors,
                        onClick = { bloodViewModel.setIncludeCompatibleDonors(false) },
                        label = { Text("Exact Group Only", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("chip_exact_match_only")
                    )
                    FilterChip(
                        selected = bloodState.includeCompatibleDonors,
                        onClick = { bloodViewModel.setIncludeCompatibleDonors(true) },
                        label = { Text("Exact + Compatible RBC", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("chip_compatible_match")
                    )
                    FilterChip(
                        selected = bloodState.onlyAvailableDonors,
                        onClick = { bloodViewModel.setOnlyAvailableDonors(!bloodState.onlyAvailableDonors) },
                        leadingIcon = if (bloodState.onlyAvailableDonors) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        label = { Text("Available Now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("chip_only_available")
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Filter by Location (বিভাগ, জেলা ও উপজেলা)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (bloodState.selectedDivision.isNotBlank() ||
                                bloodState.selectedDistrict.isNotBlank() ||
                                bloodState.selectedUpazila.isNotBlank()
                            ) {
                                OutlinedButton(
                                    onClick = { bloodViewModel.setDivisionFilter("All") },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reset Location", fontSize = 11.sp)
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
                                    value = bloodState.selectedDivision.ifBlank { "All Divisions" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Division (বিভাগ)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = divisionExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .testTag("dropdown_filter_division"),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = divisionExpanded,
                                    onDismissRequest = { divisionExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Divisions") },
                                        onClick = {
                                            bloodViewModel.setDivisionFilter("All")
                                            divisionExpanded = false
                                        }
                                    )
                                    LocationHelper.DIVISIONS.forEach { div ->
                                        DropdownMenuItem(
                                            text = { Text(div) },
                                            onClick = {
                                                bloodViewModel.setDivisionFilter(div)
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
                                    value = bloodState.selectedDistrict.ifBlank { "All Districts" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("District (জেলা)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .testTag("dropdown_filter_district"),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = districtExpanded,
                                    onDismissRequest = { districtExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Districts") },
                                        onClick = {
                                            bloodViewModel.setDistrictFilter("All")
                                            districtExpanded = false
                                        }
                                    )
                                    availableDistricts.forEach { dist ->
                                        DropdownMenuItem(
                                            text = { Text(dist) },
                                            onClick = {
                                                bloodViewModel.setDistrictFilter(dist)
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
                                    value = bloodState.selectedUpazila.ifBlank { "All Upazilas / Thanas in ${bloodState.selectedDistrict}" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Upazila / Thana (উপজেলা/থানা)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upazilaExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .testTag("dropdown_filter_upazila"),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = upazilaExpanded,
                                    onDismissRequest = { upazilaExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Upazilas / Thanas") },
                                        onClick = {
                                            bloodViewModel.setUpazilaFilter("All")
                                            upazilaExpanded = false
                                        }
                                    )
                                    availableUpazilas.forEach { upz ->
                                        DropdownMenuItem(
                                            text = { Text(upz) },
                                            onClick = {
                                                bloodViewModel.setUpazilaFilter(upz)
                                                upazilaExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = BloodSuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = BloodCompatibilityHelper.CLINICAL_DISCLAIMER_EN,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Matching Donors (${filteredDonors.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(
                        onClick = { bloodViewModel.retryLoad() },
                        modifier = Modifier.testTag("btn_refresh_donors")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Donors")
                    }
                }
            }

            if (filteredDonors.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "কোনো মিলে যাওয়া রক্তদাতা পাওয়া যায়নি",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "ফিল্টার পরিবর্তন করুন বা বিকল্প সামঞ্জস্যপূর্ণ রক্তের গ্রুপ চালু করুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        nameOrAreaQuery = ""
                                        bloodViewModel.clearFilters()
                                    }
                                ) {
                                    Text("Clear Filters")
                                }
                                Button(onClick = { bloodViewModel.retryLoad() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredDonors, key = { it.uid }) { donor ->
                    val isSelf = currentUserProfile?.uid == donor.uid
                    PublicDonorItemCard(
                        donor = donor,
                        recipientBloodGroup = bloodState.selectedBloodGroupFilter,
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
                                showAuthPromptDialog = true
                            }
                        },
                        onReportClick = if (!isSelf) {
                            {
                                if (currentUserProfile != null) {
                                    donorToReport = donor
                                } else {
                                    showAuthPromptDialog = true
                                }
                            }
                        } else null
                    )
                }

                if (bloodState.hasMoreDonors) {
                    item {
                        OutlinedButton(
                            onClick = { bloodViewModel.loadMoreDonors() },
                            enabled = !bloodState.isLoadingMoreDonors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("btn_load_more_donors"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (bloodState.isLoadingMoreDonors) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("আরও রক্তদাতা দেখুন (Load More)", fontWeight = FontWeight.Bold)
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
