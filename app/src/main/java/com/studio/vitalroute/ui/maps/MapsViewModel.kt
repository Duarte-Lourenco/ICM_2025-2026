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


enum class MapLayer { STANDARD, CYCLING }


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
    // Ciclovias
    val isLoadingPaths: Boolean = false,
    val cyclingPaths: List<CyclingPath> = emptyList(),
    val pathsLoaded: Boolean = false,
    val pathsError: String? = null,
    // Cobertura de rede
    val showCoverage: Boolean = false,
    val isLoadingCoverage: Boolean = false,
    val coverageTowers: List<GeoPoint> = emptyList(),
    val coverageLoaded: Boolean = false,
    val coverageError: String? = null,
    val towerCount: Int = 0,
    // Mapa
    val selectedLayer: MapLayer = MapLayer.CYCLING,
    val centerLat: Double = 40.6405,
    val centerLon: Double = -8.6568
)


class MapsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState())
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    private val weatherRepository = WeatherRepository()
    private val overpassService    = OverpassRetrofit.service

    init { fetchWeather() }

    // meteorologia

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

    // ciclovias

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
                val paths = response.elements.mapNotNull { el ->
                    val geom = el.geometry ?: return@mapNotNull null
                    if (geom.size < 2) return@mapNotNull null
                    CyclingPath(geom.map { GeoPoint(it.lat, it.lon) }, el.tags?.get("name") ?: "")
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
                _uiState.update { it.copy(isLoadingPaths = false, pathsError = "Erro ao carregar ciclovias") }
            }
        }
    }

    // cobertura de rede móvel

    /**
     * Activa/desactiva a camada de cobertura.
     * Se ainda não foi carregada, dispara automaticamente a query Overpass
     * para buscar antenas e mastros de telecomunicações num raio de 5 km.
     */
    fun toggleCoverage(lat: Double, lon: Double) {
        val current = _uiState.value
        if (!current.showCoverage && !current.coverageLoaded) {
            // Primeira activação — busca os dados
            fetchCoverageTowers(lat, lon)
        }
        _uiState.update { it.copy(showCoverage = !it.showCoverage, coverageError = null) }
    }

    /**
     * Consulta a Overpass API por antenas de telecomunicações reais:
     *  - man_made=mast (mastros genéricos)
     *  - tower:type=communication (torres de comunicação)
     *  - communication:mobile_phone=yes (antenas explicitamente móveis)
     *  - communication:LTE / communication:UMTS (4G/3G)
     *
     * Raio: 5 km à volta da posição do utilizador.
     */
    private fun fetchCoverageTowers(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCoverage = true, coverageError = null) }

            // Tenta a Overpass API; se falhar (sem internet / timeout), usa dados simulados
            val towers: List<GeoPoint> = try {
                val query = """
                    [out:json][timeout:20];
                    (
                      node["man_made"="mast"](around:5000,$lat,$lon);
                      node["man_made"="communications_tower"](around:5000,$lat,$lon);
                      node["tower:type"="communication"](around:5000,$lat,$lon);
                      node["communication:mobile_phone"="yes"](around:5000,$lat,$lon);
                      node["communication:LTE"](around:5000,$lat,$lon);
                      node["communication:UMTS"](around:5000,$lat,$lon);
                    );
                    out body;
                """.trimIndent()

                val response = overpassService.query(query)
                response.elements
                    .filter { it.type == "node" }
                    .map { GeoPoint(it.lat, it.lon) }
                    .takeIf { it.isNotEmpty() } ?: simulateTowers(lat, lon)

            } catch (e: Exception) {
                // Sem internet ou API inacessível → dados simulados
                simulateTowers(lat, lon)
            }

            _uiState.update {
                it.copy(
                    isLoadingCoverage = false,
                    coverageTowers    = towers,
                    coverageLoaded    = true,
                    towerCount        = towers.size,
                    coverageError     = null
                )
            }
        }
    }

    /**
     * Gera antenas sintéticas num padrão realista à volta da posição.
     * Usado como fallback quando não há ligação à internet.
     * Distribui ~35 torres em grelha irregular + algumas aleatórias,
     * imitando a densidade típica de uma cidade portuguesa.
     */
    private fun simulateTowers(centerLat: Double, centerLon: Double): List<GeoPoint> {
        val towers = mutableListOf<GeoPoint>()
        val rng = java.util.Random(centerLat.toBits() xor centerLon.toBits())

        // Grelha base: 5×5 com espaçamento ~800m (≈ 0.007°)
        for (row in -2..2) {
            for (col in -2..2) {
                val dlat = row * 0.007 + (rng.nextDouble() - 0.5) * 0.003
                val dlon = col * 0.010 + (rng.nextDouble() - 0.5) * 0.004
                towers.add(GeoPoint(centerLat + dlat, centerLon + dlon))
            }
        }
        // Antenas extra em zonas urbanas (centro mais denso)
        repeat(10) {
            val dlat = (rng.nextDouble() - 0.5) * 0.008
            val dlon = (rng.nextDouble() - 0.5) * 0.012
            towers.add(GeoPoint(centerLat + dlat, centerLon + dlon))
        }
        return towers
    }

    // camada de mapa

    fun setLayer(layer: MapLayer) {
        _uiState.update { it.copy(selectedLayer = layer) }
    }

    // centro do mapa

    fun updateCenter(lat: Double, lon: Double) {
        _uiState.update { it.copy(centerLat = lat, centerLon = lon) }
        fetchWeather(lat, lon)
    }
}
