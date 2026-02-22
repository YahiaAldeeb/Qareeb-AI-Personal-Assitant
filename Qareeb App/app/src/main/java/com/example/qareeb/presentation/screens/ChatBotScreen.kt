package com.example.qareeb.presentation.screens
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.presentation.ui.components.AppBackground
import com.example.qareeb.presentation.theme.dmSansFamily
import com.example.qareeb.presentation.ui.components.ChatInputBar
import com.example.qareeb.presentation.ui.components.WelcomeBanner

@Composable
fun ChatBotScreen(
    username: String = "Guest",
    onStartQareeb: () -> Unit
) {
    var isQareebEnabled by remember { mutableStateOf(false) }
    
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

