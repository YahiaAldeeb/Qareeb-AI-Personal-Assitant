package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.data.entity.TransactionState
import com.example.qareeb.presentation.theme.dmSansFamily
import com.example.qareeb.screens.ExpensesItem

@Composable
fun TransactionBox(
    title: String,
    transactions: List<ExpensesItem>,
    emptyMessage: String,
    onStatusChange: (ExpensesItem, TransactionState) -> Unit // ✅ added
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(color = Color.White, shape = RoundedCornerShape(20.dp))
    ) {
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color.Black,
                fontFamily = dmSansFamily,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
            )

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
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
                                .background(
                                    color = Color(0xFFEDE6FB),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            // ✅ Your existing row + dropdown on the right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Keep your current design/content
                                Box(modifier = Modifier.weight(1f)) {
                                    ExpenseRow(item = expense)
                                }

                                Spacer(Modifier.width(8.dp))

                                StatusDropdown(
                                    current = expense.status,
                                    onSelect = { newState ->
                                        onStatusChange(expense, newState)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
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

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        // Small readOnly field (looks like a dropdown chip/field)
        OutlinedTextField(
            value = current.name.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier
                .menuAnchor()
                .width(140.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.35f),
                focusedContainerColor = Color.White.copy(alpha = 0.35f)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { state ->
                DropdownMenuItem(
                    text = { Text(state.name.replace("_", " ")) },
                    onClick = {
                        onSelect(state)
                        expanded = false
                    }
                )
            }
        }
    }
}
