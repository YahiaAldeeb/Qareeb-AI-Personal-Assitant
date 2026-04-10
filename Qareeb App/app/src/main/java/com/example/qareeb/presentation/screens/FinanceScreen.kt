package com.example.qareeb.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qareeb.presentation.ui.components.*
import com.example.qareeb.presentation.viewModels.FinanceViewModel

@Composable
fun FinanceScreen(viewModel: FinanceViewModel) {

    var selectedTab by remember { mutableIntStateOf(0) }

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val todayTransactions by viewModel.todayTransactions.collectAsStateWithLifecycle()
    val tomorrowTransactions by viewModel.tomorrowTransactions.collectAsStateWithLifecycle()

    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpenses by viewModel.totalExpenses.collectAsStateWithLifecycle()
    val spendingByCategory by viewModel.spendingByCategory.collectAsStateWithLifecycle()
    val dailySpending by viewModel.dailySpendingLast30Days.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {

        AppBackground { Box(modifier = Modifier.fillMaxSize()) }

        Column(modifier = Modifier.fillMaxSize()) {

            FinanceWelcomeBanner(username = viewModel.username)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFFEDF2F7),
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFF6366F1),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    "Transactions",
                                    color = if (selectedTab == 0) Color(0xFF6366F1) else Color.Gray
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "Analytics",
                                    color = if (selectedTab == 1) Color(0xFF6366F1) else Color.Gray
                                )
                            }
                        )
                    }

                    when (selectedTab) {
                        0 -> TransactionsTab(
                            selectedDate = selectedDate,
                            selectedCategory = selectedCategory,
                            todayTransactions = todayTransactions,
                            tomorrowTransactions = tomorrowTransactions,
                            categories = viewModel.categories,
                            onDateSelected = viewModel::onDateSelected,
                            onCategorySelected = viewModel::onCategorySelected,
                            onStatusChange = viewModel::updateTransactionState
                        )
                        1 -> AnalyticsTab(
                            totalIncome = totalIncome,
                            totalExpenses = totalExpenses,
                            spendingByCategory = spendingByCategory,
                            dailySpending = dailySpending
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsTab(
    selectedDate: java.time.LocalDate,
    selectedCategory: String,
    todayTransactions: List<com.example.qareeb.domain.model.TransactionDomain>,
    tomorrowTransactions: List<com.example.qareeb.domain.model.TransactionDomain>,
    categories: List<String>,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onCategorySelected: (String) -> Unit,
    onStatusChange: (com.example.qareeb.domain.model.TransactionDomain, com.example.qareeb.domain.model.enums.TransactionState) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            SearchBarStub()
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            WeekChipsRow(
                selectedDate = selectedDate,
                onSelect = onDateSelected
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            LazyRow(
                modifier = Modifier.padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(
                        text = category,
                        isSelected = category == selectedCategory,
                        onClick = { onCategorySelected(category) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            TransactionBox(
                title = "Today's Transactions",
                transactions = todayTransactions,
                emptyMessage = "No transactions for this day ✅",
                onStatusChange = onStatusChange
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            TransactionBox(
                title = "Tomorrow's Transactions",
                transactions = tomorrowTransactions,
                emptyMessage = "No transactions for tomorrow ✅",
                onStatusChange = onStatusChange
            )
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

@Composable
private fun AnalyticsTab(
    totalIncome: Double,
    totalExpenses: Double,
    spendingByCategory: List<com.example.qareeb.presentation.viewModels.CategorySpending>,
    dailySpending: List<com.example.qareeb.presentation.viewModels.DailySpending>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            SpendingSummaryCard(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses
            )
        }

        item {
            SpendingByCategoryChart(categorySpending = spendingByCategory)
        }

        item {
            SpendingTrendChart(dailySpending = dailySpending)
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}