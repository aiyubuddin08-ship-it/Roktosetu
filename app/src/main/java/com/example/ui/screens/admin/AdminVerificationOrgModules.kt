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
import com.example.domain.BloodCompatibilityHelper
import com.example.domain.LocationHelper
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.Organization
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.model.VerificationStatus
import com.example.model.VerificationTask
import com.example.ui.components.BloodGroupBadge
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// C. Emergency Broadcast Management Module
// ============================================================================
@Composable
fun AdminEmergencyBroadcastModule(
    adminState: AdminUiState,
    adminViewModel: AdminViewModel
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var requestToBroadcast by remember { mutableStateOf<BloodRequest?>(null) }

    val eligibleEmergencyRequests = remember(adminState.allBloodRequests) {
        adminState.allBloodRequests.filter {
            it.status == RequestStatus.PENDING &&
                (it.urgency == RequestUrgency.CRITICAL_EMERGENCY || it.urgency == RequestUrgency.HIGH)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (adminState.isEmergencyBroadcastArmed) {
                MaterialTheme.colorScheme.surface
            } else {
                EmergencyRedContainer.copy(alpha = 0.45f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "জরুরি রক্তের বিজ্ঞপ্তি সম্প্রচার নিয়ন্ত্রণ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (adminState.isEmergencyBroadcastArmed) {
                            "বর্তমান অবস্থা: সক্রিয় — উপযুক্ত রক্তদাতাদের কাছে জরুরি সতর্কতা পাঠানো যাবে"
                        } else {
                            "বর্তমান অবস্থা: সাময়িকভাবে বিরত — কোনো জরুরি বিজ্ঞপ্তি পাঠানো হবে না"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (adminState.isEmergencyBroadcastArmed) BloodSuccessGreen else EmergencyRed
                    )
                }
                Switch(
                    checked = adminState.isEmergencyBroadcastArmed,
                    onCheckedChange = { adminViewModel.toggleEmergencySystem() },
                    modifier = Modifier.testTag("switch_emergency_broadcast")
                )
            }
            Text(
                text = "নিরাপত্তা নীতি: একই অনুরোধে ১৫ মিনিটের মধ্যে পুনরায় বিজ্ঞপ্তি প্রেরণ স্বয়ংক্রিয়ভাবে প্রতিরোধ করা হয় এবং শুধুমাত্র সামঞ্জস্যপূর্ণ রক্তের গ্রুপের প্রাপ্য রক্তদাতাদের কাছে নোটিফিকেশন যায়।",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (eligibleEmergencyRequests.isEmpty()) {
        EmptyAdminModuleCard("বর্তমানে সম্প্রচারের উপযুক্ত কোনো অপেক্ষমাণ জরুরি রক্তের অনুরোধ নেই।")
    } else {
        eligibleEmergencyRequests.forEach { req ->
            val compatibleGroups = remember(req.bloodGroup) {
                BloodCompatibilityHelper.getCompatibleDonorGroups(req.bloodGroup).map { it.display }
            }
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
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("রোগী: ${req.patientName} (${req.unitsRequired} ব্যাগ)", fontWeight = FontWeight.Bold)
                                Text(
                                    "${req.hospitalName}, ${req.district} (${req.division})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusTagChip(
                            label = req.urgency.bengaliDisplay,
                            isHighlight = req.urgency == RequestUrgency.CRITICAL_EMERGENCY
                        )
                    }

                    Text(
                        text = "সামঞ্জস্যপূর্ণ রক্তদাতা গ্রুপ: ${compatibleGroups.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (req.broadcastDispatchedAt != null) {
                        Text(
                            text = "সর্বশেষ জরুরি বিজ্ঞপ্তি প্রেরিত: ${sdf.format(Date(req.broadcastDispatchedAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BloodSuccessGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { requestToBroadcast = req },
                        enabled = adminState.isEmergencyBroadcastArmed && !adminState.isLoading,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("উপযুক্ত রক্তদাতাদের জরুরি বিজ্ঞপ্তি পাঠান", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    requestToBroadcast?.let { req ->
        val compatibleGroups = BloodCompatibilityHelper.getCompatibleDonorGroups(req.bloodGroup).joinToString(", ") { it.display }
        AlertDialog(
            onDismissRequest = { requestToBroadcast = null },
            icon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = EmergencyRed) },
            title = { Text("জরুরি বিজ্ঞপ্তি প্রেরণ নিশ্চিতকরণ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("আপনি কি নিচের জরুরি রক্তের অনুরোধটির জন্য বিজ্ঞপ্তি পাঠাতে চান?")
                    Text("• রোগী: ${req.patientName} (${req.bloodGroup.display}, ${req.unitsRequired} ব্যাগ)", fontWeight = FontWeight.SemiBold)
                    Text("• হাসপাতাল ও জেলা: ${req.hospitalName}, ${req.district}")
                    Text("• লক্ষ্যভুক্ত রক্তের গ্রুপ: $compatibleGroups")
                    Text(
                        text = "নিশ্চিত করলে শুধুমাত্র ${req.district}/${req.division} অঞ্চলের প্রাপ্য রক্তদাতাদের কাছে নোটিফিকেশন যাবে এবং কার্যক্রমের ইতিহাসে সংরক্ষিত হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.dispatchEmergencyBroadcast(req)
                        requestToBroadcast = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("নিশ্চিত ও প্রেরণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { requestToBroadcast = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// ============================================================================
// D. Verification Management & Donation Record Verification Module
// ============================================================================
@Composable
fun AdminVerificationsModule(
    adminState: AdminUiState,
    adminViewModel: AdminViewModel
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var showReviewedHistory by remember { mutableStateOf(false) }
    var taskToReject by remember { mutableStateOf<VerificationTask?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("") }
    var showRecordDonationDialog by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "রক্তদাতা, ডোনার কার্ড ও রক্তদান যাচাইকরণ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "অপেক্ষমাণ আবেদন: ${adminState.verificationTasks.size} টি • সম্পন্ন রক্তদান: ${adminState.allDonations.size} টি",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showRecordDonationDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("রক্তদান যাচাই", fontSize = 12.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showReviewedHistory,
                    onClick = { showReviewedHistory = false },
                    label = { Text("অপেক্ষমাণ যাচাই (${adminState.verificationTasks.size})") }
                )
                FilterChip(
                    selected = showReviewedHistory,
                    onClick = { showReviewedHistory = true },
                    label = { Text("সকল যাচাই ও রক্তদান রেকর্ড") }
                )
            }
        }
    }

    val tasksToDisplay = if (showReviewedHistory) adminState.allVerificationTasks else adminState.verificationTasks

    if (tasksToDisplay.isEmpty()) {
        EmptyAdminModuleCard(
            if (showReviewedHistory) "কোনো যাচাইয়ের রেকর্ড পাওয়া যায়নি।"
            else "বর্তমানে যাচাইয়ের অপেক্ষায় কোনো আবেদন নেই।"
        )
    } else {
        tasksToDisplay.forEach { task ->
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
                            Text(task.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            val typeLabel = when (task.type) {
                                "ORGANIZATION" -> "প্রতিষ্ঠান যাচাই"
                                "DONOR_CARD" -> "ডিজিটাল ডোনার কার্ড যাচাই"
                                "DONATION_RECORD" -> "রক্তদান সনদ যাচাই"
                                else -> "রক্তদাতা যাচাই"
                            }
                            Text(
                                text = "$typeLabel • জমার সময়: ${sdf.format(Date(task.submittedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusTagChip(
                            label = task.status.labelBn,
                            isHighlight = task.status == VerificationStatus.VERIFIED
                        )
                    }

                    if (task.documents.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "সংযুক্ত নথি: ${task.documents.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (task.reviewedByEmail.isNotBlank() || task.reviewedByUid.isNotBlank()) {
                        Text(
                            text = "পর্যালোচক: ${task.reviewedByEmail.ifBlank { task.reviewedByUid }} • পূর্ববর্তী অবস্থা: ${task.previousStatus.labelBn}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (task.rejectionReason.isNotBlank()) {
                        Text(
                            text = "বাতিলের কারণ: ${task.rejectionReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmergencyRed
                        )
                    }

                    if (task.status == VerificationStatus.PENDING_REVIEW || task.status == VerificationStatus.PENDING) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { adminViewModel.approveTask(task.id) },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("btn_approve_${task.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodSuccessGreen)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("অনুমোদন করুন", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    rejectionReasonInput = ""
                                    taskToReject = task
                                },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("btn_reject_${task.id}"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("বাতিল করুন", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReviewedHistory && adminState.allDonations.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("সাম্প্রতিক যাচাইকৃত রক্তদান সনদসমূহ", fontWeight = FontWeight.Bold)
                adminState.allDonations.take(10).forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${record.donorName} (${record.bloodGroup.display})", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${record.hospitalName}, ${record.district} • সনদ: ${record.certificateCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(sdf.format(Date(record.donationDate)), style = MaterialTheme.typography.labelSmall)
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    taskToReject?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToReject = null },
            title = { Text("যাচাই আবেদন বাতিল নিশ্চিতকরণ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${task.name}-এর আবেদনটি বাতিল করার সুনির্দিষ্ট কারণ উল্লেখ করুন:")
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        label = { Text("বাতিলের কারণ *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.rejectTask(task.id, rejectionReasonInput.trim())
                        taskToReject = null
                    },
                    enabled = rejectionReasonInput.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("বাতিল নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToReject = null }) {
                    Text("ফিরে যান")
                }
            }
        )
    }

    if (showRecordDonationDialog) {
        var donorUid by remember { mutableStateOf("") }
        var donorName by remember { mutableStateOf("") }
        var selectedBg by remember { mutableStateOf(BloodGroup.O_POSITIVE) }
        var hospitalName by remember { mutableStateOf("") }
        var division by remember { mutableStateOf("Dhaka") }
        var district by remember { mutableStateOf("Dhaka") }
        var requestId by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRecordDonationDialog = false },
            title = { Text("সম্পন্ন রক্তদান যাচাই ও সনদ প্রদান", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = donorUid,
                        onValueChange = { donorUid = it },
                        label = { Text("রক্তদাতার ইউজার আইডি *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = donorName,
                        onValueChange = { donorName = it },
                        label = { Text("রক্তদাতার নাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(BloodGroup.entries) { bg ->
                            FilterChip(
                                selected = selectedBg == bg,
                                onClick = { selectedBg = bg },
                                label = { Text(bg.display) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = hospitalName,
                        onValueChange = { hospitalName = it },
                        label = { Text("হাসপাতাল বা ব্লাড ব্যাংকের নাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = {
                            district = it
                            division = LocationHelper.inferDivisionForDistrict(it)
                        },
                        label = { Text("জেলা *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = requestId,
                        onValueChange = { requestId = it },
                        label = { Text("রক্তের অনুরোধ আইডি (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.verifyDonationRecordAsAdmin(
                            donorUid = donorUid,
                            donorName = donorName,
                            bloodGroup = selectedBg,
                            hospitalName = hospitalName,
                            division = division,
                            district = district,
                            upazila = "",
                            requestId = requestId.ifBlank { null }
                        )
                        showRecordDonationDialog = false
                    },
                    enabled = donorUid.isNotBlank() && donorName.length >= 2 && hospitalName.length >= 2
                ) {
                    Text("যাচাই ও রেকর্ড করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordDonationDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// ============================================================================
// D2. Organization Verification Module
// ============================================================================
@Composable
fun AdminOrganizationsModule(
    adminState: AdminUiState,
    adminViewModel: AdminViewModel
) {
    var orgForReject by remember { mutableStateOf<Organization?>(null) }
    var revokeReason by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "হাসপাতাল, ব্লাড ব্যাংক ও স্বেচ্ছাসেবী প্রতিষ্ঠান যাচাই (${adminState.allOrganizations.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "যাচাইকৃত প্রতিষ্ঠান: ${adminState.allOrganizations.count { it.isVerified }} • অপেক্ষমাণ প্রতিষ্ঠান: ${adminState.allOrganizations.count { !it.isVerified }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (adminState.allOrganizations.isEmpty()) {
        EmptyAdminModuleCard("বর্তমানে কোনো নিবন্ধিত প্রতিষ্ঠান নেই।")
    } else {
        adminState.allOrganizations.forEach { org ->
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
                            Text(org.name, fontWeight = FontWeight.Bold)
                            Text(
                                "রেজি নং: ${org.registrationNumber} • ${org.district.ifBlank { org.headquartersCity }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "ফোন: ${org.contactPhone} • সংগৃহীত ব্যাগ: ${org.totalBagsCollected}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusTagChip(
                            label = if (org.isVerified) "যাচাইকৃত" else "যাচাইয়ের অপেক্ষায়",
                            isHighlight = org.isVerified
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!org.isVerified) {
                            Button(
                                onClick = {
                                    adminViewModel.verifyOrganizationDirect(org, verified = true, reason = "সুপার অ্যাডমিন কর্তৃক অনুমোদিত")
                                },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodSuccessGreen)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("প্রতিষ্ঠান যাচাই অনুমোদন করুন", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    revokeReason = ""
                                    orgForReject = org
                                },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("যাচাই অনুমোদন প্রত্যাহার করুন", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    orgForReject?.let { org ->
        AlertDialog(
            onDismissRequest = { orgForReject = null },
            title = { Text("প্রতিষ্ঠানের অনুমোদন প্রত্যাহার", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("'${org.name}'-এর যাচাইকৃত মর্যাদা প্রত্যাহারের কারণ লিখুন:")
                    OutlinedTextField(
                        value = revokeReason,
                        onValueChange = { revokeReason = it },
                        label = { Text("কারণ *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.verifyOrganizationDirect(org, verified = false, reason = revokeReason)
                        orgForReject = null
                    },
                    enabled = revokeReason.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("প্রত্যাহার নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { orgForReject = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
