package com.example.qareeb.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import androidx.compose.material3.Surface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import com.example.qareeb.ui.theme.dmSansFamily
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle


enum class PlanStatus {
    COMPLETED,
    IN_PROGRESS,
    POSTPONED
}
val PlanStatus.color: Color
    get() = when (this) {
        PlanStatus.COMPLETED -> Color(0xFF16A34A)   // green
        PlanStatus.IN_PROGRESS -> Color(0xFF7C3AED) // purple
        PlanStatus.POSTPONED -> Color(0xFFF97316)   // orange
    }
enum class TransactionStatus{
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    DECLINED
}
val TransactionStatus.color: Color
    get() = when (this) {
        TransactionStatus.COMPLETED -> Color(0xFF16A34A)   // green
        TransactionStatus.IN_PROGRESS -> Color(0xFF16A34A) // purple
        TransactionStatus.DECLINED -> Color(0xFFA62700)   // orange
        TransactionStatus.PENDING -> Color(0xFF7C3AED)

    }

/**
 * Shared welcome banner composable
 * Used in both MainActivity and EntranceActivity
 */
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

