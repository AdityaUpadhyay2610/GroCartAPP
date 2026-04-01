package com.grocart.first.data

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String
)
