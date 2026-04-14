package com.grocart.first.data

import kotlinx.serialization.Serializable
@Serializable
data class Order(
    val id: Int? = null,
    val items: List<CartItemResponse> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val totalPaid: Int = 0,        // Exact amount paid (after coupon, handling, delivery)
    val couponDiscount: Int = 0    // Coupon savings applied (0 = no coupon used)
)