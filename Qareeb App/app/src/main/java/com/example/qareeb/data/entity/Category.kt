package com.example.qareeb.data.entity

import androidx.room.*
import java.util.UUID

@Entity(tableName = "category")
data class Category(
    @PrimaryKey
    @ColumnInfo(name = "category_id")
    val categoryId: String = UUID.randomUUID().toString(),

    val name: String
)
