package com.example.ui.screens.admin

import androidx.compose.foundation.clickable
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
import com.example.model.AccountStatus
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.DonorAvailability
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.VerificationStatus
import com.example.ui.components.BloodGroupBadge
import com.example.ui.theme.BloodSuccessContainer
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.AdminViewModel

enum class AdminModuleTab(val titleBn: String) {
    OVERVIEW("সারসংক্ষেপ"),
    USERS_DONORS("ব্যবহারকারী ও রক্তদাতা"),
    VERIFICATIONS("যাচাইকরণ ও রক্তদান"),
    BLOOD_REQUESTS("রক্তের অনুরোধ"),
    EMERGENCY_BROADCAST("জরুরি বিজ্ঞপ্তি"),
    ORGANIZATIONS("প্রতিষ্ঠান যাচাই"),
    BLOOD_BANKS_CAMPS("ব্লাড ব্যাংক ও ক্যাম্প"),
    NOTIFICATIONS("নোটিফিকেশন ও ঘোষণা"),
    COMPLAINTS("অভিযোগ পর্যালোচনা"),
    SYSTEM_SETTINGS("সিস্টেম সেটিংস ও স্বাস্থ্য"),
    AUDIT_LOGS("অ্যাডমিন কার্যক্রমের ইতিহাস")
}

