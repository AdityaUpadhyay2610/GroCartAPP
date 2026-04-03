package com.grocart.first.data

import kotlinx.serialization.Serializable

/**
 * Payment method options available in the app.
 */
@Serializable
enum class PaymentMethod(val label: String, val subtitle: String) {
    CARD("Card Payment", "Visa, Mastercard, RuPay"),
    COD("Cash on Delivery", "Pay when you receive"),
    UPI("UPI Payment", "GPay, PhonePe, Paytm"),
    WALLET("Wallet", "Paytm, Amazon Pay")
}

/**
 * Data model for a coupon offer.
 */
@Serializable
data class CouponOffer(
    val code: String,
    val description: String,
    val discountPercent: Int,
    val minOrder: Int = 0
)
