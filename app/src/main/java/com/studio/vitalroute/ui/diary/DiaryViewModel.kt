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

// ─────────────────────────────────────────────────────────────
//  Modelos UI do Diário
// ─────────────────────────────────────────────────────────────

data class ActivityUiItem(
    val id: String,
    val type: String,           // "cycling" | "running"
    val dateLabel: String,      // ex: "15 Abr 2025"
    val timeLabel: String,      // ex: "Manhã · 45 min"
    val distance: String,       // ex: "12.4 km"
    val duration: String,       // ex: "45 min"
    val speed: String,          // ex: "16.5 km/h"
    val elevation: String       // ex: "120 m"
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
    val isLoading: Boolean        = true,
    val activities: List<ActivityUiItem> = emptyList(),
    val monthlySummary: MonthlySummary = MonthlySummary(),
    val personalBests: PersonalBests   = PersonalBests()
)

// ─────────────────────────────────────────────────────────────
//  DiaryViewModel
// ─────────────────────────────────────────────────────────────

class DiaryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    private val repository = FirestoreRepository()

    init {
        loadActivities()
    }

    private fun loadActivities() {
        viewModelScope.launch {
            repository.getActivities()
                .catch {
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { activities ->
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

    // ── Conversão Activity → UI ───────────────────────────────

    private fun Activity.toUiItem(): ActivityUiItem {
        val dateStr = SimpleDateFormat("d MMM yyyy", Locale("pt", "PT"))
            .format(Date(startTime))
        val durationMin = durationSeconds / 60
        val period = when (Calendar.getInstance().apply { timeInMillis = startTime }.get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Manhã"
            in 12..17 -> "Tarde"
            else      -> "Noite"
        }
        return ActivityUiItem(
            id         = id,
            type       = type,
            dateLabel  = dateStr,
            timeLabel  = "$period · $durationMin min",
            distance   = "%.1f km".format(distanceKm),
            duration   = "$durationMin min",
            speed      = "%.1f km/h".format(avgSpeedKmh),
            elevation  = "$elevationM m"
        )
    }

    // ── Cálculo de resumo mensal ──────────────────────────────

    private fun computeMonthlySummary(activities: List<Activity>): MonthlySummary {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear  = cal.get(Calendar.YEAR)

        val thisMonth = activities.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.startTime }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }

        return MonthlySummary(
            totalKm        = "%.1f".format(thisMonth.sumOf { it.distanceKm }),
            totalElevation = "${thisMonth.sumOf { it.elevationM }}",
            totalMinutes   = "${thisMonth.sumOf { it.durationSeconds } / 60}",
            incidents      = "0"
        )
    }

    // ── Cálculo de recordes pessoais ──────────────────────────

    private fun computePersonalBests(activities: List<Activity>): PersonalBests {
        if (activities.isEmpty()) return PersonalBests()
        return PersonalBests(
            longestRide     = "%.1f km".format(activities.maxOf { it.distanceKm }),
            topSpeed        = "%.1f km/h".format(activities.maxOf { it.avgSpeedKmh }),
            longestDuration = "${activities.maxOf { it.durationSeconds } / 60} min",
            mostElevation   = "${activities.maxOf { it.elevationM }} m"
        )
    }
}
