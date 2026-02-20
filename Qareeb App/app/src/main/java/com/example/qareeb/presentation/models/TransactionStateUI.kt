package com.example.qareeb.presentation.models



import androidx.compose.ui.graphics.Color

sealed class TransactionStateUI(
    val displayName: String,
    val color: Color
) {
    object Completed : TransactionStateUI("Completed", Color(0xFF00A63E))
    object Declined : TransactionStateUI("Declined", Color(0xFFA62700))
    object Pending : TransactionStateUI("Pending", Color(0xFF5E10FA))
    object InProgress : TransactionStateUI("In Progress", Color(0xFF00A63E))
}
