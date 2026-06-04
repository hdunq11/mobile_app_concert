package com.dungh.concert.ui.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dungh.concert.model.Concert
import com.dungh.concert.network.RetrofitClient
import kotlinx.coroutines.launch

class FavoritesViewModel : ViewModel() {

    private val _favorites = MutableLiveData<List<Concert>>(emptyList())
    val favorites: LiveData<List<Concert>> = _favorites

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun fetchFavorites() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.concertApi.getFavorites()
                if (response.isSuccessful) {
                    _favorites.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "Không thể tải danh sách yêu thích. Lỗi: ${response.code()}"
                }
            } catch (exception: Exception) {
                _errorMessage.value = "Lỗi kết nối: ${exception.localizedMessage ?: "Không xác định"}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
