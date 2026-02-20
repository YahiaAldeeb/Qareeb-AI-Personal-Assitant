package com.example.qareeb.data.mapper
import com.example.qareeb.data.entity.Transaction
import com.example.qareeb.domain.model.TransactionDomain
import com.example.qareeb.domain.model.enums.TransactionState     // keep enum in data or move to domain
import com.example.qareeb.domain.model.enums.TaskStatus    // domain m

fun Transaction.toDomain(): TransactionDomain {
    return TransactionDomain(
        transactionId = transactionId,
        userId = userId,
        categoryId = categoryId,
        amount = amount,
        date = date,
        source = source,
        description = description,
        income = income,
        title = title,
        state = state
    )
}

fun TransactionDomain.toEntity(): Transaction {
    return Transaction(
        transactionId = transactionId,
        userId = userId,
        categoryId = categoryId,
        amount = amount,
        date = date,
        source = source,
        description = description,
        income = income,
        title = title,
        state = state
    )
}