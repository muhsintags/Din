package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MiraclePost(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "category") val category: String = "Mucizeler",
    @Json(name = "author") val author: String = "Mucizeler Kurulu",
    @Json(name = "date") val date: String = "",
    @Json(name = "content") val content: String = "",
    @Json(name = "imageUrl") val imageUrl: String = "",
    @Json(name = "reference") val reference: String = "",
    @Json(name = "hashtags") val hashtags: List<String> = emptyList(),
    @Json(name = "isBookmarked") val isBookmarked: Boolean = false
)

