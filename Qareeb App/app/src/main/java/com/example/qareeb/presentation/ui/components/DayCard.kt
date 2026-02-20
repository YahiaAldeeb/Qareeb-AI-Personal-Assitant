package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

data class DayItem(
    val label: String,     // "Fri"
    val date: LocalDate    // real date
)
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