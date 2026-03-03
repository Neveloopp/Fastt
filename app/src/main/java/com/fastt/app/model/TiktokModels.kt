package com.fastt.app.model

data class ApiResponse(
    val success: Boolean,
    val data: TiktokData?
)

data class TiktokData(
    val url: String? = null,
    val id: String? = null,
    val author: String? = null,
    val name: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    val verified: Boolean? = null,
    val description: String? = null,
    val date: String? = null,
    val resolution: String? = null,
    val quality: String? = null,
    val likes_formatted: String? = null,
    val comments_formatted: String? = null,
    val shares_formatted: String? = null,
    val views_formatted: String? = null,
    val duration: Int? = null,
    val music: String? = null,
    val artist: String? = null,
    val hashtags: String? = null,
    val videoUrl: String? = null,
    val thumbnail: String? = null
)
