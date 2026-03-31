package com.studio.vitalroute.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.api.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
//  Estado da UI — tudo o que o ecrã de Mapas precisa de saber
// ─────────────────────────────────────────────────────────────

data class WeatherInfo(
    val temperature: String,    // ex: "18°C"
    val condition: String,      // ex: "Céu limpo"
    val humidity: String,       // ex: "Humidade: 65%"
    val wind: String            // ex: "Vento: 12 km/h"
)

data class MapsUiState(
    val isLoading: Boolean = false,
    val weatherInfo: WeatherInfo? = null,
    val errorMessage: String? = null,
    // Coordenadas do centro do mapa (default: Aveiro)
    val centerLat: Double = 40.6405,
    val centerLon: Double = -8.6568
)

// ─────────────────────────────────────────────────────────────
//  MapsViewModel — gere o estado e as operações assíncronas
// ─────────────────────────────────────────────────────────────

class MapsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState())
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    private val weatherRepository = WeatherRepository()

    // Busca o tempo assim que o ViewModel é criado
    init {
        fetchWeather()
    }

    /**
     * Lança uma coroutine no viewModelScope para chamar a API de forma
     * assíncrona sem bloquear a thread principal (Main Thread).
     *
     * viewModelScope é automaticamente cancelado quando o ViewModel
     * é destruído → sem memory leaks.
     */
    fun fetchWeather(
        lat: Double = _uiState.value.centerLat,
        lon: Double = _uiState.value.centerLon
    ) {
        viewModelScope.launch {
            // 1. Mostrar loading
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 2. Chamada suspensa à API (corre em IO thread via Retrofit)
                val response = weatherRepository.getCurrentWeather(lat, lon)
                val data = response.current

                // 3. Atualizar estado com os dados recebidos
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weatherInfo = WeatherInfo(
                            temperature = "%.1f°C".format(data.temperature_2m),
                            condition   = weatherRepository.weatherCodeToDescription(data.weather_code),
                            humidity    = "💧 ${data.relative_humidity_2m}%",
                            wind        = "💨 %.0f km/h".format(data.wind_speed_10m)
                        )
                    )
                }
            } catch (e: Exception) {
                // 4. Tratar erros (sem internet, timeout, etc.)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Sem dados meteorológicos"
                    )
                }
            }
        }
    }
}
