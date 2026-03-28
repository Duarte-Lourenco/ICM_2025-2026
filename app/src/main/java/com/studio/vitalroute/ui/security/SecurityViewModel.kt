package com.studio.vitalroute.ui.security

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SecurityUiState(
    val contacts: List<TrustedContact> = listOf(
        TrustedContact("Ana (Esposa)", sosEnabled = true, zonesEnabled = true)
    ),
    val fallSensitivity: Float = 0.5f
)

data class TrustedContact(
    val name: String,
    val sosEnabled: Boolean,
    val zonesEnabled: Boolean
)

class SecurityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    fun updateFallSensitivity(value: Float) {
        _uiState.update { it.copy(fallSensitivity = value) }
    }
}