package com.example.tectest.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Note(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("title")
    val title: String = "",

    @get:PropertyName("content")
    val content: String = "",

    @ServerTimestamp
    @get:PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    @get:PropertyName("lastModified")
    var lastModified: Timestamp? = null,

    @get:PropertyName("userId")
    val userId: String = "",

    @get:PropertyName("folderId")
    val folderId: String = ""
) {
    constructor() : this("", "", "", null, null, "", "")
}