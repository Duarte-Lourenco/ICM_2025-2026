package com.studio.vitalroute.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.Activity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
//  Estado da UI
// ─────────────────────────────────────────────────────────────

data class RecordingUiState(
    val isRecording: Boolean = false,
    val elapsedTime: String = "00:00:00",
    val distance: String = "0.00",
    val speed: String = "0.0",
    val elevation: String = "0",
    val calories: String = "0",
    val sosTriggered: Boolean = false
)

// ─────────────────────────────────────────────────────────────
//  RecordingViewModel
// ─────────────────────────────────────────────────────────────

class RecordingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private val repository = FirestoreRepository()

    // Referência ao Job do cronómetro para poder cancelá-lo
    private var timerJob: Job? = null
    private var elapsedSeconds = 0L
    private var startTimeMs = 0L

    // ── Eventos da UI ─────────────────────────────────────────

    fun startRecording() {
        startTimeMs = System.currentTimeMillis()
        _uiState.update { it.copy(isRecording = true) }
        startTimer()
    }

    fun stopRecording() {
        val endTimeMs = System.currentTimeMillis()
        timerJob?.cancel()

        // Guarda a atividade no Firestore antes de limpar o estado
        val current = _uiState.value
        if (elapsedSeconds >= 10) { // só guarda se durou pelo menos 10 segundos
            viewModelScope.launch {
                try {
                    repository.saveActivity(
                        Activity(
                            type            = "cycling",
                            startTime       = startTimeMs,
                            endTime         = endTimeMs,
                            distanceKm      = current.distance.toDoubleOrNull() ?: 0.0,
                            durationSeconds = elapsedSeconds,
                            avgSpeedKmh     = current.speed.toDoubleOrNull() ?: 0.0,
                            elevationM      = current.elevation.toIntOrNull() ?: 0,
                            calories        = current.calories.toIntOrNull() ?: 0
                        )
                    )
                } catch (e: Exception) {
                    // Silencioso — o utilizador pode não estar autenticado ainda
                }
            }
        }

        elapsedSeconds = 0L
        startTimeMs    = 0L
        _uiState.update {
            it.copy(
                isRecording = false,
                elapsedTime = "00:00:00",
                distance    = "0.00",
                speed       = "0.0",
                elevation   = "0",
                calories    = "0"
            )
        }
    }

    fun triggerSos() {
        _uiState.update { it.copy(sosTriggered = true) }
    }

    fun dismissSos() {
        _uiState.update { it.copy(sosTriggered = false) }
    }

    // ── Cronómetro com Coroutines ─────────────────────────────

    /**
     * Lança uma coroutine no viewModelScope que acorda a cada segundo,
     * incrementa o contador e atualiza o estado da UI.
     *
     * - delay(1000L) é non-blocking: liberta a thread durante a espera
     * - viewModelScope garante cancelamento automático quando o
     *   ViewModel é destruído (ex: utilizador sai do ecrã)
     */
    private fun startTimer() {
        timerJob?.cancel() // Garante que não há dois timers a correr
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)          // aguarda 1 segundo sem bloquear
                elapsedSeconds++
                _uiState.update {
                    it.copy(elapsedTime = formatTime(elapsedSeconds))
                }
            }
        }
    }

    /**
     * Formata segundos para HH:MM:SS
     * ex: 3661 → "01:01:01"
     */
    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    // Cancelar o timer quando o ViewModel for destruído
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
