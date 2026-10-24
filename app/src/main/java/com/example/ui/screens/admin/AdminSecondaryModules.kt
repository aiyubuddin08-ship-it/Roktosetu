package com.example.ui.screens.admin

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuditLogEntry
import com.example.model.UserReport
import com.example.ui.theme.BloodPendingAmber
import com.example.ui.theme.BloodSuccessContainer
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// E. Notifications & Announcements Module (Preview NEVER auto-sends!)
// ============================================================================
@Composable
fun AdminNotificationsModule(
    adminState: AdminUiState,
    adminViewModel: AdminViewModel
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var titleBn by remember { mutableStateOf("") }
    var bodyBn by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("GENERAL") }
    var targetAudience by remember { mutableStateOf("ALL_USERS") }
    var targetDivision by remember { mutableStateOf("") }
    var targetDistrict by remember { mutableStateOf("") }

    var showPreviewDialog by remember { mutableStateOf(false) }
    var showConfirmSendDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "নোটিফিকেশন ও ঘোষণা প্রস্তুতকারক",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "প্রিভিউ দেখলে কখনোই প্রকৃত নোটিফিকেশন পাঠানো হবে না। শুধুমাত্র চূড়ান্ত নিশ্চিতকরণের পর বার্তা প্রেরিত হবে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = titleBn,
                onValueChange = { titleBn = it },
                label = { Text("বিজ্ঞপ্তির শিরোনাম (বাংলায়) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_announcement_title")
            )

            OutlinedTextField(
                value = bodyBn,
                onValueChange = { bodyBn = it },
                label = { Text("বিস্তারিত বার্তা *") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("input_announcement_body")
            )

            Text("প্রাপক নির্বাচন:", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val audiences = listOf(
                    "ALL_USERS" to "সকল ব্যবহারকারী",
                    "ACTIVE_DONORS" to "সক্রিয় রক্তদাতা",
                    "VERIFIED_DONORS" to "যাচাইকৃত রক্তদাতা"
                )
                items(audiences) { (code, label) ->
                    FilterChip(
                        selected = targetAudience == code,
                        onClick = { targetAudience = code },
                        label = { Text(label) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = targetDivision,
                    onValueChange = { targetDivision = it },
                    label = { Text("বিভাগ (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = targetDistrict,
                    onValueChange = { targetDistrict = it },
                    label = { Text("জেলা (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showPreviewDialog = true },
                    enabled = titleBn.isNotBlank() && bodyBn.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("প্রিভিউ দেখুন", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        adminViewModel.saveOrPublishAnnouncement(
                            titleBn = titleBn,
                            bodyBn = bodyBn,
                            category = category,
                            targetAudience = targetAudience,
                            targetDivision = targetDivision,
                            targetDistrict = targetDistrict,
                            publishNow = false
                        )
                        titleBn = ""
                        bodyBn = ""
                    },
                    enabled = titleBn.length >= 3 && bodyBn.length >= 5,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("খসড়া সংরক্ষণ", fontSize = 12.sp)
                }

                Button(
                    onClick = { showConfirmSendDialog = true },
                    enabled = titleBn.length >= 3 && bodyBn.length >= 5,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("প্রেরণ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "পূর্বে প্রেরিত ও খসড়া বিজ্ঞপ্তির তালিকা (${adminState.announcements.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (adminState.announcements.isEmpty()) {
                Text("এখনো কোনো ঘোষণা সংরক্ষিত বা প্রেরিত হয়নি।", style = MaterialTheme.typography.bodySmall)
            } else {
                adminState.announcements.forEach { ann ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ann.titleBn, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            StatusTagChip(
                                label = if (ann.status == "PUBLISHED") "প্রকাশিত (${ann.recipientCount} জন)" else "খসড়া",
                                isHighlight = ann.status == "PUBLISHED"
                            )
                        }
                        Text(ann.bodyBn, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "সময়: ${sdf.format(Date(ann.createdAt))} • প্রাপক: ${ann.targetAudience}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("নোটিফিকেশন প্রিভিউ (পাঠানো হয়নি)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(titleBn, fontWeight = FontWeight.Bold)
                            Text(bodyBn, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        text = "লক্ষ্যভুক্ত প্রাপক: $targetAudience ${if (targetDistrict.isNotBlank()) "($targetDistrict)" else ""}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "নোট: এটি শুধুমাত্র প্রিভিউ। এই ডায়ালগ থেকে কোনো নোটিফিকেশন পাঠানো হবে না।",
                        style = MaterialTheme.typography.labelSmall,
                        color = BloodPendingAmber
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("প্রিভিউ বন্ধ করুন")
                }
            }
        )
    }

    if (showConfirmSendDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmSendDialog = false },
            title = { Text("নোটিফিকেশন প্রেরণ নিশ্চিতকরণ", fontWeight = FontWeight.Bold) },
            text = {
                Text("আপনি কি নিশ্চিত যে '$titleBn' শিরোনামের বিজ্ঞপ্তিটি নির্বাচিত ব্যবহারকারীদের কাছে এখনই পাঠাতে চান?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.saveOrPublishAnnouncement(
                            titleBn = titleBn,
                            bodyBn = bodyBn,
                            category = category,
                            targetAudience = targetAudience,
                            targetDivision = targetDivision,
                            targetDistrict = targetDistrict,
                            publishNow = true
                        )
                        titleBn = ""
                        bodyBn = ""
                        showConfirmSendDialog = false
                    }
                ) {
                    Text("হ্যাঁ, এখনই পাঠান")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSendDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// ============================================================================
// F. Reports & Complaints Module
// ============================================================================
@Composable
fun AdminComplaintsModule(
    adminState: AdminUiState,
    adminViewModel: AdminViewModel
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var reportToReview by remember { mutableStateOf<UserReport?>(null) }
    var showCreateReportDialog by remember { mutableStateOf(false) }

    val filteredReports = remember(adminState.reports, selectedStatusFilter) {
        adminState.reports.filter { r ->
            selectedStatusFilter == null || r.status == selectedStatusFilter
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
                    text = "অভিযোগ ও অপব্যবহার পর্যালোচনা (${filteredReports.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                OutlinedButton(
                    onClick = { showCreateReportDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নতুন অভিযোগ নথিভুক্ত", fontSize = 12.sp)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val statuses = listOf(
                    null to "সকল",
                    "OPEN" to "নতুন (Open)",
                    "IN_REVIEW" to "পর্যালোচনাধীন",
                    "RESOLVED" to "নিষ্পত্তিকৃত",
                    "DISMISSED" to "খারিজ"
                )
                items(statuses) { (code, label) ->
                    FilterChip(
                        selected = selectedStatusFilter == code,
                        onClick = { selectedStatusFilter = code },
                        label = { Text(label) }
                    )
                }
            }
        }
    }

    if (filteredReports.isEmpty()) {
        EmptyAdminModuleCard("বর্তমানে এই তালিকায় কোনো অভিযোগ নেই।")
    } else {
        filteredReports.forEach { report ->
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
                            Text(
                                text = "অভিযুক্ত: ${report.targetName.ifBlank { report.targetUid }} (${report.targetType})",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "অভিযোগকারী: ${report.reporterName} • গুরুত্ব: ${report.severity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusTagChip(
                            label = report.status,
                            isHighlight = report.status == "RESOLVED"
                        )
                    }

                    Text("কারণ: ${report.reasonBn}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    if (report.details.isNotBlank()) {
                        Text(report.details, style = MaterialTheme.typography.bodySmall)
                    }
                    if (report.resolutionNotes.isNotBlank()) {
                        Text(
                            text = "নিষ্পত্তির নোট: ${report.resolutionNotes} (পদক্ষেপ: ${report.actionTaken})",
                            style = MaterialTheme.typography.labelSmall,
                            color = BloodSuccessGreen
                        )
                    }
                    Text(
                        text = "সময়: ${sdf.format(Date(report.createdAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { reportToReview = report },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("পর্যালোচনা ও পদক্ষেপ নিন")
                    }
                }
            }
        }
    }

    reportToReview?.let { target ->
        var newStatus by remember(target) { mutableStateOf("RESOLVED") }
        var notes by remember(target) { mutableStateOf(target.resolutionNotes) }
        var actionTaken by remember(target) { mutableStateOf(target.actionTaken.ifBlank { "সতর্কবার্তা প্রদান ও পর্যালোচনা সম্পন্ন" }) }

        AlertDialog(
            onDismissRequest = { reportToReview = null },
            title = { Text("অভিযোগ পর্যালোচনা ও নিষ্পত্তি", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("অভিযুক্ত: ${target.targetName} • কারণ: ${target.reasonBn}")
                    listOf(
                        "IN_REVIEW" to "পর্যালোচনাধীন (In Review)",
                        "RESOLVED" to "নিষ্পত্তিকৃত (Resolved)",
                        "DISMISSED" to "খারিজ (Dismissed)"
                    ).forEach { (st, label) ->
                        FilterChip(
                            selected = newStatus == st,
                            onClick = { newStatus = st },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = actionTaken,
                        onValueChange = { actionTaken = it },
                        label = { Text("গৃহীত প্রশাসনিক পদক্ষেপ *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("বিস্তারিত নিষ্পত্তি নোট *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.reviewReport(target, newStatus, notes, actionTaken)
                        reportToReview = null
                    },
                    enabled = notes.trim().length >= 3
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToReview = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showCreateReportDialog) {
        var targetUid by remember { mutableStateOf("") }
        var targetName by remember { mutableStateOf("") }
        var reasonBn by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        var severity by remember { mutableStateOf("HIGH") }

        AlertDialog(
            onDismissRequest = { showCreateReportDialog = false },
            title = { Text("নতুন অভিযোগ নথিভুক্ত করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetName,
                        onValueChange = { targetName = it },
                        label = { Text("অভিযুক্ত অ্যাকাউন্ট বা অনুরোধের নাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetUid,
                        onValueChange = { targetUid = it },
                        label = { Text("অভিযুক্ত আইডি") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reasonBn,
                        onValueChange = { reasonBn = it },
                        label = { Text("অভিযোগের কারণ (যেমন: ভুয়া অনুরোধ / অর্থ দাবি) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("বিস্তারিত বিবরণ (কমপক্ষে ১০ অক্ষর) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.submitSampleOrAdminReport(
                            targetUid = targetUid.ifBlank { "unknown" },
                            targetName = targetName,
                            targetType = "USER",
                            category = "OTHER",
                            reasonBn = reasonBn,
                            details = details,
                            severity = severity
                        )
                        showCreateReportDialog = false
                    },
                    enabled = targetName.length >= 2 && reasonBn.length >= 3 && details.trim().length >= 10
                ) {
                    Text("জমা দিন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateReportDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// ============================================================================
// G. System Configuration, Admin Roles, Directory Import, Health & Export
// ============================================================================
@Composable
fun AdminSystemSettingsModule(
    adminState: AdminUiState,
    adminViewModel: AdminViewModel
) {
    val context = LocalContext.current
    val currentConfig = adminState.systemConfig
    var maintenanceMode by remember(currentConfig) { mutableStateOf(currentConfig.maintenanceMode) }
    var maintenanceMsg by remember(currentConfig) { mutableStateOf(currentConfig.maintenanceMessageBn) }
    var registrationEnabled by remember(currentConfig) { mutableStateOf(currentConfig.registrationEnabled) }
    var donorRegEnabled by remember(currentConfig) { mutableStateOf(currentConfig.donorRegistrationEnabled) }
    var emergencyArmed by remember(currentConfig) { mutableStateOf(currentConfig.emergencyBroadcastArmed) }
    var notificationsEnabled by remember(currentConfig) { mutableStateOf(currentConfig.notificationsEnabled) }
    var expiryHoursText by remember(currentConfig) { mutableStateOf(currentConfig.requestExpiryHours.toString()) }
    var bannerText by remember(currentConfig) { mutableStateOf(currentConfig.announcementBannerBn) }
    var showConfirmSave by remember { mutableStateOf(false) }

    // 1. System Config & Feature Flags Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "সিস্টেম কনফিগারেশন ও ফিচার নিয়ন্ত্রণ",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            ConfigToggleRow(
                title = "রক্ষণাবেক্ষণ মোড (Maintenance Mode)",
                subtitle = "চালু থাকলে সাধারণ ব্যবহারকারীদের জন্য রক্ষণাবেক্ষণ বার্তা প্রদর্শিত হবে",
                checked = maintenanceMode,
                onCheckedChange = { maintenanceMode = it }
            )
            ConfigToggleRow(
                title = "নতুন অ্যাকাউন্ট নিবন্ধন",
                subtitle = "নতুন ব্যবহারকারী নিবন্ধন সুবিধা চালু/বন্ধ রাখুন",
                checked = registrationEnabled,
                onCheckedChange = { registrationEnabled = it }
            )
            ConfigToggleRow(
                title = "রক্তদাতা হিসেবে তালিকাভুক্তি",
                subtitle = "পাবলিক ডোনার ডিরেক্টরিতে নতুন নিবন্ধন নিয়ন্ত্রণ",
                checked = donorRegEnabled,
                onCheckedChange = { donorRegEnabled = it }
            )
            ConfigToggleRow(
                title = "জরুরি রক্তের বিজ্ঞপ্তি সম্প্রচার",
                subtitle = "জরুরি রক্তের আবেদনের পুশ ও ইন-অ্যাপ সতর্কতা ইঞ্জিন",
                checked = emergencyArmed,
                onCheckedChange = { emergencyArmed = it }
            )
            ConfigToggleRow(
                title = "সিস্টেম নোটিফিকেশন সেবা",
                subtitle = "সাধারণ ঘোষণা ও যাচাই নোটিফিকেশন প্রেরণ নিয়ন্ত্রণ",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            OutlinedTextField(
                value = expiryHoursText,
                onValueChange = { expiryHoursText = it.filter { ch -> ch.isDigit() } },
                label = { Text("রক্তের অনুরোধের মেয়াদ (ঘণ্টায়, ১২ - ১৬৮ ঘণ্টা)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bannerText,
                onValueChange = { bannerText = it },
                label = { Text("হোম স্ক্রিন জরুরি ঘোষণা ব্যানার (খালি রাখলে বন্ধ থাকবে)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (maintenanceMode) {
                OutlinedTextField(
                    value = maintenanceMsg,
                    onValueChange = { maintenanceMsg = it },
                    label = { Text("রক্ষণাবেক্ষণ বার্তা") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { showConfirmSave = true },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("btn_save_system_config"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("কনফিগারেশন সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }

    // 2. Default Directory Data Import Card (Phase 4)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ব্লাড ব্যাংক, অ্যাম্বুলেন্স ও ক্যাম্পের প্রাথমিক তথ্য",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                text = "ডুপ্লিকেট ছাড়াই নির্দিষ্ট আইডি ব্যবহার করে ডিফল্ট ব্লাড ব্যাংক, অ্যাম্বুলেন্স ও ক্যাম্পের তালিকা ডেটাবেসে আমদানি করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { adminViewModel.importDefaultDirectoryData() },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("btn_import_default_directory_data"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ডিফল্ট ডেটা আমদানি করুন", fontWeight = FontWeight.Bold)
            }
        }
    }

    // 3. Admin Roles & Permissions Card (Jargon-free)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "প্রশাসনিক ভূমিকা ও অনুমতি তালিকা",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "নিরাপত্তা নীতিমালা অনুযায়ী সুপার অ্যাডমিন ভূমিকা শুধুমাত্র সার্ভারের অনুমোদিত তালিকা দ্বারা নিয়ন্ত্রিত হয়; সাধারণ অ্যাপ থেকে নতুন সুপার অ্যাডমিন তৈরি করা সম্পূর্ণ নিষিদ্ধ।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (adminState.adminRegistry.isEmpty()) {
                val ver = adminState.adminVerification
                if (ver != null && ver.isVerifiedAdmin) {
                    Text(
                        text = "• সক্রিয় সুপার অ্যাডমিন: ${ver.email.ifBlank { ver.uid }} (যাচাইকৃত)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BloodSuccessGreen
                    )
                }
            } else {
                adminState.adminRegistry.forEach { entry ->
                    Text(
                        text = "• ${entry["email"].toString().ifBlank { entry["uid"].toString() }} — ভূমিকা: ${entry["role"]}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // 4. Database Health, Analytics & Export Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "ডেটাবেস স্বাস্থ্য, বিশ্লেষণ ও রিপোর্ট এক্সপোর্ট",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "• ডেটাবেস সংযোগের অবস্থা: ${adminState.metrics.serverStatus}\n" +
                    "• অ্যাপ নিরাপত্তা যাচাই: ${adminState.metrics.appCheckStatus}\n" +
                    "• অফলাইন সংরক্ষণ ও সিঙ্ক: সক্রিয়",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedButton(
                onClick = {
                    val m = adminState.metrics
                    val reportSummary = buildString {
                        appendLine("রক্তসেতু — সুপার অ্যাডমিন অপারেশনাল রিপোর্ট")
                        appendLine("মোট নিবন্ধিত ব্যবহারকারী: ${m.totalRegisteredUsers}")
                        appendLine("সক্রিয় রক্তদাতা: ${m.activeDonors}")
                        appendLine("যাচাইয়ের অপেক্ষায় আবেদন: ${m.pendingVerifications}")
                        appendLine("চলমান রক্তের অনুরোধ: ${m.pendingRequests}")
                        appendLine("জরুরি রক্তের অনুরোধ: ${m.emergencyRequests}")
                        appendLine("সম্পন্ন রক্তদান: ${m.totalDonationsCompleted}")
                        appendLine("যাচাইকৃত প্রতিষ্ঠান: ${m.verifiedOrganizations}")
                        appendLine("নতুন অভিযোগ: ${m.openReportsCount}")
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "রক্তসেতু প্রশাসনিক রিপোর্ট")
                        putExtra(Intent.EXTRA_TEXT, reportSummary)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "প্রশাসনিক রিপোর্ট এক্সপোর্ট করুন"))
                },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("অপারেশনাল রিপোর্ট এক্সপোর্ট / শেয়ার করুন")
            }
        }
    }

    if (showConfirmSave) {
        AlertDialog(
            onDismissRequest = { showConfirmSave = false },
            title = { Text("সিস্টেম কনফিগারেশন পরিবর্তন নিশ্চিতকরণ", fontWeight = FontWeight.Bold) },
            text = {
                Text("আপনি কি সিস্টেম কনফিগারেশন ও ফিচার ফ্ল্যাগগুলোর পরিবর্তন সংরক্ষণ করতে চান? এই পরিবর্তন সাথে সাথে কার্যকর হবে এবং কার্যক্রমের ইতিহাসে সংরক্ষিত হবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = expiryHoursText.toIntOrNull() ?: 72
                        adminViewModel.updateSystemConfig(
                            currentConfig.copy(
                                maintenanceMode = maintenanceMode,
                                maintenanceMessageBn = maintenanceMsg,
                                registrationEnabled = registrationEnabled,
                                donorRegistrationEnabled = donorRegEnabled,
                                emergencyBroadcastArmed = emergencyArmed,
                                notificationsEnabled = notificationsEnabled,
                                requestExpiryHours = hours,
                                announcementBannerBn = bannerText
                            )
                        )
                        showConfirmSave = false
                    }
                ) {
                    Text("নিশ্চিত ও সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSave = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// ============================================================================
// H. Administrative Audit Logs Module
// ============================================================================
@Composable
fun AdminAuditLogsModule(
    auditLogs: List<AuditLogEntry>
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(auditLogs, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) auditLogs
        else auditLogs.filter {
            it.action.contains(q, ignoreCase = true) ||
                it.actorEmail.contains(q, ignoreCase = true) ||
                it.targetId.contains(q, ignoreCase = true) ||
                it.targetType.contains(q, ignoreCase = true)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "অ্যাডমিন কার্যক্রমের ইতিহাস (${filteredLogs.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "প্রতিটি সংবেদনশীল প্রশাসনিক কাজ অপরিবর্তনীয়ভাবে সংরক্ষিত হয়।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("কাজের ধরন, ইমেইল বা আইডি দিয়ে খুঁজুন") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (filteredLogs.isEmpty()) {
        EmptyAdminModuleCard("কোনো প্রশাসনিক কার্যক্রমের রেকর্ড পাওয়া যায়নি।")
    } else {
        filteredLogs.forEach { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatAuditActionBn(log.action),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "প্রশাসক: ${log.actorEmail.ifBlank { log.actorUid }} • লক্ষ্য: ${log.targetType} (${log.targetId})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (log.details.isNotEmpty()) {
                            Text(
                                text = log.details.entries.joinToString(" • ") { "${it.key}: ${it.value}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "সময়: ${sdf.format(Date(log.timestamp))} • ফলাফল: ${if (log.outcome == "SUCCESS") "সফল" else log.outcome}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BloodSuccessGreen
                        )
                    }
                }
            }
        }
    }
}

fun formatAuditActionBn(rawAction: String): String {
    return when (rawAction) {
        "VERIFICATION_APPROVED" -> "যাচাই আবেদন অনুমোদন"
        "VERIFICATION_REJECTED" -> "যাচাই আবেদন বাতিল"
        "EMERGENCY_BROADCAST_ARMED" -> "জরুরি বিজ্ঞপ্তি সিস্টেম সক্রিয়করণ"
        "EMERGENCY_BROADCAST_PAUSED" -> "জরুরি বিজ্ঞপ্তি সিস্টেম বিরতকরণ"
        "EMERGENCY_BROADCAST_DISPATCHED" -> "জরুরি রক্তের বিজ্ঞপ্তি প্রেরণ"
        "USER_ACCOUNT_SUSPENDED" -> "ব্যবহারকারী অ্যাকাউন্ট স্থগিতকরণ"
        "USER_ACCOUNT_RESTORED" -> "ব্যবহারকারী অ্যাকাউন্ট পুনরায় সক্রিয়করণ"
        "DONOR_STATUS_UPDATED" -> "রক্তদাতা স্ট্যাটাস হালনাগাদ"
        "EXPIRE_STALE_REQUESTS" -> "মেয়াদোত্তীর্ণ অনুরোধ নিষ্পত্তি"
        "ORGANIZATION_VERIFIED" -> "প্রতিষ্ঠান যাচাই অনুমোদন"
        "ORGANIZATION_UNVERIFIED" -> "প্রতিষ্ঠানের অনুমোদন প্রত্যাহার"
        "ANNOUNCEMENT_PUBLISHED" -> "সিস্টেম ঘোষণা ও নোটিফিকেশন প্রকাশ"
        "ANNOUNCEMENT_DRAFT_SAVED" -> "ঘোষণা খসড়া সংরক্ষণ"
        "SYSTEM_CONFIG_UPDATED" -> "সিস্টেম কনফিগারেশন হালনাগাদ"
        "SECURITY_ALERT_REVIEWED" -> "নিরাপত্তা সতর্কতা পর্যালোচনা"
        "ADMIN_ACCESS_VERIFIED" -> "সুপার অ্যাডমিন প্রবেশাধিকার যাচাই"
        "BLOOD_BANK_SAVED" -> "ব্লাড ব্যাংক সংরক্ষণ/সম্পাদনা"
        "BLOOD_BANK_DELETED" -> "ব্লাড ব্যাংক মুছে ফেলা"
        "AMBULANCE_SAVED" -> "অ্যাম্বুলেন্স সেবা সংরক্ষণ/সম্পাদনা"
        "AMBULANCE_DELETED" -> "অ্যাম্বুলেন্স সেবা মুছে ফেলা"
        "BLOOD_CAMP_SAVED" -> "রক্তদান ক্যাম্প সংরক্ষণ/সম্পাদনা"
        "BLOOD_CAMP_DELETED" -> "রক্তদান ক্যাম্প মুছে ফেলা"
        "DEFAULT_DIRECTORY_IMPORTED" -> "ডিফল্ট ডিরেক্টরি ডেটা আমদানি"
        else -> rawAction
    }
}

@Composable
fun ConfigToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun StatusTagChip(label: String, isHighlight: Boolean) {
    Surface(
        color = if (isHighlight) BloodSuccessContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = if (isHighlight) BloodSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyAdminModuleCard(messageBn: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = BloodSuccessGreen,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = messageBn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatAdminTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "প্রযোজ্য নয়"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
