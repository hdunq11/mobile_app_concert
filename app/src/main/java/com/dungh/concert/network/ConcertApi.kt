package com.dungh.concert.network

import com.dungh.concert.model.*
import retrofit2.Response
import retrofit2.http.*

interface ConcertApi {
    // === Authentication Endpoints ===
    @POST("api/users/auth/login/")
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @POST("api/users/auth/register/")
    suspend fun register(@Body body: Map<String, String>): Response<User>

    @GET("api/users/me/")
    suspend fun getCurrentUser(): Response<User>

    @PUT("api/users/me/")
    suspend fun updateProfile(@Body body: Map<String, String>): Response<User>

    @POST("api/token/refresh/")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<Map<String, String>>

    // === Concert Endpoints ===
    @GET("api/concerts/concerts/")
    suspend fun getConcerts(
        @Query("search") search: String? = null,
        @Query("genre") genre: String? = null,
        @Query("city") city: String? = null,
        @Query("date") date: String? = null
    ): Response<PaginatedResponse<Concert>>

    @GET("api/concerts/concerts/{id}/")
    suspend fun getConcertDetail(@Path("id") id: String): Response<Concert>

    @GET("api/concerts/concerts/{id}/seatmap/")
    suspend fun getSeatMap(@Path("id") id: String): Response<SeatMapResponse>

    // === Recommendations & Behaviors ===
    @GET("api/behaviors/recommend/")
    suspend fun getRecommendations(
        @Query("concert_id") concertId: String? = null
    ): Response<RecommendationResponse>

    @POST("api/behaviors/behaviors/")
    suspend fun logBehavior(@Body data: Map<String, String>): Response<Map<String, Any>>

    // === Favorites ===
    @GET("api/users/me/favorites/")
    suspend fun getFavorites(): Response<List<Concert>>

    @POST("api/users/me/favorites/")
    suspend fun addFavorite(@Body data: Map<String, String>): Response<Map<String, Any>>

    @DELETE("api/users/me/favorites/{concert_id}/")
    suspend fun removeFavorite(@Path("concert_id") concertId: String): Response<Void>

    // === Booking & Order Flow ===
    @POST("api/seats/booking/reserve/")
    suspend fun reserveSeats(@Body body: Map<String, Any>): Response<com.dungh.concert.model.ReserveResponse>

    @POST("api/orders/vouchers/validate/")
    suspend fun validateVoucher(@Body body: Map<String, Any>): Response<com.dungh.concert.model.VoucherValidateResponse>

    @POST("api/orders/orders/")
    suspend fun createOrder(@Body order: Map<String, Any>): Response<Order>

    @POST("api/orders/orders/{id}/pay/")
    suspend fun payOrder(@Path("id") id: String): Response<Map<String, Any>>

    @POST("api/orders/orders/{id}/cancel/")
    suspend fun cancelOrder(@Path("id") id: String): Response<Map<String, Any>>

    @GET("api/users/me/orders/")
    suspend fun getMyOrders(): Response<List<Order>>
}
