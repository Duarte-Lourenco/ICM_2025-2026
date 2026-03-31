package com.studio.vitalroute.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val metricSystem: Boolean = true,
    val autoPause: Boolean = true,
    val email: String = "duarte@vitalroute.pt",
    val offlineStorage: String = "2.4 GB utilizados"
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleMetricSystem(enabled: Boolean) {
        _uiState.update { it.copy(metricSystem = enabled) }
    }

    fun toggleAutoPause(enabled: Boolean) {
        _uiState.update { it.copy(autoPause = enabled) }
    }
}