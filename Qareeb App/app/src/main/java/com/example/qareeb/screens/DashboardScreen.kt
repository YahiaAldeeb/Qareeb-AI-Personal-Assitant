package com.example.qareeb.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.qareeb.ui.theme.QareebTheme
import java.time.LocalDate
import com.example.qareeb.utilis.toLocalDate
import com.example.qareeb.presentation.ui.components.BigTasksBanner
import com.example.qareeb.presentation.ui.components.BottomNavBar
import com.example.qareeb.presentation.ui.components.ExpenseRow
import com.example.qareeb.presentation.ui.components.FancyGradientBackground
import com.example.qareeb.presentation.ui.components.MiniCardCompleted
import com.example.qareeb.presentation.ui.components.MiniCardPriority
import com.example.qareeb.presentation.ui.components.PlanCard
import com.example.qareeb.presentation.ui.components.SearchBarStub
import com.example.qareeb.presentation.ui.components.SectionTitle
import com.example.qareeb.presentation.ui.components.WeekChipsRow
data class TaskUi(
    val taskId: Long,
    val title: String,
    val dueDate: Long?,
    val status: com.example.qareeb.data.entity.TaskStatus // reuse your enum
)

data class ExpenseItem(
    val title: String,
    val status: TransactionStatus,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val source: String? = null,
    val description: String? = null,
    val income: Boolean
)
@Composable
fun DashboardScreen(
    username: String = "Manar",
    todayLabel: String = "08 June 2025",
    tasksCount: Int = 8
) {
    val plans = remember {
        listOf(
            TaskUi(
                taskId = 1,
                title = "Meeting at work",
                status = com.example.qareeb.data.entity.TaskStatus.IN_PROGRESS,
                dueDate = System.currentTimeMillis()
            ),
            TaskUi(
                taskId = 2,
                title = "Dinner with Ahmed",
                status = com.example.qareeb.data.entity.TaskStatus.COMPLETED,
                dueDate = System.currentTimeMillis() + 86_400_000
            ),
            TaskUi(
                taskId = 3,
                title = "Tennis Training",
                status = com.example.qareeb.data.entity.TaskStatus.POSTPONED,
                dueDate = System.currentTimeMillis() + 2 * 86_400_000
            )
        )
    }





    val expenses = remember {
        listOf(
            ExpenseItem(
                title = "Expert Consultation",
                status = TransactionStatus.COMPLETED,
                amount = 150.00,
                date = 1704067200000L,// 01 Jan 2024 (example timestamp),
                income=false
            ),
            ExpenseItem(
                title = "Office Supplies",
                status = TransactionStatus.DECLINED,

                amount = 45.00,
                date = 1704067200000L,
                income=true
            ),
            ExpenseItem(
                title = "Website Redesign",
                status = TransactionStatus.IN_PROGRESS,

                amount = 2500.00,
                date = 1704067200000L,
                income = true
            )
        )
    }



    var selectedDate by remember { mutableStateOf(LocalDate.now()) }


    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { BottomNavBar() },
    ) { padding ->

        FancyGradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                // Header (on gradient)
                WelcomeBanner(username = username)

                Spacer(Modifier.height(20.dp))
                // White background sheet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xFFECECEC),
                            shape = RoundedCornerShape(topStart = 80.dp, topEnd = 80.dp)
                        )
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            BigTasksBanner(
                                tasksCount = tasksCount,
                                todayLabel = todayLabel
                            )

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MiniCardPriority(modifier = Modifier.weight(1f))
                                MiniCardCompleted(modifier = Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(18.dp))
                            SearchBarStub()
                            Spacer(Modifier.height(14.dp))

                            WeekChipsRow(
                                selectedDate = selectedDate,
                                onSelect = { selectedDate = it }
                            )

                            Spacer(Modifier.height(24.dp))
                        }

                        // ================= TODAY'S PLANS (WHITE CARD) =================
                        item {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(vertical = 16.dp)
                            ) {
                                Column {
                                    SectionTitle(
                                        title = selectedDate.dayOfWeek.toString() + " PLANS",
                                    )

                                    Spacer(Modifier.height(15.dp))

                                    plans.forEach { plan ->
                                        if (plan.dueDate?.toLocalDate() == selectedDate) {
                                            PlanCard(plan = plan)
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(15.dp))
                                    Box(Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(Color.White, shape = RoundedCornerShape(4.dp))
                                        .border(1.dp, color = Color(0xFFECECEC))
                                    ){
                                        TextButton(onClick = {}) { Text("View All Plans", color = Color.Black, fontWeight = FontWeight.Bold,textAlign= TextAlign.Center) }
                                    }
                                }
                            }
                        }

                        // ================= TODAY'S EXPENSES (WHITE CARD) =================
                        item {
                            Spacer(Modifier.height(20.dp))

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(vertical = 16.dp)
                            ) {
                                Column {
                                    SectionTitle(
                                        title = "Recent Expenses",
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    expenses.forEach { ex ->
                                        ExpenseRow(item = ex)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    TextButton(onClick = {}) { Text("View All Expenses", color = Color.Black, fontWeight = FontWeight.Bold,textAlign= TextAlign.Center) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    QareebTheme {
        DashboardScreen()
    }
}
