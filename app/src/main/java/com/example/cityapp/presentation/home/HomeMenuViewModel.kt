package com.example.cityapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeMenuUiState(
    val driverDisplayName: String? = null
)

class HomeMenuViewModel : ViewModel() {
    private val authRepository = ServiceLocator.authRepository
    private val logoutUseCase = LogoutUseCase(authRepository)

    private val _uiState = MutableStateFlow(HomeMenuUiState())
    val uiState: StateFlow<HomeMenuUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(driverDisplayName = authRepository.getDriverDisplayName())
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onDone()
        }
    }
}
