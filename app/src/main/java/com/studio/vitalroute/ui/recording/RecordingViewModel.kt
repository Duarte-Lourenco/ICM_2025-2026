package com.studio.vitalroute.ui.recording

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.DestinationManager
import com.studio.vitalroute.data.SosManager
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.Activity
import com.studio.vitalroute.data.model.FirestoreSafeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale



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
    val distUnit: String          = "km",
    val speedUnit: String         = "km/h",
    val elevation: String         = "0",
    val calories: String          = "0",
    val activityType: ActivityType = ActivityType.CYCLING,
    // sos estado
    val isSosCountdown: Boolean   = false,
    val sosCountdownRemaining: Int = 0,
    val sosCountdownTotal: Int    = 15,
    val sosSent: Boolean          = false,
    val lastAlertLabel: String?   = null,
    // partilha de localizacao
    val isLocationSharing: Boolean = false,
    val locationSharingEnabled: Boolean = false,
    // geofencing
    val arrivedAtZone: String? = null,
    // mapa em tempo real
    val currentLat: Double = 0.0,
    val currentLng: Double = 0.0,
    val routePoints: List<String> = emptyList(),
    // destino e rota planeada
    val hasDestination: Boolean = false,
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val destinationName: String = "",
    val plannedRoutePoints: List<String> = emptyList(),
    val safeZones: List<FirestoreSafeZone> = emptyList(),
    val routeDurationSecs: Int = 0,
    val routeDistanceKm: Double = 0.0,
    val isLoadingRoute: Boolean = false
)


