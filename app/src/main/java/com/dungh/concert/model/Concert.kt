package com.dungh.concert.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Artist(
    val id: String?,
    val name: String?,
    val genre: String?,
    val image_url: String?
)

@JsonClass(generateAdapter = true)
data class ConcertArtist(
    val artist: Artist?
)

@JsonClass(generateAdapter = true)
data class Venue(
    val id: String?,
    val name: String?,
    val city: String?,
    val address: String?,
    val capacity: Int?
)

@JsonClass(generateAdapter = true)
data class Concert(
    val id: String?,
    val title: String?,
    val description: String?,
    val start_time: String?,
    val end_time: String?,
    val banner_url: String?,
    val venue: Venue?,
    val concert_artists: List<ConcertArtist>?
)

@JsonClass(generateAdapter = true)
data class SeatZone(
    val id: String?,
    val name: String?,
    val price: Double?,
    val color: String?
)

@JsonClass(generateAdapter = true)
data class Seat(
    val id: String?,
    val row_label: String?,
    val seat_number: Int?,
    val pos_x: Float?,
    val pos_y: Float?,
    val zone_id: String?
)

@JsonClass(generateAdapter = true)
data class ConcertSeat(
    val id: String?,
    val seat_id: String?,
    val status: String?,
    val seat: Seat?
)


@JsonClass(generateAdapter = true)
data class Order(
    val id: String?,
    val total_price: Double?,
    val seat_subtotal: Double? = null,
    val booking_fee: Double? = null,
    val delivery_fee: Double? = null,
    val insurance_fee: Double? = null,
    val discount_amount: Double? = null,
    val voucher_code: String? = null,
    val delivery_method: String? = null,
    val payment_method: String? = null,
    val status: String?,
    val created_at: String?,
    val concert_title: String?
)

@JsonClass(generateAdapter = true)
data class VoucherValidateResponse(
    val valid: Boolean?,
    val code: String?,
    val discount_percent: Double?,
    val discount_amount: Double?,
    val description: String?,
    val error: String?
)

@JsonClass(generateAdapter = true)
data class ReserveResponse(
    val message: String?,
    val reserved_until: String?,
    val reserved_count: Int?
)

@JsonClass(generateAdapter = true)
data class OrderPricingPreview(
    val seat_subtotal: Double?,
    val booking_fee: Double?,
    val delivery_fee: Double?,
    val insurance_fee: Double?,
    val discount_amount: Double?,
    val total_price: Double?
)

@JsonClass(generateAdapter = true)
data class OrderItem(
    val id: String?,
    val seat_id: String?,
    val price: Double?
)

@JsonClass(generateAdapter = true)
data class User(
    val id: String?,
    val email: String?,
    val full_name: String?,
    val avatar_url: String?,
    val role: String?
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val access: String?,
    val refresh: String?,
    val user: User?
)

@JsonClass(generateAdapter = true)
data class PaginatedResponse<T>(
    val count: Int?,
    val next: String?,
    val previous: String?,
    val results: List<T>?
)

@JsonClass(generateAdapter = true)
data class SeatMapSeat(
    val seat_id: String?,
    val row: String?,
    val number: Int?,
    var status: String?,
    val pos_x: Float?,
    val pos_y: Float?
)

@JsonClass(generateAdapter = true)
data class SeatMapZone(
    val zone_id: String?,
    val name: String?,
    val price: Double?,
    val color: String?,
    val seats: List<SeatMapSeat>?
)

@JsonClass(generateAdapter = true)
data class SeatMapResponse(
    val zones: List<SeatMapZone>?
)

@JsonClass(generateAdapter = true)
data class RecommendationResponse(
    val recommendedConcerts: List<Concert>?,
    val recommendedZone: String?
)