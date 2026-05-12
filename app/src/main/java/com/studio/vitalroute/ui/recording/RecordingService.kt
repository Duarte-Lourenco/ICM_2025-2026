package com.studio.vitalroute.ui.recording

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.studio.vitalroute.MainActivity
import com.studio.vitalroute.data.SosManager
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.FirestoreSafeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt


data class RecordingServiceState(
    val isRecording: Boolean    = false,
    val elapsedSeconds: Long    = 0L,
    val distanceKm: Double      = 0.0,
    val speedKmh: Double        = 0.0,
    val elevationM: Int         = 0,
    val calories: Int           = 0,
    val startTimeMs: Long       = 0L,
    val activityType: String    = "cycling",
    // Deteção de quedas / imobilidade
    val isSosCountdown: Boolean  = false,
    val sosCountdownRemaining: Int = 0,
    val sosSent: Boolean         = false,
    val lastAlertLabel: String?  = null,
    // Partilha de localização em tempo real
    val isLocationSharing: Boolean = false,
    // Geofencing — chegada a zona segura
    val arrivedAtZone: String?  = null,
    // Coordenadas GPS atuais (para link de partilha)
    val currentLat: Double = 0.0,
    val currentLng: Double = 0.0,
    // Dados gravados — exportados ao parar (para persistência no Firestore)
    val elevationPoints: List<Int>  = emptyList(),
    val routePoints: List<String>   = emptyList()
)

//

