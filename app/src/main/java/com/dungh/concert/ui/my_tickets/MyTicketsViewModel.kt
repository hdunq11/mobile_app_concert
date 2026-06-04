package com.dungh.concert.ui.my_tickets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dungh.concert.model.Order
import com.dungh.concert.network.RetrofitClient
import kotlinx.coroutines.launch

class MyTicketsViewModel : ViewModel() {

    private val _orders = MutableLiveData<List<Order>>(emptyList())
    val orders: LiveData<List<Order>> = _orders

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        fetchMyOrders()
    }

    fun fetchMyOrders() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.concertApi.getMyOrders()
                if (response.isSuccessful) {
                    _orders.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "Không thể tải vé. Lỗi: ${response.code()}"
                }
            } catch (exception: Exception) {
                _errorMessage.value = "Lỗi kết nối: ${exception.localizedMessage ?: "Không xác định"}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
