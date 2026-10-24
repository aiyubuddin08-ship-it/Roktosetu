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
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

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
    val rawUid = currentUserProfile?.uid?.ifBlank { null } ?: "guest_donor"
    val donorId = rawUid.take(8).uppercase()
    val city = currentUserProfile?.location?.district?.ifBlank { currentUserProfile?.location?.city }?.ifBlank { null } ?: "Dhaka"
    val totalDonations = currentUserProfile?.donationCount ?: 0
    val verificationStatus = currentUserProfile?.verificationStatus ?: VerificationStatus.UNVERIFIED
    val isVerified = verificationStatus == VerificationStatus.VERIFIED

    // Phase 8: Privacy-safe QR payload containing ONLY uid and verification code (never phone/email)
    val verificationCode = remember(rawUid, verificationStatus, totalDonations) {
        buildPrivacySafeVerificationCode(rawUid, verificationStatus.name, totalDonations)
    }
    val qrPayload = remember(rawUid, verificationCode) {
        "ROKTOSETU:UID:$rawUid:CODE:$verificationCode"
    }
    val qrBitmap = remember(qrPayload) {
        generateQrCodeBitmap(qrPayload, 240)
    }

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(256.dp)
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("ROKTOSETU • রক্তসেতু", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontSize = 14.sp)
                                            Text("স্বেচ্ছায় রক্তদাতা পরিচয়পত্র", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "ID: $donorId • $city", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "রক্তদান সংখ্যা: $totalDonations বার", color = BloodGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    BloodGroupBadge(bloodGroup = bloodGroup, sizeInDp = 52)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("যাচাই কোড: $verificationCode", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Text("গোপনীয়তা সুরক্ষিত QR (ফোন/ইমেইল ছাড়া)", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                    }
                                    if (qrBitmap != null) {
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.size(58.dp).testTag("donor_qr_code_image")
                                        ) {
                                            Image(
                                                bitmap = qrBitmap.asImageBitmap(),
                                                contentDescription = "রক্তদাতা যাচাই কিউআর কোড",
                                                modifier = Modifier.fillMaxSize().padding(3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
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
                                text = "VOLUNTARY BLOOD DONOR RECOGNITION",
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
                                    Text("Verification Code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(verificationCode, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val statusLabel = when (verificationStatus) {
                                        VerificationStatus.VERIFIED -> if (isBangla) "যাচাইকৃত" else "Verified"
                                        VerificationStatus.PENDING -> if (isBangla) "বিবেচনাধীন" else "Pending"
                                        else -> if (isBangla) "অযাচাইকৃত" else "Unverified"
                                    }
                                    val statusColor = if (isVerified) BloodSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    Text(statusLabel, fontWeight = FontWeight.Bold, color = statusColor)
                                }
                            }
                        }
                    }
                }

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
                                    "আমি রক্তসেতু (RoktoSetu)-এর একজন স্বেচ্ছাসেবী রক্তদাতা!\nনাম: $name\nরক্তের গ্রুপ: ${bloodGroup.display}\nডোনার আইডি: $donorId\nযাচাই কোড: $verificationCode"
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
                                verificationCode = verificationCode,
                                qrBitmap = qrBitmap,
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

internal fun buildDonorQrPayload(profile: com.example.model.UserProfile): String {
    val rawUid = profile.uid.ifBlank { "guest_donor" }
    val code = buildPrivacySafeVerificationCode(rawUid, profile.verificationStatus.name, profile.donationCount)
    return "ROKTOSETU:UID:$rawUid:CODE:$code"
}

internal fun buildPrivacySafeVerificationCode(
    uid: String,
    verificationStatusName: String,
    donationCount: Int
): String {
    val raw = "$uid|$verificationStatusName|$donationCount|ROKTOSETU"
    val hash = raw.hashCode().toUInt().toString(16).uppercase().padStart(8, '0')
    return "RS-${hash.take(6)}"
}

internal fun generateQrCodeBitmap(payload: String, sizePx: Int = 240): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bmp
    } catch (_: Exception) {
        null
    }
}

private fun saveDonorCardBitmapToGallery(
    context: Context,
    name: String,
    bloodGroup: String,
    donorId: String,
    verificationCode: String,
    qrBitmap: Bitmap?,
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
        canvas.drawText(name.take(28), 56f, 270f, namePaint)

        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xEEFFFFFF.toInt()
            textSize = 30f
        }
        canvas.drawText("Donor ID: $donorId  •  City: $city", 56f, 330f, infoPaint)
        canvas.drawText("Verification Code: $verificationCode", 56f, 380f, infoPaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD54F.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Total Donations: $totalDonations", 56f, 440f, goldPaint)

        val badgeCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        }
        canvas.drawCircle(width - 150f, 180f, 75f, badgeCirclePaint)

        val bloodTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB71C1C.toInt()
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(bloodGroup, width - 150f, 198f, bloodTextPaint)

        if (qrBitmap != null) {
            val qrRect = RectF(width - 240f, 310f, width - 60f, 490f)
            canvas.drawRoundRect(qrRect, 16f, 16f, badgeCirclePaint)
            canvas.drawBitmap(
                qrBitmap,
                null,
                RectF(width - 230f, 320f, width - 70f, 480f),
                null
            )
        }

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
