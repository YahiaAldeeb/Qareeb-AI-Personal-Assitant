package com.example.qareeb.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.data.AppDatabase
import com.example.qareeb.data.SessionManager
import com.example.qareeb.presentation.ui.components.FancyGradientBackground
import com.example.qareeb.ui.theme.QareebTheme
import com.example.qareeb.ui.theme.dmSansFamily

class ChatBotScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val database = AppDatabase.getDatabase(this)
        val sessionManager = SessionManager.getInstance(this)

        setContent {
            QareebTheme {
                val userId = sessionManager.getUserId()
                val user by database.userDao().getUserById(userId).collectAsState(initial = null)
                val username = user?.name ?: "Guest"

                FancyGradientBackground {
                    HomeScreenContent(username)
                }
            }
        }
    }
}

// Home screen composables
@Composable
fun HomeScreenContent(username: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WelcomeBanner(username)
        Spacer(modifier = Modifier.weight(0.1f))
        Middlecontent(username)
        Spacer(modifier = Modifier.weight(1f))
        ChatInputBar()
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
fun ChatInputBar() {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 115.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(15.dp),
                    spotColor = Color.Black
                )
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(15.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.attachment),
                    contentDescription = "Attach",
                    tint = Color(0xFF000000),
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                text = "Type a message...",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                fontFamily = dmSansFamily
                            )
                        }
                        innerTextField()
                    }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = Color(0xFFA277FF),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.mic),
                        contentDescription = "Mic",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    painter = painterResource(id = R.drawable.send),
                    contentDescription = "Send",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}
