package com.fitness.app.presentation.coach.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.app.presentation.coach.*
import com.fitness.app.domain.model.CoachMessage
import com.fitness.app.domain.model.CoachSession
import com.fitness.app.ui.theme.*
import com.fitness.app.ui.components.GlowDivider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(
    viewModel: CoachViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Ensure we start with a fresh state if no session is active and no messages exist
    LaunchedEffect(Unit) {
        if (uiState.activeSessionId == null && uiState.messages.isEmpty()) {
            viewModel.startNewChat()
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HistoryDrawerContent(
                sessions = uiState.sessions,
                activeSessionId = uiState.activeSessionId,
                onNewChat = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onSelectSession = { id ->
                    viewModel.loadSession(id)
                    scope.launch { drawerState.close() }
                },
                onDeleteSession = { id ->
                    viewModel.deleteSession(id)
                },
                onClearHistory = {
                    viewModel.clearHistory()
                    scope.launch { drawerState.close() }
                },
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            containerColor = DarkBackground,
            topBar = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🤖  Wellness Coach",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (uiState.activeSessionId == null) "New Conversation" else "Conversation History",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "History",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    GlowDivider(color = PrimaryPurple.copy(alpha = 0.4f))
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.messages) { message ->
                        MessageBubble(message)
                    }
                    
                    if (uiState.isLoading) {
                        item {
                            TypingIndicator()
                        }
                    }
                }

                // Quick Action Buttons
                QuickActions(
                    onActionClick = { viewModel.onCategorySelect(it) }
                )

                // Input Area
                ChatInput(
                    value = uiState.messageInput,
                    onValueChange = { viewModel.onInputChange(it) },
                    onSend = { viewModel.sendMessage() }
                )
            }
        }
    }
}

@Composable
fun HistoryDrawerContent(
    sessions: List<CoachSession>,
    activeSessionId: String?,
    onNewChat: () -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = DarkSurface,
        drawerContentColor = TextPrimary,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Messages",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            // New Chat Button
            Button(
                onClick = onNewChat,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start New Chat", color = PrimaryPurple, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            GlowDivider(color = GlassBorder)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "History",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            if (sessions.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("No past conversations", color = TextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(sessions) { session ->
                        val isSelected = session.id == activeSessionId
                        SessionHistoryItem(
                            session = session,
                            isSelected = isSelected,
                            onSelect = { onSelectSession(session.id) },
                            onDelete = { onDeleteSession(session.id) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear All Chats", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SessionHistoryItem(
    session: CoachSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = if (isSelected) PrimaryPurple.copy(alpha = 0.15f) else Color.Transparent
    val border = if (isSelected) PrimaryPurple.copy(alpha = 0.3f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = if (isSelected) PrimaryPurple else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.firstMessage,
                fontSize = 14.sp,
                color = if (isSelected) TextPrimary else TextSecondary,
                maxLines = 1,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(session.timestamp)),
                fontSize = 10.sp,
                color = TextMuted
            )
        }

        if (isSelected) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = ErrorRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: CoachMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgBrush = if (message.isUser) {
        Brush.horizontalGradient(listOf(PrimaryPurple, NeonPurple))
    } else {
        Brush.verticalGradient(listOf(DarkSurfaceVariant, DarkCard))
    }
    val textColor = TextPrimary
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = shape,
                    ambientColor = if (message.isUser) PrimaryPurple.copy(alpha = 0.3f) else Color.Transparent,
                    spotColor = if (message.isUser) PrimaryPurple.copy(alpha = 0.4f) else Color.Transparent
                )
                .clip(shape)
                .background(bgBrush)
                .border(
                    width = 1.dp,
                    color = if (message.isUser) PrimaryPurple.copy(alpha = 0.5f) else GlassBorder,
                    shape = shape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
        Text(
            text = if (message.isUser) "You" else "Coach",
            fontSize = 10.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
fun QuickActions(onActionClick: (String) -> Unit) {
    val actions = listOf(
        "Motivation" to "🔥",
        "Workout tips" to "💪",
        "Nutrition advice" to "🥗",
        "Better sleep" to "😴"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { (label, emoji) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { onActionClick(label) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(0.dp))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Ask your coach...", color = TextMuted) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkBackground,
                    unfocusedContainerColor = DarkBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = PrimaryPurple,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 4
            )
            
            Spacer(Modifier.width(12.dp))
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(PrimaryPurple, NeonPurple)))
                    .clickable(enabled = value.isNotBlank(), onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val alpha1 by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse)
        )
        val alpha2 by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = 200), repeatMode = RepeatMode.Reverse)
        )
        val alpha3 by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = 400), repeatMode = RepeatMode.Reverse)
        )

        Box(Modifier.size(6.dp).clip(CircleShape).background(TextSecondary.copy(alpha = alpha1)))
        Box(Modifier.size(6.dp).clip(CircleShape).background(TextSecondary.copy(alpha = alpha2)))
        Box(Modifier.size(6.dp).clip(CircleShape).background(TextSecondary.copy(alpha = alpha3)))
    }
}
