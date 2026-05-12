package com.studio.vitalroute.ui.recording

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.studio.vitalroute.data.SosManager


enum class ActivityType(val key: String, val label: String, val emoji: String) {
    CYCLING("cycling",  "Ciclismo",  "🚴"),
    RUNNING("running",  "Corrida",   "🏃"),
    WALKING("walking",  "Caminhada", "🚶"),
}


data class RecordingUiState(
    val isRecording: Boolean      = false,
    val elapsedTime: String       = "00:00:00",
    val distance: String          = "0.00",
    val speed: String             = "0.0",
    val elevation: String         = "0",
    val calories: String          = "0",
    val activityType: ActivityType = ActivityType.CYCLING,
    // SOS estado
    val isSosCountdown: Boolean   = false,
    val sosCountdownRemaining: Int = 0,
    val sosSent: Boolean          = false,
    val lastAlertLabel: String?   = null,
    // Partilha de localização
    val isLocationSharing: Boolean = false,
    val locationSharingEnabled: Boolean = false,
    // Geofencing
    val arrivedAtZone: String? = null
)


class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx        = application.applicationContext
    private val repository = FirestoreRepository()

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private var manualSosJob: Job? = null

    // Definições carregadas do Firestore para passar ao serviço
    private var fallSensitivity        = 0.6f
    private var immobilityEnabled      = true
    private var immobilityMinutes      = 5
    private var sosDelaySecs           = 15
    private var routeDeviationEnabled  = false
    private var arrivalAlertEnabled    = false

    init {
        // Observa o estado do serviço em tempo real
        viewModelScope.launch {
            RecordingService.state.collect { s ->
                _uiState.update {
                    it.copy(
                        isRecording           = s.isRecording,
                        elapsedTime           = formatTime(s.elapsedSeconds),
                        distance              = "%.2f".format(s.distanceKm),
                        speed                 = "%.1f".format(s.speedKmh),
                        elevation             = s.elevationM.toString(),
                        calories              = s.calories.toString(),
                        isSosCountdown        = s.isSosCountdown,
                        sosCountdownRemaining = s.sosCountdownRemaining,
                        sosSent               = s.sosSent,
                        lastAlertLabel        = s.lastAlertLabel,
                        isLocationSharing     = s.isLocationSharing,
                        arrivedAtZone         = s.arrivedAtZone
                    )
                }
            }
        }
        // Carrega as definições de segurança para usar ao iniciar a gravação
        loadSecuritySettings()
    }

    private fun loadSecuritySettings() {
        viewModelScope.launch {
            try {
                val settings = repository.getSettings()
                fallSensitivity       = settings.fallSensitivity
                immobilityEnabled     = settings.immobilityAlertEnabled
                immobilityMinutes     = settings.immobilityMinutes
                sosDelaySecs          = settings.sosCountdownSecs
                routeDeviationEnabled = settings.routeDeviationEnabled
                arrivalAlertEnabled   = settings.arrivalAlertEnabled
            } catch (_: Exception) {
                // Mantém os valores por omissão se offline
            }
        }
    }

    // partilha de localização

    fun toggleLocationSharing(enabled: Boolean) {
        _uiState.update { it.copy(locationSharingEnabled = enabled) }
    }

    // geofencing

    fun dismissArrival() {
        _uiState.update { it.copy(arrivedAtZone = null) }
    }

    // link de partilha de localização

    fun copyLocationLink(context: Context) {
        val s    = RecordingService.state.value
        val type = when (s.activityType) {
            "running" -> "corrida"
            "walking" -> "caminhada"
            else      -> "ciclismo"
        }
        val mapsUrl = if (s.currentLat != 0.0 || s.currentLng != 0.0) {
            val lat = "%.5f".format(s.currentLat)
            val lng = "%.5f".format(s.currentLng)
            "https://maps.google.com/?q=$lat,$lng"
        } else {
            "https://maps.google.com"
        }
        val text = "Estou a fazer $type com o VitalRoute e a partilhar a minha localização.\n" +
            "Última posição conhecida: $mapsUrl\n" +
            "-- Enviado automaticamente pela app VitalRoute --"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("VitalRoute localização", text))
    }

    // tipo de atividade

    fun selectActivityType(type: ActivityType) {
        if (!_uiState.value.isRecording) {
            _uiState.update { it.copy(activityType = type) }
        }
    }

    // iniciar gravação

    fun startRecording() {
        val state = _uiState.value
        val type  = state.activityType
        val intent = Intent(ctx, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_ACTIVITY_TYPE,           type.key)
            putExtra(RecordingService.EXTRA_FALL_SENSITIVITY,        fallSensitivity)
            putExtra(RecordingService.EXTRA_IMMOBILITY_ENABLED,      immobilityEnabled)
            putExtra(RecordingService.EXTRA_IMMOBILITY_MINUTES,      immobilityMinutes)
            putExtra(RecordingService.EXTRA_SOS_DELAY_SECS,          sosDelaySecs)
            putExtra(RecordingService.EXTRA_ROUTE_DEVIATION_ENABLED, routeDeviationEnabled)
            putExtra(RecordingService.EXTRA_LOCATION_SHARING,        state.locationSharingEnabled)
            putExtra(RecordingService.EXTRA_ARRIVAL_ALERT_ENABLED,   arrivalAlertEnabled)
        }
        ctx.startForegroundService(intent)
    }

    // parar gravação

    fun stopRecording() {
        val s = RecordingService.state.value

        if (s.elapsedSeconds >= 10) {
            viewModelScope.launch {
                try {
                    repository.saveActivity(
                        Activity(
                            type            = s.activityType,
                            startTime       = s.startTimeMs,
                            endTime         = System.currentTimeMillis(),
                            distanceKm      = s.distanceKm,
                            durationSeconds = s.elapsedSeconds,
                            avgSpeedKmh     = s.speedKmh,
                            elevationM      = s.elevationM,
                            calories        = s.calories,
                            elevationPoints = s.elevationPoints,
                            routePoints     = s.routePoints
                        )
                    )
                } catch (_: Exception) { /* modo offline */ }
            }
        }

        val intent = Intent(ctx, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        ctx.startService(intent)
    }

    // sos manual (slider)

    fun triggerSos() {
        if (_uiState.value.isRecording) {
            ctx.startService(Intent(ctx, RecordingService::class.java).apply {
                action = RecordingService.ACTION_TRIGGER_SOS
            })
        } else {
            startManualSosCountdown()
        }
    }

    private fun startManualSosCountdown() {
        manualSosJob?.cancel()
        _uiState.update {
            it.copy(
                isSosCountdown        = true,
                sosCountdownRemaining = sosDelaySecs,
                sosSent               = false,
                lastAlertLabel        = "SOS manual acionado!"
            )
        }
        manualSosJob = viewModelScope.launch {
            for (remaining in (sosDelaySecs - 1) downTo 0) {
                kotlinx.coroutines.delay(1_000L)
                if (!_uiState.value.isSosCountdown) return@launch
                _uiState.update { it.copy(sosCountdownRemaining = remaining) }
            }
            _uiState.update {
                it.copy(
                    isSosCountdown        = false,
                    sosCountdownRemaining = 0,
                    sosSent               = true,
                    lastAlertLabel        = "SOS enviado!"
                )
            }
            launch(Dispatchers.IO) {
                SosManager.sendSos(ctx)
            }
        }
    }

    // cancelar sos countdown

    fun cancelSos() {
        if (_uiState.value.isRecording) {
            ctx.startService(Intent(ctx, RecordingService::class.java).apply {
                action = RecordingService.ACTION_CANCEL_SOS
            })
        } else {
            manualSosJob?.cancel()
            _uiState.update { it.copy(isSosCountdown = false, sosCountdownRemaining = 0) }
        }
    }

    fun dismissSosSent() {
        _uiState.update { it.copy(sosSent = false, lastAlertLabel = null) }
    }

    // helpers

    private fun formatTime(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
