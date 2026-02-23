package com.example.qareeb.data.entity

import androidx.room.*
import com.example.qareeb.domain.model.enums.TransactionState
import java.util.UUID

@Entity(
    tableName = "transaction",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userID"],
            childColumns = ["userID"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userID"), Index("category_id")]
)
data class Transaction(
    @PrimaryKey
    @ColumnInfo(name = "transaction_id")
    val transactionId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "userID")
    val userId: String,

    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,

    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val source: String? = null,
    val description: String? = null,
    val title: String,

    val income: Boolean = false,

    val state: TransactionState = TransactionState.PENDING,

    // ── Sync fields ──
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "is_synced")
    val is_synced: Boolean = false,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String = java.time.OffsetDateTime.now().toString()
)