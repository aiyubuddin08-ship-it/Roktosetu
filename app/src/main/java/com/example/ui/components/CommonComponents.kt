package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BloodGroup
import com.example.model.BloodRequest
import com.example.model.DonorAvailability
import com.example.model.PublicDonorView
import com.example.model.RequestStatus
import com.example.model.RequestUrgency
import com.example.model.VerificationStatus
import com.example.ui.theme.BloodGold
import com.example.ui.theme.BloodPendingAmber
import com.example.ui.theme.BloodPendingContainer
import com.example.ui.theme.BloodRed
import com.example.ui.theme.BloodSuccessContainer
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer

@Composable
fun BloodGroupBadge(
    bloodGroup: BloodGroup,
    modifier: Modifier = Modifier,
    sizeInDp: Int = 46,
    isHighlighted: Boolean = false
) {
    Box(
        modifier = modifier
            .size(sizeInDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHighlighted) EmergencyRed else MaterialTheme.colorScheme.primaryContainer)
            .border(
                1.5.dp,
                if (isHighlighted) BloodGold else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = bloodGroup.display,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (sizeInDp > 40) 18.sp else 14.sp
                ),
                color = if (isHighlighted) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun StatusChip(status: RequestStatus) {
    val (bgColor, textColor, icon) = when (status) {
        RequestStatus.PENDING -> Triple(BloodPendingContainer, BloodPendingAmber, Icons.Default.Info)
        RequestStatus.ACCEPTED -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), Icons.Default.CheckCircle)
        RequestStatus.FULFILLED -> Triple(BloodSuccessContainer, BloodSuccessGreen, Icons.Default.Verified)
        RequestStatus.CANCELLED -> Triple(Color(0xFFEEEEEE), Color(0xFF757575), Icons.Default.Warning)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.display,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UrgencyBadge(urgency: RequestUrgency) {
    val isEmergency = urgency == RequestUrgency.CRITICAL_EMERGENCY
    val isHigh = urgency == RequestUrgency.HIGH

    val bgColor = if (isEmergency) EmergencyRedContainer else if (isHigh) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
    val textColor = if (isEmergency) EmergencyRed else if (isHigh) Color(0xFFE65100) else Color(0xFF2E7D32)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEmergency) {
                Icon(Icons.Default.Emergency, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = urgency.display,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoktoTopAppBar(
    title: String,
    onNavigateToAdmin: (() -> Unit)? = null,
    onNavigateToSecurity: (() -> Unit)? = null,
    onNavigateToChat: (() -> Unit)? = null,
    isGuest: Boolean = false,
    onNavigateToAuth: (() -> Unit)? = null,
    currentLanguageName: String? = null,
    onToggleLanguage: (() -> Unit)? = null,
    unreadNotifCount: Int = 0,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "রক্তসেতু • Emergency Life Network",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick, modifier = Modifier.testTag("nav_back_button")) {
                    Icon(imageVector = Icons.Default.MedicalServices, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        actions = {
            if (onToggleLanguage != null && currentLanguageName != null) {
                Surface(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .testTag("btn_toggle_language")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentLanguageName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isGuest && onNavigateToAuth != null) {
                Button(
                    onClick = onNavigateToAuth,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .testTag("btn_top_bar_login"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("লগইন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (onNavigateToSecurity != null) {
                IconButton(onClick = onNavigateToSecurity, modifier = Modifier.testTag("btn_security_center")) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Center",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (onNavigateToAdmin != null) {
                IconButton(onClick = onNavigateToAdmin, modifier = Modifier.testTag("btn_admin_dashboard")) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Panel",
                        tint = BloodGold
                    )
                }
            }
            if (onNavigateToChat != null && !isGuest) {
                IconButton(onClick = onNavigateToChat, modifier = Modifier.testTag("btn_chat_list")) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chats",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun AuthRequiredDialog(
    title: String = "সাইন ইন প্রয়োজন (Sign In Required)",
    message: String = "এই কাজটি সম্পন্ন করতে (যেমন: রক্তের আবেদন করা, রক্তদাতা হিসেবে সাড়া দেওয়া বা চ্যাট করা) অনুগ্রহ করে লগইন করুন বা একটি নতুন একাউন্ট তৈরি করুন।",
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onNavigateToLogin()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("dialog_btn_login")
            ) {
                Text("লগইন করুন (Sign In)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onNavigateToRegister()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_btn_register")
            ) {
                Text("রেজিস্ট্রেশন (Register)")
            }
        }
    )
}

@Composable
fun PublicDonorItemCard(
    donor: PublicDonorView,
    onCallClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("donor_card_${donor.uid}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BloodGroupBadge(bloodGroup = donor.bloodGroup, sizeInDp = 52)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = donor.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (donor.verificationStatus == VerificationStatus.VERIFIED) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Donor",
                            tint = BloodSuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = donor.generalLocation.ifEmpty { "General Area" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (donor.distanceKm != null && donor.distanceKm > 0) {
                        Text(
                            text = " • ${donor.distanceKm} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val availText = when (donor.availability) {
                        DonorAvailability.AVAILABLE -> "Available to Donate"
                        DonorAvailability.RESTING -> "Resting Period"
                        DonorAvailability.EMERGENCY_ONLY -> "Emergency Calls Only"
                        DonorAvailability.UNAVAILABLE -> "Currently Unavailable"
                    }
                    val availColor = if (donor.availability == DonorAvailability.AVAILABLE) BloodSuccessGreen else Color.Gray

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(availColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "$availText (${donor.donationCount} donations)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (donor.contactPhone.isNotBlank()) {
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BloodSuccessContainer)
                            .testTag("btn_call_donor_${donor.uid}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Donor",
                            tint = BloodSuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("btn_chat_donor_${donor.uid}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat with Donor",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BloodRequestItemCard(
    request: BloodRequest,
    onAcceptClick: () -> Unit,
    onCardClick: () -> Unit,
    canAccept: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onCardClick() }
            .testTag("request_card_${request.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (request.urgency == RequestUrgency.CRITICAL_EMERGENCY)
                EmergencyRedContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BloodGroupBadge(
                        bloodGroup = request.bloodGroup,
                        isHighlighted = request.urgency == RequestUrgency.CRITICAL_EMERGENCY
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${request.unitsRequired} Bag(s) Required",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Patient: ${request.patientName} (${request.patientAge}y)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusChip(status = request.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${request.hospitalName}, ${request.city}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (request.medicalReason.isNotBlank()) {
                Text(
                    text = "Reason: ${request.medicalReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UrgencyBadge(urgency = request.urgency)

                if (request.status == RequestStatus.PENDING && canAccept) {
                    Button(
                        onClick = onAcceptClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_accept_request_${request.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I Can Donate", fontWeight = FontWeight.Bold)
                    }
                } else if (request.status == RequestStatus.ACCEPTED) {
                    Text(
                        text = "Donor Assigned: ${request.acceptedDonorName ?: "Hero Donor"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = BloodSuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
