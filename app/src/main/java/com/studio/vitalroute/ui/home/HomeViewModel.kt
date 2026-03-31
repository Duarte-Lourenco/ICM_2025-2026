package com.studio.vitalroute.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class HomeUiState(
    val greeting: String = "Bom dia",
    val gpsStatus: String = "Forte",
    val batteryLevel: String = "82%",
    val isReady: Boolean = true,
    // Estatísticas semanais
    val weeklyKm: String = "12.4",
    val weeklyTime: String = "42 min",
    val weeklyIncidents: String = "0",
    val weeklyGoalKm: Float = 100f
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        updateGreeting()
    }

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Bom dia 🌅"
            hour < 19 -> "Boa tarde ☀️"
            else      -> "Boa noite 🌙"
        }
        _uiState.value = _uiState.value.copy(greeting = greeting)
    }
}
