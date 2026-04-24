package com.studio.vitalroute.ui.recording

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.studio.vitalroute.MainActivity
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

// ─────────────────────────────────────────────────────────────
//  Estado partilhado entre o Serviço e o ViewModel
// ─────────────────────────────────────────────────────────────

data class RecordingServiceState(
    val isRecording: Boolean  = false,
    val elapsedSeconds: Long  = 0L,
    val distanceKm: Double    = 0.0,
    val speedKmh: Double      = 0.0,
    val elevationM: Int       = 0,
    val calories: Int         = 0,
    val startTimeMs: Long     = 0L
)

// ─────────────────────────────────────────────────────────────
//  RecordingService — Foreground Service
//
//  Mantém o cronómetro e o GPS a correr mesmo com:
//  - Ecrã desligado
//  - App em segundo plano
//  - Outra aplicação em uso
// ─────────────────────────────────────────────────────────────

class RecordingService : Service() {

    // ── Singleton de estado — acessível do ViewModel ──────────
    companion object {
        const val CHANNEL_ID        = "vitalroute_recording"
        const val NOTIFICATION_ID   = 1001
        const val ACTION_START      = "com.studio.vitalroute.START_RECORDING"
        const val ACTION_STOP       = "com.studio.vitalroute.STOP_RECORDING"

        private val _state = MutableStateFlow(RecordingServiceState())
        val state: StateFlow<RecordingServiceState> = _state.asStateFlow()
    }

    private val serviceScope  = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    private var totalDistanceM = 0.0

    // ── Binder (não usado, mas necessário) ────────────────────
    inner class LocalBinder : Binder()
    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    // ── Lifecycle ─────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP  -> stopRecording()
        }
        return START_STICKY  // reinicia se o SO matar o serviço
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        stopLocationUpdates()
        serviceScope.cancel()
        _state.value = RecordingServiceState()
    }

    // ── Iniciar gravação ──────────────────────────────────────

    private fun startRecording() {
        val now = System.currentTimeMillis()
        totalDistanceM = 0.0
        lastLocation   = null

        _state.value = RecordingServiceState(
            isRecording = true,
            startTimeMs = now
        )

        // Mostra a notificação persistente (obrigatório para Foreground Service)
        val notification = buildNotification("00:00:00", "0.00 km")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // Fallback sem tipo de localização (permissão pode não estar concedida)
            startForeground(NOTIFICATION_ID, notification)
        }

        startTimer()
        startLocationUpdates()
    }

    // ── Parar gravação ────────────────────────────────────────

    private fun stopRecording() {
        timerJob?.cancel()
        stopLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        _state.update { it.copy(isRecording = false) }
    }

    // ── Cronómetro ────────────────────────────────────────────

    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (true) {
                delay(1_000L)
                _state.update { s ->
                    val secs = s.elapsedSeconds + 1
                    val cals = estimateCalories(secs, s.distanceKm)
                    s.copy(elapsedSeconds = secs, calories = cals)
                }
                updateNotification()
            }
        }
    }

    // ── GPS ───────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1_000L,   // mín 1 segundo entre atualizações
                5f,       // mín 5 metros entre atualizações
                locationListener
            )
        } catch (_: Exception) {
            // Permissão não concedida — cronómetro continua, só sem GPS
        }
    }

    private fun stopLocationUpdates() {
        try { locationManager.removeUpdates(locationListener) }
        catch (_: Exception) {}
    }

    private val locationListener = LocationListener { location ->
        val last = lastLocation
        if (last != null) {
            val deltaM = last.distanceTo(location)
            if (deltaM < 500f) { // ignora saltos absurdos de GPS
                totalDistanceM += deltaM
            }
        }
        lastLocation = location

        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
        val altM     = if (location.hasAltitude()) location.altitude.toInt() else 0
        val distKm   = totalDistanceM / 1000.0

        _state.update { s ->
            s.copy(
                distanceKm = distKm,
                speedKmh   = speedKmh.toDouble(),
                elevationM = altM,
                calories   = estimateCalories(s.elapsedSeconds, distKm)
            )
        }
    }

    // ── Notificação persistente ───────────────────────────────

    private fun buildNotification(time: String, distance: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("VitalRoute — A gravar")
            .setContentText("$time  ·  $distance")
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Parar", pendingStop)
            .setOngoing(true)           // não pode ser deslizada para fechar
            .setSilent(true)            // sem som / vibração
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val s = _state.value
        val notification = buildNotification(
            formatTime(s.elapsedSeconds),
            "%.2f km".format(s.distanceKm)
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gravação de Atividade",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mostra o tempo e distância durante uma gravação ativa"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun formatTime(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private fun estimateCalories(secs: Long, distKm: Double): Int {
        // Estimativa: ~35 kcal/km para ciclismo (70 kg, esforço moderado)
        val byDistance = distKm * 35.0
        // Mínimo de 4 kcal/min se em movimento
        val byTime = (secs / 60.0) * 4.0
        return maxOf(byDistance, byTime).toInt()
    }
}
