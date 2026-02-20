package com.example.qareeb.data.entity

import androidx.room.*
import com.example.qareeb.domain.model.enums.TransactionState

@Entity(
    tableName = "transaction",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("user_id"), Index("category_id")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val source: String? = null,
    val description: String?= null,
    val title: String,

    // true = income (green), false = outcome/expense (red)
    val income: Boolean = false,

    // Transaction state using enum: "completed", "declined", "pending", "in_progress"
    val state: TransactionState = TransactionState.PENDING
)
