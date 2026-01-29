package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.presentation.mapper.toUI
import com.example.qareeb.presentation.theme.interFamily
import com.example.qareeb.presentation.utilis.formatDate
import com.example.qareeb.screens.TasksUi
import com.example.qareeb.data.entity.TaskStatus

@Composable
fun PlanCardTask(
    plan: TasksUi,
    onStatusChange: (TasksUi, TaskStatus) -> Unit
) {
    val stateUI = plan.status.toUI()

    var expanded by remember { mutableStateOf(false) }

    // ✅ Only the options you asked for
    val options = listOf(
        TaskStatus.COMPLETED,
        TaskStatus.POSTPONED,
        TaskStatus.PENDING,
        TaskStatus.IN_PROGRESS
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(2.dp, color = Color(0xFFE9D8FD), shape = RoundedCornerShape(4.dp))
            .height(108.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0E8FF)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.checktask),
                        contentDescription = null,
                        Modifier.size(25.dp)
                    )

                    Text(
                        "TASK-" + plan.taskId,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF726B81)
                    )

                    Spacer(Modifier.width(120.dp))

                    // ✅ Dropdown anchored to the same badge (NO UI design change)
                    Box {
                        Surface(
                            onClick = { expanded = true }, // ✅ now clickable
                            border = BorderStroke(1.dp, stateUI.color),
                            color = stateUI.color.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(9.dp)
                        ) {
                            Text(
                                text = stateUI.displayName,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = stateUI.color,
                                fontSize = 12.sp,
                                fontFamily = interFamily
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            options.forEach { newState ->
                                val ui = newState.toUI() // ✅ so menu can show same displayName style if you want
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = ui.displayName,
                                            fontFamily = interFamily
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        onStatusChange(plan, newState)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    plan.title,
                    fontFamily = interFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Spacer(Modifier.height(8.dp))

                Row(Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(id = R.drawable.calendar),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = plan.dueDate?.let { formatDate(it) } ?: "No due date",
                        fontSize = 12.sp,
                        color = Color.Black,
                        fontFamily = interFamily,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}
