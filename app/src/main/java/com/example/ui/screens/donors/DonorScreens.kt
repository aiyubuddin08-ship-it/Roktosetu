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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.model.BloodGroup
import com.example.ui.components.AuthRequiredDialog
import com.example.ui.components.PublicDonorItemCard
import com.example.ui.components.RoktoTopAppBar
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BloodViewModel
import com.example.ui.viewmodel.ChatViewModel

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
    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val bloodState by bloodViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAuthPromptDialog by remember { mutableStateOf(false) }

    val filteredDonors = bloodState.donors.filter { donor ->
        searchQuery.isBlank() ||
                donor.name.contains(searchQuery, ignoreCase = true) ||
                donor.generalLocation.contains(searchQuery, ignoreCase = true)
    }

    if (showAuthPromptDialog) {
        AuthRequiredDialog(
            title = "চ্যাট করতে সাইন ইন করুন",
            message = "রক্তদাতার সাথে নিরাপদে ১-টু-১ চ্যাট বা মেসেজ পাঠাতে অনুগ্রহ করে আপনার একাউন্টে লগইন করুন বা রেজিস্ট্রেশন করুন।",
            onDismiss = { showAuthPromptDialog = false },
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToRegister = onNavigateToRegister
        )
    }

    Scaffold(
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
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or area...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
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

            // Blood Group Filter Chips
            item {
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
                            )
                        )
                    }
                    items(BloodGroup.entries) { bg ->
                        FilterChip(
                            selected = bloodState.selectedBloodGroupFilter == bg,
                            onClick = {
                                val next = if (bloodState.selectedBloodGroupFilter == bg) null else bg
                                bloodViewModel.setBloodGroupFilter(next)
                            },
                            label = { Text(bg.display, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
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
                        text = "Verified Active Donors (${filteredDonors.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
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
                                text = "No donors found for this filter",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Try selecting another blood group or clearing your search term.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredDonors) { donor ->
                    PublicDonorItemCard(
                        donor = donor,
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
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
