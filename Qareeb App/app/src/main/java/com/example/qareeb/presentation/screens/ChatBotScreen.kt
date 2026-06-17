package com.example.qareeb.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qareeb.R
import com.example.qareeb.presentation.ui.components.AppBackground
import com.example.qareeb.presentation.theme.dmSansFamily
import com.example.qareeb.presentation.ui.components.ChatInputBar
import com.example.qareeb.presentation.ui.components.WelcomeBanner
import com.example.qareeb.presentation.viewModels.ChatBotViewModel
import com.example.qareeb.presentation.viewModels.ChatMessage
import com.example.qareeb.presentation.viewModels.ChatUiState
import kotlinx.coroutines.launch

@Composable
fun ChatBotScreen(
    viewModel: ChatBotViewModel,
    username: String = "Guest",
    onStartQareeb: () -> Unit
) {
    var isQareebEnabled by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    
    val isThinking = uiState is ChatUiState.Thinking
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        coroutineScope.launch {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    AppBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WelcomeBanner(username)
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isNotEmpty()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { message ->
                            ChatMessageItem(message = message)
                        }
                        
                        if (isThinking) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF6366F1),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Thinking...",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontFamily = dmSansFamily,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Middlecontent(username)
                }
            }
            
            ChatInputBar(
                onSendMessage = { text ->
                    viewModel.sendMessage(text)
                },
                enabled = !isThinking
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            QareebSwitch(
                isEnabled = isQareebEnabled,
                onCheckedChange = { checked ->
                    isQareebEnabled = checked
                    if (checked) {
                        onStartQareeb()
                    }
                }
            )
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val backgroundColor = if (message.isUser) {
        Color(0xFF6366F1)
    } else {
        Color.White.copy(alpha = 0.9f)
    }
    
    val textColor = if (message.isUser) Color.White else Color(0xFF1F2937)
    
    val alignment = if (message.isUser) Alignment.TopEnd else Alignment.TopStart
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontFamily = dmSansFamily,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun Middlecontent(username: String) {
    Image(
        painter = painterResource(id = R.drawable.qareeb),
        contentDescription = "QareebLogo",
        modifier = Modifier.fillMaxWidth()
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Good Evening, $username!",
            color = Color.White,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 23.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 8f,
                )
            )
        )
        Text(
            text = "How can I assist you today?",
            color = Color.White,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Light,
            fontSize = 17.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 8f,
                )
            )
        )
    }
}

@Composable
fun QareebSwitch(
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Enable Qareeb Voice Assistant",
            color = Color.White,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 8f,
                )
            )
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = onCheckedChange
        )
    }
}