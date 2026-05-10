package com.example.cityapp.presentation.map

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.model.LiveTripMarker
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripMapUiState(
    val markers: List<LiveTripMarker> = emptyList(),
    val myLat: Double? = null,
    val myLng: Double? = null,
    /** Показувати маркер і центрувати мапу лише після явного запиту й перевірки зони Києва. */
    val showDeviceGpsOnMap: Boolean = false,
    val gpsHint: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class TripMapViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ServiceLocator.tripMapRepository
    private val _uiState = MutableStateFlow(TripMapUiState())
    val uiState: StateFlow<TripMapUiState> = _uiState.asStateFlow()

    init {
        refreshMarkers()
    }

    fun refreshMarkers() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = repo.getLiveTripMarkers()
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(markers = result.getOrNull().orEmpty(), loading = false, error = null)
                } else {
                    it.copy(loading = false, error = "Не вдалося завантажити позиції рейсів")
                }
            }
        }
    }

    fun dismissGpsHint() {
        _uiState.update { it.copy(gpsHint = null) }
    }

    /** Запитує поточне місце; поза зоною Києва координати емулятора не використовуємо для мапи. */
    fun requestDeviceGpsForRouteArea() {
        refreshMyLocationKyivOnly()
    }

    @SuppressLint("MissingPermission")
    private fun refreshMyLocationKyivOnly() {
        val client = LocationServices.getFusedLocationProviderClient(getApplication())
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                _uiState.update {
                    it.copy(showDeviceGpsOnMap = false, gpsHint = "Не вдалося отримати координати GPS.")
                }
                return@addOnSuccessListener
            }
            if (!isRoughKyivMetropolitan(loc.latitude, loc.longitude)) {
                _uiState.update {
                    it.copy(
                        showDeviceGpsOnMap = false,
                        gpsHint = "Отримані координати не схожі на Київ (типово для емулятора). На пристрої з увімкненою геолокацією має відобразитись коректно."
                    )
                }
                return@addOnSuccessListener
            }
            _uiState.update {
                it.copy(
                    myLat = loc.latitude,
                    myLng = loc.longitude,
                    showDeviceGpsOnMap = true,
                    gpsHint = null
                )
            }
        }
    }

    private fun isRoughKyivMetropolitan(lat: Double, lng: Double): Boolean =
        lat in 50.22..50.72 && lng in 30.28..30.95
}
