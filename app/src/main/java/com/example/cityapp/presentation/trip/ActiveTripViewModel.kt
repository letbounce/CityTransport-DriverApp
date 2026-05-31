package com.example.cityapp.presentation.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.usecase.EndTripUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveTripUiState(
    val isCompleting: Boolean = false,
    val completed: Boolean = false,
    val error: String? = null
)

class ActiveTripViewModel : ViewModel() {
    private val endTripUseCase = EndTripUseCase(ServiceLocator.waybillRepository)
    private val _uiState = MutableStateFlow(ActiveTripUiState())
    val uiState: StateFlow<ActiveTripUiState> = _uiState.asStateFlow()

    fun completeTrip(waybillId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true)
            val result = endTripUseCase(waybillId)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isCompleting = false, completed = true)
            } else {
                _uiState.value.copy(isCompleting = false, error = "Не вдалося завершити рейс")
            }
        }
    }
}
