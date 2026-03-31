package com.studio.vitalroute.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ─────────────────────────────────────────────────────────────
//  WeatherRepository — abstrai a origem dos dados
//  A ViewModel não sabe se os dados vêm da rede, cache, etc.
// ─────────────────────────────────────────────────────────────

class WeatherRepository {

    // Instância singleton do Retrofit configurada para a Open-Meteo
    private val api: WeatherApiService = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)

    /**
     * Busca o tempo atual para as coordenadas fornecidas.
     * É uma suspend function → deve ser chamada dentro de uma coroutine.
     * Lança exceção em caso de erro de rede.
     */
    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherResponse {
        return api.getCurrentWeather(lat = lat, lon = lon)
    }

    /**
     * Converte o weather_code WMO numa descrição legível.
     * https://open-meteo.com/en/docs#weathervariables
     */
    fun weatherCodeToDescription(code: Int): String = when (code) {
        0            -> "Céu limpo"
        in 1..3      -> "Parcialmente nublado"
        in 45..48    -> "Nevoeiro"
        in 51..67    -> "Chuva"
        in 71..77    -> "Neve"
        in 80..82    -> "Aguaceiros"
        in 95..99    -> "Trovoada"
        else         -> "Variável"
    }
}
