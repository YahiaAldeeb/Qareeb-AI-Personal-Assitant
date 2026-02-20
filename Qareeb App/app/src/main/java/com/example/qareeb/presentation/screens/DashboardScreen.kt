package com.example.qareeb.presentation.screens

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
import com.example.qareeb.presentation.theme.QareebTheme
import com.example.qareeb.presentation.ui.components.*
import com.example.qareeb.presentation.utilis.toLocalDate
import com.example.qareeb.presentation.viewModels.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onViewAllPlans: () -> Unit,
    onViewAllExpenses: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val todayTasks by viewModel.todayTasks.collectAsState()
    val todayTasksCount by viewModel.todayTasksCount.collectAsState()
    val priorityTasksCount by viewModel.priorityTasksCount.collectAsState()
    val completedTasksCount by viewModel.completedTasksCount.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val priorityTasks by viewModel.priorityTasks.collectAsState()

    // formats date as "20 February 2026"
    val todayLabel = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        AppBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                // ── Header ──
                WelcomeBanner(username = viewModel.username)
                Spacer(Modifier.height(20.dp))

                // ── White Sheet ──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xFFECECEC),
                            shape = RoundedCornerShape(topStart = 80.dp, topEnd = 80.dp)
                        )
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {

                        // ── Stats Banner ──
                        item {
                            BigTasksBanner(
                                tasksCount = todayTasksCount,
                                todayLabel = todayLabel  // "20 February 2026"
                            )
                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MiniCardPriority(
                                    modifier = Modifier.weight(1f),
                                    tasks=priorityTasks
                                )
                                MiniCardCompleted(
                                    modifier = Modifier.weight(1f),
                                    count = completedTasksCount
                                )
                            }

                            Spacer(Modifier.height(18.dp))
                            SearchBarStub()
                            Spacer(Modifier.height(14.dp))

                            // ── Week Chips ──
                            WeekChipsRow(
                                selectedDate = selectedDate,
                                onSelect = { viewModel.onDateSelected(it) }
                            )
                            Spacer(Modifier.height(24.dp))
                        }

                        // ── Today's Plans ──
                        item {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(20.dp))
                                    .padding(vertical = 16.dp)
                            ) {
                                Column {
                                    SectionTitle(
                                        title = "${selectedDate.dayOfWeek} PLANS"
                                    )
                                    Spacer(Modifier.height(15.dp))

                                    if (todayTasks.isEmpty()) {
                                        Text(
                                            text = "No tasks for this day",
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = Color.Gray
                                        )
                                    } else {
                                        todayTasks.forEach { task ->
                                            PlanCard(task = task)
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }

                                    Spacer(Modifier.height(15.dp))
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFFECECEC))
                                    ) {
                                        TextButton(
                                            onClick = onViewAllPlans,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "View All Plans",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Recent Expenses ──
                        item {
                            Spacer(Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(20.dp))
                                    .padding(vertical = 16.dp)
                            ) {
                                Column {
                                    SectionTitle(title = "Recent Expenses")
                                    Spacer(Modifier.height(12.dp))

                                    if (recentTransactions.isEmpty()) {
                                        Text(
                                            text = "No recent transactions",
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = Color.Gray
                                        )
                                    } else {
                                        recentTransactions.forEach { transaction ->
                                            ExpenseRow(transaction = transaction)
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }

                                    TextButton(
                                        onClick = onViewAllExpenses,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "View All Expenses",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Preview ──
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    QareebTheme {
        // fake static data just for preview
        DashboardScreen(
            viewModel = TODO("use a fake ViewModel or split into DashboardContent"),
            onViewAllPlans = {},
            onViewAllExpenses = {}
        )
    }
}