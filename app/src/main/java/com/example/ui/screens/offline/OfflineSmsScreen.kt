package com.example.ui.screens.offline

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.model.AppLanguage
import com.example.model.BloodGroup
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.viewmodel.ExtraFeaturesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineSmsScreen(
    extraViewModel: ExtraFeaturesViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by extraViewModel.uiState.collectAsState()
    val isBangla = uiState.language == AppLanguage.BANGLA

    var selectedBloodGroup by remember { mutableStateOf(BloodGroup.O_POSITIVE) }
    var bloodGroupExpanded by remember { mutableStateOf(false) }
    var patientName by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Dhaka") }
    var contactNumber by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var unitsRequired by remember { mutableStateOf("1") }

    val generatedSmsBody = extraViewModel.createEmergencySmsBody(
        patientBloodGroup = selectedBloodGroup.display,
        patientName = patientName.ifBlank { "Patient" },
        hospital = hospitalName.ifBlank { "General Hospital" },
        city = city.ifBlank { "Dhaka" },
        units = unitsRequired.toIntOrNull() ?: 1,
        contact = contactNumber.ifBlank { "Direct Caller" }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBangla) "জরুরী অফলাইন এসএমএস এলার্ট" else "Offline Emergency SMS",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isBangla) "ইন্টারনেট ছাড়াও রক্তদাতা খুঁজুন" else "Find Donors Without Internet",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Offline Mode Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmergencyRedContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isBangla) "অফলাইন ইমার্জেন্সি মোড" else "Offline Emergency Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmergencyRed)
                        )
                        Text(
                            text = if (isBangla) "ইন্টারনেট না থাকলেও স্থানীয় ডোনারদের সরাসরি এসএমএস পাঠিয়ে রক্ত সংগ্রহ করুন।" else "Generate instant standard SMS payload to send to donors via cellular network.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Input Fields
            ExposedDropdownMenuBox(
                expanded = bloodGroupExpanded,
                onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded }
            ) {
                OutlinedTextField(
                    value = selectedBloodGroup.display,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isBangla) "প্রয়োজনীয় রক্তের গ্রুপ" else "Required Blood Group") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = bloodGroupExpanded,
                    onDismissRequest = { bloodGroupExpanded = false }
                ) {
                    BloodGroup.values().forEach { bg ->
                        DropdownMenuItem(
                            text = { Text(bg.display, fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedBloodGroup = bg
                                bloodGroupExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text(if (isBangla) "রোগীর নাম" else "Patient Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = hospitalName,
                onValueChange = { hospitalName = it },
                label = { Text(if (isBangla) "হাসপাতালের নাম ও ঠিকানা" else "Hospital Name & Area") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(if (isBangla) "জেলা / শহর" else "City") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unitsRequired,
                    onValueChange = { unitsRequired = it },
                    label = { Text(if (isBangla) "রক্তের ব্যাগ" else "Units") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text(if (isBangla) "জরুরী ফোন নম্বর (আপনার)" else "Emergency Contact Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = recipientPhone,
                onValueChange = { recipientPhone = it },
                label = { Text(if (isBangla) "প্রাপকের নম্বর (ঐচ্ছিক / ফাঁকা রাখলে ইনবক্স খুলবে)" else "Recipient Phone (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // SMS Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isBangla) "এসএমএস প্রিভিউ:" else "SMS Preview:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = generatedSmsBody, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Action Button
            Button(
                onClick = {
                    val smsUri = if (recipientPhone.isNotBlank()) "smsto:$recipientPhone" else "smsto:"
                    val sendIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(smsUri)).apply {
                        putExtra("sms_body", generatedSmsBody)
                    }
                    try {
                        context.startActivity(sendIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open SMS app", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_send_offline_sms"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBangla) "এসএমএস অ্যাপে পাঠান (Offline SMS)" else "Send Offline Emergency SMS",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
