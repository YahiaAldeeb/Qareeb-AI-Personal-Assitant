package com.example.qareeb.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qareeb.data.AppDatabase
import com.example.qareeb.data.SessionManager
import com.example.qareeb.data.dao.TransactionDao
import com.example.qareeb.data.entity.Transaction
import com.example.qareeb.data.entity.TransactionState
import com.example.qareeb.presentation.ui.components.CategoryChip
import com.example.qareeb.presentation.ui.components.FancyGradientBackground
import com.example.qareeb.presentation.ui.components.FinanceWelcomeBanner
import com.example.qareeb.presentation.ui.components.SearchBarStub
import com.example.qareeb.presentation.ui.components.TransactionBox
import com.example.qareeb.presentation.ui.components.WeekChipsRow
import com.example.qareeb.presentation.utilis.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExpensesItem(
    val title: String,
    val status: TransactionState,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val source: String? = null,
    val description: String? = null,
    val income: Boolean
)

private fun Transaction.toExpensesItem(): ExpensesItem {
    // Your Transaction table doesn't have a "title", so we derive something stable for UI.
    // If you later add a "title/name" column, replace this line.
    val derivedTitle = description?.takeIf { it.isNotBlank() }
        ?: source?.takeIf { it.isNotBlank() }
        ?: "Transaction #$transactionId"

    return ExpensesItem(
        title = derivedTitle,
        status = this.state,
        amount = this.amount,
        date = this.date,
        source = this.source,
        description = this.description,
        income = this.income
    )
}

private fun categoryToStateOrNull(category: String): TransactionState? {
    return when (category) {
        "Pending" -> TransactionState.PENDING
        "Completed" -> TransactionState.COMPLETED
        "Declined" -> TransactionState.DECLINED
        "In Progress" -> TransactionState.IN_PROGRESS
        else -> null // "All"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFinanceScreen() {
    // Get context, database, and session manager
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val sessionManager = remember { SessionManager.getInstance(context) }

    // Get userId and username from session
    val userId = sessionManager.getUserId()
    val user by database.userDao().getUserById(userId).collectAsState(initial = null)
    val username = user?.name ?: "Guest"

    // Get transactionDao from database
    val transactionDao = database.transactionDao()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Pending", "Completed", "Declined", "In Progress")

    // Dates
    val todayDate = selectedDate
    val tomorrowDate = selectedDate.plusDays(1)

    // --- DAO FLOW (replaces mock data) ---
    val stateFilter = remember(selectedCategory) { categoryToStateOrNull(selectedCategory) }

    val transactionsFlow: Flow<List<Transaction>> = remember(userId, stateFilter) {
        if (stateFilter == null) {
            transactionDao.getTransactionsByUser(userId)
        } else {
            transactionDao.getTransactionsByState(userId, stateFilter)
        }
    }

    val transactionsFromDb by transactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Map DB entities -> UI items
    val allExpenses = remember(transactionsFromDb) {
        transactionsFromDb.map { it.toExpensesItem() }
    }

    val filteredTodayExpenses = remember(selectedDate, allExpenses) {
        allExpenses.filter { it.date.toLocalDate() == todayDate }
    }

    val filteredTomorrowExpenses = remember(selectedDate, allExpenses) {
        allExpenses.filter { it.date.toLocalDate() == tomorrowDate }
    }

    // --- Status change updates Room (best match using transactionId) ---
    val onStatusChange: (ExpensesItem, TransactionState) -> Unit = { oldItem, newState ->

        val match = transactionsFromDb.firstOrNull { t ->
            val tTitle = t.description?.takeIf { it.isNotBlank() }
                ?: t.source?.takeIf { it.isNotBlank() }
                ?: "Transaction #${t.transactionId}"

            tTitle == oldItem.title &&
                    t.date == oldItem.date &&
                    t.amount == oldItem.amount &&
                    t.income == oldItem.income
        }

        if (match != null) {
            CoroutineScope(Dispatchers.IO).launch {
                transactionDao.updateTransaction(match.copy(state = newState))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Gradient Background
        FancyGradientBackground { Box(modifier = Modifier.fillMaxSize()) }

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
                    modifier = Modifier.fillMaxSize()
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
                            modifier = Modifier.padding(horizontal = 15.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                            emptyMessage = "No transactions for this day ✅",
                            onStatusChange = onStatusChange
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    // Tomorrow's Transactions Box
                    item {
                        TransactionBox(
                            title = "Tomorrow's Transactions",
                            transactions = filteredTomorrowExpenses,
                            emptyMessage = "No transactions for tomorrow ✅",
                            onStatusChange = onStatusChange
                        )
                    }

                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MyFinanceScreenPreview() {
    // Preview can't access Room directly.
    // MyFinanceScreen()
}