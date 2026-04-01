package com.grocart.first.data

import kotlinx.serialization.Serializable
@Serializable
data class FirestoreResponse(val documents: List<FirestoreDocument> = emptyList())


@Serializable
data class FirestoreDocument(val name: String, val fields: FirestoreFields) {
    fun toInternetItem(): InternetItem {
        return InternetItem(
            id = name.split("/").last().toLongOrNull() ?: 0L,
            itemName = fields.itemName?.stringValue ?: "",
            itemCategory = fields.itemCategory?.stringValue ?: "",
            itemQuantity = fields.itemQuantity?.stringValue ?: "",
            itemPrice = fields.itemPrice?.integerValue?.toIntOrNull() ?: 0,
            imageUrl = fields.imageUrl?.stringValue ?: ""
        )
    }
}

@Serializable
data class FirestoreFields(
    val itemName: FirestoreStringValue? = null,
    val itemCategory: FirestoreStringValue? = null,
    val itemQuantity: FirestoreStringValue? = null,
    val itemPrice: FirestoreIntegerValue? = null,
    val imageUrl: FirestoreStringValue? = null
)

@Serializable
data class FirestoreStringValue(val stringValue: String)

@Serializable
data class FirestoreIntegerValue(val integerValue: String)
@Serializable
data class InternetItem(
    val id: Long = 0L,
    val itemName: String = "",
    val itemCategory: String = "",
    val itemQuantity: String = "",
    val itemPrice: Int = 0,
    val imageUrl: String = ""
)

@Serializable
data class CartItemResponse(
    val id: Long = 0L,
    val itemName: String = "",
    val itemPrice: Int = 0,
    val imageUrl: String = "",
    val quantity: Int = 1
) {
    fun toInternetItem(): InternetItem {
        return InternetItem(
            id = id,
            itemName = itemName,
            itemPrice = itemPrice,
            imageUrl = imageUrl
        )
    }
}
