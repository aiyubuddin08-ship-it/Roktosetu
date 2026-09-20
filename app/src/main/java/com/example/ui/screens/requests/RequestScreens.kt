package com.example.ui.screens.requests

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.ui.components.AuthRequiredDialog
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.StatusChip
import com.example.ui.components.UrgencyBadge
import com.example.ui.theme.BloodGold
import com.example.ui.theme.BloodRed
import com.example.ui.theme.BloodSuccessContainer
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BloodViewModel
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    authViewModel: AuthViewModel,
    bloodViewModel: BloodViewModel,
    onNavigateBack: () -> Unit,
    onRequestCreated: (String) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val uiState by bloodViewModel.uiState.collectAsState()

    var patientName by remember { mutableStateOf("") }
    var patientAge by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf(BloodGroup.A_POSITIVE) }
    var unitsRequired by remember { mutableStateOf(1) }
    var hospitalName by remember { mutableStateOf("") }
    var hospitalAddress by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Dhaka") }
    var urgency by remember { mutableStateOf(RequestUrgency.HIGH) }
    var contactPhone by remember { mutableStateOf("") }
    var medicalReason by remember { mutableStateOf("") }
    var isEmergencyBroadcast by remember { mutableStateOf(true) }

    var expandedBloodMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            bloodViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Request Blood", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("রক্তের আবেদন পোস্ট করুন", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (currentUserProfile == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "সাইন ইন প্রয়োজন (Sign In Required)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "রোগীর জন্য রক্তের আবেদন প্রকাশ করতে এবং রক্তদাতাদের তাৎক্ষণিক নোটিফিকেশন পাঠাতে অনুগ্রহ করে লগইন করুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_req_login"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("লগইন করুন (Sign In)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_req_register"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("নতুন একাউন্ট খুলুন (Register)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Urgency Selector
            Text("Urgency Level (জরুরিতা):", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RequestUrgency.entries.forEach { urg ->
                    FilterChip(
                        selected = urgency == urg,
                        onClick = { urgency = urg },
                        label = { Text(urg.display, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (urg == RequestUrgency.CRITICAL_EMERGENCY) EmergencyRed else MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Patient Information", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name (রোগীর নাম)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("input_req_patient_name"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = patientAge,
                            onValueChange = { patientAge = it },
                            label = { Text("Age (বয়স)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        // Blood Group Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedBloodMenu,
                            onExpandedChange = { expandedBloodMenu = !expandedBloodMenu },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            OutlinedTextField(
                                value = selectedBloodGroup.display,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Blood Group") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBloodMenu) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedBloodMenu,
                                onDismissRequest = { expandedBloodMenu = false }
                            ) {
                                BloodGroup.entries.forEach { bg ->
                                    DropdownMenuItem(
                                        text = { Text("${bg.display} (${bg.bengaliDisplay})") },
                                        onClick = {
                                            selectedBloodGroup = bg
                                            expandedBloodMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Units Required Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Units / Bags Required:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (unitsRequired > 1) unitsRequired-- },
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = "$unitsRequired Bag(s)",
                                modifier = Modifier.padding(horizontal = 14.dp),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = { if (unitsRequired < 10) unitsRequired++ },
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Hospital & Location", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    OutlinedTextField(
                        value = hospitalName,
                        onValueChange = { hospitalName = it },
                        label = { Text("Hospital / Clinic Name (হাসপাতালের নাম)") },
                        leadingIcon = { Icon(Icons.Default.LocalHospital, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("input_req_hospital"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City (শহর)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = hospitalAddress,
                            onValueChange = { hospitalAddress = it },
                            label = { Text("Area / Address") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Contact Phone (জরুরী যোগাযোগের নম্বর)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("input_req_phone"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(
                        value = medicalReason,
                        onValueChange = { medicalReason = it },
                        label = { Text("Medical Reason (e.g., Surgery, Thalassemia, Accident, Dengue)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2
                    )
                }
            }

            // Emergency Broadcast Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EmergencyRedContainer.copy(alpha = 0.5f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Instant Emergency Push Broadcast",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmergencyRed)
                    )
                    Text(
                        text = "Broadcast immediately to nearby donors via Firebase Cloud Messaging",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEmergencyBroadcast,
                    onCheckedChange = { isEmergencyBroadcast = it }
                )
            }

            Button(
                onClick = {
                    val age = patientAge.toIntOrNull() ?: 25
                    bloodViewModel.createRequest(
                        patientName = patientName,
                        patientAge = age,
                        bloodGroup = selectedBloodGroup,
                        unitsRequired = unitsRequired,
                        hospitalName = hospitalName,
                        hospitalAddress = hospitalAddress,
                        city = city,
                        urgency = urgency,
                        contactPhone = contactPhone,
                        medicalReason = medicalReason,
                        isEmergencyBroadcast = isEmergencyBroadcast,
                        onSuccess = onRequestCreated
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_request"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (urgency == RequestUrgency.CRITICAL_EMERGENCY) EmergencyRed else MaterialTheme.colorScheme.primary
                ),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Publish Blood Request", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    requestId: String,
    authViewModel: AuthViewModel,
    bloodViewModel: BloodViewModel,
    chatViewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChatConversation: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val bloodState by bloodViewModel.uiState.collectAsState()
    val request = bloodState.requests.firstOrNull { it.id == requestId }
    var showAuthPromptDialog by remember { mutableStateOf(false) }
    var authPromptMessage by remember { mutableStateOf("এই সুবিধাটি ব্যবহার করতে অনুগ্রহ করে লগইন করুন।") }

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
                title = { Text("Request Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (request == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Request details not found or completed.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Top Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BloodGroupBadge(bloodGroup = request.bloodGroup, sizeInDp = 56)
                        StatusChip(status = request.status)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "${request.unitsRequired} Bag(s) of ${request.bloodGroup.display} Blood Needed",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    UrgencyBadge(urgency = request.urgency)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Patient Details
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Patient: ${request.patientName} (${request.patientAge} years)", fontWeight = FontWeight.SemiBold)
                            Text("Hospital: ${request.hospitalName}, ${request.city}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (request.medicalReason.isNotBlank()) {
                                Text("Reason: ${request.medicalReason}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Quick Call / Chat Requester
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contact Requester", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = "Phone: ${request.contactPhone}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${request.contactPhone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodSuccessGreen)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Direct Call", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (currentUserProfile != null) {
                                    chatViewModel.startChatWithDonor(
                                        donorUid = request.requesterUid,
                                        donorName = request.requesterName,
                                        donorBloodGroup = request.bloodGroup.display,
                                        requestId = request.id,
                                        onReady = { convId ->
                                            onNavigateToChatConversation(convId, request.requesterName, request.bloodGroup.display)
                                        }
                                    )
                                } else {
                                    authPromptMessage = "রোগীর বা আবেদনকারীর সাথে ১-টু-১ চ্যাট করতে অনুগ্রহ করে লগইন করুন।"
                                    showAuthPromptDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Direct Chat")
                        }
                    }
                }
            }

            // Status Actions
            if (request.status == RequestStatus.PENDING) {
                Button(
                    onClick = {
                        if (currentUserProfile != null) {
                            bloodViewModel.acceptRequest(request.id) {}
                        } else {
                            authPromptMessage = "রোগীর জন্য রক্তদানের অঙ্গীকার করতে অনুগ্রহ করে সাইন ইন করুন।"
                            showAuthPromptDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I Can Donate for this Patient", fontWeight = FontWeight.Bold)
                }
            } else if (request.status == RequestStatus.ACCEPTED) {
                Button(
                    onClick = {
                        if (currentUserProfile != null) {
                            bloodViewModel.updateRequestStatus(request.id, RequestStatus.FULFILLED)
                        } else {
                            authPromptMessage = "রক্তদানের স্ট্যাটাস পরিবর্তন করতে অনুগ্রহ করে সাইন ইন করুন।"
                            showAuthPromptDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BloodSuccessGreen)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark Blood Received / Fulfilled", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