// ============================================================================
// A. User & Donor Management Module
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminUsersDonorsModule(
    adminState: AdminUiState,
    adminViewModel: AdminViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf<BloodGroup?>(null) }
    var selectedAccountStatus by remember { mutableStateOf<AccountStatus?>(null) }
    var onlyDonors by remember { mutableStateOf(false) }

    var userForStatusAction by remember { mutableStateOf<UserProfile?>(null) }
    var statusActionReason by remember { mutableStateOf("") }
    var userForDonorEdit by remember { mutableStateOf<UserProfile?>(null) }
    var userForHistoryView by remember { mutableStateOf<UserProfile?>(null) }

    val filteredUsers = remember(
        adminState.allUsers,
        searchQuery,
        selectedBloodGroup,
        selectedAccountStatus,
        onlyDonors
    ) {
        adminState.allUsers.filter { user ->
            val q = searchQuery.trim()
            val matchesQuery = q.isBlank() ||
                user.name.contains(q, ignoreCase = true) ||
                user.email.contains(q, ignoreCase = true) ||
                user.contactPhone.contains(q, ignoreCase = true) ||
                user.location.district.contains(q, ignoreCase = true) ||
                user.location.division.contains(q, ignoreCase = true)
            val matchesBg = selectedBloodGroup == null || user.bloodGroup == selectedBloodGroup
            val matchesStatus = selectedAccountStatus == null || user.accountStatus == selectedAccountStatus
            val matchesDonor = !onlyDonors || user.isDonor
            matchesQuery && matchesBg && matchesStatus && matchesDonor
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "ব্যবহারকারী ও রক্তদাতা পরিচালনা (${filteredUsers.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "সুপার অ্যাডমিন ভূমিকা এবং রক্তদাতা যাচাই স্ট্যাটাস সম্পূর্ণ আলাদাভাবে পরিচালিত হয়।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("নাম, ইমেইল, ফোন বা জেলা দিয়ে খুঁজুন") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "মুছুন",
                            modifier = Modifier.clickable { searchQuery = "" }
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_user_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = onlyDonors,
                        onClick = { onlyDonors = !onlyDonors },
                        label = { Text("শুধু রক্তদাতা") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedAccountStatus == AccountStatus.ACTIVE,
                        onClick = {
                            selectedAccountStatus = if (selectedAccountStatus == AccountStatus.ACTIVE) null else AccountStatus.ACTIVE
                        },
                        label = { Text("সক্রিয় অ্যাকাউন্ট") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedAccountStatus == AccountStatus.SUSPENDED,
                        onClick = {
                            selectedAccountStatus = if (selectedAccountStatus == AccountStatus.SUSPENDED) null else AccountStatus.SUSPENDED
                        },
                        label = { Text("স্থগিত অ্যাকাউন্ট") }
                    )
                }
                items(BloodGroup.entries) { bg ->
                    FilterChip(
                        selected = selectedBloodGroup == bg,
                        onClick = { selectedBloodGroup = if (selectedBloodGroup == bg) null else bg },
                        label = { Text(bg.display) }
                    )
                }
            }
        }
    }

    if (filteredUsers.isEmpty()) {
        EmptyAdminModuleCard("কোনো ব্যবহারকারী বা রক্তদাতা পাওয়া যায়নি।")
    } else {
        filteredUsers.forEach { user ->
            val isSuspended = user.accountStatus == AccountStatus.SUSPENDED
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            BloodGroupBadge(bloodGroup = user.bloodGroup, sizeInDp = 42)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user.name.ifBlank { "নামবিহীন ব্যবহারকারী" }, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${user.email.ifBlank { "ইমেইল নেই" }} • ${user.contactPhone.ifBlank { "ফোন নেই" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "অবস্থান: ${user.location.district}, ${user.location.division}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = if (isSuspended) EmergencyRedContainer else BloodSuccessContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = user.accountStatus.labelBn,
                                color = if (isSuspended) EmergencyRed else BloodSuccessGreen,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusTagChip(
                            label = "ভূমিকা: ${if (user.role == UserRole.SUPER_ADMIN) "সুপার অ্যাডমিন" else "সাধারণ ব্যবহারকারী"}",
                            isHighlight = user.role == UserRole.SUPER_ADMIN
                        )
                        StatusTagChip(
                            label = "রক্তদাতা যাচাই: ${user.verificationStatus.labelBn}",
                            isHighlight = user.verificationStatus == VerificationStatus.VERIFIED
                        )
                        if (user.isDonor) {
                            StatusTagChip(
                                label = "প্রাপ্যতা: ${user.donorAvailability.displayBn}",
                                isHighlight = user.donorAvailability == DonorAvailability.AVAILABLE
                            )
                            StatusTagChip(
                                label = "মোট রক্তদান: ${user.donationCount} বার",
                                isHighlight = user.donationCount > 0
                            )
                        }
                    }

                    if (!user.suspensionReason.isNullOrBlank()) {
                        Text(
                            text = "স্থগিতের কারণ: ${user.suspensionReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmergencyRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { userForDonorEdit = user },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("রক্তদাতা স্ট্যাটাস", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { userForHistoryView = user },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ইতিহাস দেখুন", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                statusActionReason = ""
                                userForStatusAction = user
                            },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSuspended) BloodSuccessGreen else EmergencyRed
                            )
                        ) {
                            Text(
                                text = if (isSuspended) "সক্রিয় করুন" else "স্থগিত করুন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    userForStatusAction?.let { target ->
        val willSuspend = target.accountStatus == AccountStatus.ACTIVE
        AlertDialog(
            onDismissRequest = { userForStatusAction = null },
            title = {
                Text(
                    text = if (willSuspend) "অ্যাকাউন্ট স্থগিত নিশ্চিতকরণ" else "অ্যাকাউন্ট পুনরায় সক্রিয় নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (willSuspend) {
                            "আপনি কি নিশ্চিত যে '${target.name}' (${target.email})-এর অ্যাকাউন্ট স্থগিত করতে চান? এটি কার্যক্রমের ইতিহাসে সংরক্ষিত হবে।"
                        } else {
                            "আপনি কি '${target.name}'-এর স্থগিতাদেশ প্রত্যাহার করে অ্যাকাউন্টটি পুনরায় সক্রিয় করতে চান?"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = statusActionReason,
                        onValueChange = { statusActionReason = it },
                        label = { Text(if (willSuspend) "স্থগিত করার সুনির্দিষ্ট কারণ *" else "পুনর্বহালের নোট") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newStatus = if (willSuspend) AccountStatus.SUSPENDED else AccountStatus.ACTIVE
                        adminViewModel.updateUserAccountStatus(target, newStatus, statusActionReason)
                        userForStatusAction = null
                    },
                    enabled = !willSuspend || statusActionReason.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (willSuspend) EmergencyRed else BloodSuccessGreen
                    )
                ) {
                    Text(if (willSuspend) "স্থগিত নিশ্চিত করুন" else "সক্রিয় নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForStatusAction = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    userForDonorEdit?.let { target ->
        var selAvail by remember(target) { mutableStateOf(target.donorAvailability) }
        var selVer by remember(target) { mutableStateOf(target.verificationStatus) }
        var adminNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { userForDonorEdit = null },
            title = { Text("রক্তদাতা প্রাপ্যতা ও যাচাই স্ট্যাটাস", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${target.name} (${target.bloodGroup.display})", fontWeight = FontWeight.SemiBold)
                    Text("রক্তদানের প্রাপ্যতা:", style = MaterialTheme.typography.labelMedium)
                    DonorAvailability.entries.forEach { avail ->
                        FilterChip(
                            selected = selAvail == avail,
                            onClick = { selAvail = avail },
                            label = { Text(avail.displayBn) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("রক্তদাতা যাচাই স্ট্যাটাস (সুপার অ্যাডমিন রোল থেকে স্বতন্ত্র):", style = MaterialTheme.typography.labelMedium)
                    listOf(
                        VerificationStatus.UNVERIFIED,
                        VerificationStatus.PENDING,
                        VerificationStatus.VERIFIED,
                        VerificationStatus.REJECTED
                    ).forEach { vs ->
                        FilterChip(
                            selected = selVer == vs,
                            onClick = { selVer = vs },
                            label = { Text(vs.labelBn) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = adminNotes,
                        onValueChange = { adminNotes = it },
                        label = { Text("প্রশাসনিক নোট (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.updateDonorStatus(target, selAvail, selVer, adminNotes)
                        userForDonorEdit = null
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForDonorEdit = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    userForHistoryView?.let { target ->
        val userDonations = adminState.allDonations.filter { it.donorUid == target.uid }
        val userRequests = adminState.allBloodRequests.filter { it.requesterUid == target.uid }
        val respondedRequests = adminState.allBloodRequests.filter {
            it.acceptedDonorUid == target.uid || target.uid in it.respondedDonorUids
        }
        AlertDialog(
            onDismissRequest = { userForHistoryView = null },
            title = { Text("${target.name}-এর কার্যক্রমের ইতিহাস", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("যাচাইকৃত রক্তদান: ${userDonations.size} টি", fontWeight = FontWeight.SemiBold)
                    userDonations.take(3).forEach { d ->
                        Text("• ${d.hospitalName} (${d.district}) - সনদ: ${d.certificateCode}", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    Text("রক্তের আবেদন করেছেন: ${userRequests.size} টি", fontWeight = FontWeight.SemiBold)
                    userRequests.take(3).forEach { r ->
                        Text("• রোগী: ${r.patientName} (${r.bloodGroup.display}) - ${r.status.bengaliDisplay}", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    Text("রক্তদানে সাড়া দিয়েছেন: ${respondedRequests.size} টি অনুরোধে", fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = { userForHistoryView = null }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }
}

// ============================================================================
// B. Blood Request Management Module
// ============================================================================
@Composable
fun AdminBloodRequestsModule(
    adminState: AdminUiState,
    adminViewModel: AdminViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<RequestStatus?>(null) }
    var selectedBloodGroup by remember { mutableStateOf<BloodGroup?>(null) }
    var onlyEmergency by remember { mutableStateOf(false) }
    var requestForEdit by remember { mutableStateOf<BloodRequest?>(null) }

    val filteredRequests = remember(
        adminState.allBloodRequests,
        searchQuery,
        selectedStatus,
        selectedBloodGroup,
        onlyEmergency
    ) {
        adminState.allBloodRequests.filter { req ->
            val q = searchQuery.trim()
            val matchesQuery = q.isBlank() ||
                req.patientName.contains(q, ignoreCase = true) ||
                req.hospitalName.contains(q, ignoreCase = true) ||
                req.district.contains(q, ignoreCase = true) ||
                req.division.contains(q, ignoreCase = true) ||
                req.contactPhone.contains(q, ignoreCase = true)
            val matchesStatus = selectedStatus == null || req.status == selectedStatus
            val matchesBg = selectedBloodGroup == null || req.bloodGroup == selectedBloodGroup
            val matchesUrg = !onlyEmergency || req.urgency == RequestUrgency.CRITICAL_EMERGENCY
            matchesQuery && matchesStatus && matchesBg && matchesUrg
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "রক্তের অনুরোধ পরিচালনা (${filteredRequests.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                OutlinedButton(
                    onClick = { adminViewModel.expireStaleRequests() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("মেয়াদোত্তীর্ণ নিষ্পত্তি", fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("রোগীর নাম, হাসপাতাল, জেলা বা ফোন নম্বর খুঁজুন") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = onlyEmergency,
                        onClick = { onlyEmergency = !onlyEmergency },
                        label = { Text("শুধু অতি জরুরি") }
                    )
                }
                items(RequestStatus.entries) { st ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { selectedStatus = if (selectedStatus == st) null else st },
                        label = { Text(st.bengaliDisplay) }
                    )
                }
                items(BloodGroup.entries) { bg ->
                    FilterChip(
                        selected = selectedBloodGroup == bg,
                        onClick = { selectedBloodGroup = if (selectedBloodGroup == bg) null else bg },
                        label = { Text(bg.display) }
                    )
                }
            }
        }
    }

    if (filteredRequests.isEmpty()) {
        EmptyAdminModuleCard("নির্বাচিত ফিল্টারে কোনো রক্তের অনুরোধ পাওয়া যায়নি।")
    } else {
        filteredRequests.forEach { req ->
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            BloodGroupBadge(bloodGroup = req.bloodGroup, sizeInDp = 42)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "রোগী: ${req.patientName} (${req.unitsRequired} ব্যাগ)",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${req.hospitalName} • ${req.district}, ${req.division}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusTagChip(
                            label = req.status.bengaliDisplay,
                            isHighlight = req.status == RequestStatus.FULFILLED
                        )
                    }

                    Text(
                        text = "জরুরিতা: ${req.urgency.bengaliDisplay} • আবেদনকারী: ${req.requesterName} (${req.contactPhone})",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (req.urgency == RequestUrgency.CRITICAL_EMERGENCY) EmergencyRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!req.acceptedDonorName.isNullOrBlank()) {
                        Text(
                            text = "সাড়াদানকারী রক্তদাতা: ${req.acceptedDonorName} (মোট সাড়া: ${req.respondedDonorUids.size.coerceAtLeast(1)} জন)",
                            style = MaterialTheme.typography.bodySmall,
                            color = BloodSuccessGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "রক্তদাতার সাড়া: এখনো কোনো রক্তদাতা নিশ্চিত হননি",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (req.adminNotes.isNotBlank()) {
                        Text(
                            text = "প্রশাসনিক নোট: ${req.adminNotes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { requestForEdit = req },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("অবস্থা ও নোট পরিবর্তন করুন", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    requestForEdit?.let { target ->
        var newStatus by remember(target) { mutableStateOf(target.status) }
        var notes by remember(target) { mutableStateOf(target.adminNotes) }

        AlertDialog(
            onDismissRequest = { requestForEdit = null },
            title = { Text("রক্তের অনুরোধ ব্যবস্থাপনা", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("রোগী: ${target.patientName} (${target.bloodGroup.display}) - ${target.hospitalName}")
                    Text("নতুন অবস্থা নির্বাচন করুন:", style = MaterialTheme.typography.labelMedium)
                    RequestStatus.entries.forEach { st ->
                        FilterChip(
                            selected = newStatus == st,
                            onClick = { newStatus = st },
                            label = { Text(st.bengaliDisplay) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("প্রশাসনিক পর্যালোচনা নোট") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.updateBloodRequestStatus(target, newStatus, notes)
                        requestForEdit = null
                    }
                ) {
                    Text("হালনাগাদ নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { requestForEdit = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
