package com.studio.vitalroute.ui.recording

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
//  Estado da UI (mapeado a partir do RecordingServiceState)
// ─────────────────────────────────────────────────────────────

data class RecordingUiState(
    val isRecording: Boolean = false,
    val elapsedTime: String  = "00:00:00",
    val distance: String     = "0.00",
    val speed: String        = "0.0",
    val elevation: String    = "0",
    val calories: String     = "0",
    val sosTriggered: Boolean = false
)

// ─────────────────────────────────────────────────────────────
//  RecordingViewModel
//
//  Não guarda estado próprio de gravação — delega tudo ao
//  RecordingService (Foreground Service) que corre em background.
//  Coleciona o StateFlow partilhado do serviço e mapeia para UI.
// ─────────────────────────────────────────────────────────────

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx        = application.applicationContext
    private val repository = FirestoreRepository()

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    init {
        // Ouve o estado do serviço em tempo real
        viewModelScope.launch {
            RecordingService.state.collect { s ->
                _uiState.update {
                    it.copy(
                        isRecording = s.isRecording,
                        elapsedTime = formatTime(s.elapsedSeconds),
                        distance    = "%.2f".format(s.distanceKm),
                        speed       = "%.1f".format(s.speedKmh),
                        elevation   = s.elevationM.toString(),
                        calories    = s.calories.toString()
                    )
                }
            }
        }
    }

    // ── Iniciar gravação ──────────────────────────────────────

    fun startRecording() {
        val intent = Intent(ctx, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        }
        ctx.startForegroundService(intent)
    }

    // ── Parar gravação ────────────────────────────────────────

    fun stopRecording() {
        val s = RecordingService.state.value

        // Guarda a atividade no Firestore se durou ≥ 10 segundos
        if (s.elapsedSeconds >= 10) {
            viewModelScope.launch {
                try {
                    repository.saveActivity(
                        Activity(
                            type            = "cycling",
                            startTime       = s.startTimeMs,
                            endTime         = System.currentTimeMillis(),
                            distanceKm      = s.distanceKm,
                            durationSeconds = s.elapsedSeconds,
                            avgSpeedKmh     = s.speedKmh,
                            elevationM      = s.elevationM,
                            calories        = s.calories
                        )
                    )
                } catch (_: Exception) { /* modo offline — ignora */ }
            }
        }

        val intent = Intent(ctx, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        ctx.startService(intent)
    }

    // ── SOS ───────────────────────────────────────────────────

    fun triggerSos() {
        _uiState.update { it.copy(sosTriggered = true) }
    }

    fun dismissSos() {
        _uiState.update { it.copy(sosTriggered = false) }
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun formatTime(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
