package com.example.ui.screens.camps

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.BloodDonationCamp
import com.example.ui.components.AuthRequiredDialog
import com.example.ui.theme.BloodGold
import com.example.ui.theme.BloodRed
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ExtraFeaturesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodCampScreen(
    extraViewModel: ExtraFeaturesViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by extraViewModel.uiState.collectAsState()
    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val isBangla = uiState.language == AppLanguage.BANGLA
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddCampDialog by remember { mutableStateOf(false) }
    var showAuthPromptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            extraViewModel.clearStatusMessage()
        }
    }

    if (showAuthPromptDialog) {
        AuthRequiredDialog(
            title = if (isBangla) "সাইন ইন প্রয়োজন" else "Sign In Required",
            message = if (isBangla) "রক্তদান ক্যাম্পে অংশগ্রহণ নিশ্চিত করতে অথবা নতুন ক্যাম্প প্রকাশ করতে অনুগ্রহ করে লগইন করুন।" else "Please sign in to RSVP for donation camps or create new events.",
            onDismiss = { showAuthPromptDialog = false },
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToRegister = onNavigateToRegister
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBangla) "রক্তদান ক্যাম্প ও কর্মসূচি" else "Blood Donation Camps",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isBangla) "স্বেচ্ছাসেবী রক্তদান ইভেন্টসমূহ" else "Voluntary Blood Donation Drives",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentUserProfile != null) {
                        showAddCampDialog = true
                    } else {
                        showAuthPromptDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_camp")
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBangla) "ক্যাম্প যোগ করুন" else "Host Camp", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Top Info Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (isBangla) "আপনার এলাকার রক্তদান ক্যাম্প" else "Local Blood Donation Drives",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isBangla) "ক্যাম্পে অংশ নিয়ে জীবন বাঁচান এবং বিনামূল্যে রক্তের গ্রুপ জেনে নিন।" else "Participate, donate blood, and receive donor health certificates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(uiState.camps) { camp ->
                BloodCampCard(
                    camp = camp,
                    isBangla = isBangla,
                    onRsvp = {
                        if (currentUserProfile != null) {
                            extraViewModel.toggleCampRsvp(camp.id)
                        } else {
                            showAuthPromptDialog = true
                        }
                    },
                    onCallOrganizer = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${camp.contactPhone}"))
                        context.startActivity(intent)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    if (showAddCampDialog) {
        AddCampDialog(
            isBangla = isBangla,
            onDismiss = { showAddCampDialog = false },
            onAdd = { title, org, loc, date, time, phone, target, desc ->
                extraViewModel.addNewCamp(title, org, loc, date, time, phone, target, desc)
                showAddCampDialog = false
            }
        )
    }
}

@Composable
fun BloodCampCard(
    camp: BloodDonationCamp,
    isBangla: Boolean,
    onRsvp: () -> Unit,
    onCallOrganizer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBangla) camp.titleBn else camp.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "আয়োজক: ${camp.organizer}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (camp.isUserRsvp) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BloodSuccessGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BloodSuccessGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RSVP DONE", style = MaterialTheme.typography.labelSmall, color = BloodSuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "${camp.date} • ${camp.time}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = camp.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (camp.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = camp.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${camp.registeredCount} জন রক্তদাতা অংশ নিচ্ছেন (লক্ষ্য: ${camp.targetUnits} ব্যাগ)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRsvp,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (camp.isUserRsvp) BloodSuccessGreen else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(if (camp.isUserRsvp) Icons.Default.CheckCircle else Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (camp.isUserRsvp) (if (isBangla) "অংশ নিচ্ছি ✓" else "Going ✓") else (if (isBangla) "আমি অংশ নেব" else "RSVP Join"),
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onCallOrganizer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddCampDialog(
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, String, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("09:00 AM - 05:00 PM") }
    var phone by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("100") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBangla) "নতুন রক্তদান ক্যাম্প প্রকাশ করুন" else "Publish New Blood Drive", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isBangla) "ক্যাম্পের শিরোনাম" else "Camp Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = organizer,
                    onValueChange = { organizer = it },
                    label = { Text(if (isBangla) "আয়োজক সংস্থা / ক্লাবের নাম" else "Organizer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(if (isBangla) "স্থান / ঠিকানা" else "Venue / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text(if (isBangla) "তারিখ (যেমন 15 Oct)" else "Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isBangla) "যোগাযোগ নম্বর" else "Contact Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isBangla) "সংক্ষিপ্ত বিবরণ" else "Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && organizer.isNotBlank() && location.isNotBlank()) {
                        onAdd(title, organizer, location, date.ifBlank { "Upcoming" }, time, phone, target.toIntOrNull() ?: 100, description)
                    }
                },
                enabled = title.isNotBlank() && organizer.isNotBlank() && location.isNotBlank()
            ) {
                Text(if (isBangla) "প্রকাশ করুন" else "Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBangla) "বাতিল" else "Cancel")
            }
        }
    )
}
