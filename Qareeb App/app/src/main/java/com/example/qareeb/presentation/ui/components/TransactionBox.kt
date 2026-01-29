package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.data.entity.TransactionState
import com.example.qareeb.presentation.mapper.toUI
import com.example.qareeb.presentation.theme.dmSansFamily
import com.example.qareeb.screens.ExpensesItem

@Composable
fun TransactionBox(
    title: String,
    transactions: List<ExpensesItem>,
    emptyMessage: String,
    onStatusChange: (ExpensesItem, TransactionState) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(color = Color.White, shape = RoundedCornerShape(20.dp))
            .padding(bottom = 8.dp)
    ) {
        Column {

            Text(
                text = title,
                fontSize = 16.sp,
                fontFamily = dmSansFamily,
                color = Color.Black,
                modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 10.dp)
            )

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
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
                                .background(
                                    color = Color(0xFFEDE6FB),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top

                            ) {

                                Box(modifier = Modifier.weight(1f)) {
                                    ExpenseRowFinance(item = expense)
                                }

                                Spacer(Modifier.width(10.dp))


                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${expense.amount} $",
                                        fontFamily = dmSansFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (expense.income) Color(0xFF1E8E3E) else Color(0xFFD93025)
                                    )

                                    Spacer(Modifier.width(10.dp))

                                    StatusDropdownPill(
                                        current = expense.status,
                                        onSelect = { newState ->
                                            onStatusChange(expense, newState)
                                        }
                                    )
                                }

                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDropdownPill(
    current: TransactionState,
    onSelect: (TransactionState) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(
        TransactionState.PENDING,
        TransactionState.IN_PROGRESS,
        TransactionState.COMPLETED,
        TransactionState.DECLINED
    )


//    val pillBg = Color(0xFFEDE6FB)
//    val pillText = Color(0xFF6D4CFF)
    val stateUI = current.toUI()



    fun labelOf(state: TransactionState): String = when (state) {
        TransactionState.PENDING -> "Pending"
        TransactionState.IN_PROGRESS -> "In Progress"
        TransactionState.COMPLETED -> "Completed"
        TransactionState.DECLINED -> "Declined"
    }

    Box {
        Surface(
            onClick = { expanded = true },
            border = BorderStroke(1.dp, stateUI.color),
            color = stateUI.color.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp) // or 50 if you want more pill
        ) {
            Text(
                text = current.name.replace("_", " "),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = stateUI.color,
                fontSize = 12.sp,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Medium
            )
        }

    }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            //containerColor = Color.White
        ) {
            options.forEach { state ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = labelOf(state),
                            fontFamily = dmSansFamily,
                            color = Color.Black
                        )
                    },
                    onClick = {
                        onSelect(state)
                        expanded = false
                    }
                )
            }
        }
    }
