package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.domain.model.TransactionDomain
import com.example.qareeb.presentation.mapper.toUI
import com.example.qareeb.presentation.theme.interFamily
import com.example.qareeb.presentation.utilis.formatDate

@Composable
fun ExpenseRow(transaction: TransactionDomain) {
    val stateUI = transaction.state.toUI()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT: title + date
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.title,
                fontFamily = interFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatDate(transaction.date),
                fontFamily = interFamily,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Light
            )
        }

        // RIGHT: status + amount
        Row(
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                border = BorderStroke(1.dp, stateUI.color),
                color = stateUI.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = transaction.state.name.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = stateUI.color,
                    fontSize = 12.sp,
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "${transaction.amount}$",
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (transaction.income) Color(0xFF00A63E) else Color(0xFFA62700)
            )
        }
    }
}