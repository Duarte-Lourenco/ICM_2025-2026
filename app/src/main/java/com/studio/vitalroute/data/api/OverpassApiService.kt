package com.studio.vitalroute.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


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


interface OverpassApiService {
    @FormUrlEncoded
    @POST("api/interpreter")
    suspend fun query(@Field("data") query: String): OverpassResponse
}


object OverpassRetrofit {
    val service: OverpassApiService by lazy {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Accept", "*/*")
                    .header("User-Agent", "VitalRoute/1.0 (Android)")
                    .build()
                chain.proceed(req)
            }
            .build()
        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApiService::class.java)
    }
}
