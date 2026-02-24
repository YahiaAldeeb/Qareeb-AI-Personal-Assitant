package com.example.qareeb

import com.google.gson.annotations.SerializedName

data class QareebResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("intent")
    val intent: String? = null,
    
    @SerializedName("transcription")
    val transcription: String? = null,
    
    @SerializedName("result")
    val result: ResultData? = null
)

data class ResultData(
    @SerializedName("success")
    val success: Boolean? = null,
    
    @SerializedName("data")
    val data: TransactionTaskData? = null,
    
    @SerializedName("error")
    val error: String? = null
)

data class TransactionTaskData(
    @SerializedName("transaction")
    val transaction: TransactionResponse? = null,
    
    @SerializedName("task")
    val task: TaskResponse? = null
)

data class TransactionResponse(
    @SerializedName("transactionID")
    val transactionID: String,
    
    @SerializedName("userID")
    val userID: String,
    
    @SerializedName("amount")
    val amount: Double? = null,
    
    @SerializedName("date")
    val date: String? = null,
    
    @SerializedName("source")
    val source: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("income")
    val income: Boolean? = false,
    
    @SerializedName("state")
    val state: String? = null,
    
    @SerializedName("categoryID")
    val categoryID: String? = null,
    
    @SerializedName("created_at")
    val created_at: String? = null,
    
    @SerializedName("is_deleted")
    val is_deleted: Boolean? = false
)

data class TaskResponse(
    @SerializedName("taskID")
    val taskID: String,
    
    @SerializedName("userID")
    val userID: String,
    
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("status")
    val status: String? = null,
    
    @SerializedName("progressPercentage")
    val progressPercentage: Int? = 0,
    
    @SerializedName("priority")
    val priority: String? = null,
    
    @SerializedName("dueDate")
    val dueDate: String? = null,
    
    @SerializedName("created_at")
    val created_at: String? = null,
    
    @SerializedName("updated_at")
    val updated_at: String? = null,
    
    @SerializedName("is_deleted")
    val is_deleted: Boolean? = false
)