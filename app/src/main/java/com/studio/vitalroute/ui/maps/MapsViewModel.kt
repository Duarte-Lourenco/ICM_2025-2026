package com.studio.vitalroute.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.api.OverpassRetrofit
import com.studio.vitalroute.data.api.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

// ─────────────────────────────────────────────────────────────
//  Camadas de mapa disponíveis
// ─────────────────────────────────────────────────────────────

enum class MapLayer { STANDARD, CYCLING }

// ─────────────────────────────────────────────────────────────
//  Modelos de dados da UI
// ─────────────────────────────────────────────────────────────

data class WeatherInfo(
    val temperature: String,
    val condition: String,
    val humidity: String,
    val wind: String
)

data class CyclingPath(
    val points: List<GeoPoint>,
    val name: String = ""
)

data class MapsUiState(
    // Meteo
    val isLoadingWeather: Boolean = true,
    val weatherInfo: WeatherInfo? = null,
    val weatherError: String? = null,
    // Percursos
    val isLoadingPaths: Boolean = false,
    val cyclingPaths: List<CyclingPath> = emptyList(),
    val pathsLoaded: Boolean = false,
    val pathsError: String? = null,
    // Mapa
    val selectedLayer: MapLayer = MapLayer.CYCLING,
    val centerLat: Double = 40.6405,
    val centerLon: Double = -8.6568
)

// ─────────────────────────────────────────────────────────────
//  MapsViewModel
// ─────────────────────────────────────────────────────────────

class MapsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState())
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    private val weatherRepository = WeatherRepository()
    private val overpassService = OverpassRetrofit.service

    init {
        fetchWeather()
    }

    // ── Meteorologia ─────────────────────────────────────────

    fun fetchWeather(
        lat: Double = _uiState.value.centerLat,
        lon: Double = _uiState.value.centerLon
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeather = true, weatherError = null) }
            try {
                val response = weatherRepository.getCurrentWeather(lat, lon)
                val data = response.current
                _uiState.update {
                    it.copy(
                        isLoadingWeather = false,
                        weatherInfo = WeatherInfo(
                            temperature = "%.1f°C".format(data.temperature_2m),
                            condition   = weatherRepository.weatherCodeToDescription(data.weather_code),
                            humidity    = "💧 ${data.relative_humidity_2m}%",
                            wind        = "💨 %.0f km/h".format(data.wind_speed_10m)
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingWeather = false, weatherError = "Sem dados meteo") }
            }
        }
    }

    // ── Ciclovias via Overpass API ───────────────────────────

    /**
     * Consulta a Overpass API (OpenStreetMap) para buscar ciclovias
     * num raio de 3 km à volta da posição dada.
     *
     * Query Overpass QL:
     *  - highway=cycleway → ciclovias dedicadas
     *  - highway=path + bicycle=designated/yes → trilhos ciclável
     *  - highway=track + bicycle=yes → caminhos rurais cicláveis
     */
    fun fetchCyclingPaths(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPaths = true, pathsError = null) }
            try {
                val query = """
                    [out:json][timeout:25];
                    (
                      way["highway"="cycleway"](around:3000,$lat,$lon);
                      way["highway"="path"]["bicycle"="designated"](around:3000,$lat,$lon);
                      way["highway"="path"]["bicycle"="yes"](around:3000,$lat,$lon);
                      way["highway"="track"]["bicycle"="yes"](around:3000,$lat,$lon);
                    );
                    out geom;
                """.trimIndent()

                val response = overpassService.query(query)

                val paths = response.elements.mapNotNull { element ->
                    val geom = element.geometry ?: return@mapNotNull null
                    if (geom.size < 2) return@mapNotNull null
                    CyclingPath(
                        points = geom.map { GeoPoint(it.lat, it.lon) },
                        name   = element.tags?.get("name") ?: ""
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoadingPaths = false,
                        cyclingPaths   = paths,
                        pathsLoaded    = true,
                        pathsError     = if (paths.isEmpty()) "Nenhuma ciclovia encontrada aqui" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingPaths = false, pathsError = "Erro ao carregar ciclovias")
                }
            }
        }
    }

    // ── Camada do mapa ────────────────────────────────────────

    fun setLayer(layer: MapLayer) {
        _uiState.update { it.copy(selectedLayer = layer) }
    }

    // ── Centro do mapa ────────────────────────────────────────

    fun updateCenter(lat: Double, lon: Double) {
        _uiState.update { it.copy(centerLat = lat, centerLon = lon) }
        fetchWeather(lat, lon)
    }
}
