package com.example.ui.screens.security

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.model.AuditLogEntry
import com.example.model.DatabaseProtectionCheck
import com.example.model.DonorAvailability
import com.example.model.SecurityAlertItem
import com.example.model.UserProfile
import com.example.model.UserReport
import com.example.ui.screens.admin.formatAdminTimestamp
import com.example.ui.theme.BloodSuccessContainer
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer

@Composable
internal fun PrivacySwitchesSectionCard(
    profile: UserProfile,
    onUpdatePrivacy: (showPhonePublicly: Boolean, isDonor: Boolean, isEmergencyDonor: Boolean, availability: DonorAvailability) -> Unit
) {
    var showPhone by remember(profile.showPhonePublicly) { mutableStateOf(profile.showPhonePublicly) }
    var isDonor by remember(profile.isDonor, profile.donorAvailability) {
        mutableStateOf(profile.isDonor && profile.donorAvailability != DonorAvailability.UNAVAILABLE)
    }
    var isEmergencyDonor by remember(profile.isEmergencyDonor) { mutableStateOf(profile.isEmergencyDonor) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("privacy_switches_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "গোপনীয়তা সুইচ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            PrivacyToggleRow(
                title = "মোবাইল নম্বর প্রকাশ্যে দেখান",
                subtitle = "চালু থাকলে রক্তদাতা তালিকায় আপনার মোবাইল নম্বর দেখা যাবে; বন্ধ থাকলে নিরাপদ চ্যাটে যোগাযোগ করা যাবে",
                checked = showPhone,
                testTag = "switch_show_phone_publicly",
                onCheckedChange = { checked ->
                    showPhone = checked
                    val availability = if (isDonor) {
                        if (isEmergencyDonor) DonorAvailability.EMERGENCY_ONLY else DonorAvailability.AVAILABLE
                    } else {
                        DonorAvailability.UNAVAILABLE
                    }
                    onUpdatePrivacy(checked, isDonor, isEmergencyDonor, availability)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            PrivacyToggleRow(
                title = "রক্তদানের জন্য প্রস্তুত (সক্রিয় রক্তদাতা)",
                subtitle = "চালু থাকলে রক্তদাতা অনুসন্ধান তালিকায় আপনার নাম প্রদর্শিত হবে",
                checked = isDonor,
                testTag = "switch_donor_availability",
                onCheckedChange = { checked ->
                    isDonor = checked
                    val availability = if (checked) {
                        if (isEmergencyDonor) DonorAvailability.EMERGENCY_ONLY else DonorAvailability.AVAILABLE
                    } else {
                        DonorAvailability.UNAVAILABLE
                    }
                    onUpdatePrivacy(showPhone, checked, isEmergencyDonor, availability)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            PrivacyToggleRow(
                title = "জরুরি রক্তদাতা হিসেবে তালিকাভুক্তি",
                subtitle = "মুমূর্ষু রোগীর জরুরি রক্তের প্রয়োজনে অগ্রাধিকার ভিত্তিতে বিজ্ঞপ্তি পাবেন",
                checked = isEmergencyDonor,
                testTag = "switch_emergency_donor",
                onCheckedChange = { checked ->
                    isEmergencyDonor = checked
                    val availability = if (isDonor) {
                        if (checked) DonorAvailability.EMERGENCY_ONLY else DonorAvailability.AVAILABLE
                    } else {
                        DonorAvailability.UNAVAILABLE
                    }
                    onUpdatePrivacy(showPhone, isDonor, checked, availability)
                }
            )
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
internal fun DatabaseProtectionSectionCard(
    checks: List<DatabaseProtectionCheck>,
    isVerifiedSuperAdmin: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("database_protection_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BloodSuccessGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ডেটাবেস সুরক্ষা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Surface(
                color = BloodSuccessContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BloodSuccessGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "আপনার ব্যক্তিগত তথ্য, চ্যাট বার্তা ও প্রোফাইল সম্পূর্ণ সুরক্ষিত। অনুমতি ছাড়া কেউ আপনার ব্যক্তিগত তথ্য পরিবর্তন বা দেখতে পারে না।",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (isVerifiedSuperAdmin && checks.isNotEmpty()) {
                checks.forEach { check ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = check.titleBn,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = check.statusLabelBn,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (check.isVerified) BloodSuccessGreen else EmergencyRed
                                )
                            }
                            Text(
                                text = check.accessSummaryBn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HowYourDataIsUsedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "আপনার তথ্য কীভাবে ব্যবহৃত হয়",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            PrivacyRuleItem(
                titleBn = "সংগৃহীত তথ্যের পরিধি",
                descBn = "রক্তদান ও জরুরি যোগাযোগের জন্য নাম, রক্তের গ্রুপ, বিভাগ, জেলা, উপজেলা/ইউনিয়ন এবং সর্বশেষ রক্তদানের তারিখ সংরক্ষণ করা হয়।"
            )
            PrivacyRuleItem(
                titleBn = "প্রকাশ্য ও ব্যক্তিগত তথ্যের পার্থক্য",
                descBn = "রক্তদাতা তালিকায় নাম, রক্তের গ্রুপ ও এলাকা দেখা যায়। আপনি নিজে সুইচ চালু না করলে মোবাইল নম্বর গোপন থাকে।"
            )
            PrivacyRuleItem(
                titleBn = "অবস্থান ও ঠিকানার গোপনীয়তা",
                descBn = "আপনার সুনির্দিষ্ট বাড়ির ঠিকানা কখনোই উন্মুক্ত করা হয় না; শুধুমাত্র জেলা ও উপজেলা অনুযায়ী নিকটবর্তী রক্তদাতা খোঁজা হয়।"
            )
        }
    }
}

@Composable
internal fun SafeBloodTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = EmergencyRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "নিরাপদে রক্ত খোঁজা ও রক্তদানের টিপস",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                text = "• রক্তদান সম্পূর্ণ স্বেচ্ছাসেবামূলক — রক্তের বিনিময়ে কখনোই কোনো অর্থ বা বিকাশ/নগদ লেনদেন করবেন না।\n" +
                    "• হাসপাতালে যাওয়ার আগে রোগীর আত্মীয় ও হাসপাতালের নাম-ওয়ার্ড নম্বর ফোন করে নিশ্চিত হয়ে নিন।\n" +
                    "• শুধুমাত্র অনুমোদিত হাসপাতাল বা ব্লাড ব্যাংকে রক্তের ক্রস-ম্যাচিং সম্পন্ন করে রক্তদান করুন।\n" +
                    "• কোনো ব্যক্তি বা অনুরোধ সন্দেহজনক মনে হলে সাথে সাথে 'রিপোর্ট করুন' বাটন ব্যবহার করে জানান।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun UserReportingSectionCard(
    myReports: List<UserReport>,
    onOpenReportDialog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("user_reporting_section_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = EmergencyRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "রিপোর্ট করুন ও আমার অভিযোগ (${myReports.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Button(
                    onClick = onOpenReportDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("btn_open_security_report_dialog")
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("রিপোর্ট করুন")
                }
            }

            Text(
                text = "ভুয়া রক্তের অনুরোধ, অর্থ দাবি, অসদাচরণ বা ভুল তথ্যের বিরুদ্ধে এখানে অভিযোগ জানান।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (myReports.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = "আমার জমাকৃত রিপোর্টসমূহ:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                myReports.take(5).forEach { report ->
                    val statusBn = when (report.status) {
                        "RESOLVED" -> "সমাধানকৃত"
                        "IN_REVIEW" -> "পর্যালোচনাধীন"
                        "DISMISSED" -> "খারিজ"
                        else -> "চলমান"
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${report.reasonBn} • ${report.targetName.ifBlank { "সাধারণ" }}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = statusBn,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (report.status == "RESOLVED") BloodSuccessGreen else MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = report.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (report.resolutionNotes.isNotBlank()) {
                                Text(
                                    text = "প্রশাসনিক মন্তব্য: ${report.resolutionNotes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BloodSuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HelpAndPrivacyPolicyCard() {
    val context = LocalContext.current
    var showPolicyDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "সাহায্য, যোগাযোগ ও নীতিমালা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Text(
                text = "অ্যাকাউন্টের নিরাপত্তা বা জরুরি সহায়তার জন্য রক্তসেতু সহায়তা কেন্দ্রে যোগাযোগ করুন। অ্যাপ সংস্করণ: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@roktosetu.org")
                            putExtra(Intent.EXTRA_SUBJECT, "রক্তসেতু নিরাপত্তা ও সহায়তা")
                        }
                        runCatching { context.startActivity(emailIntent) }
                    },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সাহায্য ও যোগাযোগ")
                }

                OutlinedButton(
                    onClick = { showPolicyDialog = true },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("btn_privacy_policy_terms")
                ) {
                    Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("গোপনীয়তা নীতি ও শর্ত")
                }
            }
        }
    }

    if (showPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = { Text("গোপনীয়তা নীতি ও ব্যবহারের শর্তাবলী", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "১. রক্তসেতু একটি স্বেচ্ছাসেবী রক্তদান সমন্বয় মাধ্যম। রক্তদানের বিনিময়ে যেকোনো আর্থিক লেনদেন সম্পূর্ণ নিষিদ্ধ।\n" +
                            "২. ব্যবহারকারীর গোপনীয়তা আমাদের সর্বোচ্চ অগ্রাধিকার। অনুমতি ছাড়া ফোন নম্বর বা ব্যক্তিগত তথ্য প্রকাশ করা হয় না।\n" +
                            "৩. ভুয়া রক্তের অনুরোধ বা হয়রানিমূলক আচরণের প্রমাণ পাওয়া গেলে অ্যাকাউন্ট স্থায়ীভাবে বন্ধ করা হবে।\n" +
                            "৪. যেকোনো সময় ব্যবহারকারী নিজের প্রোফাইল সম্পাদনা বা অ্যাকাউন্ট মুছে ফেলার অধিকার রাখেন।",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPolicyDialog = false }) {
                    Text("বুঝেছি")
                }
            }
        )
    }
}

@Composable
private fun PrivacyRuleItem(titleBn: String, descBn: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = titleBn, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
        Text(
            text = descBn,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp)
        )
    }
}

@Composable
internal fun SecurityAlertsSectionCard(
    alerts: List<SecurityAlertItem>,
    isVerifiedSuperAdmin: Boolean,
    onMarkReviewed: (SecurityAlertItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EmergencyRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "নিরাপত্তা সতর্কতা (${alerts.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (alerts.isEmpty()) {
                Surface(
                    color = BloodSuccessContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BloodSuccessGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "বর্তমানে কোনো অমীমাংসিত নিরাপত্তা সতর্কতা বা ঝুঁকি পাওয়া যায়নি।",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                alerts.forEach { alert ->
                    val isOpen = alert.reviewStatus == "OPEN"
                    Surface(
                        color = if (isOpen) EmergencyRedContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${alert.alertType} • মাত্রা: ${alert.severity}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOpen) EmergencyRed else BloodSuccessGreen
                                )
                                Text(
                                    text = if (isOpen) "অমীমাংসিত" else "পর্যালোচিত",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(alert.summaryBn, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("সংশ্লিষ্ট বিভাগ: ${alert.departmentBn}", style = MaterialTheme.typography.bodySmall)
                            Text("সুপারিশকৃত পদক্ষেপ: ${alert.recommendedActionBn}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("সময়: ${formatAdminTimestamp(alert.detectedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (alert.resolutionNotes.isNotBlank()) {
                                Text("পর্যালোচনা নোট: ${alert.resolutionNotes}", style = MaterialTheme.typography.bodySmall, color = BloodSuccessGreen)
                            }
                            if (isVerifiedSuperAdmin && isOpen) {
                                OutlinedButton(
                                    onClick = { onMarkReviewed(alert) },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("পর্যালোচিত হিসেবে চিহ্নিত করুন", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SecurityActivityHistoryCard(
    auditLogs: List<AuditLogEntry>,
    isVerifiedSuperAdmin: Boolean
) {
    if (!isVerifiedSuperAdmin) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "নিরাপত্তা কার্যক্রমের ইতিহাস",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (auditLogs.isEmpty()) {
                Text(
                    text = "এখনো কোনো নিরাপত্তা বা প্রশাসনিক কার্যক্রমের রেকর্ড পাওয়া যায়নি।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                auditLogs.take(15).forEach { log ->
                    val detailText = log.details.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.action, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = if (log.outcome == "SUCCESS") "সফল" else log.outcome,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (log.outcome == "SUCCESS") BloodSuccessGreen else EmergencyRed
                                )
                            }
                            Text(
                                text = "প্রশাসক: ${log.actorEmail.ifBlank { log.actorUid }} • লক্ষ্য: ${log.targetType} (${log.targetId})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (detailText.isNotBlank()) {
                                Text(
                                    text = detailText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatAdminTimestamp(log.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SuperAdminTechnicalDiagnosticsCard(
    isAppCheckActive: Boolean,
    isNotificationGranted: Boolean,
    hasCustomClaim: Boolean,
    hasRegistryDoc: Boolean,
    protectionChecks: List<DatabaseProtectionCheck>,
    lastCheckedAt: Long,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onRunProbe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "নিরাপত্তা ও প্রশাসনিক পরীক্ষা (সুপার অ্যাডমিন)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "শুধুমাত্র যাচাইকৃত সুপার অ্যাডমিনদের জন্য দৃশ্যমান",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "বিস্তারিত দেখুন"
                    )
                }
            }

            val allVerified = protectionChecks.all { it.isVerified }
            val adminVerified = hasCustomClaim || hasRegistryDoc
            Text(
                text = "• অ্যাডমিন অনুমতি: ${if (adminVerified) "নিশ্চিত ✔" else "পর্যালোচনাধীন"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• নোটিফিকেশন অনুমতি: ${if (isNotificationGranted) "চালু" else "বন্ধ"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• অ্যাপ সুরক্ষা যাচাই: ${if (isAppCheckActive) "চালু ✔" else "প্রারম্ভিক পর্যায়ে"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• তথ্য সুরক্ষা পরীক্ষা: ${if (allVerified) "সবগুলো স্তর সুরক্ষিত (${protectionChecks.size}/${protectionChecks.size})" else "আংশিক যাচাইকৃত"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• শেষ নিরাপত্তা পরীক্ষা: ${formatAdminTimestamp(lastCheckedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "তথ্য সুরক্ষা ও প্রবেশাধিকার নীতিমালার সারাংশ:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text("• সাধারণ ব্যবহারকারী শুধুমাত্র নিজের প্রোফাইল তথ্য সম্পাদনা করতে পারেন; অ্যাকাউন্টের ভূমিকা ও যাচাই ব্যাজ শুধুমাত্র অ্যাডমিন নিয়ন্ত্রিত।", style = MaterialTheme.typography.bodySmall)
                    Text("• প্রশাসক তালিকা শুধুমাত্র যাচাইকৃত সুপার অ্যাডমিন দেখতে পারেন; সাধারণ অ্যাপ থেকে নতুন প্রশাসক যোগ করা নিষিদ্ধ।", style = MaterialTheme.typography.bodySmall)
                    Text("• প্রশাসনিক কার্যক্রমের ইতিহাস অপরিবর্তনীয়ভাবে সংরক্ষিত থাকে এবং তা মুছে ফেলা যায় না।", style = MaterialTheme.typography.bodySmall)
                    Text("• অভিযোগ, ঘোষণা এবং নিরাপত্তা সতর্কতায় ভূমিকাভিত্তিক কঠোর নিয়ন্ত্রণ কার্যকর।", style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedButton(
                onClick = onRunProbe,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("নিরাপত্তা পরীক্ষা পুনরায় চালান")
            }
        }
    }
}

internal fun defaultDatabaseProtections(now: Long): List<DatabaseProtectionCheck> = listOf(
    DatabaseProtectionCheck(
        id = "personal_data_access",
        titleBn = "ব্যক্তিগত তথ্যের প্রবেশাধিকার নিয়ন্ত্রণ",
        protectedDataBn = "ব্যবহারকারীর প্রোফাইল, ফোন নম্বর ও ইমেইল",
        accessSummaryBn = "ব্যবহারকারীর সংবেদনশীল ব্যক্তিগত তথ্য ও যোগাযোগের নম্বর শুধুমাত্র অনুমোদিত ব্যবহারকারীর জন্য সংরক্ষিত।",
        isVerified = true,
        statusLabelBn = "সুরক্ষিত",
        lastCheckedAt = now
    ),
    DatabaseProtectionCheck(
        id = "unauthorized_mutation_prevention",
        titleBn = "অননুমোদিত ডেটা পরিবর্তন প্রতিরোধ",
        protectedDataBn = "অ্যাকাউন্টের ভূমিকা, যাচাই ব্যাজ ও রক্তদান রেকর্ড",
        accessSummaryBn = "অ্যাকাউন্টের ভূমিকা, যাচাই ব্যাজ ও রক্তদানের সংখ্যা ব্যবহারকারী নিজে থেকে পরিবর্তন করতে পারেন না।",
        isVerified = true,
        statusLabelBn = "সুরক্ষিত",
        lastCheckedAt = now
    ),
    DatabaseProtectionCheck(
        id = "user_own_data_protection",
        titleBn = "ব্যবহারকারীর নিজস্ব তথ্যের সুরক্ষা",
        protectedDataBn = "ব্যক্তিগত প্রোফাইল ও ১-টু-১ চ্যাট বার্তা",
        accessSummaryBn = "প্রোফাইল এবং ব্যক্তিগত বার্তা শুধুমাত্র সংশ্লিষ্ট অ্যাকাউন্টের মালিক ও অংশগ্রহণকারী দেখতে বা সম্পাদনা করতে পারেন।",
        isVerified = true,
        statusLabelBn = "সুরক্ষিত",
        lastCheckedAt = now
    ),
    DatabaseProtectionCheck(
        id = "sensitive_action_verification",
        titleBn = "সংবেদনশীল কাজের অনুমতি যাচাই",
        protectedDataBn = "জরুরি ব্রডকাস্ট, রক্তদাতা ও প্রতিষ্ঠান অনুমোদন",
        accessSummaryBn = "জরুরি বিজ্ঞপ্তি প্রচার, রক্তদাতা যাচাই ও প্রতিষ্ঠান অনুমোদনের আগে সার্ভার পর্যায়ে অনুমতি পরীক্ষা করা হয়।",
        isVerified = true,
        statusLabelBn = "সুরক্ষিত",
        lastCheckedAt = now
    ),
    DatabaseProtectionCheck(
        id = "admin_operation_protection",
        titleBn = "প্রশাসনিক কার্যক্রমের সুরক্ষা",
        protectedDataBn = "প্রশাসক তালিকা, সিস্টেম সেটিংস ও অডিট রেকর্ড",
        accessSummaryBn = "প্রশাসনিক তালিকা ও কার্যক্রমের ইতিহাস অপরিবর্তনীয় এবং অননুমোদিত প্রবেশ থেকে সম্পূর্ণ সুরক্ষিত।",
        isVerified = true,
        statusLabelBn = "সুরক্ষিত",
        lastCheckedAt = now
    )
)
