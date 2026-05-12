package com.studio.vitalroute.ui.home

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar



data class HomeUiState(
    val greeting: String = "Bom dia",
    val userName: String = "",
    val gpsEnabled: Boolean = false,
    val gpsStatus: String = "A verificar...",
    val batteryLevel: String = "—%",
    val isReady: Boolean = true,
    // Estatísticas semanais (calculadas a partir do Firestore)
    val weeklyKm: String = "0.0",
    val weeklyTime: String = "0 min",
    val weeklyIncidents: String = "0",
    val weeklyGoalProgress: Float = 0f,  // 0..1
    val weeklyGoalKm: Float = 100f,
    // Última atividade
    val lastActivityType: String = "",
    val lastActivityDate: String = "",
    val lastActivityDist: String = "—",
    val lastActivityTime: String = "—",
    val lastActivitySpeed: String = "—",
    val lastActivityElev: String = "—"
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val repository = FirestoreRepository()

    init {
        updateGreeting()
        loadUserName()
        loadActivities()
        loadSettings()
        loadDeviceStatus()
    }

    private fun loadDeviceStatus() {
        val ctx = getApplication<Application>()

        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }

        _uiState.update {
            it.copy(
                batteryLevel = if (level >= 0) "$level%" else "—%",
                gpsEnabled   = gpsEnabled,
                gpsStatus    = if (gpsEnabled) "Ativo" else "Inativo"
            )
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getSettingsFlow()
                .catch { }
                .collect { settings ->
                    _uiState.update { s ->
                        s.copy(
                            weeklyGoalKm       = settings.weeklyGoalKm,
                            weeklyGoalProgress = (s.weeklyKm.toFloatOrNull() ?: 0f) / settings.weeklyGoalKm
                        )
                    }
                }
        }
    }

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Bom dia 🌅"
            hour < 19 -> "Boa tarde ☀️"
            else      -> "Boa noite 🌙"
        }
        _uiState.update { it.copy(greeting = greeting) }
    }

    private fun loadUserName() {
        val name = Firebase.auth.currentUser?.displayName
            ?: Firebase.auth.currentUser?.email?.substringBefore("@")
            ?: ""
        _uiState.update { it.copy(userName = name) }
    }

    private fun loadActivities() {
        viewModelScope.launch {
            repository.getActivities()
                .catch { /* sem autenticação ou erro de rede */ }
                .collect { activities ->
                    val weeklyStats = computeWeeklyStats(activities)
                    val last = activities.firstOrNull()

                    _uiState.update { state ->
                        state.copy(
                            weeklyKm       = "%.1f".format(weeklyStats.first),
                            weeklyTime     = "${weeklyStats.second} min",
                            weeklyGoalProgress = (weeklyStats.first.toFloat() / state.weeklyGoalKm).coerceIn(0f, 1f),
                            lastActivityType  = last?.type ?: "",
                            lastActivityDate  = last?.let { formatDate(it.startTime) } ?: "",
                            lastActivityDist  = last?.let { "%.1f km".format(it.distanceKm) } ?: "—",
                            lastActivityTime  = last?.let { "${it.durationSeconds / 60} min" } ?: "—",
                            lastActivitySpeed = last?.let { "%.1f km/h".format(it.avgSpeedKmh) } ?: "—",
                            lastActivityElev  = last?.let { "${it.elevationM} m" } ?: "—"
                        )
                    }
                }
        }
    }

    private fun computeWeeklyStats(activities: List<Activity>): Pair<Double, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val weekStart = cal.timeInMillis

        val thisWeek = activities.filter { it.startTime >= weekStart }
        val km  = thisWeek.sumOf { it.distanceKm }
        val min = thisWeek.sumOf { it.durationSeconds } / 60

        return Pair(km, min)
    }

    private fun formatDate(ms: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val months = listOf("Jan","Fev","Mar","Abr","Mai","Jun",
                            "Jul","Ago","Set","Out","Nov","Dez")
        return "$day ${months[cal.get(Calendar.MONTH)]}"
    }
}