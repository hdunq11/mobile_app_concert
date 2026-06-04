package com.dungh.concert.ui.seat_selection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dungh.concert.model.SeatMapZone
import com.dungh.concert.model.Concert
import com.dungh.concert.network.RetrofitClient
import kotlinx.coroutines.launch

data class SelectedSeatDetail(
    val seatId: String,
    val zoneId: String,
    val zoneName: String,
    val row: String,
    val number: Int,
    val price: Double
)

class SeatSelectionViewModel : ViewModel() {

    private val _seatMap = MutableLiveData<List<SeatMapZone>>(emptyList())
    val seatMap: LiveData<List<SeatMapZone>> = _seatMap

    private val _concert = MutableLiveData<Concert?>(null)
    val concert: LiveData<Concert?> = _concert

    private val _selectedSeats = MutableLiveData<List<String>>(emptyList())
    val selectedSeats: LiveData<List<String>> = _selectedSeats

    private val _selectedSeatsDetails = MutableLiveData<List<SelectedSeatDetail>>(emptyList())
    val selectedSeatsDetails: LiveData<List<SelectedSeatDetail>> = _selectedSeatsDetails

    private val _totalPrice = MutableLiveData(0.0)
    val totalPrice: LiveData<Double> = _totalPrice

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // ProVIP: Selected Zone for Zoom Detail mode (null means Arena Overview is visible)
    private val _selectedZone = MutableLiveData<SeatMapZone?>(null)
    val selectedZone: LiveData<SeatMapZone?> = _selectedZone

    // ProVIP: Price Filters
    private val _minPriceFilter = MutableLiveData<Double>(0.0)
    val minPriceFilter: LiveData<Double> = _minPriceFilter

    private val _maxPriceFilter = MutableLiveData<Double>(3000000.0)
    val maxPriceFilter: LiveData<Double> = _maxPriceFilter

    fun fetchSeatMap(concertId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.concertApi.getSeatMap(concertId)
                if (response.isSuccessful) {
                    val zones = response.body()?.zones ?: emptyList()
                    _seatMap.value = zones
                    
                    // Setup initial slider range based on actual prices
                    val prices = zones.mapNotNull { it.price }
                    if (prices.isNotEmpty()) {
                        _minPriceFilter.value = prices.minOrNull() ?: 0.0
                        _maxPriceFilter.value = prices.maxOrNull() ?: 3000000.0
                    }
                } else {
                    _errorMessage.value = "Không thể tải sơ đồ ghế. Lỗi: ${response.code()}"
                }
            } catch (exception: Exception) {
                _errorMessage.value = "Lỗi kết nối: ${exception.localizedMessage ?: "Không xác định"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchConcertDetail(concertId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.concertApi.getConcertDetail(concertId)
                if (response.isSuccessful) {
                    _concert.value = response.body()
                }
            } catch (e: Exception) {
                // Fail silently
            }
        }
    }

    fun selectZone(zone: SeatMapZone?) {
        _selectedZone.value = zone
    }

    fun setPriceFilter(min: Double, max: Double) {
        _minPriceFilter.value = min
        _maxPriceFilter.value = max
    }

    fun toggleSeat(seatId: String, zoneId: String, zoneName: String, row: String, number: Int, price: Double) {
        val currentSeats = _selectedSeats.value?.toMutableList() ?: mutableListOf()
        val currentDetails = _selectedSeatsDetails.value?.toMutableList() ?: mutableListOf()

        if (currentSeats.contains(seatId)) {
            currentSeats.remove(seatId)
            currentDetails.removeAll { it.seatId == seatId }
            _totalPrice.value = (_totalPrice.value ?: 0.0) - price
        } else {
            currentSeats.add(seatId)
            currentDetails.add(SelectedSeatDetail(seatId, zoneId, zoneName, row, number, price))
            _totalPrice.value = (_totalPrice.value ?: 0.0) + price
        }

        _selectedSeats.value = currentSeats
        _selectedSeatsDetails.value = currentDetails
    }

    fun removeSeatById(seatId: String) {
        val detail = _selectedSeatsDetails.value?.find { it.seatId == seatId } ?: return
        toggleSeat(
            seatId = detail.seatId,
            zoneId = detail.zoneId,
            zoneName = detail.zoneName,
            row = detail.row,
            number = detail.number,
            price = detail.price
        )
    }

    private val _reservedUntilIso = MutableLiveData<String?>(null)
    val reservedUntilIso: LiveData<String?> = _reservedUntilIso

    fun reserveSeatsAndProceed(
        concertId: String,
        onSuccess: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        val seatIds = _selectedSeats.value ?: emptyList()
        if (seatIds.isEmpty()) {
            onError("Vui lòng chọn ít nhất một ghế")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val reserveData = mapOf(
                    "concert_id" to concertId,
                    "seat_ids" to seatIds
                )
                val response = RetrofitClient.concertApi.reserveSeats(reserveData)
                if (response.isSuccessful) {
                    val iso = response.body()?.reserved_until
                    _reservedUntilIso.value = iso
                    onSuccess(iso)
                } else {
                    onError("Không thể giữ chỗ ghế. Có thể ghế đã bị người khác chọn.")
                }
            } catch (exception: Exception) {
                onError("Lỗi kết nối: ${exception.localizedMessage ?: "Không xác định"}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
