package com.studio.vitalroute.ui.recording

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

//  o estado
data class RecordingUiState(
    val isRecording: Boolean = false,
    val elapsedTime: String = "00:00:00",
    val distance: String = "0.0",
    val sosTriggered: Boolean = false
)

// O ViewModel — guarda o estado e processa eventos
class RecordingViewModel : ViewModel() {

    // estado privado — só o ViewModel pode alterar
    private val _uiState = MutableStateFlow(RecordingUiState())

    // estado público — a UI só pode ler
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    // eventos que a UI pode enviar

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true) }
        // No futuro: iniciar serviço GPS, cronómetro, etc
    }

    fun stopRecording() {
        _uiState.update { it.copy(
            isRecording = false,
            elapsedTime = "00:00:00",
            distance = "0.0"
        )}
    }

    fun triggerSos() {
        _uiState.update { it.copy(sosTriggered = true) }
        // No futuro: enviar alerta para contactos de emergência
    }

    fun dismissSos() {
        _uiState.update { it.copy(sosTriggered = false) }
    }
}