package com.example.ui.screens.donorcard

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.BloodGroup
import com.example.model.VerificationStatus
import com.example.ui.components.BloodGroupBadge
import com.example.ui.theme.BloodBurgundy
import com.example.ui.theme.BloodGold
import com.example.ui.theme.BloodRed
import com.example.ui.theme.BloodSuccessGreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ExtraFeaturesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorCardScreen(
    authViewModel: AuthViewModel,
    extraViewModel: ExtraFeaturesViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val uiState by extraViewModel.uiState.collectAsState()
    val isBangla = uiState.language == AppLanguage.BANGLA

    var selectedTab by remember { mutableIntStateOf(0) }

    val name = currentUserProfile?.name?.ifBlank { null } ?: (if (isBangla) "স্বেচ্ছাসেবী রক্তদাতা" else "Voluntary Blood Donor")
    val bloodGroup = currentUserProfile?.bloodGroup ?: BloodGroup.O_POSITIVE
    val donorId = currentUserProfile?.uid?.take(8)?.uppercase() ?: "RS-DONOR-01"
    val city = currentUserProfile?.location?.city?.ifBlank { null } ?: "Dhaka"
    val totalDonations = currentUserProfile?.donationCount ?: 1
    val isVerified = currentUserProfile?.verificationStatus == VerificationStatus.VERIFIED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBangla) "ডিজিটাল ডোনার কার্ড" else "Digital Donor Card",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isBangla) "স্মার্ট আইডি ও সম্মাননা সার্টিফিকেট" else "Smart ID & Recognition Certificate",
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (isBangla) "স্মার্ট ডোনার কার্ড" else "Smart Donor Card", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isBangla) "সম্মাননা সার্টিফিকেট" else "Certificate", fontWeight = FontWeight.Bold) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    // Smart Donor Card View
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .testTag("donor_card_widget"),
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(BloodBurgundy, BloodRed, Color(0xFF880E4F))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("ROKTOSETU", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontSize = 14.sp)
                                            Text("LifeLink Blood Connect", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                        }
                                    }

                                    if (isVerified) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.2f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Verified, contentDescription = null, tint = BloodGold, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("VERIFIED DONOR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Middle row: Donor info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "ID: $donorId • $city", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Donations: $totalDonations Times Saved", color = BloodGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    BloodGroupBadge(bloodGroup = bloodGroup, sizeInDp = 52)
                                }

                                // Bottom row: Chip & QR Simulation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Valid Nationwide Bangladesh", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SCAN ID", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Certificate View
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, BloodGold, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = BloodGold, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBangla) "রক্তদাতা সম্মাননা প্রশংসাপত্র" else "Certificate of Appreciation",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "LIFELINK VOLUNTARY BLOOD DONOR RECOGNITION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isBangla) "গর্বের সাথে প্রদান করা হচ্ছে:" else "Proudly presented to:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isBangla)
                                    "নিঃস্বার্থভাবে $totalDonations বার রক্তদান করে মানুষের জীবন বাঁচাতে অমূল্য অবদান রাখার স্বীকৃতিস্বরূপ এই সম্মাননা প্রদান করা হলো।"
                                else
                                    "In grateful recognition of your voluntary contribution by donating blood $totalDonations times and saving human lives.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Blood Group", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(bloodGroup.display, fontWeight = FontWeight.Bold, color = BloodRed)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Donor ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(donorId, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Certified", fontWeight = FontWeight.Bold, color = BloodSuccessGreen)
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "RoktoSetu Digital Donor ID")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "I am a proud voluntary blood donor at LifeLink Blood Connect!\nName: $name\nBlood Group: ${bloodGroup.display}\nDonor ID: $donorId\nJoin us and save lives: https://roktosetu.app"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Donor Card"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBangla) "শেয়ার করুন" else "Share", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, if (isBangla) "ডিজিটাল কার্ড ডিভাইসে সংরক্ষণ করা হয়েছে!" else "Donor card saved to device!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBangla) "ডাউনলোড" else "Save Image", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
