package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.presentation.theme.dmSansFamily


@Composable
fun FinanceWelcomeBanner(username: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 50.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Finance",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = dmSansFamily
            )
            Icon(
                painter = painterResource(id = R.drawable.mingcute_notification_fill),
                contentDescription = "Notification",
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .padding(10.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "All your spendings and savings in one place!",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.9f),
            fontFamily = dmSansFamily
        )
    }
}