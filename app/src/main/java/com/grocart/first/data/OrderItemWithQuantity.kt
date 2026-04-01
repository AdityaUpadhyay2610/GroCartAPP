package com.grocart.first.data
// ✅ DATA CLASSES FOR CART AND ORDERS

data class OrderItemWithQuantity(
    val internetItem: InternetItem,
    val quantity: Int
)
