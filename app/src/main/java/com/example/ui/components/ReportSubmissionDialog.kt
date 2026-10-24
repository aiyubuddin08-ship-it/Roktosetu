package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmergencyRed

data class ReportCategoryOption(
    val code: String,
    val labelBn: String
)

val REPORT_CATEGORY_OPTIONS = listOf(
    ReportCategoryOption("FAKE_REQUEST", "ভুয়া রিকোয়েস্ট"),
    ReportCategoryOption("HARASSMENT", "অসদাচরণ"),
    ReportCategoryOption("WRONG_INFO", "ভুল তথ্য"),
    ReportCategoryOption("OTHER", "অন্যান্য")
)

val REPORT_TARGET_TYPE_OPTIONS = listOf(
    "BLOOD_REQUEST" to "রক্তের রিকোয়েস্ট",
    "DONOR" to "রক্তদাতা প্রোফাইল",
    "USER" to "ব্যবহারকারী",
    "CHAT" to "চ্যাট কথোপকথন"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportSubmissionDialog(
    currentUserUid: String?,
    initialTargetUid: String = "",
    initialTargetName: String = "",
    initialTargetType: String = "USER",
    targetOwnerUid: String? = null,
    lockTarget: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (
        targetUid: String,
        targetName: String,
        targetType: String,
        category: String,
        reasonBn: String,
        details: String
    ) -> Unit
) {
    var targetName by remember { mutableStateOf(initialTargetName) }
    var targetUid by remember { mutableStateOf(initialTargetUid) }
    var selectedTargetType by remember {
        mutableStateOf(initialTargetType.ifBlank { "USER" })
    }
    var selectedCategory by remember {
        mutableStateOf(
            if (initialTargetType == "BLOOD_REQUEST") "FAKE_REQUEST" else "HARASSMENT"
        )
    }
    var details by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val selectedCategoryLabel = REPORT_CATEGORY_OPTIONS
        .firstOrNull { it.code == selectedCategory }?.labelBn ?: "অভিযোগ"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = "রিপোর্ট আইকন",
                tint = EmergencyRed
            )
        },
        title = {
            Text(
                text = "রিপোর্ট করুন (অভিযোগ জানান)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ভুয়া রক্তের অনুরোধ, অর্থ দাবি, হয়রানি বা ভুল তথ্যের বিরুদ্ধে এখানে বিস্তারিত অভিযোগ জানান।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (lockTarget && initialTargetName.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "যার বিরুদ্ধে রিপোর্ট: $initialTargetName",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val typeLabel = REPORT_TARGET_TYPE_OPTIONS.firstOrNull { it.first == selectedTargetType }?.second ?: selectedTargetType
                            Text(
                                text = "ধরন: $typeLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "কিসের বিরুদ্ধে রিপোর্ট করতে চান?",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        REPORT_TARGET_TYPE_OPTIONS.forEach { (typeCode, labelBn) ->
                            FilterChip(
                                selected = selectedTargetType == typeCode,
                                onClick = { selectedTargetType = typeCode },
                                label = { Text(labelBn, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = targetName,
                        onValueChange = {
                            targetName = it
                            validationError = null
                        },
                        label = { Text("অভিযুক্ত ব্যক্তি / রিকোয়েস্টের নাম *") },
                        placeholder = { Text("যেমন: রিকোয়েস্টকারীর নাম বা হাসপাতালের নাম") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_report_target_name")
                    )
                }

                Text(
                    text = "অভিযোগের বিষয় নির্বাচন করুন:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    REPORT_CATEGORY_OPTIONS.forEach { option ->
                        FilterChip(
                            selected = selectedCategory == option.code,
                            onClick = {
                                selectedCategory = option.code
                                validationError = null
                            },
                            label = {
                                Text(
                                    text = option.labelBn,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmergencyRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_report_cat_${option.code}")
                        )
                    }
                }

                OutlinedTextField(
                    value = details,
                    onValueChange = {
                        if (it.length <= 500) {
                            details = it
                            validationError = null
                        }
                    },
                    label = { Text("বিস্তারিত বিবরণ (১০–৫০০ অক্ষর) *") },
                    placeholder = { Text("ঘটনাটি সংক্ষেপে কিন্তু স্পষ্টভাবে লিখুন (কমপক্ষে ১০ অক্ষর)...") },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "কমপক্ষে ১০ অক্ষর আবশ্যক",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "${details.trim().length}/500",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_report_details")
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = targetName.trim()
                    val resolvedUid = targetUid.trim().ifBlank {
                        "target_${trimmedName.lowercase().replace(Regex("[^a-z0-9\\u0980-\\u09FF]"), "_").take(32)}"
                    }
                    val trimmedDetails = details.trim()

                    if (!currentUserUid.isNullOrBlank() &&
                        (resolvedUid == currentUserUid || targetOwnerUid == currentUserUid)
                    ) {
                        validationError = "নিজের প্রোফাইল বা নিজের রিকোয়েস্টের বিরুদ্ধে অভিযোগ করা যাবে না।"
                        return@Button
                    }
                    if (trimmedName.length < 2) {
                        validationError = "অনুগ্রহ করে যার বিরুদ্ধে অভিযোগ তার নাম লিখুন।"
                        return@Button
                    }
                    if (trimmedDetails.length < 10) {
                        validationError = "অভিযোগের বিবরণ কমপক্ষে ১০ অক্ষরের হতে হবে।"
                        return@Button
                    }
                    if (trimmedDetails.length > 500) {
                        validationError = "অভিযোগের বিবরণ সর্বোচ্চ ৫০০ অক্ষরের মধ্যে হতে হবে।"
                        return@Button
                    }

                    onSubmit(
                        resolvedUid,
                        trimmedName,
                        selectedTargetType,
                        selectedCategory,
                        selectedCategoryLabel,
                        trimmedDetails
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_submit_report")
            ) {
                Text("রিপোর্ট জমা দিন", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
