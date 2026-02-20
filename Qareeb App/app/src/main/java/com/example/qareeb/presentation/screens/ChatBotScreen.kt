package com.example.qareeb.presentation.screens
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.presentation.ui.components.AppBackground
import com.example.qareeb.presentation.theme.dmSansFamily
import com.example.qareeb.presentation.ui.components.ChatInputBar
import com.example.qareeb.presentation.ui.components.WelcomeBanner

@Composable
fun ChatBotScreen(username: String="Guest") {
    AppBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WelcomeBanner(username)
            Spacer(modifier = Modifier.weight(0.1f))
            Middlecontent(username)
            Spacer(modifier = Modifier.weight(8f))
            ChatInputBar()
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

