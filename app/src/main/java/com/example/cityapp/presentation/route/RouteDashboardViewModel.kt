package com.example.cityapp.presentation.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.model.Route
import com.example.cityapp.domain.usecase.GetActiveRouteUseCase
import com.example.cityapp.domain.usecase.StartTripUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RouteDashboardUiState(
    val isLoading: Boolean = false,
    val routes: List<Route> = emptyList(),
    val activeWaybillId: String? = null,
    val error: String? = null
)

class RouteDashboardViewModel : ViewModel() {
    private val getActiveRouteUseCase = GetActiveRouteUseCase(ServiceLocator.routeRepository)
    private val startTripUseCase = StartTripUseCase(ServiceLocator.waybillRepository)
    private val _uiState = MutableStateFlow(RouteDashboardUiState())
    val uiState: StateFlow<RouteDashboardUiState> = _uiState.asStateFlow()

    fun loadRoutes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = getActiveRouteUseCase()
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, routes = result.getOrDefault(emptyList()))
            } else {
                _uiState.value.copy(isLoading = false, error = "Маршрути недоступні")
            }
        }
    }

    fun startTrip(routeId: String) {
        viewModelScope.launch {
            val result = startTripUseCase(routeId)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(activeWaybillId = result.getOrNull()?.id)
            } else {
                _uiState.value.copy(error = "Не вдалося отримати дорожній лист")
            }
        }
    }
}