class RecordingService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID      = "vitalroute_recording"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START       = "com.studio.vitalroute.START_RECORDING"
        const val ACTION_STOP        = "com.studio.vitalroute.STOP_RECORDING"
        const val ACTION_CANCEL_SOS  = "com.studio.vitalroute.CANCEL_SOS"
        const val ACTION_TRIGGER_SOS = "com.studio.vitalroute.TRIGGER_SOS"

        // Extras para configuração
        const val EXTRA_ACTIVITY_TYPE           = "activity_type"
        const val EXTRA_FALL_SENSITIVITY        = "fall_sensitivity"       // Float 0..1
        const val EXTRA_IMMOBILITY_ENABLED      = "immobility_enabled"     // Boolean
        const val EXTRA_IMMOBILITY_MINUTES      = "immobility_minutes"     // Int
        const val EXTRA_SOS_DELAY_SECS          = "sos_delay_secs"         // Int
        const val EXTRA_ROUTE_DEVIATION_ENABLED = "route_deviation_enabled" // Boolean
        const val EXTRA_LOCATION_SHARING        = "location_sharing"       // Boolean
        const val EXTRA_ARRIVAL_ALERT_ENABLED   = "arrival_alert_enabled"  // Boolean

        // Notificação de chegada a zona segura
        const val CHANNEL_ARRIVAL_ID    = "vitalroute_arrival"
        const val NOTIF_ARRIVAL_ID      = 1002

        // Threshold de desvio: salto GPS > 400m em < 60s → anomalia
        private const val ROUTE_DEVIATION_JUMP_M  = 400f
        private const val ROUTE_DEVIATION_JUMP_MS = 60_000L
        // Velocidade anómala: > 80 km/h durante > 30s numa atividade não motorizada
        private const val ANOMALOUS_SPEED_KMH  = 80.0
        private const val ANOMALOUS_SPEED_MS   = 30_000L

        // Estado partilhado — acessível do ViewModel sem binding
        private val _state = MutableStateFlow(RecordingServiceState())
        val state: StateFlow<RecordingServiceState> = _state.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job?           = null
    private var sosJob: Job?             = null
    private var immobilityJob: Job?      = null
    private var deviationJob: Job?       = null
    private var locationSharingJob: Job? = null

    // GPS
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    private var lastMovementTime: Long  = 0L
    private var totalDistanceM = 0.0
    private var lastKnownLocation: Location? = null

    // Acelerómetro
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null
    private var fallSensitivity  = 0.6f  // 0 = baixa, 1 = alta
    private var lastFallTime     = 0L    // debounce: não re-acionar por 10s
    private var fallPhase        = FallPhase.NONE

    // Desvio de rota
    private var routeDeviationEnabled  = false
    private var anomalousSpeedStart    = 0L
    private var lastLocationTimestamp  = 0L

    // Partilha de localização em tempo real
    private var locationSharingEnabled = false
    private val firestoreRepository    = FirestoreRepository()

    // Geofencing
    private var arrivalAlertEnabled    = false
    private val safeZones              = mutableListOf<FirestoreSafeZone>()
    private val triggeredZones         = mutableSetOf<String>()  // IDs já notificados (debounce)

    // Amostras para gráficos (acumuladas em memória, exportadas ao parar)
    private val elevationSamples       = mutableListOf<Int>()    // altitude a cada ~30 s
    private val routeSamples           = mutableListOf<String>() // "lat,lng" a cada ~60 s
    private var lastElevationSampleSec = 0L   // elapsedSeconds na última amostra de elevação
    private var lastRouteSampleSec     = 0L   // elapsedSeconds na última amostra de rota

    // Configuração
    private var activityType         = "cycling"
    private var immobilityEnabled    = true
    private var immobilityMinutes    = 5
    private var sosDelaySecs         = 15

    // binder
    inner class LocalBinder : Binder()
    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    // lifecycle

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        sensorManager   = getSystemService(SENSOR_SERVICE)   as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannel()
        createArrivalNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TRIGGER_SOS -> triggerSosCountdown("SOS manual acionado!")
            ACTION_START     -> {
                // Lê as configurações passadas pelo ViewModel
                activityType           = intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: "cycling"
                fallSensitivity        = intent.getFloatExtra(EXTRA_FALL_SENSITIVITY, 0.6f)
                immobilityEnabled      = intent.getBooleanExtra(EXTRA_IMMOBILITY_ENABLED, true)
                immobilityMinutes      = intent.getIntExtra(EXTRA_IMMOBILITY_MINUTES, 5)
                sosDelaySecs           = intent.getIntExtra(EXTRA_SOS_DELAY_SECS, 15)
                routeDeviationEnabled  = intent.getBooleanExtra(EXTRA_ROUTE_DEVIATION_ENABLED, false)
                locationSharingEnabled = intent.getBooleanExtra(EXTRA_LOCATION_SHARING, false)
                arrivalAlertEnabled    = intent.getBooleanExtra(EXTRA_ARRIVAL_ALERT_ENABLED, false)
                startRecording()
            }
            ACTION_STOP      -> stopRecording()
            ACTION_CANCEL_SOS -> cancelSos()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        sosJob?.cancel()
        immobilityJob?.cancel()
        deviationJob?.cancel()
        locationSharingJob?.cancel()
        stopLocationUpdates()
        stopAccelerometer()
        serviceScope.cancel()
        _state.value = RecordingServiceState()
    }

    // iniciar gravação

    private fun startRecording() {
        val now = System.currentTimeMillis()
        totalDistanceM        = 0.0
        lastLocation          = null
        lastMovementTime      = now
        elevationSamples.clear()
        routeSamples.clear()
        lastElevationSampleSec = 0L
        lastRouteSampleSec     = 0L

        _state.value = RecordingServiceState(
            isRecording  = true,
            startTimeMs  = now,
            activityType = activityType
        )

        val notification = buildNotification("00:00:00", "0.00 km")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTimer()
        startLocationUpdates()
        startAccelerometer()
        if (immobilityEnabled) startImmobilityMonitor()
        if (routeDeviationEnabled) startRouteDeviationMonitor()
        if (locationSharingEnabled) startLocationSharing()
        if (arrivalAlertEnabled) loadSafeZonesForGeofencing()
    }

    // parar gravação

    private fun stopRecording() {
        timerJob?.cancel()
        sosJob?.cancel()
        immobilityJob?.cancel()
        deviationJob?.cancel()
        locationSharingJob?.cancel()
        stopLocationUpdates()
        stopAccelerometer()
        // Para de partilhar localização ao terminar a gravação
        if (locationSharingEnabled) {
            serviceScope.launch(Dispatchers.IO) {
                firestoreRepository.stopLiveLocation()
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        safeZones.clear()
        triggeredZones.clear()
        _state.update { it.copy(isRecording = false, isLocationSharing = false, arrivedAtZone = null) }
    }

    // sos: cancelar (botão "estou bem")

    private fun cancelSos() {
        sosJob?.cancel()
        _state.update {
            it.copy(
                isSosCountdown        = false,
                sosCountdownRemaining = 0,
                lastAlertLabel        = "SOS cancelado — estás bem!"
            )
        }
    }

    // cronómetro

    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (true) {
                delay(1_000L)
                _state.update { s ->
                    s.copy(
                        elapsedSeconds = s.elapsedSeconds + 1,
                        calories = estimateCalories(s.elapsedSeconds + 1, s.speedKmh, s.activityType)
                    )
                }
                updateNotification()
            }
        }
    }

    // gps

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // GPS — precisão total
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1_000L, 5f,
                locationListener
            )
        } catch (_: Exception) {}
        // Network provider — fix inicial muito mais rápido (torres / Wi-Fi)
        // Substitui GPS enquanto o chip não obtém satélites, depois é sobreposto
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3_000L, 10f,
                    locationListener
                )
            }
        } catch (_: Exception) {}
    }

    private fun stopLocationUpdates() {
        try { locationManager.removeUpdates(locationListener) } catch (_: Exception) {}
    }

    private val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {
            lastKnownLocation = location
            val now  = System.currentTimeMillis()
            val last = lastLocation

            if (last != null) {
                val deltaM   = last.distanceTo(location)
                val deltaMs  = now - lastLocationTimestamp

                // desvio de rota: salto gps anómalo
                if (routeDeviationEnabled
                    && !_state.value.isSosCountdown
                    && _state.value.elapsedSeconds > 120
                    && deltaMs in 1_000L..ROUTE_DEVIATION_JUMP_MS
                    && deltaM > ROUTE_DEVIATION_JUMP_M
                ) {
                    val impliedSpeedKmh = (deltaM / (deltaMs / 1000f)) * 3.6f
                    if (impliedSpeedKmh > ANOMALOUS_SPEED_KMH) {
                        _state.update { it.copy(lastAlertLabel = "Salto GPS anómalo detetado!") }
                        triggerSosCountdown("Desvio de rota detetado! A enviar SOS em...")
                    }
                }

                if (deltaM < 500f) {
                    totalDistanceM += deltaM
                    if (deltaM > 1f) lastMovementTime = now
            lastLocation          = location
            lastLocationTimestamp = now

            val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
            val altM     = if (location.hasAltitude()) location.altitude.toInt() else 0
            val distKm   = totalDistanceM / 1000.0

            // desvio de rota: velocidade anómala sustentada
            if (routeDeviationEnabled && !_state.value.isSosCountdown) {
                if (speedKmh > ANOMALOUS_SPEED_KMH) {
                    if (anomalousSpeedStart == 0L) anomalousSpeedStart = now
                    else if (now - anomalousSpeedStart >= ANOMALOUS_SPEED_MS) {
                        anomalousSpeedStart = 0L
                        _state.update { it.copy(lastAlertLabel = "Velocidade anómala (${speedKmh.toInt()} km/h)!") }
                        triggerSosCountdown("Velocidade anómala detetada! A enviar SOS em...")
                    }
                } else {
                    anomalousSpeedStart = 0L
                }
            }

            // ── Amostras periódicas: elevação (30 s) e rota (60 s)
            val elapsed = _state.value.elapsedSeconds
            if (altM > 0 && elapsed - lastElevationSampleSec >= 30L) {
                elevationSamples.add(altM)
                lastElevationSampleSec = elapsed
            }
            if (elapsed - lastRouteSampleSec >= 60L
                && (location.latitude != 0.0 || location.longitude != 0.0)
            ) {
                routeSamples.add("%.6f,%.6f".format(location.latitude, location.longitude))
                lastRouteSampleSec = elapsed
            }

            _state.update { s ->
                s.copy(
                    distanceKm      = distKm,
                    speedKmh        = speedKmh.toDouble(),
                    elevationM      = altM,
                    calories        = estimateCalories(s.elapsedSeconds, speedKmh.toDouble(), s.activityType),
                    currentLat      = location.latitude,
                    currentLng      = location.longitude,
                    elevationPoints = elevationSamples.toList(),
                    routePoints     = routeSamples.toList()
                )
            }

            // ── Geofencing: verificar proximidade de zonas seguras
            if (arrivalAlertEnabled && safeZones.isNotEmpty()) {
                checkGeofenceArrival(location)
            }
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onStatusChanged(provider: String, status: Int, extras: android.os.Bundle?) {}
    }

    private fun checkGeofenceArrival(location: android.location.Location) {
        for (zone in safeZones) {
            if (zone.id in triggeredZones) continue   // já notificado nesta sessão

            val zoneLocation = android.location.Location("zone").apply {
                latitude  = zone.lat
                longitude = zone.lng
            }
            val distanceM = location.distanceTo(zoneLocation)

            if (distanceM <= zone.radiusM) {
                triggeredZones.add(zone.id)
                onArrivalAtZone(zone)
            }
        }
    }

    private fun onArrivalAtZone(zone: FirestoreSafeZone) {
        _state.update { it.copy(arrivedAtZone = zone.name) }

        // Notificação push de chegada
        val notification = androidx.core.app.NotificationCompat
            .Builder(this, CHANNEL_ARRIVAL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("✅  Chegaste a ${zone.name}")
            .setContentText("Os teus contactos serão notificados da tua chegada.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .notify(NOTIF_ARRIVAL_ID + zone.id.hashCode(), notification)

        // SMS para contactos com zonesEnabled = true
        serviceScope.launch(Dispatchers.IO) {
            try {
                val contacts = firestoreRepository.getZoneContactsOnce()
                val lat = "%.5f".format(lastKnownLocation?.latitude ?: 0.0)
                val lon = "%.5f".format(lastKnownLocation?.longitude ?: 0.0)
                val message = "VitalRoute: ${zone.name} atingida!\n" +
                    "O teu contacto chegou em segurança.\n" +
                    (if (lastKnownLocation != null) "Localização: https://maps.google.com/?q=$lat,$lon\n" else "") +
                    "-- VitalRoute --"
                val smsManager = SosManager.getSmsManagerPublic(applicationContext)
                contacts.forEach { contact ->
                    try {
                        smsManager.sendTextMessage(contact.phone, null, message, null, null)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    // acelerómetro — deteção de quedas

    private fun startAccelerometer() {
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun stopAccelerometer() {
        try { sensorManager.unregisterListener(this) } catch (_: Exception) {}
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (!_state.value.isRecording) return
        if (_state.value.isSosCountdown) return  // já há um SOS ativo

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)   // m/s² incluindo gravidade (≈9.8 em repouso)

        // Thresholds baseados na sensibilidade configurada
        // Sensibilidade 0.0 → threshold alto (poucos falsos positivos)
        // Sensibilidade 1.0 → threshold baixo (deteção mais fácil)
        val freeFallThreshold = lerp(4.0f, 2.0f, fallSensitivity)   // m/s²
        val impactThreshold   = lerp(30f, 18f, fallSensitivity)      // m/s²

        val now = System.currentTimeMillis()
        // Debounce: não re-acionar durante 10 segundos após a última queda
        if (now - lastFallTime < 10_000L) return

        when (fallPhase) {
            FallPhase.NONE -> {
                // Fase 1: queda livre (magnitude muito baixa = pouco peso sentido)
                if (magnitude < freeFallThreshold) {
                    fallPhase = FallPhase.FREE_FALL
                }
            }
            FallPhase.FREE_FALL -> {
                // Fase 2: impacto (magnitude muito alta) logo a seguir à queda livre
                if (magnitude > impactThreshold) {
                    lastFallTime = now
                    fallPhase = FallPhase.NONE
                    onFallDetected()
                } else if (magnitude > freeFallThreshold * 2.5f) {
                    // Movimento normal — reset fase
                    fallPhase = FallPhase.NONE
                }
            }
        }
    }

    private fun onFallDetected() {
        _state.update { it.copy(lastAlertLabel = "Queda detetada!") }
        triggerSosCountdown("Queda detetada! A enviar SOS em...")
    }

    // monitor de imobilidade

    private fun startImmobilityMonitor() {
        immobilityJob = serviceScope.launch {
            delay(60_000L) // espera 1 minuto antes de começar a monitorizar
            while (true) {
                delay(30_000L) // verifica a cada 30 segundos
                if (!_state.value.isRecording) break
                if (_state.value.isSosCountdown) continue

                val inactiveMs  = System.currentTimeMillis() - lastMovementTime
                val thresholdMs = immobilityMinutes * 60_000L

                if (inactiveMs >= thresholdMs && _state.value.elapsedSeconds > 60) {
                    triggerSosCountdown("Imobilidade detetada! A enviar SOS em...")
                    // Depois de acionar, espera pelo menos o dobro do tempo configurado
                    delay(thresholdMs * 2)
                }
            }
        }
    }

    // monitor de desvio de rota

    private fun startRouteDeviationMonitor() {
        deviationJob = serviceScope.launch {
            delay(120_000L) // espera 2 minutos — utilizador estabelece rota
            while (true) {
                delay(60_000L) // verifica a cada minuto
                if (!_state.value.isRecording) break
                if (_state.value.isSosCountdown) continue

                val s = _state.value
                // Alerta se velocidade > 80 km/h durante atividade pedestre/ciclismo
                // (verificação periódica adicional à deteção em tempo real no GPS listener)
                if (s.speedKmh > ANOMALOUS_SPEED_KMH) {
                    if (anomalousSpeedStart == 0L)
                        anomalousSpeedStart = System.currentTimeMillis()
                }
            }
        }
    }

    // geofencing: carrega zonas ao início

    private fun loadSafeZonesForGeofencing() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val zones = firestoreRepository.getSafeZonesOnce()
                safeZones.clear()
                safeZones.addAll(zones)
            } catch (_: Exception) {}
        }
    }

    // partilha de localização em tempo real

    private fun startLocationSharing() {
        _state.update { it.copy(isLocationSharing = true) }
        locationSharingJob = serviceScope.launch {
            while (true) {
                delay(10_000L) // atualiza a cada 10 segundos
                if (!_state.value.isRecording) break
                val loc = lastKnownLocation ?: continue
                launch(Dispatchers.IO) {
                    firestoreRepository.updateLiveLocation(
                        lat       = loc.latitude,
                        lng       = loc.longitude,
                        speedKmh  = _state.value.speedKmh,
                        distKm    = _state.value.distanceKm
                    )
                }
            }
        }
    }

    // sos countdown

    private fun triggerSosCountdown(label: String) {
        sosJob?.cancel()
        _state.update {
            it.copy(
                isSosCountdown        = true,
                sosCountdownRemaining = sosDelaySecs,
                sosSent               = false,
                lastAlertLabel        = label
            )
        }
        sosJob = serviceScope.launch {
            for (remaining in (sosDelaySecs - 1) downTo 0) {
                delay(1_000L)
                if (!_state.value.isSosCountdown) return@launch // foi cancelado
                _state.update { it.copy(sosCountdownRemaining = remaining) }
            }
            // Chegou a zero → envia SMS
            _state.update {
                it.copy(
                    isSosCountdown = false,
                    sosCountdownRemaining = 0,
                    sosSent        = true,
                    lastAlertLabel = "SOS enviado!"
                )
            }
            // Envia SMS em IO dispatcher
            launch(Dispatchers.IO) {
                SosManager.sendSos(
                    context  = applicationContext,
                    location = lastKnownLocation
                )
            }
        }
    }

    // notificação persistente

    private fun buildNotification(time: String, distance: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_STOP }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelSosIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_CANCEL_SOS }
        val pendingCancelSos = PendingIntent.getService(
            this, 2, cancelSosIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val s = _state.value
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingOpen)

        if (s.isSosCountdown) {
            builder
                .setContentTitle("⚠️  SOS em ${s.sosCountdownRemaining}s")
                .setContentText("Toca para cancelar se estiveres bem")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(false)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Estou bem", pendingCancelSos)
        } else {
            val typeLabel = when (s.activityType) {
                "running"  -> "🏃 A correr"
                "walking"  -> "🚶 A caminhar"
                else       -> "🚴 A gravar"
            }
            builder
                .setContentTitle("VitalRoute — $typeLabel")
                .setContentText("$time  ·  $distance")
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Parar", pendingStop)
        }
        return builder.build()
    }

    private fun updateNotification() {
        val s = _state.value
        val notification = buildNotification(
            formatTime(s.elapsedSeconds),
            "%.2f km".format(s.distanceKm)
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Gravação de Atividade",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mostra o tempo e distância durante uma gravação ativa"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun createArrivalNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ARRIVAL_ID, "Chegada a Zona Segura",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifica quando o utilizador chega a uma zona segura configurada"
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    // helpers

    private fun formatTime(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    /**
     * Calorias baseadas em MET (Metabolic Equivalent of Task) com velocidade real.
     * Fórmula padrão: kcal = MET × peso(kg) × tempo(horas)
     * Peso assumido: 70 kg (valor médio; futuramente configurável no perfil).
     *
     * Valores MET por velocidade — baseados em compêndio de atividades físicas
     * (Ainsworth et al., 2011):
     *   Ciclismo: 4.0 (< 10 km/h) → 10.0 (> 30 km/h)
     *   Corrida:  6.0 (< 6 km/h)  → 14.5 (> 18 km/h)
     *   Caminhada: 2.5 (< 2 km/h) →  5.0 (> 6 km/h)
     */
    private fun estimateCalories(secs: Long, speedKmh: Double, type: String): Int {
        if (secs < 1) return 0
        val met = when (type) {
            "running" -> when {
                speedKmh < 6.0  -> 6.0
                speedKmh < 8.0  -> 8.3
                speedKmh < 10.0 -> 9.8
                speedKmh < 12.0 -> 11.0
                speedKmh < 14.0 -> 11.8
                speedKmh < 16.0 -> 12.8
                speedKmh < 18.0 -> 14.0
                else             -> 14.5
            }
            "walking" -> when {
                speedKmh < 2.0  -> 2.5
                speedKmh < 3.5  -> 3.0
                speedKmh < 5.0  -> 3.5
                speedKmh < 6.0  -> 4.3
                speedKmh < 7.0  -> 5.0
                else             -> 6.0   // marcha rápida
            }
            else -> when { // ciclismo
                speedKmh < 10.0 -> 4.0
                speedKmh < 16.0 -> 5.8
                speedKmh < 20.0 -> 7.0
                speedKmh < 25.0 -> 8.5
                speedKmh < 30.0 -> 10.0
                else             -> 12.0
            }
        }
        val weightKg = 70.0
        val hours    = secs / 3600.0
        return (met * weightKg * hours).toInt()
    }

    /** Interpolação linear entre dois valores */
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}

// Fases do algoritmo de deteção de queda (queda livre → impacto)
private enum class FallPhase { NONE, FREE_FALL }
