package com.example.qareeb.domain.model

import com.example.qareeb.domain.model.enums.TransactionState

data class TransactionDomain(
    val transactionId: String,
    val userId: String,

    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val source: String? = null,
    val description: String?=null,
    val title: String,

    // true = income (green), false = outcome/expense (red)
    val income: Boolean = false,

    // Transaction state using enum: "completed", "declined", "pending", "in_progress"
    val state: TransactionState = TransactionState.PENDING,
    val categoryId: String? = null,

    )
