package com.studio.vitalroute.ui.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SecurityUiState(
    val contacts: List<TrustedContact> = listOf(
        TrustedContact("Ana", "Esposa", "+351 912 345 678", sosEnabled = true,  zonesEnabled = true),
        TrustedContact("João", "Pai",   "+351 963 456 789", sosEnabled = true,  zonesEnabled = false)
    ),
    val fallSensitivity: Float = 0.5f,
    val sosCountdownSecs: Int = 15,
    val safeZones: List<SafeZone> = listOf(
        SafeZone("Casa",      "Aveiro, Portugal",      Icons.Default.Home),
        SafeZone("Trabalho",  "Campus UA, Aveiro",     Icons.Default.Work)
    ),
    val immobilityAlertEnabled: Boolean = true,
    val immobilityMinutes: Int = 5,
    val arrivalAlertEnabled: Boolean = true,
    val routeDeviationEnabled: Boolean = false
)

data class TrustedContact(
    val name: String,
    val relation: String,
    val phone: String,
    val sosEnabled: Boolean,
    val zonesEnabled: Boolean
)

data class SafeZone(
    val name: String,
    val address: String,
    val icon: ImageVector
)

class SecurityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    fun updateFallSensitivity(value: Float) {
        _uiState.update { it.copy(fallSensitivity = value) }
    }

    fun toggleImmobilityAlert(enabled: Boolean) {
        _uiState.update { it.copy(immobilityAlertEnabled = enabled) }
    }

    fun toggleArrivalAlert(enabled: Boolean) {
        _uiState.update { it.copy(arrivalAlertEnabled = enabled) }
    }

    fun toggleRouteDeviation(enabled: Boolean) {
        _uiState.update { it.copy(routeDeviationEnabled = enabled) }
    }
}
