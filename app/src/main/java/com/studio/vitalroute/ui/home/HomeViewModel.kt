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
    val weeklyGoalProgress: Float = 0f,
    val weeklyGoalKm: Float = 100f,
    val distUnit: String = "km",
    val weeklyGoalDisplay: String = "100",
    // Última atividade
    val lastActivityId: String = "",
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

    private var useMetric = true
    private var cachedActivities: List<Activity> = emptyList()

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getSettingsFlow()
                .catch { }
                .collect { settings ->
                    useMetric = settings.metricSystem
                    val goalDisplay = if (useMetric) settings.weeklyGoalKm
                                     else settings.weeklyGoalKm * 0.621371f
                    _uiState.update { s ->
                        s.copy(
                            weeklyGoalKm      = settings.weeklyGoalKm,
                            weeklyGoalDisplay = "%.0f".format(goalDisplay),
                            distUnit          = if (useMetric) "km" else "mi"
                        )
                    }
                    // Reformatar atividades com nova unidade
                    if (cachedActivities.isNotEmpty()) formatActivities(cachedActivities)
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
        // Tenta primeiro o Firestore (fonte primária do nome)
        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile()
                val name = profile?.name?.takeIf { it.isNotBlank() }
                    ?: Firebase.auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: Firebase.auth.currentUser?.email?.substringBefore("@")
                    ?: ""
                _uiState.update { it.copy(userName = name) }
            } catch (_: Exception) {
                val fallback = Firebase.auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: Firebase.auth.currentUser?.email?.substringBefore("@")
                    ?: ""
                _uiState.update { it.copy(userName = fallback) }
            }
        }
    }

    private fun loadActivities() {
        viewModelScope.launch {
            repository.getActivities()
                .catch { }
                .collect { activities ->
                    cachedActivities = activities
                    formatActivities(activities)
                }
        }
    }

    private fun formatActivities(activities: List<Activity>) {
        val weeklyStats = computeWeeklyStats(activities)
        val last = activities.firstOrNull()
        val metric = useMetric

        _uiState.update { state ->
            val weeklyDist  = if (metric) weeklyStats.first else weeklyStats.first * 0.621371
            val goalDisplay = state.weeklyGoalDisplay.toFloatOrNull()
                ?: if (metric) state.weeklyGoalKm else state.weeklyGoalKm * 0.621371f
            state.copy(
                weeklyKm           = "%.1f".format(weeklyDist),
                weeklyTime         = "${weeklyStats.second} min",
                weeklyGoalProgress = (weeklyDist.toFloat() / goalDisplay.coerceAtLeast(1f)).coerceIn(0f, 1f),
                lastActivityId     = last?.id ?: "",
                lastActivityType   = last?.type ?: "",
                lastActivityDate   = last?.let { formatDate(it.startTime) } ?: "",
                lastActivityDist   = last?.let {
                    val d = if (metric) it.distanceKm else it.distanceKm * 0.621371
                    "%.1f ${if (metric) "km" else "mi"}".format(d)
                } ?: "—",
                lastActivityTime   = last?.let { "${it.durationSeconds / 60} min" } ?: "—",
                lastActivitySpeed  = last?.let {
                    val sp = if (metric) it.avgSpeedKmh else it.avgSpeedKmh * 0.621371
                    "%.1f ${if (metric) "km/h" else "mph"}".format(sp)
                } ?: "—",
                lastActivityElev   = last?.let { "${it.elevationM} m" } ?: "—"
            )
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