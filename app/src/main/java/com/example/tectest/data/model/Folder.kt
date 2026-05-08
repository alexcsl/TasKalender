package com.example.tectest.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Folder(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("name")
    val name: String = "",

    @ServerTimestamp
    @get:PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    @get:PropertyName("userId")
    val userId: String = ""
) {
    constructor() : this("", "", null, "")
}