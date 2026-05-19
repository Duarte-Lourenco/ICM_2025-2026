package com.studio.vitalroute.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.Destination
import com.studio.vitalroute.data.DestinationManager
import com.studio.vitalroute.data.api.OverpassRetrofit
import com.studio.vitalroute.data.api.WeatherRepository
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.FirestoreSafeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint


enum class MapLayer { STANDARD, CYCLING }

enum class PathType { CYCLEWAY, PATH, TRACK }

data class WeatherInfo(
    val icon: String,
    val temperature: String,
    val condition: String,
    val humidity: String,
    val wind: String
)

data class CyclingPath(
    val points: List<GeoPoint>,
    val name: String = "",
    val type: PathType = PathType.CYCLEWAY
)

data class MapsUiState(
    val isLoadingWeather: Boolean = true,
    val weatherInfo: WeatherInfo? = null,
    val isLoadingPaths: Boolean = false,
    val cyclingPaths: List<CyclingPath> = emptyList(),
    val pathsLoaded: Boolean = false,
    val pathsError: String? = null,
    val cyclewayCount: Int = 0,
    val pathCount: Int = 0,
    val trackCount: Int = 0,
    val safeZones: List<FirestoreSafeZone> = emptyList(),
    val isAddingZone: Boolean = false,
    val showZoneNameDialog: Boolean = false,
    val pendingZoneLat: Double = 0.0,
    val pendingZoneLng: Double = 0.0,
    val pendingZoneName: String = "",
    val pendingZoneRadius: Int = 150,
    val isSavingZone: Boolean = false,
    val selectedZone: FirestoreSafeZone? = null,
    val editingRadius: Int = 150,
    val editingColor: String = "#FF6F00",
    val showDeleteConfirm: Boolean = false,
    val selectedLayer: MapLayer = MapLayer.STANDARD,
    val searchRadiusM: Int = 5000,
    val centerLat: Double = 40.6405,
    val centerLon: Double = -8.6568,
    val showDestinationSheet: Boolean = false,
    val pendingDestLat: Double = 0.0,
    val pendingDestLng: Double = 0.0,
    val pendingDestRadius: Int = 150,
    val activeDestination: Destination? = null,
    val showDestinationInfo: Boolean = false
)


class MapsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState())
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    private val weatherRepository = WeatherRepository()
    private val overpassService   = OverpassRetrofit.service
    private val repository        = FirestoreRepository()

    init {
        fetchWeather()
        loadSafeZones()
        viewModelScope.launch {
            DestinationManager.destination.collect { dest ->
                _uiState.update { it.copy(activeDestination = dest) }
            }
        }
    }

    fun fetchWeather(
        lat: Double = _uiState.value.centerLat,
        lon: Double = _uiState.value.centerLon
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeather = true) }
            try {
                val response = weatherRepository.getCurrentWeather(lat, lon)
                val data = response.current
                val icon = weatherCodeToIcon(data.weather_code)
                _uiState.update {
                    it.copy(
                        isLoadingWeather = false,
                        weatherInfo = WeatherInfo(
                            icon        = icon,
                            temperature = "%.0f°C".format(data.temperature_2m),
                            condition   = weatherRepository.weatherCodeToDescription(data.weather_code),
                            humidity    = "💧 ${data.relative_humidity_2m}%",
                            wind        = "💨 %.0f km/h".format(data.wind_speed_10m)
                        )
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingWeather = false) }
            }
        }
    }

    private fun weatherCodeToIcon(code: Int): String = when (code) {
        0           -> "☀️"
        1, 2        -> "⛅"
        3           -> "☁️"
        45, 48      -> "🌫️"
        in 51..67   -> "🌧️"
        in 71..77   -> "❄️"
        in 80..82   -> "🌦️"
        in 95..99   -> "⛈️"
        else        -> "🌡️"
    }

    fun fetchCyclingPaths(
        lat: Double = _uiState.value.centerLat,
        lon: Double = _uiState.value.centerLon
    ) {
        val radius = _uiState.value.searchRadiusM
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPaths = true, pathsError = null) }
            try {
                val query = """
                    [out:json][timeout:30];
                    (
                      way["highway"="cycleway"](around:$radius,$lat,$lon);
                      way["highway"="path"]["bicycle"="designated"](around:$radius,$lat,$lon);
                      way["highway"="path"]["bicycle"="yes"](around:$radius,$lat,$lon);
                      way["highway"="footway"]["bicycle"="yes"](around:$radius,$lat,$lon);
                      way["highway"="track"]["bicycle"="yes"](around:$radius,$lat,$lon);
                      way["highway"="track"]["bicycle"="designated"](around:$radius,$lat,$lon);
                    );
                    out geom;
                """.trimIndent()
                val response = overpassService.query(query)
                val paths = response.elements.mapNotNull { el ->
                    val geom = el.geometry ?: return@mapNotNull null
                    if (geom.size < 2) return@mapNotNull null
                    val hw = el.tags?.get("highway") ?: ""
                    val bicycle = el.tags?.get("bicycle") ?: ""
                    val type = when {
                        hw == "cycleway" -> PathType.CYCLEWAY
                        hw == "track"    -> PathType.TRACK
                        bicycle == "designated" && hw != "cycleway" -> PathType.CYCLEWAY
                        else             -> PathType.PATH
                    }
                    CyclingPath(
                        points = geom.map { GeoPoint(it.lat, it.lon) },
                        name   = el.tags?.get("name") ?: "",
                        type   = type
                    )
                }
                _uiState.update {
                    it.copy(
                        isLoadingPaths = false,
                        cyclingPaths   = paths,
                        pathsLoaded    = true,
                        cyclewayCount  = paths.count { p -> p.type == PathType.CYCLEWAY },
                        pathCount      = paths.count { p -> p.type == PathType.PATH },
                        trackCount     = paths.count { p -> p.type == PathType.TRACK },
                        pathsError     = if (paths.isEmpty()) "Sem ciclovias nesta área" else null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("MapsViewModel", "fetchCyclingPaths [${e::class.simpleName}]: ${e.message}")
                _uiState.update {
                    it.copy(isLoadingPaths = false, pathsError = "${e::class.simpleName}: ${e.message?.take(80)}")
                }
            }
        }
    }

    fun setSearchRadius(meters: Int) {
        _uiState.update { it.copy(searchRadiusM = meters, pathsLoaded = false) }
        fetchCyclingPaths()
    }

    private fun loadSafeZones() {
        viewModelScope.launch {
            repository.getSafeZones()
                .catch { }
                .collect { zones -> _uiState.update { it.copy(safeZones = zones) } }
        }
    }

    fun enterAddZoneMode() { _uiState.update { it.copy(isAddingZone = true) } }

    fun exitAddZoneMode()  {
        _uiState.update { it.copy(isAddingZone = false, showZoneNameDialog = false, pendingZoneName = "") }
    }

    fun onMapTapped(lat: Double, lon: Double) {
        when {
            _uiState.value.isAddingZone -> _uiState.update {
                it.copy(pendingZoneLat = lat, pendingZoneLng = lon, showZoneNameDialog = true, pendingZoneName = "")
            }
            _uiState.value.selectedZone == null -> _uiState.update {
                it.copy(showDestinationSheet = true, pendingDestLat = lat, pendingDestLng = lon)
            }
        }
    }

    fun setAsDestination() {
        val s = _uiState.value
        DestinationManager.set(s.pendingDestLat, s.pendingDestLng, radiusM = s.pendingDestRadius)
        _uiState.update { it.copy(showDestinationSheet = false) }
    }

    fun setPendingDestRadius(r: Int) { _uiState.update { it.copy(pendingDestRadius = r) } }

    fun dismissDestinationSheet() {
        _uiState.update { it.copy(showDestinationSheet = false) }
    }

    fun setZoneAsDestination(zone: FirestoreSafeZone) {
        DestinationManager.set(zone.lat, zone.lng, zone.name, zone.radiusM)
        _uiState.update { it.copy(selectedZone = null, showDeleteConfirm = false) }
    }

    fun clearDestination() {
        DestinationManager.clear()
        _uiState.update { it.copy(showDestinationInfo = false) }
    }

    fun showDestinationInfo()   { _uiState.update { it.copy(showDestinationInfo = true) } }
    fun dismissDestinationInfo() { _uiState.update { it.copy(showDestinationInfo = false) } }

    fun updatePendingZoneName(name: String) { _uiState.update { it.copy(pendingZoneName = name) } }
    fun setPendingZoneRadius(r: Int)         { _uiState.update { it.copy(pendingZoneRadius = r) } }

    fun dismissZoneDialog() {
        _uiState.update { it.copy(showZoneNameDialog = false, isAddingZone = false, pendingZoneName = "") }
    }

    fun selectZone(zoneId: String) {
        val zone = _uiState.value.safeZones.find { it.id == zoneId } ?: return
        _uiState.update { it.copy(
            selectedZone      = zone,
            editingRadius     = zone.radiusM,
            editingColor      = zone.color,
            showDeleteConfirm = false
        )}
    }

    fun deselectZone() {
        _uiState.update { it.copy(selectedZone = null, showDeleteConfirm = false) }
    }

    fun updateEditingRadius(r: Int)      { _uiState.update { it.copy(editingRadius = r) } }
    fun updateEditingColor(color: String) { _uiState.update { it.copy(editingColor = color) } }
    fun toggleDeleteConfirm()            { _uiState.update { it.copy(showDeleteConfirm = !it.showDeleteConfirm) } }

    fun saveZoneEdits() {
        val s    = _uiState.value
        val zone = s.selectedZone ?: return
        viewModelScope.launch {
            try {
                repository.saveSafeZone(zone.copy(radiusM = s.editingRadius, color = s.editingColor))
                _uiState.update { it.copy(selectedZone = null) }
            } catch (_: Exception) {}
        }
    }

    fun deleteSelectedZone() {
        val zone = _uiState.value.selectedZone ?: return
        viewModelScope.launch {
            try {
                repository.deleteSafeZone(zone.id)
                _uiState.update { it.copy(selectedZone = null, showDeleteConfirm = false) }
            } catch (_: Exception) {}
        }
    }

    fun savePendingZone() {
        val s = _uiState.value
        if (s.pendingZoneName.isBlank()) return
        _uiState.update { it.copy(isSavingZone = true) }
        viewModelScope.launch {
            try {
                repository.saveSafeZone(
                    FirestoreSafeZone(
                        name    = s.pendingZoneName.trim(),
                        address = "",
                        lat     = s.pendingZoneLat,
                        lng     = s.pendingZoneLng,
                        radiusM = s.pendingZoneRadius
                    )
                )
                _uiState.update { it.copy(isSavingZone = false, showZoneNameDialog = false, isAddingZone = false, pendingZoneName = "") }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSavingZone = false) }
            }
        }
    }

    fun setLayer(layer: MapLayer) {
        _uiState.update { it.copy(selectedLayer = layer) }
        if (layer == MapLayer.CYCLING && !_uiState.value.pathsLoaded) {
            fetchCyclingPaths()
        }
    }

    fun updateCenter(lat: Double, lon: Double) {
        _uiState.update { it.copy(centerLat = lat, centerLon = lon) }
        fetchWeather(lat, lon)
    }

    fun refreshPaths() {
        _uiState.update { it.copy(pathsLoaded = false) }
        fetchCyclingPaths()
    }
}
