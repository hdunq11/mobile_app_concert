package com.dungh.concert.ui.concert_detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dungh.concert.model.Concert
import com.dungh.concert.network.RetrofitClient
import kotlinx.coroutines.launch

class ConcertDetailViewModel : ViewModel() {

    private val _concert = MutableLiveData<Concert?>(null)
    val concert: LiveData<Concert?> = _concert

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isFavorite = MutableLiveData(false)
    val isFavorite: LiveData<Boolean> = _isFavorite

    fun fetchConcertDetail(concertId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.concertApi.getConcertDetail(concertId)
                if (response.isSuccessful) {
                    _concert.value = response.body()
                    logViewBehavior(concertId)
                    loadFavoriteState(concertId)
                } else {
                    _errorMessage.value = "Không thể tải chi tiết sự kiện. Lỗi: ${response.code()}"
                }
            } catch (exception: Exception) {
                _errorMessage.value = "Lỗi kết nối: ${exception.localizedMessage ?: "Không xác định"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun logViewBehavior(concertId: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.concertApi.logBehavior(
                    mapOf("concert_id" to concertId, "action" to "view")
                )
            } catch (_: Exception) {
                // Optional analytics
            }
        }
    }

    private fun loadFavoriteState(concertId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.concertApi.getFavorites()
                if (response.isSuccessful) {
                    val ids = response.body()?.mapNotNull { it.id } ?: emptyList()
                    _isFavorite.value = ids.contains(concertId)
                }
            } catch (_: Exception) {
                _isFavorite.value = false
            }
        }
    }

    fun toggleFavorite(concertId: String) {
        viewModelScope.launch {
            try {
                if (_isFavorite.value == true) {
                    val response = RetrofitClient.concertApi.removeFavorite(concertId)
                    if (response.isSuccessful || response.code() == 204) {
                        _isFavorite.value = false
                    } else {
                        _errorMessage.value = "Không thể bỏ yêu thích"
                    }
                } else {
                    val response = RetrofitClient.concertApi.addFavorite(
                        mapOf("concert_id" to concertId)
                    )
                    if (response.isSuccessful) {
                        _isFavorite.value = true
                        RetrofitClient.concertApi.logBehavior(
                            mapOf("concert_id" to concertId, "action" to "favorite")
                        )
                    } else {
                        _errorMessage.value = "Lỗi khi thêm vào yêu thích"
                    }
                }
            } catch (_: Exception) {
                _errorMessage.value = "Lỗi khi cập nhật yêu thích"
            }
        }
    }
}
