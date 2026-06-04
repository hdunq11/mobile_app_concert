package com.dungh.concert.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dungh.concert.model.Concert
import com.dungh.concert.network.ApiMappers
import com.dungh.concert.network.RetrofitClient
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _concerts = MutableLiveData<List<Concert>>(emptyList())
    val concerts: LiveData<List<Concert>> = _concerts

    private val _recommendedConcerts = MutableLiveData<List<Concert>>(emptyList())
    val recommendedConcerts: LiveData<List<Concert>> = _recommendedConcerts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private var currentSearch: String? = null
    private var currentGenre: String? = null
    private var currentCity: String? = null
    private var currentDate: String? = null

    init {
        fetchConcerts()
        fetchRecommendations()
    }

    fun setFilters(search: String?, genre: String?, city: String?) {
        currentSearch = if (search.isNullOrBlank()) null else search
        currentGenre = ApiMappers.mapGenre(genre)
        currentCity = ApiMappers.mapCity(city)
        fetchConcerts()
    }

    fun setDateFilter(date: String?) {
        currentDate = date
        fetchConcerts()
    }

    fun fetchConcerts() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.concertApi.getConcerts(
                    search = currentSearch,
                    genre = currentGenre,
                    city = currentCity,
                    date = currentDate
                )
                if (response.isSuccessful) {
                    _concerts.value = response.body()?.results ?: emptyList()
                } else {
                    _errorMessage.value = "Không thể tải sự kiện. Lỗi: ${response.code()}"
                }
            } catch (exception: Exception) {
                _errorMessage.value = "Lỗi kết nối: ${exception.localizedMessage ?: "Không xác định"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchRecommendations() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.concertApi.getRecommendations()
                if (response.isSuccessful) {
                    _recommendedConcerts.value = response.body()?.recommendedConcerts ?: emptyList()
                }
            } catch (e: Exception) {
                // Fail silently for recommendations
            }
        }
    }
}
