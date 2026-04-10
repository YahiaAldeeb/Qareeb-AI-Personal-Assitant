package com.example.qareeb.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qareeb.data.entity.Category
import com.example.qareeb.domain.model.TransactionDomain
import com.example.qareeb.domain.model.enums.TransactionState
import com.example.qareeb.domain.repository.CategoryRepository
import com.example.qareeb.domain.repository.TransactionRepository
import com.example.qareeb.domain.usecase.transaction.AddTransactionUseCase
import com.example.qareeb.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.qareeb.domain.usecase.transaction.GetTransactionsByUserUseCase
import com.example.qareeb.domain.usecase.transaction.UpdateTransactionUseCase
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val amount: Double
)

data class DailySpending(
    val date: LocalDate,
    val amount: Double
)

class FinanceViewModel(
    private val getTransactionsByUser: GetTransactionsByUserUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val sessionManager: SessionManager,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    val username: String
) : ViewModel() {

    private val userId: String = sessionManager.getUserId() ?: ""

    // TODO: Remove after full category setup - temporary demo population
    private val demoCategories = listOf("Food", "Transportation", "Shopping", "Entertainment", "Bills", "Health", "Other")

    init {
        android.util.Log.d("FINANCE_VM", "FinanceViewModel userId: $userId")
        initializeDemoCategories()
    }

    private fun initializeDemoCategories() {
        viewModelScope.launch {
            val existingCategories = categoryRepository.getAllCategories().first()
            if (existingCategories.isEmpty()) {
                demoCategories.forEach { name ->
                    categoryRepository.insertCategory(Category(name = name))
                }
                android.util.Log.d("FINANCE_VM", "Created demo categories: $demoCategories")
            }

            val allCategories = categoryRepository.getAllCategories().first()
            val uncategorized = transactionRepository.getUncategorizedTransactions(userId)
            if (uncategorized.isNotEmpty()) {
                android.util.Log.d("FINANCE_VM", "Assigning categories to ${uncategorized.size} uncategorized transactions")
                uncategorized.forEach { transaction ->
                    val randomCategory = allCategories.random()
                    transactionRepository.updateTransactionCategoryId(transaction.transactionId, randomCategory.categoryId)
                }
            }
        }
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val categories = listOf("All", "Pending", "Completed", "Declined", "In Progress")

    // all transactions from DB
    private val allTransactions: StateFlow<List<TransactionDomain>> =
        getTransactionsByUser(userId)
            .onEach { list ->
                android.util.Log.d("FINANCE_VM", "Transactions loaded: ${list.size}")
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // today's transactions filtered by date and category
    val todayTransactions: StateFlow<List<TransactionDomain>> = combine(
        allTransactions, _selectedDate, _selectedCategory
    ) { transactions, date, category ->
        transactions
            .filter { t ->
                val tDate = Instant.ofEpochMilli(t.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                tDate == date
            }
            .filter { matchesCategory(it, category) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // tomorrow's transactions filtered by date and category
    val tomorrowTransactions: StateFlow<List<TransactionDomain>> = combine(
        allTransactions, _selectedDate, _selectedCategory
    ) { transactions, date, category ->
        transactions
            .filter { t ->
                val tDate = Instant.ofEpochMilli(t.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                tDate == date.plusDays(1)
            }
            .filter { matchesCategory(it, category) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics: total income
    val totalIncome: StateFlow<Double> = allTransactions
        .map { transactions ->
            transactions.filter { it.income }.sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Analytics: total expenses
    val totalExpenses: StateFlow<Double> = allTransactions
        .map { transactions ->
            transactions.filter { !it.income }.sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Analytics: spending by category (expenses only)
    val spendingByCategory: StateFlow<List<CategorySpending>> = combine(
        allTransactions,
        categoryRepository.getAllCategories()
    ) { transactions, categories ->
        val categoryMap = categories.associateBy { it.categoryId }
        transactions
            .filter { !it.income && it.categoryId != null }
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, txns) ->
                val category = categoryMap[categoryId]
                category?.let {
                    CategorySpending(
                        categoryId = it.categoryId,
                        categoryName = it.name,
                        amount = txns.sumOf { t -> t.amount }
                    )
                }
            }
            .sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics: daily spending for last 30 days
    val dailySpendingLast30Days: StateFlow<List<DailySpending>> = allTransactions
        .map { transactions ->
            val thirtyDaysAgo = LocalDate.now().minusDays(30)
            transactions
                .filter { !it.income }
                .map { t ->
                    val tDate = Instant.ofEpochMilli(t.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    tDate to t.amount
                }
                .filter { it.first >= thirtyDaysAgo }
                .groupBy { it.first }
                .map { (date, pairs) ->
                    DailySpending(date = date, amount = pairs.sumOf { it.second })
                }
                .sortedBy { it.date }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun updateTransactionState(transaction: TransactionDomain, newState: TransactionState) {
        viewModelScope.launch {
            updateTransaction(transaction.copy(state = newState))
        }
    }

    private fun matchesCategory(transaction: TransactionDomain, category: String): Boolean {
        return when (category) {
            "All"         -> true
            "Pending"     -> transaction.state == TransactionState.PENDING
            "Completed"   -> transaction.state == TransactionState.COMPLETED
            "Declined"    -> transaction.state == TransactionState.DECLINED
            "In Progress" -> transaction.state == TransactionState.IN_PROGRESS
            else          -> true
        }
    }
}

class FinanceViewModelFactory(
    private val getTransactionsByUser: GetTransactionsByUserUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val sessionManager: SessionManager,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val username: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FinanceViewModel(
            getTransactionsByUser, updateTransaction,
            addTransaction, deleteTransaction, sessionManager,
            transactionRepository, categoryRepository, username
        ) as T
    }
}