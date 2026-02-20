package com.example.qareeb.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qareeb.presentation.ui.components.AppBackground
import com.example.qareeb.presentation.ui.components.CategoryChip
import com.example.qareeb.presentation.ui.components.FinanceWelcomeBanner
import com.example.qareeb.presentation.ui.components.SearchBarStub
import com.example.qareeb.presentation.ui.components.TransactionBox
import com.example.qareeb.presentation.ui.components.WeekChipsRow
import com.example.qareeb.presentation.viewModels.FinanceViewModel

@Composable
fun FinanceScreen(viewModel: FinanceViewModel) {

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val todayTransactions by viewModel.todayTransactions.collectAsStateWithLifecycle()
    val tomorrowTransactions by viewModel.tomorrowTransactions.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {

        AppBackground { Box(modifier = Modifier.fillMaxSize()) }

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──
            FinanceWelcomeBanner(username = viewModel.username)

            // ── White Sheet ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFFEDF2F7),
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                    )
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // ── Search Bar ──
                    item {
                        SearchBarStub()
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ── Week Chips ──
                    item {
                        WeekChipsRow(
                            selectedDate = selectedDate,
                            onSelect = { viewModel.onDateSelected(it) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Category Filter Chips ──
                    item {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 15.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            items(viewModel.categories) { category ->
                                CategoryChip(
                                    text = category,
                                    isSelected = category == selectedCategory,
                                    onClick = { viewModel.onCategorySelected(category) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ── Today's Transactions ──
                    item {
                        TransactionBox(
                            title = "Today's Transactions",
                            transactions = todayTransactions,
                            emptyMessage = "No transactions for this day ✅",
                            onStatusChange = { transaction, newState ->
                                viewModel.updateTransactionState(transaction, newState)
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    // ── Tomorrow's Transactions ──
                    item {
                        TransactionBox(
                            title = "Tomorrow's Transactions",
                            transactions = tomorrowTransactions,
                            emptyMessage = "No transactions for tomorrow ✅",
                            onStatusChange = { transaction, newState ->
                                viewModel.updateTransactionState(transaction, newState)
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }
        }
    }
}