class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx        = application.applicationContext
    private val repository = FirestoreRepository()

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private var manualSosJob: Job?  = null
    private var routeFetchJob: Job? = null
    private var autoStopHandled     = false
    private var prevServiceSosSent  = false

    // definicoes carregadas do firestore para passar ao servico
    private var fallSensitivity        = 0.6f
    private var immobilityEnabled      = true
    private var immobilityMinutes      = 5
    private var sosDelaySecs           = 15
    private var routeDeviationEnabled  = false
    private var arrivalAlertEnabled    = false
    private var userWeightKg           = 70f
    private var useMetric              = true

    init {
        // observa o estado do servico em tempo real
        viewModelScope.launch {
            RecordingService.state.collect { s ->
                // chegada automatica a zona segura ou destino — para a gravacao
                if (s.autoStopped && s.isRecording && !autoStopHandled) {
                    autoStopHandled = true
                    stopRecording()
                }
                // sosSent: só propagar quando é uma nova transição false→true
                // Se o servico mantém sosSent=true entre ticks mas o utilizador já dispensou,
                // não re-mostrar o dialogo
                val isNewSosSent = s.sosSent && !prevServiceSosSent
                prevServiceSosSent = s.sosSent

                val metric = useMetric
                _uiState.update { ui ->
                    ui.copy(
                        isRecording           = s.isRecording,
                        elapsedTime           = formatTime(s.elapsedSeconds),
                        distance              = if (metric) "%.2f".format(s.distanceKm)
                                               else "%.2f".format(s.distanceKm * 0.621371),
                        speed                 = if (metric) "%.1f".format(s.speedKmh)
                                               else "%.1f".format(s.speedKmh * 0.621371),
                        distUnit              = if (metric) "km" else "mi",
                        speedUnit             = if (metric) "km/h" else "mph",
                        elevation             = s.elevationM.toString(),
                        calories              = s.calories.toString(),
                        isSosCountdown        = s.isSosCountdown,
                        sosCountdownRemaining = s.sosCountdownRemaining,
                        sosCountdownTotal     = s.sosCountdownTotal,
                        sosSent               = when {
                            isNewSosSent -> true       // nova transição: mostrar dialogo
                            !s.sosSent   -> false      // servico limpou: esconder
                            else         -> ui.sosSent // servico ainda true mas já dispensado: manter estado
                        },
                        lastAlertLabel        = s.lastAlertLabel,
                        isLocationSharing     = s.isLocationSharing,
                        arrivedAtZone         = s.arrivedAtZone,
                        currentLat            = s.currentLat,
                        currentLng            = s.currentLng,
                        routePoints           = s.routePoints
                    )
                }
            }
        }
        // observa o destino selecionado no ecra do mapa
        viewModelScope.launch {
            DestinationManager.destination.collect { dest ->
                _uiState.update {
                    it.copy(
                        hasDestination  = dest != null,
                        destinationLat  = dest?.lat ?: 0.0,
                        destinationLng  = dest?.lng ?: 0.0,
                        destinationName = dest?.name ?: ""
                    )
                }
                // pre-fetch da rota mal o destino é definido — sem esperar pelo GPS
                if (dest != null) {
                    val loc = getLastKnownLocation()
                    if (loc != null) {
                        _uiState.update { it.copy(isLoadingRoute = true, routeDurationSecs = 0, routeDistanceKm = 0.0) }
                        fetchPlannedRoute(loc.first, loc.second, dest.lat, dest.lng, _uiState.value.activityType)
                    }
                }
            }
        }

        // carrega zonas seguras para mostrar no mapa de gravacao
        viewModelScope.launch {
            repository.getSafeZones()
                .catch { }
                .collect { zones -> _uiState.update { it.copy(safeZones = zones) } }
        }

        // carrega definicoes de seguranca para usar ao iniciar a gravacao
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
                useMetric             = settings.metricSystem
                _uiState.update { it.copy(distUnit = if (useMetric) "km" else "mi", speedUnit = if (useMetric) "km/h" else "mph") }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile()
                if (profile != null && profile.weightKg > 0f) {
                    userWeightKg = profile.weightKg
                }
            } catch (_: Exception) {}
        }
    }

    // partilha de localizacao

    fun toggleLocationSharing(enabled: Boolean) {
        _uiState.update { it.copy(locationSharingEnabled = enabled) }
    }

    // geofencing

    fun dismissArrival() {
        _uiState.update { it.copy(arrivedAtZone = null) }
    }

    fun clearDestination() {
        DestinationManager.clear()
        _uiState.update { it.copy(routeDurationSecs = 0, routeDistanceKm = 0.0, isLoadingRoute = false) }
    }

    // link de partilha de localizacao

    fun copyLocationLink(context: Context) {
        val s    = RecordingService.state.value
        val type = when (s.activityType) {
            "running" -> "corrida"
            "walking" -> "caminhada"
            else      -> "ciclismo"
        }
        val mapsUrl = if (s.currentLat != 0.0 || s.currentLng != 0.0) {
            val lat = "%.5f".format(java.util.Locale.US, s.currentLat)
            val lng = "%.5f".format(java.util.Locale.US, s.currentLng)
            "https://maps.google.com/?q=$lat,$lng"
        } else {
            null
        }
        val text = if (mapsUrl != null) {
            "VitalRoute — Localização em tempo real ($type):\n$mapsUrl\n" +
            "(Link atualizado — toca para abrir no Google Maps)\n" +
            "-- Enviado automaticamente pela app VitalRoute --"
        } else {
            "VitalRoute — A aguardar sinal GPS para partilhar localização ($type).\n" +
            "-- Enviado automaticamente pela app VitalRoute --"
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("VitalRoute localização", text))
    }

    // tipo de atividade

    fun selectActivityType(type: ActivityType) {
        if (!_uiState.value.isRecording) {
            _uiState.update { it.copy(activityType = type) }
            // recalcula rota com novo tipo de atividade se houver destino
            val dest = DestinationManager.destination.value
            if (dest != null) {
                val loc = getLastKnownLocation()
                if (loc != null) {
                    _uiState.update { it.copy(isLoadingRoute = true, routeDurationSecs = 0, routeDistanceKm = 0.0) }
                    fetchPlannedRoute(loc.first, loc.second, dest.lat, dest.lng, type)
                }
            }
        }
    }

    // iniciar gravacao

    fun startRecording() {
        autoStopHandled    = false
        prevServiceSosSent = false
        _uiState.update { it.copy(plannedRoutePoints = emptyList()) }
        val state = _uiState.value
        val type  = state.activityType
        val dest  = DestinationManager.destination.value
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
            putExtra(RecordingService.EXTRA_WEIGHT_KG,               userWeightKg)
            if (dest != null) {
                putExtra(RecordingService.EXTRA_DEST_ENABLED, true)
                putExtra(RecordingService.EXTRA_DEST_LAT,     dest.lat)
                putExtra(RecordingService.EXTRA_DEST_LNG,     dest.lng)
                putExtra(RecordingService.EXTRA_DEST_RADIUS,  dest.radiusM)
            }
        }
        ctx.startForegroundService(intent)

        // se houver destino e a rota ainda não foi calculada, tenta agora com localização disponível
        if (dest != null && _uiState.value.routeDurationSecs == 0) {
            routeFetchJob?.cancel()
            routeFetchJob = viewModelScope.launch {
                val loc = getLastKnownLocation()
                if (loc != null) {
                    _uiState.update { it.copy(isLoadingRoute = true) }
                    fetchPlannedRoute(loc.first, loc.second, dest.lat, dest.lng, type)
                }
            }
        }
    }

    // parar gravacao

    private fun fetchPlannedRoute(
        startLat: Double, startLng: Double,
        destLat: Double,  destLng: Double,
        type: ActivityType
    ) {
        viewModelScope.launch {
            try {
                val profile = when (type) {
                    ActivityType.CYCLING -> "bike"
                    ActivityType.RUNNING, ActivityType.WALKING -> "foot"
                }
                val url = "https://router.project-osrm.org/route/v1/$profile/" +
                    "%.6f,%.6f;%.6f,%.6f?geometries=geojson&overview=full"
                    .format(Locale.US, startLng, startLat, destLng, destLat)

                val body = withContext(Dispatchers.IO) {
                    runCatching {
                        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 8_000
                        conn.readTimeout    = 8_000
                        conn.inputStream.bufferedReader().readText()
                    }.getOrNull()
                }
                if (body == null) {
                    _uiState.update { it.copy(isLoadingRoute = false) }
                    return@launch
                }

                val routes = JSONObject(body).optJSONArray("routes") ?: return@launch
                if (routes.length() == 0) return@launch
                val route      = routes.getJSONObject(0)
                val distanceKm = route.optDouble("distance", 0.0) / 1000.0

                // duração ajustada ao tipo de atividade
                val durationSecs = when (type) {
                    ActivityType.RUNNING -> (distanceKm / 10.0 * 3600).toInt()  // 10 km/h
                    ActivityType.WALKING -> route.optDouble("duration", 0.0).toInt()  // ~5 km/h (OSRM foot)
                    ActivityType.CYCLING -> route.optDouble("duration", 0.0).toInt()  // OSRM bike
                }

                val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
                val points = (0 until coords.length()).map { i ->
                    val pair = coords.getJSONArray(i)
                    "%.6f,%.6f".format(Locale.US, pair.getDouble(1), pair.getDouble(0))
                }
                _uiState.update {
                    it.copy(
                        plannedRoutePoints = points,
                        routeDurationSecs  = durationSecs,
                        routeDistanceKm    = distanceKm,
                        isLoadingRoute     = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingRoute = false) }
            }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Pair<Double, Double>? = try {
        val lm = ctx.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
        if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0))
            Pair(loc.latitude, loc.longitude)
        else null
    } catch (_: Exception) { null }

    fun stopRecording() {
        routeFetchJob?.cancel()
        _uiState.update { it.copy(plannedRoutePoints = emptyList(), routeDurationSecs = 0, routeDistanceKm = 0.0, isLoadingRoute = false) }
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
                            maxSpeedKmh     = s.maxSpeedKmh,
                            elevationM      = s.elevationM,
                            calories        = s.calories,
                            elevationPoints = s.elevationPoints,
                            routePoints     = s.routePoints
                        )
                    )
                } catch (_: Exception) {
                    _uiState.update { it.copy(lastAlertLabel = "Atividade não guardada (sem ligação à internet)") }
                }
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
                sosCountdownTotal     = sosDelaySecs,
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
