package com.example.qareeb.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.presentation.ui.components.BottomNavBar
import com.example.qareeb.presentation.ui.components.CategoryChip
import com.example.qareeb.presentation.ui.components.FancyGradientBackground
import com.example.qareeb.presentation.ui.components.ExpenseRow
import com.example.qareeb.presentation.ui.components.FinanceWelcomeBanner
import com.example.qareeb.presentation.ui.components.SearchBarStub
import com.example.qareeb.presentation.ui.components.TransactionBox
import com.example.qareeb.presentation.ui.components.WeekChipsRow
import com.example.qareeb.ui.theme.dmSansFamily
import com.example.qareeb.utilis.toLocalDate
import java.time.LocalDate

data class ExpensesItem(
    val title: String,
    val status: TransactionStatus,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val source: String? = null,
    val description: String? = null,
    val income: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFinanceScreen(
    username: String = "User"
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Pending", "Completed", "Declined")

    val expenses = remember {
        listOf(
            ExpenseItem(
                title = "Expert Consultation",
                status = TransactionStatus.COMPLETED,
                amount = 150.00,
                date = System.currentTimeMillis(),
                income = false
            ),
            ExpenseItem(
                title = "Office Supplies",
                status = TransactionStatus.DECLINED,
                amount = 45.00,
                date = System.currentTimeMillis(),
                income = true
            ),
            ExpenseItem(
                title = "Website Redesign",
                status = TransactionStatus.IN_PROGRESS,
                amount = 2500.00,
                date = System.currentTimeMillis(),
                income = true
            )
        )
    }

    val tomorrowExpenses = remember {
        listOf(
            ExpenseItem(
                title = "Client Meeting",
                status = TransactionStatus.COMPLETED,
                amount = 200.00,
                date = System.currentTimeMillis() + 86_400_000,
                income = true
            ),
            ExpenseItem(
                title = "Software License",
                status = TransactionStatus.IN_PROGRESS,
                amount = 99.00,
                date = System.currentTimeMillis() + 86_400_000,
                income = false
            )
        )
    }

    // Filter logic based on selected date
    val todayDate = selectedDate
    val tomorrowDate = selectedDate.plusDays(1)

    val filteredTodayExpenses = remember(selectedDate, expenses, tomorrowExpenses) {
        (expenses + tomorrowExpenses).filter { expense ->
            expense.date.toLocalDate() == todayDate
        }
    }

    val filteredTomorrowExpenses = remember(selectedDate, expenses, tomorrowExpenses) {
        (expenses + tomorrowExpenses).filter { expense ->
            expense.date.toLocalDate() == tomorrowDate
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { BottomNavBar() }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient Background
            FancyGradientBackground { Box(modifier = Modifier.fillMaxSize()) }

            // Content
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                FinanceWelcomeBanner(username = username)

                // Main container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xFFEDF2F7),
                            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                        )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Search Bar
                        item {
                            SearchBarStub()
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Week chips
                        item {
                            WeekChipsRow(
                                selectedDate = selectedDate,
                                onSelect = { selectedDate = it }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Category filters
                        item {
                            LazyRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(categories) { category ->
                                    CategoryChip(
                                        text = category,
                                        isSelected = category == selectedCategory,
                                        onClick = { selectedCategory = category }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Today's Transactions Box
                        item {
                            TransactionBox(
                                title = "Today's Transactions",
                                transactions = filteredTodayExpenses,
                                emptyMessage = "No transactions for this day ✅"
                            )
                        }

                        // Space between the two boxes
                        item { Spacer(modifier = Modifier.height(24.dp)) }

                        // Tomorrow's Transactions Box
                        item {
                            TransactionBox(
                                title = "Tomorrow's Transactions",
                                transactions = filteredTomorrowExpenses,
                                emptyMessage = "No transactions for tomorrow ✅"
                            )
                        }

                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MyFinanceScreenPreview() {
    MyFinanceScreen(username = "Farida")
}