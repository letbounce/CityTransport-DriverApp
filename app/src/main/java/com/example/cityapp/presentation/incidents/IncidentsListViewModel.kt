package com.example.cityapp.presentation.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.domain.usecase.ListIncidentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IncidentsListUiState(
    val isLoading: Boolean = false,
    val incidents: List<IncidentItem> = emptyList(),
    val error: String? = null
)

class IncidentsListViewModel : ViewModel() {
    private val listIncidentsUseCase = ListIncidentsUseCase(ServiceLocator.incidentRepository)
    private val _uiState = MutableStateFlow(IncidentsListUiState())
    val uiState: StateFlow<IncidentsListUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = listIncidentsUseCase()
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, incidents = result.getOrDefault(emptyList()))
                } else {
                    it.copy(isLoading = false, error = "Не вдалося завантажити інциденти")
                }
            }
        }
    }
}
