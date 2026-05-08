package com.example.tectest.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Task(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("title")
    val title: String = "",

    @get:PropertyName("category")
    val category: String = "Other",

    @get:PropertyName("dueDate")
    val dueDate: Timestamp = Timestamp.now(),

    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,

    @ServerTimestamp
    @get:PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    @get:PropertyName("userId")
    val userId: String = ""
) {
    constructor() : this("", "", "Other", Timestamp.now(), false, null, "")
}