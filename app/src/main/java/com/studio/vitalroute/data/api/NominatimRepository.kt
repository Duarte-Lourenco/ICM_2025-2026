package com.studio.vitalroute.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// ─────────────────────────────────────────────────────────────
//  NominatimRepository — geocoding de endereços via Nominatim
//  (OpenStreetMap). Gratuito, sem autenticação.
//
//  Endpoint: https://nominatim.openstreetmap.org/search
// ─────────────────────────────────────────────────────────────

object NominatimRepository {

    data class GeoResult(val lat: Double, val lng: Double, val displayName: String)

    /**
     * Geocodifica um endereço em texto livre e devolve o primeiro resultado.
     * Devolve null se não encontrar nenhum resultado ou em caso de erro.
     * Deve ser chamado a partir de uma coroutine.
     */
    suspend fun geocode(address: String): GeoResult? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(address.trim(), "UTF-8")
            val url = URL(
                "https://nominatim.openstreetmap.org/search" +
                "?q=$encoded&format=json&limit=1&addressdetails=0"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout    = 8_000
                // Nominatim exige um User-Agent identificativo
                setRequestProperty("User-Agent", "VitalRoute/1.0 (pt.ua.vitalroute)")
            }

            if (conn.responseCode != 200) return@withContext null

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val arr = JSONArray(body)
            if (arr.length() == 0) return@withContext null

            val obj = arr.getJSONObject(0)
            GeoResult(
                lat         = obj.getDouble("lat"),
                lng         = obj.getDouble("lon"),
                displayName = obj.optString("display_name", address)
            )
        } catch (_: Exception) {
            null
        }
    }
}
