package com.example.qareeb.presentation.mapper


import com.example.qareeb.domain.model.enums.TransactionState
import com.example.qareeb.presentation.models.TransactionStateUI


fun TransactionState.toUI(): TransactionStateUI {
    return when (this) {
        TransactionState.COMPLETED -> TransactionStateUI.Completed
        TransactionState.DECLINED -> TransactionStateUI.Declined
        TransactionState.PENDING -> TransactionStateUI.Pending
        TransactionState.IN_PROGRESS -> TransactionStateUI.InProgress
    }
}
