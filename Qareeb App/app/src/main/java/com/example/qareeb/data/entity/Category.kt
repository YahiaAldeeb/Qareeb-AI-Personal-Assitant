package com.example.qareeb.data.entity

import androidx.room.*

@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    val categoryId: Long = 0,

    val name: String
)
