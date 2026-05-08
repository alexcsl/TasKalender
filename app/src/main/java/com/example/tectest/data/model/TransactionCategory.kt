package com.example.tectest.data.model

import com.google.firebase.firestore.PropertyName

data class TransactionCategory(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("name")
    val name: String = "",

    @get:PropertyName("userId")
    val userId: String = ""
) {
    constructor() : this("", "", "")
}