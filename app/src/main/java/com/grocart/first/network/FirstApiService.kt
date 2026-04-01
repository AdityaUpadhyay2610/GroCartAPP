package com.grocart.first.network

import com.grocart.first.data.InternetItem
import com.grocart.first.data.Order
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*

@Serializable
data class UserResponse(val id: String, val username: String, val email: String)

private const val BASE_URL = "https://groceryapp-7ad95-default-rtdb.firebaseio.com/"
private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

private val retrofit = Retrofit.Builder()
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .baseUrl(BASE_URL)
    .build()

interface FirstApiService {
    /**
     * Fetches the list of all grocery items.
     * Use JsonElement to handle both Map and Array formats from Firebase.
     */
    @GET("items.json")
    suspend fun getItems(@Query("auth") token: String? = null): JsonElement?

    /**
     * Adds an item to the specified user's cart.
     */
    @PUT("carts/{userId}/{itemId}.json")
    suspend fun addCartItem(
        @Path("userId") userId: String,
        @Path("itemId") itemId: Long,
        @Body item: com.grocart.first.data.CartItemResponse,
        @Query("auth") token: String? = null
    ): Response<okhttp3.ResponseBody>

    /**
     * Deletes an item from the cart.
     */
    @DELETE("carts/{userId}/{itemId}.json")
    suspend fun decreaseCartItem(
        @Path("userId") userId: String,
        @Path("itemId") itemId: Long,
        @Query("auth") token: String? = null
    ): Response<okhttp3.ResponseBody>

    /**
     * Retrieves the cart for a user.
     */
    @GET("carts/{userId}.json")
    suspend fun getUserCart(
        @Path("userId") userId: String,
        @Query("auth") token: String? = null
    ): Response<JsonElement?>

    /**
     * Clears the cart for a user.
     */
    @DELETE("carts/{userId}.json")
    suspend fun clearUserCart(
        @Path("userId") userId: String,
        @Query("auth") token: String? = null
    ): Response<okhttp3.ResponseBody>

    /**
     * Places a new order for a user.
     */
    @POST("orders/{userId}.json")
    suspend fun placeOrder(
        @Path("userId") userId: String, 
        @Body order: Order,
        @Query("auth") token: String? = null
    ): Response<okhttp3.ResponseBody>

    /**
     * Fetches past orders for a user.
     */
    @GET("orders/{userId}.json")
    suspend fun getOrders(
        @Path("userId") userId: String,
        @Query("auth") token: String? = null
    ): JsonElement?
}

object FirstApi {
    val retrofitService: FirstApiService by lazy { retrofit.create(FirstApiService::class.java) }
}