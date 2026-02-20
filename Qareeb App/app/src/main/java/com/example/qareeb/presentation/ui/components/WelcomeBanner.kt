package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.presentation.theme.dmSansFamily

@Composable
fun WelcomeBanner(username: String) {
    Row(
        modifier = Modifier
            .padding(top = 30.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Welcome, $username!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "You've got things to accomplish — let's do it!",
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = dmSansFamily
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.mingcute_notification_fill),
            contentDescription = "Notification",
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .padding(10.dp)
        )
    }
}
@Composable
fun CardBackground(modifier: Modifier = Modifier,
                   cornerRadius: Int = 18,
                   content: @Composable BoxScope.() -> Unit){
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF6B46C1), Color(0xFF2C7A7B))
            ))){
        Image(
            painter = painterResource(id = R.drawable.stars),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(id = R.drawable.stars),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(id = R.drawable.stars),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(id = R.drawable.stars),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.12f))
        )

        content()
    }

}
@Composable
fun Pill(
    text: String,
    leading: String? = null,
    trailing: String? = null
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Text(leading, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
            }

            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = dmSansFamily
            )

            if (trailing != null) {
                Spacer(Modifier.width(6.dp))
                Text(trailing, fontSize = 12.sp)
            }
        }
    }
}