package com.grocart.first.data

import kotlinx.serialization.Serializable
@Serializable
data class Order(
    val id: Int? = null, // Optional for new orders
    val items: List<CartItemResponse> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)