package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.domain.model.TransactionDomain
import com.example.qareeb.presentation.theme.dmSansFamily
import com.example.qareeb.presentation.utilis.toLocalDate

@Composable
fun ExpenseRowFinance(transaction: TransactionDomain) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.Black,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = transaction.date.toLocalDate().toString(),
                fontFamily = dmSansFamily,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(10.dp))
    }
}
