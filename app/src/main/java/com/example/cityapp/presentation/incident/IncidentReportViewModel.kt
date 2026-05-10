package com.example.cityapp.presentation.incident

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.usecase.ReportIncidentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IncidentReportUiState(
    val type: String = "breakdown",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null
)

class IncidentReportViewModel : ViewModel() {
    private val reportIncidentUseCase = ReportIncidentUseCase(ServiceLocator.incidentRepository)
    private val _uiState = MutableStateFlow(IncidentReportUiState())
    val uiState: StateFlow<IncidentReportUiState> = _uiState.asStateFlow()

    fun onTypeChange(value: String) {
        _uiState.value = _uiState.value.copy(type = value)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun submit(waybillId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            val result = reportIncidentUseCase(
                waybillId = waybillId,
                type = _uiState.value.type,
                description = _uiState.value.description,
                lat = 50.4501,
                lng = 30.5234
            )
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isSubmitting = false, submitted = true)
            } else {
                _uiState.value.copy(isSubmitting = false, error = "Не вдалося відправити звіт")
            }
        }
    }
}
