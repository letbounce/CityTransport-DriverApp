package com.example.cityapp.presentation.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.model.Route
import com.example.cityapp.domain.model.Vehicle
import com.example.cityapp.domain.model.Waybill
import com.example.cityapp.domain.usecase.GetActiveRouteUseCase
import com.example.cityapp.domain.usecase.ListVehiclesUseCase
import com.example.cityapp.domain.usecase.ListWaybillsUseCase
import com.example.cityapp.domain.usecase.StartTripUseCase
import com.example.cityapp.domain.usecase.UpdateWaybillUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class RouteDashboardUiState(
    val driverDisplayName: String? = null,
    val isLoading: Boolean = false,
    val routes: List<Route> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val selectedRouteId: String = "",
    val selectedVehicleId: String = "",
    val waybills: List<Waybill> = emptyList(),
    val activeWaybill: Waybill? = null,
    val newTripNotes: String = "",
    val pendingNavigateWaybillId: String? = null,
    val saveSuccessMessage: String? = null,
    val vehiclesLoaded: Boolean = false,
    val error: String? = null
)

class RouteDashboardViewModel : ViewModel() {
    private val getActiveRouteUseCase = GetActiveRouteUseCase(ServiceLocator.routeRepository)
    private val listVehiclesUseCase = ListVehiclesUseCase(ServiceLocator.vehicleRepository)
    private val startTripUseCase = StartTripUseCase(ServiceLocator.waybillRepository)
    private val listWaybillsUseCase = ListWaybillsUseCase(ServiceLocator.waybillRepository)
    private val updateWaybillUseCase = UpdateWaybillUseCase(ServiceLocator.waybillRepository)
    private val authRepository = ServiceLocator.authRepository
    private val waybillRepository = ServiceLocator.waybillRepository

    private val _uiState = MutableStateFlow(RouteDashboardUiState())
    val uiState: StateFlow<RouteDashboardUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val pendingSnapshot = _uiState.value.pendingNavigateWaybillId
            _uiState.update { it.copy(isLoading = true, error = null, saveSuccessMessage = null) }

            val routesResult = getActiveRouteUseCase()
            val vehiclesResult = listVehiclesUseCase()
            val listResult = listWaybillsUseCase()
            val activeResult = waybillRepository.getActiveWaybill()

            val routesList = routesResult.getOrDefault(emptyList())
            val vehiclesList = vehiclesResult.getOrDefault(emptyList())
            val vehiclesOk = vehiclesResult.isSuccess

            val err = buildList {
                if (routesResult.isFailure) add("Маршрути недоступні")
                if (listResult.isFailure) add("Не вдалося завантажити список дорожніх листів")
                if (!vehiclesOk) add("Не вдалося завантажити парк транспорту (перевірте сервер)")
            }.joinToString("\n").ifBlank { null }

            val driverName = authRepository.getDriverDisplayName()

            _uiState.update { prev ->
                val routeId = prev.selectedRouteId.takeIf { id -> routesList.any { it.id == id } }
                    ?: routesList.firstOrNull()?.id.orEmpty()
                val defaultVehicle = vehiclesList.firstOrNull()?.vehicleId.orEmpty()
                val vehicleId = prev.selectedVehicleId.takeIf { v -> vehiclesList.any { it.vehicleId == v } }
                    ?: defaultVehicle.ifBlank { "BUS-007" }

                prev.copy(
                    isLoading = false,
                    driverDisplayName = driverName,
                    routes = routesList,
                    vehicles = vehiclesList,
                    selectedRouteId = routeId,
                    selectedVehicleId = vehicleId,
                    waybills = listResult.getOrDefault(emptyList()),
                    activeWaybill = activeResult.getOrNull(),
                    error = err,
                    pendingNavigateWaybillId = pendingSnapshot,
                    vehiclesLoaded = vehiclesOk
                )
            }
        }
    }

    fun onRouteSelected(routeId: String) {
        _uiState.update { it.copy(selectedRouteId = routeId) }
    }

    fun onVehicleSelected(vehicleId: String) {
        _uiState.update { it.copy(selectedVehicleId = vehicleId) }
    }

    fun onNewTripNotesChange(value: String) {
        _uiState.update { it.copy(newTripNotes = value) }
    }

    fun createWaybill() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val routeId = _uiState.value.selectedRouteId
            if (routeId.isBlank()) {
                _uiState.update { it.copy(error = "Оберіть маршрут") }
                return@launch
            }
            val vehicleId = _uiState.value.selectedVehicleId.ifBlank { "BUS-007" }
            val notes = _uiState.value.newTripNotes.trim().ifBlank { null }
            val result = startTripUseCase(routeId, vehicleId, notes)
            val code = (result.exceptionOrNull() as? HttpException)?.code()
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        pendingNavigateWaybillId = result.getOrNull()?.id,
                        newTripNotes = ""
                    )
                } else {
                    it.copy(
                        error = when (code) {
                            409 -> "Вже є активний дорожній лист. Завершіть рейс або відредагуйте поточний лист."
                            else -> "Не вдалося створити дорожній лист"
                        }
                    )
                }
            }
            refresh()
        }
    }

    fun consumePendingNavigation() {
        _uiState.update { it.copy(pendingNavigateWaybillId = null) }
    }

    fun updateWaybill(waybillId: String, vehicleId: String, notes: String) {
        viewModelScope.launch {
            val result = updateWaybillUseCase(
                waybillId,
                vehicleId.ifBlank { null },
                notes.trim().ifBlank { null }
            )
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(saveSuccessMessage = "Зміни збережено")
                } else {
                    it.copy(error = "Не вдалося оновити дорожній лист")
                }
            }
            refresh()
        }
    }

    fun consumeSaveMessage() {
        _uiState.update { it.copy(saveSuccessMessage = null) }
    }

    fun continueTrip(waybillId: String) {
        _uiState.update { it.copy(pendingNavigateWaybillId = waybillId) }
    }
}
