package com.example.cityapp.presentation.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.domain.usecase.ArchiveIncidentUseCase
import com.example.cityapp.domain.usecase.ListArchivedIncidentsUseCase
import com.example.cityapp.domain.usecase.ListIncidentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IncidentsListUiState(
    val isLoading: Boolean = false,
    val incidents: List<IncidentItem> = emptyList(),
    val archivedIncidents: List<IncidentItem> = emptyList(),
    val feedbackMessage: String? = null,
    val error: String? = null
)

class IncidentsListViewModel : ViewModel() {
    private val listIncidentsUseCase = ListIncidentsUseCase(ServiceLocator.incidentRepository)
    private val listArchivedIncidentsUseCase = ListArchivedIncidentsUseCase(ServiceLocator.incidentRepository)
    private val archiveIncidentUseCase = ArchiveIncidentUseCase(ServiceLocator.incidentRepository)

    private val _uiState = MutableStateFlow(IncidentsListUiState())
    val uiState: StateFlow<IncidentsListUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val activeResult = listIncidentsUseCase()
            val archivedResult = listArchivedIncidentsUseCase()

            val err = buildList {
                if (activeResult.isFailure) add("Не вдалося завантажити інциденти")
                if (archivedResult.isFailure) add("Не вдалося завантажити архів інцидентів")
            }.joinToString("\n").ifBlank { null }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    incidents = activeResult.getOrDefault(emptyList()),
                    archivedIncidents = archivedResult.getOrDefault(emptyList()),
                    error = err
                )
            }
        }
    }

    fun archiveIncident(incidentId: String, reasonCode: String, reasonNote: String?) {
        viewModelScope.launch {
            val result = archiveIncidentUseCase(incidentId, reasonCode, reasonNote)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(feedbackMessage = "Інцидент переведено у «Завершено» й архів; причину збережено")
                }
                refresh()
            } else {
                _uiState.update {
                    it.copy(error = "Не вдалося архівувати інцидент")
                }
            }
        }
    }

    fun consumeFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
