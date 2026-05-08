package com.example.tectest.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class TransactionType {
    INCOME, EXPENSE
}

data class Transaction(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("title")
    val title: String = "",

    @get:PropertyName("amount")
    val amount: Double = 0.0,

    @get:PropertyName("type")
    val type: String = TransactionType.EXPENSE.name, // store as String either income or expense

    @get:PropertyName("date")
    val date: Timestamp = Timestamp.now(),

    @get:PropertyName("category")
    val category: String = "Other",

    @ServerTimestamp
    @get:PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    @get:PropertyName("userId")
    val userId: String = ""
) {
    constructor() : this("", "", 0.0, TransactionType.EXPENSE.name, Timestamp.now(), "Other", null, "")
}