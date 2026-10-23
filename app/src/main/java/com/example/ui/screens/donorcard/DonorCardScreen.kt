package com.example.ui.screens.donorcard

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    val totalDonations = currentUserProfile?.donationCount ?: 0
    val verificationStatus = currentUserProfile?.verificationStatus ?: VerificationStatus.UNVERIFIED
    val isVerified = verificationStatus == VerificationStatus.VERIFIED

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
                                    val statusLabel = when (verificationStatus) {
                                        VerificationStatus.VERIFIED -> if (isBangla) "যাচাইকৃত (Verified)" else "Verified"
                                        VerificationStatus.PENDING -> if (isBangla) "বিবেচনাধীন (Pending)" else "Pending Review"
                                        else -> if (isBangla) "অযাচাইকৃত (Unverified)" else "Unverified"
                                    }
                                    val statusColor = if (isVerified) BloodSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    Text(statusLabel, fontWeight = FontWeight.Bold, color = statusColor)
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
                                    "I am a voluntary blood donor at RoktoSetu (রক্তসেতু)!\nName: $name\nBlood Group: ${bloodGroup.display}\nDonor ID: $donorId\nDonations: $totalDonations"
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
                            val saved = saveDonorCardBitmapToGallery(
                                context = context,
                                name = name,
                                bloodGroup = bloodGroup.display,
                                donorId = donorId,
                                city = city,
                                totalDonations = totalDonations,
                                isVerified = isVerified
                            )
                            val msg = if (saved) {
                                if (isBangla) "ডোনার কার্ড ছবি হিসেবে গ্যালারিতে সংরক্ষিত হয়েছে!"
                                else "Donor card PNG saved to Pictures/RoktoSetu!"
                            } else {
                                if (isBangla) "ছবি সংরক্ষণ করা যায়নি। পুনরায় চেষ্টা করুন।"
                                else "Could not save donor card image."
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).testTag("btn_save_donor_card"),
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

private fun saveDonorCardBitmapToGallery(
    context: Context,
    name: String,
    bloodGroup: String,
    donorId: String,
    city: String,
    totalDonations: Int,
    isVerified: Boolean
): Boolean {
    return try {
        val width = 1000
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                intArrayOf(0xFF7F0000.toInt(), 0xFFC62828.toInt(), 0xFF880E4F.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 44f, 44f, bgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ROKTOSETU • রক্তসেতু", 56f, 92f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xDDFFFFFF.toInt()
            textSize = 26f
        }
        val badgeText = if (isVerified) "VERIFIED BLOOD DONOR" else "VOLUNTARY BLOOD DONOR"
        canvas.drawText(badgeText, 56f, 134f, subtitlePaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(name.take(28), 56f, 290f, namePaint)

        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xEEFFFFFF.toInt()
            textSize = 32f
        }
        canvas.drawText("Donor ID: $donorId  •  City: $city", 56f, 350f, infoPaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD54F.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Total Donations: $totalDonations", 56f, 410f, goldPaint)

        val badgeCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        }
        canvas.drawCircle(width - 150f, 300f, 90f, badgeCirclePaint)

        val bloodTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB71C1C.toInt()
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(bloodGroup, width - 150f, 320f, bloodTextPaint)

        val fileName = "RoktoSetu_Donor_${donorId}_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RoktoSetu")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return false
        resolver.openOutputStream(uri)?.use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        } ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        true
    } catch (_: Exception) {
        false
    }
}
