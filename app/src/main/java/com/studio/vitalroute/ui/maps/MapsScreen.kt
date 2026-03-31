package com.studio.vitalroute.ui.maps

import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import com.studio.vitalroute.ui.theme.*

// ─────────────────────────────────────────────────────────────
//  MapsScreen — Mapa OpenStreetMap + dados da API meteorológica
//
//  NOTA: Esta implementação usa OSMDroid (OpenStreetMap), que
//  funciona sem API key. Para usar Mapbox:
//   1. Obtém tokens em account.mapbox.com
//   2. Adiciona MAPBOX_DOWNLOADS_TOKEN a ~/.gradle/gradle.properties
//   3. Descomentar o repo em settings.gradle.kts
//   4. Substituir as dependências no build.gradle.kts
//   5. Substituir AndroidView por MapboxMap composable
// ─────────────────────────────────────────────────────────────

@Composable
fun MapsScreen(
    viewModel: MapsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Configura o OSMDroid (user agent obrigatório)
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Mapa OSMDroid via AndroidView ─────────────────────
        // AndroidView integra Views tradicionais do Android no Compose
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    // Estilo escuro (USGS Topo como alternativa dark)
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    // Posição inicial: Aveiro
                    controller.setZoom(14.0)
                    controller.setCenter(GeoPoint(40.6405, -8.6568))

                    // Overlay de localização (seta de posição atual)
                    val locationOverlay = MyLocationNewOverlay(
                        GpsMyLocationProvider(ctx), this
                    )
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)
                }
            },
            update = { mapView ->
                // Chamado sempre que o estado muda — pode animar câmara, etc.
                mapView.onResume()
            }
        )

        // ── Overlay superior: cabeçalho ──────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "MAPAS & ROTAS",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }

        // ── Card meteorológico (canto superior direito) ──────────
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = VitalOrange,
                        strokeWidth = 2.dp
                    )
                }

                uiState.weatherInfo != null -> {
                    WeatherCard(weather = uiState.weatherInfo!!)
                }

                uiState.errorMessage != null -> {
                    IconButton(
                        onClick = { viewModel.fetchWeather() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Tentar novamente",
                            tint = VitalOrange
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Card com dados meteorológicos — chamada à Open-Meteo API
// ─────────────────────────────────────────────────────────────

@Composable
private fun WeatherCard(weather: WeatherInfo) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = weather.temperature,
                color = VitalOrange,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = weather.condition,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            HorizontalDivider(
                color = Color.DarkGray,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text(text = weather.humidity, color = Color.Gray, fontSize = 11.sp)
            Text(text = weather.wind, color = Color.Gray, fontSize = 11.sp)
        }
    }
}
