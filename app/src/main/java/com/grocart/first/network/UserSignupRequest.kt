package com.grocart.first.network

import kotlinx.serialization.Serializable
@Serializable
data class UserSignupRequest(
    val username: String,
    val email: String,
    val password: String
)