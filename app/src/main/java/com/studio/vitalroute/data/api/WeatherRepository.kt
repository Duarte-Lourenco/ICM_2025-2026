package com.studio.vitalroute.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class WeatherRepository {

    // instancia singleton do retrofit configurada para a open meteo
    private val api: WeatherApiService = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)

    /**
     * busca o tempo atual para as coordenadas fornecidas
     * e uma suspend function deve ser chamada dentro de uma coroutine
     * lanca excecao em caso de erro de rede
     */
    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherResponse {
        return api.getCurrentWeather(lat = lat, lon = lon)
    }

    /**
     * converte o weather code wmo numa descricao legivel
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
