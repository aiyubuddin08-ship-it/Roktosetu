package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseSecurityHelper
import com.example.domain.LocationHelper
import com.example.model.AmbulanceService
import com.example.model.BloodBank
import com.example.model.BloodDonationCamp
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.AdminViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBloodBanksCampsModule(
    adminState: AdminUiState,
    adminViewModel: AdminViewModel
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Blood Banks, 1 = Ambulances, 2 = Blood Camps
    var selectedDistrict by remember { mutableStateOf("সকল জেলা") }
    var searchQuery by remember { mutableStateOf("") }

    var editingBloodBank by remember { mutableStateOf<BloodBank?>(null) }
    var showAddBloodBank by remember { mutableStateOf(false) }

    var editingAmbulance by remember { mutableStateOf<AmbulanceService?>(null) }
    var showAddAmbulance by remember { mutableStateOf(false) }

    var editingCamp by remember { mutableStateOf<BloodDonationCamp?>(null) }
    var showAddCamp by remember { mutableStateOf(false) }

    var itemToDelete by remember { mutableStateOf<Triple<String, String, String>?>(null) } // (type, id, name)

    val allDistricts = remember {
        listOf("সকল জেলা") + LocationHelper.ALL_DISTRICTS
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ব্লাড ব্যাংক, অ্যাম্বুলেন্স ও ক্যাম্প নিয়ন্ত্রণ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "সংযোজন, সম্পাদনা, মুছে ফেলা, সক্রিয়/নিষ্ক্রিয় ও জেলা ফিল্টার",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        when (selectedSubTab) {
                            0 -> showAddBloodBank = true
                            1 -> showAddAmbulance = true
                            else -> showAddCamp = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("btn_add_directory_item")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নতুন যোগ করুন", fontSize = 12.sp)
                }
            }

            // Sub-tab selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    label = { Text("ব্লাড ব্যাংক (${adminState.bloodBanks.size})") },
                    modifier = Modifier.testTag("subtab_blood_banks")
                )
                FilterChip(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    label = { Text("অ্যাম্বুলেন্স (${adminState.ambulances.size})") },
                    modifier = Modifier.testTag("subtab_ambulances")
                )
                FilterChip(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    label = { Text("ক্যাম্প (${adminState.bloodCamps.size})") },
                    modifier = Modifier.testTag("subtab_blood_camps")
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("নাম, ঠিকানা বা ফোন নম্বর দিয়ে খুঁজুন") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // District filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allDistricts) { district ->
                    FilterChip(
                        selected = selectedDistrict == district,
                        onClick = { selectedDistrict = district },
                        label = { Text(district, fontSize = 12.sp) }
                    )
                }
            }
        }
    }

    when (selectedSubTab) {
        0 -> {
            val filtered = remember(adminState.bloodBanks, selectedDistrict, searchQuery) {
                val q = searchQuery.trim()
                adminState.bloodBanks.filter { item ->
                    val matchesDistrict = selectedDistrict == "সকল জেলা" || item.district.equals(selectedDistrict, ignoreCase = true)
                    val matchesQuery = q.isBlank() ||
                        item.name.contains(q, ignoreCase = true) ||
                        item.nameBn.contains(q, ignoreCase = true) ||
                        item.address.contains(q, ignoreCase = true) ||
                        item.phone.contains(q, ignoreCase = true)
                    matchesDistrict && matchesQuery
                }
            }
            if (filtered.isEmpty()) {
                EmptyAdminModuleCard("এই ফিল্টারে কোনো ব্লাড ব্যাংক পাওয়া যায়নি।")
            } else {
                filtered.forEach { bank ->
                    val displayTitle = bank.nameBn.ifBlank { bank.name }
                    val openHoursText = if (bank.is24Hours) "২৪ ঘণ্টা খোলা" else "নির্ধারিত সময়"
                    DirectoryAdminItemCard(
                        title = displayTitle,
                        subtitle = "${bank.organization.ifBlank { "ব্লাড ব্যাংক" }} • ${bank.district}",
                        detailLine = "ঠিকানা: ${bank.address}\nফোন: ${bank.phone} • সময়: $openHoursText",
                        isActive = bank.isActive,
                        onToggleActive = { active ->
                            adminViewModel.saveBloodBank(bank.copy(isActive = active))
                        },
                        onEdit = { editingBloodBank = bank },
                        onDelete = { itemToDelete = Triple("BLOOD_BANK", bank.id, displayTitle) }
                    )
                }
            }
        }

        1 -> {
            val filtered = remember(adminState.ambulances, selectedDistrict, searchQuery) {
                val q = searchQuery.trim()
                adminState.ambulances.filter { item ->
                    val matchesDistrict = selectedDistrict == "সকল জেলা" || item.district.equals(selectedDistrict, ignoreCase = true)
                    val matchesQuery = q.isBlank() ||
                        item.title.contains(q, ignoreCase = true) ||
                        item.titleBn.contains(q, ignoreCase = true) ||
                        item.provider.contains(q, ignoreCase = true) ||
                        item.coverageArea.contains(q, ignoreCase = true) ||
                        item.phone.contains(q, ignoreCase = true)
                    matchesDistrict && matchesQuery
                }
            }
            if (filtered.isEmpty()) {
                EmptyAdminModuleCard("এই ফিল্টারে কোনো অ্যাম্বুলেন্স বা জরুরি সেবা পাওয়া যায়নি।")
            } else {
                filtered.forEach { amb ->
                    val displayTitle = amb.titleBn.ifBlank { amb.title }
                    DirectoryAdminItemCard(
                        title = displayTitle,
                        subtitle = "${amb.type} • ${amb.district}",
                        detailLine = "পরিচালনায়: ${amb.provider} • এলাকা: ${amb.coverageArea} • ফোন: ${amb.phone}",
                        isActive = amb.isActive,
                        onToggleActive = { active ->
                            adminViewModel.saveAmbulance(amb.copy(isActive = active))
                        },
                        onEdit = { editingAmbulance = amb },
                        onDelete = { itemToDelete = Triple("AMBULANCE", amb.id, displayTitle) }
                    )
                }
            }
        }

        else -> {
            val filtered = remember(adminState.bloodCamps, selectedDistrict, searchQuery) {
                val q = searchQuery.trim()
                adminState.bloodCamps.filter { item ->
                    val matchesDistrict = selectedDistrict == "সকল জেলা" || item.district.equals(selectedDistrict, ignoreCase = true)
                    val matchesQuery = q.isBlank() ||
                        item.title.contains(q, ignoreCase = true) ||
                        item.titleBn.contains(q, ignoreCase = true) ||
                        item.organizer.contains(q, ignoreCase = true) ||
                        item.location.contains(q, ignoreCase = true)
                    matchesDistrict && matchesQuery
                }
            }
            if (filtered.isEmpty()) {
                EmptyAdminModuleCard("এই ফিল্টারে কোনো রক্তদান ক্যাম্প পাওয়া যায়নি।")
            } else {
                filtered.forEach { camp ->
                    val displayTitle = camp.titleBn.ifBlank { camp.title }
                    DirectoryAdminItemCard(
                        title = displayTitle,
                        subtitle = "আয়োজক: ${camp.organizer} • ${camp.district}",
                        detailLine = "স্থান: ${camp.location} • তারিখ: ${camp.date} (${camp.time})\nফোন: ${camp.contactPhone} • লক্ষ্য: ${camp.targetUnits} ব্যাগ (নিবন্ধিত: ${camp.registeredCount})",
                        isActive = camp.isActive,
                        onToggleActive = { active ->
                            adminViewModel.saveBloodCamp(camp.copy(isActive = active))
                        },
                        onEdit = { editingCamp = camp },
                        onDelete = { itemToDelete = Triple("CAMP", camp.id, displayTitle) }
                    )
                }
            }
        }
    }

    // Add / Edit Blood Bank Dialog
    if (showAddBloodBank || editingBloodBank != null) {
        val initial = editingBloodBank
        var name by remember(initial) { mutableStateOf(initial?.nameBn?.ifBlank { initial.name } ?: "") }
        var organization by remember(initial) { mutableStateOf(initial?.organization ?: "সরকারি / স্বেচ্ছাসেবী") }
        var district by remember(initial) { mutableStateOf(initial?.district ?: "Dhaka") }
        var address by remember(initial) { mutableStateOf(initial?.address ?: "") }
        var phone by remember(initial) { mutableStateOf(initial?.phone ?: "") }
        var is24Hours by remember(initial) { mutableStateOf(initial?.is24Hours ?: true) }
        var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }
        var phoneError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showAddBloodBank = false
                editingBloodBank = null
            },
            title = {
                Text(
                    text = if (initial == null) "নতুন ব্লাড ব্যাংক যোগ করুন" else "ব্লাড ব্যাংক সম্পাদনা করুন",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("ব্লাড ব্যাংকের নাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = organization,
                        onValueChange = { organization = it },
                        label = { Text("পরিচালনাকারী সংস্থা / ধরন *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("জেলা (যেমন Dhaka, Chattogram) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("বিস্তারিত ঠিকানা *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = null
                        },
                        label = { Text("যোগাযোগের ফোন নম্বর *") },
                        isError = phoneError != null,
                        supportingText = phoneError?.let { err -> { Text(err, color = EmergencyRed) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = is24Hours, onCheckedChange = { is24Hours = it })
                        Text("২৪ ঘণ্টা জরুরি সেবা চালু", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isActive) "সক্রিয়" else "নিষ্ক্রিয়", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!FirebaseSecurityHelper.isValidEmergencyOrContactPhone(phone)) {
                            phoneError = "সঠিক বাংলাদেশী ফোন বা জরুরি হটলাইন নম্বর দিন"
                            return@Button
                        }
                        val cleanName = name.trim()
                        val cleanDistrict = district.trim().ifBlank { "Dhaka" }
                        adminViewModel.saveBloodBank(
                            BloodBank(
                                id = initial?.id ?: "bb-${UUID.randomUUID().toString().take(8)}",
                                name = cleanName,
                                nameBn = cleanName,
                                organization = organization.trim(),
                                address = address.trim(),
                                city = cleanDistrict,
                                district = cleanDistrict,
                                phone = phone.trim(),
                                emergencyHotline = phone.trim(),
                                is24Hours = is24Hours,
                                availableGroups = initial?.availableGroups ?: com.example.model.BloodGroup.entries,
                                verified = true,
                                isActive = isActive
                            )
                        )
                        showAddBloodBank = false
                        editingBloodBank = null
                    },
                    enabled = name.trim().length >= 2 && district.trim().isNotBlank() && address.trim().length >= 3
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddBloodBank = false
                        editingBloodBank = null
                    }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Add / Edit Ambulance Dialog
    if (showAddAmbulance || editingAmbulance != null) {
        val initial = editingAmbulance
        var title by remember(initial) { mutableStateOf(initial?.titleBn?.ifBlank { initial.title } ?: "") }
        var provider by remember(initial) { mutableStateOf(initial?.provider ?: "") }
        var vehicleType by remember(initial) { mutableStateOf(initial?.type ?: "এসি / আইসিইউ") }
        var district by remember(initial) { mutableStateOf(initial?.district ?: "Dhaka") }
        var coverageArea by remember(initial) { mutableStateOf(initial?.coverageArea ?: "") }
        var phone by remember(initial) { mutableStateOf(initial?.phone ?: "") }
        var isAvailable24x7 by remember(initial) { mutableStateOf(initial?.isAvailable24x7 ?: true) }
        var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }
        var phoneError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showAddAmbulance = false
                editingAmbulance = null
            },
            title = {
                Text(
                    text = if (initial == null) "নতুন অ্যাম্বুলেন্স সেবা যোগ করুন" else "অ্যাম্বুলেন্স সেবা সম্পাদনা করুন",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("সেবার নাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("পরিচালনাকারী সংস্থা") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vehicleType,
                        onValueChange = { vehicleType = it },
                        label = { Text("গাড়ির ধরন (আইসিইউ / এসি / জরুরি) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("জেলা (যেমন Dhaka, Chattogram) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = coverageArea,
                        onValueChange = { coverageArea = it },
                        label = { Text("সেবা এলাকা *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = null
                        },
                        label = { Text("ফোন / হটলাইন নম্বর *") },
                        isError = phoneError != null,
                        supportingText = phoneError?.let { err -> { Text(err, color = EmergencyRed) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isAvailable24x7, onCheckedChange = { isAvailable24x7 = it })
                        Text("২৪/৭ জরুরি সেবা চালু", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isActive) "সক্রিয়" else "নিষ্ক্রিয়", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!FirebaseSecurityHelper.isValidEmergencyOrContactPhone(phone)) {
                            phoneError = "সঠিক বাংলাদেশী ফোন বা জরুরি হটলাইন নম্বর দিন"
                            return@Button
                        }
                        val cleanTitle = title.trim()
                        val cleanDistrict = district.trim().ifBlank { "Dhaka" }
                        adminViewModel.saveAmbulance(
                            AmbulanceService(
                                id = initial?.id ?: "amb-${UUID.randomUUID().toString().take(8)}",
                                title = cleanTitle,
                                titleBn = cleanTitle,
                                provider = provider.trim().ifBlank { cleanTitle },
                                coverageArea = coverageArea.trim().ifBlank { cleanDistrict },
                                district = cleanDistrict,
                                city = cleanDistrict,
                                phone = phone.trim(),
                                type = vehicleType.trim(),
                                isAvailable24x7 = isAvailable24x7,
                                isActive = isActive
                            )
                        )
                        showAddAmbulance = false
                        editingAmbulance = null
                    },
                    enabled = title.trim().length >= 2 && district.trim().isNotBlank()
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddAmbulance = false
                        editingAmbulance = null
                    }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Add / Edit Blood Camp Dialog
    if (showAddCamp || editingCamp != null) {
        val initial = editingCamp
        var title by remember(initial) { mutableStateOf(initial?.titleBn?.ifBlank { initial.title } ?: "") }
        var organizerName by remember(initial) { mutableStateOf(initial?.organizer ?: "") }
        var district by remember(initial) { mutableStateOf(initial?.district ?: "Dhaka") }
        var venue by remember(initial) { mutableStateOf(initial?.location ?: "") }
        var dateText by remember(initial) { mutableStateOf(initial?.date ?: "") }
        var timeText by remember(initial) { mutableStateOf(initial?.time ?: "সকাল ৯:০০ - বিকাল ৪:০০") }
        var contactPhone by remember(initial) { mutableStateOf(initial?.contactPhone ?: "") }
        var targetUnitsText by remember(initial) { mutableStateOf((initial?.targetUnits ?: 100).toString()) }
        var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
        var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }
        var phoneError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showAddCamp = false
                editingCamp = null
            },
            title = {
                Text(
                    text = if (initial == null) "নতুন রক্তদান ক্যাম্প যোগ করুন" else "রক্তদান ক্যাম্প সম্পাদনা করুন",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("ক্যাম্পের শিরোনাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = organizerName,
                        onValueChange = { organizerName = it },
                        label = { Text("আয়োজক সংগঠন *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("জেলা (যেমন Dhaka, Chattogram) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("স্থান / ভেন্যু *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = { dateText = it },
                            label = { Text("তারিখ *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetUnitsText,
                            onValueChange = { targetUnitsText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("লক্ষ্যমাত্রা (ব্যাগ)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("সময়কাল") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = {
                            contactPhone = it
                            phoneError = null
                        },
                        label = { Text("যোগাযোগের মোবাইল নম্বর *") },
                        isError = phoneError != null,
                        supportingText = phoneError?.let { err -> { Text(err, color = EmergencyRed) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("সংক্ষিপ্ত বিবরণ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isActive) "সক্রিয়" else "নিষ্ক্রিয়", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!FirebaseSecurityHelper.isValidEmergencyOrContactPhone(contactPhone)) {
                            phoneError = "সঠিক বাংলাদেশী ফোন নম্বর দিন (যেমন 017XXXXXXXX)"
                            return@Button
                        }
                        val cleanTitle = title.trim()
                        val cleanDistrict = district.trim().ifBlank { "Dhaka" }
                        adminViewModel.saveBloodCamp(
                            BloodDonationCamp(
                                id = initial?.id ?: "camp-${UUID.randomUUID().toString().take(8)}",
                                title = cleanTitle,
                                titleBn = cleanTitle,
                                organizer = organizerName.trim(),
                                location = venue.trim(),
                                district = cleanDistrict,
                                city = cleanDistrict,
                                date = dateText.trim(),
                                time = timeText.trim(),
                                contactPhone = contactPhone.trim(),
                                targetUnits = targetUnitsText.toIntOrNull()?.coerceAtLeast(1) ?: 100,
                                registeredCount = initial?.registeredCount ?: 0,
                                isUserRsvp = initial?.isUserRsvp ?: false,
                                description = description.trim(),
                                isActive = isActive
                            )
                        )
                        showAddCamp = false
                        editingCamp = null
                    },
                    enabled = title.trim().length >= 3 && organizerName.trim().length >= 2 && venue.trim().length >= 2 && dateText.trim().isNotBlank()
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCamp = false
                        editingCamp = null
                    }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { (type, id, name) ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("মুছে ফেলা নিশ্চিত করুন", fontWeight = FontWeight.Bold, color = EmergencyRed) },
            text = { Text("আপনি কি নিশ্চিতভাবে '$name' তালিকা থেকে মুছে ফেলতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        when (type) {
                            "BLOOD_BANK" -> adminViewModel.deleteBloodBank(id, name)
                            "AMBULANCE" -> adminViewModel.deleteAmbulance(id, name)
                            "CAMP" -> adminViewModel.deleteBloodCamp(id, name)
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun DirectoryAdminItemCard(
    title: String,
    subtitle: String,
    detailLine: String,
    isActive: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isActive) "সক্রিয়" else "নিষ্ক্রিয়",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) BloodSuccessGreen else EmergencyRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(checked = isActive, onCheckedChange = onToggleActive)
                }
            }

            Text(
                text = detailLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("সম্পাদনা")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("মুছে ফেলুন")
                }
            }
        }
    }
}
