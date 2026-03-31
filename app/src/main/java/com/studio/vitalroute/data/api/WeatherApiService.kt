package com.studio.vitalroute.data.api

import retrofit2.http.GET
import retrofit2.http.Query

// ─────────────────────────────────────────────────────────────
//  Modelos de resposta — Open-Meteo API (https://open-meteo.com)
//  Gratuita, sem autenticação, dados em tempo real
// ─────────────────────────────────────────────────────────────

data class WeatherResponse(
    val current: CurrentWeatherData
)

data class CurrentWeatherData(
    val temperature_2m: Double,
    val relative_humidity_2m: Int,
    val wind_speed_10m: Double,
    val weather_code: Int
)

// ─────────────────────────────────────────────────────────────
//  Interface Retrofit — define os endpoints da API
//  Cada função é uma suspend function → corre numa coroutine
// ─────────────────────────────────────────────────────────────

interface WeatherApiService {

    /**
     * Busca condições meteorológicas atuais para uma dada coordenada.
     *
     * Exemplo de URL gerado:
     * https://api.open-meteo.com/v1/forecast
     *   ?latitude=40.6405
     *   &longitude=-8.6568
     *   &current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code
     *   &timezone=auto
     */
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude")  lat: Double,
        @Query("longitude") lon: Double,
        @Query("current")   current: String = "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code",
        @Query("timezone")  timezone: String = "auto"
    ): WeatherResponse
}
