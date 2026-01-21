package com.example.qareeb.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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


// Font definition - shared throughout the app
val dmSansFamily = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_bold, FontWeight.Bold),
    Font(R.font.dmsans_extralight, weight = FontWeight.ExtraLight),
)

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
            .border(2.dp,color= Color(0xFFE9D8FD), shape = RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0E8FF)),
        shape = RoundedCornerShape(16.dp)

    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(plan.title, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(plan.time, fontSize = 12.sp, color = Color.Gray)
            }
            Surface(
                border = BorderStroke(1.dp, plan.tagColor),
                color = plan.tagColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    plan.tag,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = plan.tagColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekChipsRow(selected: String, onSelect: (String) -> Unit) {
    val days = listOf("Fri", "Sat", "Sun", "Mon", "Tue", "Wed", "Thu")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { day ->
            FilterChip(
                selected = day == selected,
                onClick = { onSelect(day) },
                label = { Text(day) }
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
        shape = RoundedCornerShape(15.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Gray,     // 👈 outline when not focused
            focusedBorderColor = Color.Gray,       // 👈 outline when focused
            cursorColor = Color.Gray,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}



