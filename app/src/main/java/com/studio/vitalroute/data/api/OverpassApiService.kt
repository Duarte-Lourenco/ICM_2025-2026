package com.studio.vitalroute.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// ─────────────────────────────────────────────────────────────
//  Modelos de dados da Overpass API (OpenStreetMap)
// ─────────────────────────────────────────────────────────────

data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

data class OverpassElement(
    val type: String = "",
    val id: Long = 0,
    // Coordenadas diretas (nós/nodes — ex: antenas, mastros)
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val tags: Map<String, String>? = null,
    // Geometria de linha (ways — ex: ciclovias)
    val geometry: List<OverpassGeomPoint>? = null
)

data class OverpassGeomPoint(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

// ─────────────────────────────────────────────────────────────
//  Interface Retrofit — Overpass API
//  Base URL: https://overpass-api.de/
//  Aceita queries em Overpass QL via POST form-encoded
// ─────────────────────────────────────────────────────────────

interface OverpassApiService {
    @FormUrlEncoded
    @POST("api/interpreter")
    suspend fun query(@Field("data") query: String): OverpassResponse
}

// ─────────────────────────────────────────────────────────────
//  Singleton Retrofit para Overpass
// ─────────────────────────────────────────────────────────────

object OverpassRetrofit {
    val service: OverpassApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApiService::class.java)
    }
}
