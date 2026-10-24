package com.example.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ReportSubmissionDialog
import com.example.ui.theme.EmergencyRed
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToConversation: (String, String, String) -> Unit
) {
    val chatState by chatViewModel.uiState.collectAsState()
    val myProfile by authViewModel.currentUserProfile.collectAsState()
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(chatState.errorMessage) {
        chatState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            chatViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Direct Messages", fontWeight = FontWeight.Bold)
                        Text("ব্যক্তিগত যোগাযোগ • সুরক্ষিত বার্তা", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "রক্তদাতা ও রোগীর মধ্যকার ১-টু-১ ব্যক্তিগত কথোপকথন",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (chatState.conversations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("এখনো কোনো কথোপকথন শুরু হয়নি", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "রক্তদাতা তালিকা বা রক্তের অনুরোধ থেকে 'চ্যাট' বাটনে চাপ দিয়ে সরাসরি মেসেজ পাঠাতে পারেন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(chatState.conversations, key = { it.id }) { conv ->
                    val myUid = myProfile?.uid ?: ""
                    val otherUid = conv.participants.firstOrNull { it != myUid } ?: ""
                    val otherName = conv.participantNames[otherUid] ?: "রক্তদাতা / ব্যবহারকারী"
                    val otherBlood = conv.participantBloodGroups[otherUid] ?: "Blood Donor"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigateToConversation(conv.id, otherName, otherBlood)
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = otherName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(otherName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        text = sdf.format(Date(conv.lastMessageTimestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = conv.lastMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    conversationId: String,
    otherName: String,
    otherBloodGroup: String,
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val chatState by chatViewModel.uiState.collectAsState()
    val myProfile by authViewModel.currentUserProfile.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val snackbarHostState = remember { SnackbarHostState() }

    val activeConversation = chatState.conversations.firstOrNull { it.id == conversationId }
    val myUid = myProfile?.uid ?: ""
    val otherUid = activeConversation?.participants?.firstOrNull { it != myUid } ?: conversationId

    LaunchedEffect(conversationId) {
        chatViewModel.openConversation(conversationId)
    }

    LaunchedEffect(chatState.errorMessage) {
        chatState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            chatViewModel.clearError()
        }
    }

    if (showReportDialog) {
        ReportSubmissionDialog(
            currentUserUid = myProfile?.uid,
            initialTargetUid = otherUid,
            initialTargetName = otherName,
            initialTargetType = "CHAT",
            targetOwnerUid = otherUid,
            lockTarget = true,
            onDismiss = { showReportDialog = false },
            onSubmit = { tUid, tName, tType, cat, reasonBn, details ->
                showReportDialog = false
                authViewModel.submitUserReport(
                    targetUid = tUid,
                    targetName = tName,
                    targetType = tType,
                    category = cat,
                    reasonBn = reasonBn,
                    details = details
                ) { _, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(otherName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "রক্তের গ্রুপ: $otherBloodGroup • ব্যক্তিগত বার্তা",
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
                actions = {
                    IconButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.testTag("btn_report_chat")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "কথোপকথন রিপোর্ট করুন",
                            tint = EmergencyRed
                        )
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
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🔒 এই কথোপকথনটি ব্যক্তিগত ও সুরক্ষিত — শুধুমাত্র আপনি এবং প্রাপক বার্তা দেখতে পারেন।",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(chatState.messages, key = { it.id.ifBlank { "${it.senderUid}_${it.timestamp}" } }) { msg ->
                    val isMe = msg.senderUid == (myProfile?.uid ?: "")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            ),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = msg.text,
                                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = sdf.format(Date(msg.timestamp)),
                                    color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("বার্তা লিখুন...") },
                        modifier = Modifier.weight(1f).testTag("input_chat_message"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                chatViewModel.sendMessage(conversationId, messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("btn_send_chat_message")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "পাঠান", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
