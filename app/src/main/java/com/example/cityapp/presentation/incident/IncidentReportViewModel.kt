package com.example.cityapp.presentation.incident

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.usecase.ReportIncidentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IncidentReportUiState(
    val incidentType: IncidentApiType = IncidentApiType.BREAKDOWN,
    val description: String = IncidentApiType.BREAKDOWN.templateUa(),
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null
)

class IncidentReportViewModel : ViewModel() {
    private val reportIncidentUseCase = ReportIncidentUseCase(ServiceLocator.incidentRepository)
    private val _uiState = MutableStateFlow(IncidentReportUiState())
    val uiState: StateFlow<IncidentReportUiState> = _uiState.asStateFlow()

    fun onIncidentTypeSelected(type: IncidentApiType) {
        _uiState.update {
            it.copy(
                incidentType = type,
                description = type.templateUa()
            )
        }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun applyTemplateForCurrentType() {
        _uiState.update { it.copy(description = it.incidentType.templateUa()) }
    }

    fun submit(waybillId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val s = _uiState.value
            val result = reportIncidentUseCase(
                waybillId = waybillId,
                type = s.incidentType.apiKey,
                description = s.description.trim(),
                lat = 50.4501,
                lng = 30.5234
            )
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isSubmitting = false, submitted = true)
                } else {
                    it.copy(
                        isSubmitting = false,
                        error = "Не вдалося відправити звіт. Перевірте тип і зв’язок із сервером."
                    )
                }
            }
        }
    }
}
