package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qareeb.presentation.viewModels.DailySpending
import java.time.format.DateTimeFormatter

@Composable
fun SpendingTrendChart(
    dailySpending: List<DailySpending>,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Spending Trend (Last 30 Days)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (dailySpending.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No spending data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            val maxAmount = dailySpending.maxOfOrNull { it.amount } ?: 1.0

            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dailySpending.takeLast(7)) { daily ->
                    DailySpendingRow(
                        date = daily.date.format(dateFormatter),
                        amount = daily.amount,
                        maxAmount = maxAmount
                    )
                }
            }
        }
    }
}

@Composable
private fun DailySpendingRow(
    date: String,
    amount: Double,
    maxAmount: Double
) {
    val percentage = (amount / maxAmount).toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            modifier = Modifier.width(50.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE5E7EB))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF6366F1))
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$${String.format("%.0f", amount)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F2937),
            modifier = Modifier.width(60.dp)
        )
    }
}