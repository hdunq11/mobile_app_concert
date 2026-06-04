package com.dungh.concert.ui.checkout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dungh.concert.model.Order
import com.dungh.concert.model.VoucherValidateResponse
import com.dungh.concert.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CheckoutViewModel : ViewModel() {

    private val _orderResult = MutableLiveData<Order?>(null)
    val orderResult: LiveData<Order?> = _orderResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _simulatedProgressText = MutableLiveData<String?>(null)
    val simulatedProgressText: LiveData<String?> = _simulatedProgressText

    private val _voucherResult = MutableLiveData<VoucherValidateResponse?>(null)
    val voucherResult: LiveData<VoucherValidateResponse?> = _voucherResult

    private val _voucherError = MutableLiveData<String?>(null)
    val voucherError: LiveData<String?> = _voucherError

    fun validateVoucher(code: String, seatSubtotal: Double) {
        _voucherError.value = null
        viewModelScope.launch {
            try {
                val response = RetrofitClient.concertApi.validateVoucher(
                    mapOf(
                        "code" to code,
                        "seat_subtotal" to seatSubtotal
                    )
                )
                if (response.isSuccessful && response.body()?.valid == true) {
                    _voucherResult.value = response.body()
                } else {
                    _voucherResult.value = null
                    _voucherError.value = response.body()?.error
                        ?: "Mã giảm giá không hợp lệ!"
                }
            } catch (e: Exception) {
                _voucherResult.value = null
                _voucherError.value = "Lỗi kết nối: ${e.localizedMessage}"
            }
        }
    }

    fun clearVoucher() {
        _voucherResult.value = null
        _voucherError.value = null
    }

    fun executeBookingFlow(
        concertId: String,
        seatIds: Array<String>,
        deliveryMethod: String,
        hasInsurance: Boolean,
        voucherCode: String?,
        paymentMethod: String
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        _simulatedProgressText.value = "Đang khởi tạo đơn đặt vé..."

        viewModelScope.launch {
            try {
                delay(400)
                val orderData = mutableMapOf<String, Any>(
                    "concert_id" to concertId,
                    "seat_ids" to seatIds.toList(),
                    "delivery_method" to deliveryMethod,
                    "has_insurance" to hasInsurance,
                    "payment_method" to paymentMethod,
                )
                if (!voucherCode.isNullOrBlank()) {
                    orderData["voucher_code"] = voucherCode
                }

                val orderResponse = RetrofitClient.concertApi.createOrder(orderData)
                if (!orderResponse.isSuccessful || orderResponse.body() == null) {
                    _errorMessage.value = "Tạo đơn đặt vé thất bại. Lỗi: ${orderResponse.code()}"
                    _isLoading.value = false
                    return@launch
                }
                val order = orderResponse.body()!!

                val formattedMethodName = when (paymentMethod) {
                    "momo" -> "Momo"
                    "credit_card" -> "Visa/Mastercard"
                    else -> "VNPAY"
                }
                _simulatedProgressText.value = "Đang kết nối cổng thanh toán $formattedMethodName..."
                delay(1200)

                _simulatedProgressText.value = "Đang xử lý giao dịch an toàn..."
                delay(800)

                val payResponse = RetrofitClient.concertApi.payOrder(order.id ?: "")
                if (payResponse.isSuccessful) {
                    _orderResult.value = order
                } else {
                    _errorMessage.value = "Thanh toán thất bại. Lỗi: ${payResponse.code()}"
                }
            } catch (exception: Exception) {
                _errorMessage.value = "Lỗi kết nối hệ thống: ${exception.localizedMessage ?: "Không xác định"}"
            } finally {
                _isLoading.value = false
                _simulatedProgressText.value = null
            }
        }
    }
}
