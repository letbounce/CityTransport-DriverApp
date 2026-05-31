package com.example.cityapp.presentation.trip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.domain.model.Stop
import com.example.cityapp.domain.model.Waybill
import com.example.cityapp.domain.usecase.EndTripUseCase
import com.example.cityapp.domain.usecase.GetActiveRouteUseCase
import com.example.cityapp.domain.usecase.GetWaybillUseCase
import com.example.cityapp.domain.usecase.ListIncidentsUseCase
import com.example.cityapp.presentation.map.BusRouteGeoJsonParser
import com.example.cityapp.presentation.map.TripMapCatalog
import com.example.cityapp.presentation.route.RouteTripDirection
import com.example.cityapp.presentation.route.filterStopsForDirection
import com.example.cityapp.presentation.route.parseTripDirectionFromNotes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ActiveTripUiState(
    val loading: Boolean = true,
    val waybill: Waybill? = null,
    val routeName: String? = null,
    val stops: List<Stop> = emptyList(),
    val openIncidents: List<IncidentItem> = emptyList(),
    val driverDisplayName: String? = null,
    val loadError: String? = null,
    val isCompleting: Boolean = false,
    val completed: Boolean = false,
    val error: String? = null
)

class ActiveTripViewModel(application: Application) : AndroidViewModel(application) {
    private val endTripUseCase = EndTripUseCase(ServiceLocator.waybillRepository)
    private val getWaybillUseCase = GetWaybillUseCase(ServiceLocator.waybillRepository)
    private val getRoutesUseCase = GetActiveRouteUseCase(ServiceLocator.routeRepository)
    private val listIncidentsUseCase = ListIncidentsUseCase(ServiceLocator.incidentRepository)

    private val _uiState = MutableStateFlow(ActiveTripUiState())
    val uiState: StateFlow<ActiveTripUiState> = _uiState.asStateFlow()

    fun loadTrip(waybillId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadError = null) }
            val driverName = ServiceLocator.authRepository.getDriverDisplayName()
            val wb = getWaybillUseCase(waybillId).getOrNull()
            if (wb == null) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        loadError = "Не вдалося завантажити дорожній лист"
                    )
                }
                return@launch
            }

            val routes = getRoutesUseCase().getOrDefault(emptyList())
            val route = routes.find { it.id == wb.routeId }
            val tripDirection = parseTripDirectionFromNotes(wb.notes)
            var stops = route?.stops.orEmpty().sortedBy { it.stopNumber }
            if (stops.isEmpty()) {
                val entry = TripMapCatalog.findById(wb.routeNumber.trim())
                val path = entry?.stopsAssetPath
                if (!path.isNullOrBlank()) {
                    stops = BusRouteGeoJsonParser.loadStopsOrderedWithSchedule(
                        getApplication(),
                        path
                    )
                }
            }
            stops = filterStopsForDirection(stops, tripDirection)

            val incidents = listIncidentsUseCase()
                .getOrDefault(emptyList())
                .filter { it.waybillId == waybillId && !it.isInactive }

            _uiState.update {
                it.copy(
                    loading = false,
                    waybill = wb,
                    routeName = route?.routeName?.takeIf { name -> name.isNotBlank() },
                    stops = stops,
                    openIncidents = incidents,
                    driverDisplayName = driverName,
                    loadError = null
                )
            }
        }
    }

    fun completeTrip(waybillId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true, error = null) }
            val result = endTripUseCase(waybillId)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isCompleting = false, completed = true)
                } else {
                    it.copy(isCompleting = false, error = "Не вдалося завершити рейс")
                }
            }
        }
    }
}

internal fun waybillStatusUa(status: String): String = when (status) {
    "assigned" -> "Призначено"
    "in_progress" -> "В дорозі"
    "completed" -> "Завершено"
    "cancelled" -> "Скасовано"
    else -> status
}

internal fun formatTripTimestamp(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return runCatching {
        val instant = Instant.parse(iso)
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("uk"))
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }.getOrElse { iso.take(16).replace('T', ' ') }
}
