package com.example.qareeb.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qareeb.NetworkModule
import com.example.qareeb.QareebResponse
import com.example.qareeb.TextMessageRequest
import com.example.qareeb.data.dao.MemoryDao
import com.example.qareeb.data.dao.PromptDao
import com.example.qareeb.data.dao.TaskDao
import com.example.qareeb.data.dao.TransactionDao
import com.example.qareeb.data.entity.Memory
import com.example.qareeb.data.entity.Prompt
import com.example.qareeb.data.entity.Task
import com.example.qareeb.data.entity.Transaction
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.domain.model.enums.TaskStatus
import com.example.qareeb.domain.model.enums.TransactionState
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

sealed class ChatUiState {
    data object Idle : ChatUiState()
    data object Thinking : ChatUiState()
    data class Success(val message: String) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatBotViewModel(
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val promptDao: PromptDao,   // ← add
    private val memoryDao: MemoryDao,   // ← add
    private val sessionManager: SessionManager,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val userId: String = sessionManager.getUserId() ?: ""

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        // Add welcome message
        _messages.value = listOf(
            ChatMessage(
                text = "Hi! I'm Qareeb. Tell me what you'd like to do - like 'I spent 50 pounds on food' or 'I have a meeting at 3pm' or 'open YouTube'",
                isUser = false
            )
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value is ChatUiState.Thinking) return

        // Add user message
        _messages.value = _messages.value + ChatMessage(text = text, isUser = true)

        // Show thinking state
        _uiState.value = ChatUiState.Thinking

        viewModelScope.launch {
            try {
                // 1. Load short-term memory (last 10 prompts)
                val recentPrompts = promptDao.getRecentPrompts(userId, 10)
                    .reversed() // oldest first
                    .joinToString("\n") { "User: ${it.userMessage}\nQareeb: ${it.qareebResponse}" }

                // 2. Load long-term memory (all facts)
                val memories = memoryDao.getAllMemories(userId)
                    .joinToString("\n") { "- ${it.fact}" }

                // 3. Build enriched text with context
                val enrichedText = buildString {
                    if (memories.isNotEmpty()) {
                        append("[User habits and preferences:\n$memories]\n\n")
                    }
                    if (recentPrompts.isNotEmpty()) {
                        append("[Recent conversation:\n$recentPrompts]\n\n")
                    }
                    append(text)
                }

                val response = NetworkModule.api.sendTextMessage(
                    TextMessageRequest(text = enrichedText, userID = userId)
                )

                // 4. Save this turn to local prompt history
                val botReply = extractBotReply(response)
                promptDao.insertPrompt(
                    Prompt(
                        userId = userId,
                        userMessage = text, // save original, not enriched
                        qareebResponse = botReply,
                        promptType = "TEXT",
                        module = "CHATBOT",
                        intentDetected = response.intent
                    )
                )

                // 5. Extract and save long-term memory fact if useful
                extractAndSaveMemory(text, botReply)

                handleResponse(response)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed: ${e.message}")
                addBotMessage("Sorry, I couldn't process that. Please try again.")
            }
        }
    }
    private fun extractBotReply(response: QareebResponse): String {
        return when (response.intent) {
            "TASK_TRACKER" -> "Created task: ${response.result?.data?.task?.title}"
            "FINANCE" -> "Recorded transaction of ${response.result?.data?.transaction?.amount}"
            else -> response.status
        }
    }
    private suspend fun extractAndSaveMemory(userText: String, botReply: String) {
        // Simple rule-based extraction — no extra LLM call needed
        val lower = userText.lowercase()
        val fact: String? = when {
            lower.contains("every monday")||lower.contains("on mondays") ->
                "User has a recurring activity on Mondays"
            lower.contains("every morning") ->
                "User prefers morning routines"
            lower.contains("every evening") ||lower.contains("every night") ->
                "User prefers evening routines"
            lower.contains("i always") ->
                "User habit: $userText"
            lower.contains("i usually") ->
                "User usually: ${userText.replace("i usually", "").trim()}"
            lower.contains("i prefer") ->
                "User preference: ${userText.replace("i prefer", "").trim()}"
            else -> null
        }

        fact?.let {
            memoryDao.deleteByKeyPattern(userId, "%${it.take(20)}%")
            memoryDao.insertMemory(
                Memory(userId = userId, fact = it)
            )
        }
    }
    private fun handleResponse(response: QareebResponse) {
        viewModelScope.launch {
            try {
                when (response.status) {
                    "success" -> {
                        when (response.intent) {
                            "FINANCE" -> {
                                val transactionData = response.result?.data?.transaction
                                if (transactionData != null) {
                                    val transaction = parseTransactionFromResponse(transactionData, userId)
                                    transactionDao.upsertTransaction(transaction)
                                    val amount = transactionData.amount ?: 0.0
                                    val isIncome = transactionData.income ?: false
                                    val prefix = if (isIncome) "Added" else "Spent"
                                    val amountText = "£${String.format("%.2f", amount)}"
                                    val message = if (isIncome) "$amountText income ✅" else "$amountText expense ✅"
                                    _uiState.value = ChatUiState.Success(message)
                                    addBotMessage(message)
                                } else {
                                    _uiState.value = ChatUiState.Error("Could not create transaction")
                                    addBotMessage("Sorry, I couldn't create the transaction. Please try again.")
                                }
                            }
                            "TASK_TRACKER" -> {
                                val taskData = response.result?.data?.task
                                if (taskData != null) {
                                    val task = parseTaskFromResponse(taskData, userId)
                                    taskDao.upsertTask(task)
                                    try {
                                        withContext(Dispatchers.IO) {     syncRepository.sync(userId) }

                                    } catch (e: Exception) {
                                        android.util.Log.e("CHATBOT", "Sync pull failed: ${e.message}")
                                    }
                                    val title = taskData.title ?: "task"
                                    _uiState.value = ChatUiState.Success("Created task: $title ✅")
                                    addBotMessage("Created task: $title ✅")
                                } else {
                                    _uiState.value = ChatUiState.Error("Could not create task")
                                    addBotMessage("Sorry, I couldn't create the task. Please try again.")
                                }
                            }
                            else -> {
                                _uiState.value = ChatUiState.Idle
                            }
                        }
                    }
                    "accepted" -> {
                        // UI Automation - job started
                        val command = response.text ?: ""
                        _uiState.value = ChatUiState.Success("Opening $command...")
                        addBotMessage("Opening $command... (This may take a moment)")
                    }
                    "unknown_intent" -> {
                        _uiState.value = ChatUiState.Error("I didn't understand that. Try something like 'open YouTube', 'I spent 50 pounds', or 'create a task'")
                        addBotMessage("I didn't understand that. Try something like:\n• 'Open YouTube'\n• 'I spent 50 pounds'\n• 'Create a meeting at 3pm'")
                    }
                    else -> {
                        _uiState.value = ChatUiState.Error(response.result?.error ?: "Something went wrong")
                        addBotMessage("Sorry, something went wrong. Please try again.")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Error: ${e.message}")
                addBotMessage("Sorry, I encountered an error. Please try again.")
            }
        }
    }

    private fun addBotMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(text = text, isUser = false)
    }

    fun resetState() {
        _uiState.value = ChatUiState.Idle
    }

    private fun parseTransactionFromResponse(
        transactionData: com.example.qareeb.TransactionResponse,
        userId: String
    ): Transaction {
        val dateMillis = try {
            transactionData.date?.let {
                try {
                    OffsetDateTime.parse(it).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    try {
                        Instant.parse(it).toEpochMilli()
                    } catch (e2: Exception) {
                        System.currentTimeMillis()
                    }
                }
            } ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val state = try {
            transactionData.state?.let { TransactionState.valueOf(it.uppercase()) }
                ?: TransactionState.PENDING
        } catch (e: Exception) {
            TransactionState.PENDING
        }

        return Transaction(
            transactionId = transactionData.transactionID,
            userId = userId,
            categoryId = transactionData.categoryID,
            amount = transactionData.amount ?: 0.0,
            date = dateMillis,
            source = transactionData.source,
            description = transactionData.description,
            title = transactionData.title ?: transactionData.description ?: "Transaction",
            income = transactionData.income ?: false,
            state = state,
            isDeleted = transactionData.is_deleted ?: false,
            is_synced = true,
            updatedAt = transactionData.created_at ?: OffsetDateTime.now().toString()
        )
    }

    private fun parseTaskFromResponse(
        taskData: com.example.qareeb.TaskResponse,
        userId: String
    ): Task {
        val dueDateMillis: Long? = taskData.dueDate?.let { dueDateStr ->
            try {
                try {
                    OffsetDateTime.parse(dueDateStr).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    try {
                        Instant.parse(dueDateStr).toEpochMilli()
                    } catch (e2: Exception) {
                        java.time.LocalDate.parse(dueDateStr)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

        val status = try {
            taskData.status?.let { TaskStatus.valueOf(it.uppercase()) }
                ?: TaskStatus.PENDING
        } catch (e: Exception) {
            TaskStatus.PENDING
        }

        val priority = taskData.priority ?: "MEDIUM"

        return Task(
            taskId = taskData.taskID,
            userId = userId,
            title = taskData.title ?: "Task",
            description = taskData.description,
            status = status,
            progressPercentage = taskData.progressPercentage ?: 0,
            priority = priority,
            dueDate = dueDateMillis,
            isDeleted = taskData.is_deleted ?: false,
            is_synced = true,
            updatedAt = taskData.updated_at ?: OffsetDateTime.now().toString()
        )
    }
}

class ChatBotViewModelFactory(
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val promptDao: PromptDao,     // ✅ add this
    private val memoryDao: MemoryDao,     // ✅ add this
    private val sessionManager: SessionManager,
    private val syncRepository: SyncRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatBotViewModel(
            transactionDao,
            taskDao,
            promptDao,        // ✅ pass it
            memoryDao,        // ✅ pass it
            sessionManager,
            syncRepository
        ) as T
    }
}