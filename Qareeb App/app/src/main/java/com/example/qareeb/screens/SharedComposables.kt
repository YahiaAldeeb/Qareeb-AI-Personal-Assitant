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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle




// Font definition - shared throughout the app
val dmSansFamily = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_bold, FontWeight.Bold),
    Font(R.font.dmsans_extralight, weight = FontWeight.ExtraLight),
)
val interFamily= FontFamily(
    Font(R.font.inter_24pt_regular, FontWeight.Normal),
    Font(R.font.inter_24pt_bold, FontWeight.Bold),
    Font(R.font.inter_24pt_medium, FontWeight.Medium),
    Font(R.font.inter_24pt_semibold, FontWeight.SemiBold),
    Font(R.font.inter_24pt_extrabold, FontWeight.ExtraBold),
    Font(R.font.inter_24pt_light, FontWeight.Light),
    )

data class DayItem(
    val label: String,     // "Fri"
    val date: LocalDate    // real date
)
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


@Composable
fun FancyGradientBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Horizontal gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6B46C1), Color(0xFF2C7A7B))
                    )
                )
        )

        // 2. Stars overlay image
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
        Image(
            painter = painterResource(id = R.drawable.stars),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 3. Vertical gradient fade to white at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White),
                        startY = 1100f
                    )
                )
        )

        // 4. Content on top
        content()
    }
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
@Composable
fun PlanCard(plan: PlanItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(2.dp,color= Color(0xFFE9D8FD), shape = RoundedCornerShape(4.dp))
            .height(108.dp),

        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0E8FF)),
        shape = RoundedCornerShape(4.dp)

    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.checktask),
                        contentDescription = null,
                        Modifier.size(25.dp)
                    )
                    Text("TASK-"+plan.taskId, fontWeight = FontWeight.Light, color = Color(0xFF726B81))
                    Spacer(Modifier.width(120.dp))
                    Surface(
                        border = BorderStroke(1.dp, plan.tag.color),
                        color = plan.tag.color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(9.dp)
                    ) {
                        Text(
                            text = plan.tag.name.replace("_", " "),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = plan.tag.color,
                            fontSize = 12.sp,
                            fontFamily = interFamily
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(plan.title, fontFamily = interFamily,fontSize=16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(id = R.drawable.calendar),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = plan.dueDate?.let { formatDate(it) } ?: "No due date",
                        fontSize = 12.sp,
                        color = Color.Black,
                        fontFamily = interFamily,
                        fontWeight = FontWeight.Light
                    )
                }
                }
            }


        }
    }

@Composable
fun DayCard(
    dayLabel: String,
    dayNumber: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) Color(0xFF6B46C1) else Color.White
    val textColor = if (selected) Color.White else Color.Black
    val dotColor = if (selected) Color(0xFF22D3EE) else Color(0xFF111827)

    Column(
        modifier = Modifier
            .width(45.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = Color(0xFFE5E7EB),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )

        Text(
            text = dayLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )

        Text(
            text = dayNumber.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(Modifier.height(8.dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekChipsRow(
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val days = remember(today) { buildNext7DaysFromToday(today) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(days) { item ->
            DayCard(
                dayLabel = item.label,
                dayNumber = item.date.dayOfMonth,     // ✅ real calendar day number
                selected = item.date == selectedDate, // ✅ purple only when selected
                onClick = { onSelect(item.date) }
            )
        }
    }
}




@Composable
fun SearchBarStub() {
    OutlinedTextField(

        value = "",
        onValueChange = {},
        placeholder = { Text("Search for Plans", color = Color.Gray) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),

        shape = RoundedCornerShape(12.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,

        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFD6D6D6),     // 👈 outline when not focused
            focusedBorderColor = Color(0xFFD6D6D6),
            unfocusedContainerColor = Color(0xFFFAF5FF),// 👈 outline when not focused
            focusedContainerColor = Color(0xFFFAF5FF),// 👈 outline when focused
            cursorColor = Color(0xFFD6D6D6),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}
fun buildNext7DaysFromToday(today: LocalDate): List<DayItem> {
    return (0..6).map { i ->
        val d = today.plusDays(i.toLong())
        DayItem(
            label = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), // Fri, Sat...
            date = d
        )
    }
}


fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

