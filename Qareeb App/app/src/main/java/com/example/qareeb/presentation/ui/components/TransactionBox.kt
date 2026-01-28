package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.screens.ExpenseItem
import com.example.qareeb.ui.theme.dmSansFamily
import kotlin.collections.forEach

@Composable
fun TransactionBox(
    title: String,
    transactions: List<ExpenseItem>,
    emptyMessage: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontFamily = dmSansFamily,
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 16.dp,
                    bottom = 12.dp
                )
            )

            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
            ) {
                if (transactions.isEmpty()) {
                    Text(
                        text = emptyMessage,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontFamily = dmSansFamily,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    transactions.forEach { expense ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFD6C6F5), // slightly darker purple
                                    shape = RoundedCornerShape(8.dp)
                                )

                                .background(
                                    color = Color(0xFFEDE6FB),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            ExpenseRow(item = expense)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
