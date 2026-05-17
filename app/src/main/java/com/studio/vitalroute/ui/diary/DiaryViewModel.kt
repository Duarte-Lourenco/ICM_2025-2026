package com.studio.vitalroute.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

// Referência às Activities Firestore (para exportação)
// O ViewModel mantém a lista raw para passar ao ActivityExporter



data class ActivityUiItem(
    val id: String,
    val type: String,                           // "cycling" | "running" | "walking"
    val dateLabel: String,                      // ex: "15 Abr 2025"
    val timeLabel: String,                      // ex: "Manhã · 45 min"
    val distance: String,                       // ex: "12.4 km"
    val duration: String,                       // ex: "45 min"
    val speed: String,                          // ex: "16.5 km/h"
    val elevation: String,                      // ex: "120 m"
    val elevationM: Int = 0,                    // valor numérico para o gráfico
    val elevationPoints: List<Int> = emptyList(), // amostras de altitude para curva
    val routePoints: List<String>  = emptyList()  // "lat,lng" para mini-mapa
)

data class MonthlySummary(
    val totalKm: String     = "0.0",
    val totalElevation: String = "0",
    val totalMinutes: String = "0",
    val incidents: String   = "0"
)

data class PersonalBests(
    val longestRide: String  = "—",
    val topSpeed: String     = "—",
    val longestDuration: String = "—",
    val mostElevation: String   = "—"
)

data class DiaryUiState(
    val isLoading: Boolean               = true,
    val activities: List<ActivityUiItem> = emptyList(),
    val monthlySummary: MonthlySummary   = MonthlySummary(),
    val personalBests: PersonalBests     = PersonalBests(),
    val showExportMenu: Boolean          = false,
    val distUnit: String                 = "km"
)


class DiaryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    private val repository = FirestoreRepository()

    private var rawActivities: List<Activity> = emptyList()
    private var useMetric = true

    init {
        loadSettings()
        loadActivities()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getSettingsFlow()
                .catch { }
                .collect { settings ->
                    useMetric = settings.metricSystem
                    _uiState.update { it.copy(distUnit = if (useMetric) "km" else "mi") }
                    if (rawActivities.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                activities     = rawActivities.map { a -> a.toUiItem() },
                                monthlySummary = computeMonthlySummary(rawActivities),
                                personalBests  = computePersonalBests(rawActivities)
                            )
                        }
                    }
                }
        }
    }

    fun getRawActivities(): List<Activity> = rawActivities

    fun showExportMenu()  { _uiState.update { it.copy(showExportMenu = true) } }
    fun hideExportMenu()  { _uiState.update { it.copy(showExportMenu = false) } }

    private fun loadActivities() {
        viewModelScope.launch {
            repository.getActivities()
                .catch {
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { activities ->
                    rawActivities = activities
                    _uiState.update {
                        it.copy(
                            isLoading      = false,
                            activities     = activities.map { a -> a.toUiItem() },
                            monthlySummary = computeMonthlySummary(activities),
                            personalBests  = computePersonalBests(activities)
                        )
                    }
                }
        }
    }

    // conversão activity → ui

    private fun Activity.toUiItem(): ActivityUiItem {
        val dateStr = SimpleDateFormat("d MMM yyyy", Locale("pt", "PT"))
            .format(Date(startTime))
        val durationMin = durationSeconds / 60
        val period = when (Calendar.getInstance().apply { timeInMillis = startTime }.get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Manhã"
            in 12..17 -> "Tarde"
            else      -> "Noite"
        }
        val metric = useMetric
        val dist  = if (metric) distanceKm  else distanceKm  * 0.621371
        val speed = if (metric) avgSpeedKmh else avgSpeedKmh * 0.621371
        return ActivityUiItem(
            id              = id,
            type            = type,
            dateLabel       = dateStr,
            timeLabel       = "$period · $durationMin min",
            distance        = "%.1f ${if (metric) "km" else "mi"}".format(dist),
            duration        = "$durationMin min",
            speed           = "%.1f ${if (metric) "km/h" else "mph"}".format(speed),
            elevation       = "$elevationM m",
            elevationM      = elevationM,
            elevationPoints = elevationPoints,
            routePoints     = routePoints
        )
    }

    // cálculo de resumo mensal

    private fun computeMonthlySummary(activities: List<Activity>): MonthlySummary {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear  = cal.get(Calendar.YEAR)

        val thisMonth = activities.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.startTime }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }

        val totalDistKm = thisMonth.sumOf { it.distanceKm }
        val totalDist   = if (useMetric) totalDistKm else totalDistKm * 0.621371
        return MonthlySummary(
            totalKm        = "%.1f".format(totalDist),
            totalElevation = "${thisMonth.sumOf { it.elevationM }}",
            totalMinutes   = "${thisMonth.sumOf { it.durationSeconds } / 60}",
            incidents      = "0"
        )
    }

    // cálculo de recordes pessoais

    private fun computePersonalBests(activities: List<Activity>): PersonalBests {
        if (activities.isEmpty()) return PersonalBests()
        val metric = useMetric
        val maxDist  = activities.maxOf { it.distanceKm }
        val maxSpeed = activities.maxOf { it.avgSpeedKmh }
        return PersonalBests(
            longestRide     = "%.1f ${if (metric) "km" else "mi"}".format(if (metric) maxDist  else maxDist  * 0.621371),
            topSpeed        = "%.1f ${if (metric) "km/h" else "mph"}".format(if (metric) maxSpeed else maxSpeed * 0.621371),
            longestDuration = "${activities.maxOf { it.durationSeconds } / 60} min",
            mostElevation   = "${activities.maxOf { it.elevationM }} m"
        )
    }
}
