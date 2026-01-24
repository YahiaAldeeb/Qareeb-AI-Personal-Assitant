package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.screens.CardBackground

@Composable
fun MiniCardPriority(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(16.dp)
    ) {
        CardBackground(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            cornerRadius = 18
        ){
            Column(Modifier.padding(14.dp)) {
                Text("Priority Tasks", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                PriorityItem("Meeting", true)
                PriorityItem("Email", false)
            }
        }
    }
}

@Composable
fun PriorityItem(label: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    if (checked) Color(0xFF7C3AED) else Color.LightGray,
                    CircleShape
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, color = Color.White)
    }
}