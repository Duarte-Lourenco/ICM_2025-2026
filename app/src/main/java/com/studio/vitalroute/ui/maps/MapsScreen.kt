package com.studio.vitalroute.ui.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SignalCellularAlt
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.vitalroute.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapsScreen(viewModel: MapsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context  = LocalContext.current

    val userLocation = remember {
        getDeviceLocation(context) ?: GeoPoint(40.6405, -8.6568)
    }

    val mapViewRef        = remember { mutableStateOf<MapView?>(null) }
    val heatmapOverlayRef = remember { mutableStateOf<CoverageHeatmapOverlay?>(null) }

    LaunchedEffect(Unit) {
        viewModel.updateCenter(userLocation.latitude, userLocation.longitude)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Mapa OSMDroid ─────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(cyclOSMTileSource())
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(userLocation)

                    val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    myLocationOverlay.enableMyLocation()
                    overlays.add(myLocationOverlay)

                    // Cria o heatmap overlay uma só vez e mantém referência
                    val heatmap = CoverageHeatmapOverlay()
                    heatmapOverlayRef.value = heatmap

                    mapViewRef.value = this
                }
            },
            update = { map ->
                // ── Tile source ───────────────────────────────
                val newSource = when (uiState.selectedLayer) {
                    MapLayer.CYCLING  -> cyclOSMTileSource()
                    MapLayer.STANDARD -> TileSourceFactory.MAPNIK
                }
                map.setTileSource(newSource)

                // ── Ciclovias (polylines) ─────────────────────
                map.overlays.removeAll { it is Polyline }
                uiState.cyclingPaths.forEach { path ->
                    Polyline(map).apply {
                        setPoints(path.points)
                        outlinePaint.color       = android.graphics.Color.parseColor("#4CAF50")
                        outlinePaint.strokeWidth = 9f
                        outlinePaint.alpha       = 210
                        map.overlays.add(this)
                    }
                }

                // ── Heatmap de cobertura ──────────────────────
                val heatmap = heatmapOverlayRef.value ?: return@AndroidView

                if (uiState.showCoverage) {
                    heatmap.updateTowers(uiState.coverageTowers)
                    if (!map.overlays.contains(heatmap)) {
                        map.overlays.add(heatmap)
                    }
                } else {
                    map.overlays.remove(heatmap)
                }

                map.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Chips de controlo (topo) ──────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Camada Normal
            MapChip(
                label    = "🗺  Normal",
                selected = uiState.selectedLayer == MapLayer.STANDARD,
                onClick  = { viewModel.setLayer(MapLayer.STANDARD) }
            )
            // Camada Ciclismo
            MapChip(
                label    = "🚴  Ciclismo",
                selected = uiState.selectedLayer == MapLayer.CYCLING,
                onClick  = { viewModel.setLayer(MapLayer.CYCLING) }
            )
            // Toggle cobertura de rede
            MapChip(
                label    = if (uiState.isLoadingCoverage) "📶  A carregar…"
                           else if (uiState.showCoverage) "📶  Rede ✓"
                           else "📶  Rede",
                selected = uiState.showCoverage,
                accentColor = Color(0xFF4FC3F7),   // azul para distinguir
                onClick  = {
                    viewModel.toggleCoverage(userLocation.latitude, userLocation.longitude)
                }
            )
        }

        // ── Card meteo (topo direito) ─────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 12.dp, top = 12.dp)
        ) {
            when {
                uiState.isLoadingWeather -> CircularProgressIndicator(
                    modifier = Modifier.size(28.dp), color = VitalGreen, strokeWidth = 2.dp
                )
                uiState.weatherInfo != null -> WeatherOverlayCard(uiState.weatherInfo!!)
            }
        }

        // ── Legenda de cobertura (centro-inferior) ────────────
        AnimatedVisibility(
            visible = uiState.showCoverage && !uiState.isLoadingCoverage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp),
            enter = fadeIn(),
            exit  = fadeOut()
        ) {
            CoverageLegendCard(towerCount = uiState.towerCount)
        }

        // ── Erro de cobertura ─────────────────────────────────
        uiState.coverageError?.let { err ->
            AnimatedVisibility(
                visible = uiState.showCoverage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 88.dp),
                enter = fadeIn(), exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xDD1A0000),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text     = err,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color    = Color(0xFFFF6B6B),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── Loading indicator cobertura ───────────────────────
        if (uiState.isLoadingCoverage) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 88.dp),
                color = Color(0xDD111111),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        color       = Color(0xFF4FC3F7),
                        strokeWidth = 2.dp
                    )
                    Text(
                        "A carregar antenas…",
                        color    = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── FAB localização (fundo direito) ───────────────────
        FloatingActionButton(
            onClick        = { mapViewRef.value?.controller?.animateTo(userLocation, 16.0, 800L) },
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 80.dp),
            containerColor = Color(0xFF1E1E1E),
            contentColor   = Color.White,
            shape          = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "A minha localização")
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Componentes privados
// ─────────────────────────────────────────────────────────────

@Composable
private fun MapChip(
    label: String,
    selected: Boolean,
    accentColor: Color = VitalGreen,
    onClick: () -> Unit
) {
    Surface(
        onClick         = onClick,
        color           = if (selected) accentColor else Color(0xDD111111),
        contentColor    = if (selected) Color.Black else Color.White,
        shape           = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
            fontSize   = 13.sp
        )
    }
}

@Composable
private fun WeatherOverlayCard(weather: WeatherInfo) {
    Surface(
        color = Color(0xDD111111),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(weather.temperature, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(weather.condition,   color = Color.Gray,  fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(weather.humidity, color = Color.Gray, fontSize = 11.sp)
            Text(weather.wind,     color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CoverageLegendCard(towerCount: Int) {
    Surface(
        color  = Color(0xF0111111),
        shape  = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.SignalCellularAlt,
                    null,
                    tint     = Color(0xFF4FC3F7),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Cobertura de Rede Móvel",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 12.sp
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "$towerCount antenas",
                    color    = Color.Gray,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                LegendItem(Color(0xFF1ECC50), "Boa cobertura")
                LegendItem(Color(0xFFDDCC00), "Cobertura média")
                LegendItem(Color(0xFFCC2222), "Sem cobertura")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            color    = color,
            shape    = RoundedCornerShape(50)
        ) {}
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}

// ─────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────

private fun cyclOSMTileSource(): OnlineTileSourceBase =
    object : OnlineTileSourceBase(
        "CyclOSM", 1, 19, 256, ".png",
        arrayOf(
            "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String =
            baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex)    + "/" +
            MapTileIndex.getY(pMapTileIndex)    + mImageFilenameEnding
    }

private fun getDeviceLocation(context: Context): GeoPoint? = try {
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) null
    else {
        val lm  = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        loc?.let { GeoPoint(it.latitude, it.longitude) }
    }
} catch (e: Exception) { null }
