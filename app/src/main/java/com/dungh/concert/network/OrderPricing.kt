package com.dungh.concert.network

/**
 * Mirrors backend order pricing constants (see be/app/orders/pricing.py).
 */
object OrderPricing {
    const val BOOKING_FEE = 20_000.0
    const val DELIVERY_PAPER_FEE = 30_000.0
    const val INSURANCE_PER_SEAT = 50_000.0

    fun previewTotal(
        seatSubtotal: Double,
        seatCount: Int,
        deliveryMethod: String,
        hasInsurance: Boolean,
        discountAmount: Double
    ): Double {
        val delivery = if (deliveryMethod == "paper") DELIVERY_PAPER_FEE else 0.0
        val insurance = if (hasInsurance) INSURANCE_PER_SEAT * seatCount else 0.0
        val total = seatSubtotal + BOOKING_FEE + delivery + insurance - discountAmount
        return if (total < 0) 0.0 else total
    }
}
