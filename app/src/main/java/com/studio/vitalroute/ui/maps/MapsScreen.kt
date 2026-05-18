package com.studio.vitalroute.ui.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.location.LocationManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.vitalroute.ui.theme.*
import org.osmdroid.config.Configuration
import android.view.MotionEvent
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

// cores por tipo de infraestrutura ciclavel
private val COLOR_CYCLEWAY = android.graphics.Color.parseColor("#00C853") // verde vivo
private val COLOR_PATH     = android.graphics.Color.parseColor("#29B6F6") // azul claro
private val COLOR_TRACK    = android.graphics.Color.parseColor("#FFA726") // laranja

@Composable
fun MapsScreen(viewModel: MapsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context  = LocalContext.current

    val userLocation = remember {
        getDeviceLocation(context) ?: GeoPoint(40.6405, -8.6568)
    }

    val mapViewRef      = remember { mutableStateOf<MapView?>(null) }
    val markerIconCache = remember { mutableMapOf<String, BitmapDrawable>() }

    // carrega tudo ao abrir o ecra
    LaunchedEffect(Unit) {
        viewModel.updateCenter(userLocation.latitude, userLocation.longitude)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // mapa osmdroid
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().apply {
                    userAgentValue          = ctx.packageName
                    osmdroidBasePath        = File(ctx.cacheDir, "osmdroid")
                    osmdroidTileCache       = File(ctx.cacheDir, "osmdroid/tiles")
                    tileFileSystemCacheMaxBytes  = 250L * 1024L * 1024L
                    expirationOverrideDuration   = 7L * 24L * 60L * 60L * 1_000L
                    tileDownloadThreads          = 4
                    tileDownloadMaxQueueSize     = 40
                }
                MapView(ctx).apply {
                    setTileSource(cartoDarkTileSource())
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(userLocation)

                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)

                    // overlay de eventos de toque para criar zonas
                    overlays.add(object : Overlay() {
                        override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                            val pt = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
                            viewModel.onMapTapped(pt.latitude, pt.longitude)
                            return true
                        }
                    })

                    mapViewRef.value = this
                }
            },
            update = { map ->
                // tile source sempre cartodb voyager
                map.setTileSource(cartoDarkTileSource())

                // ciclovias so visiveis na camada de ciclismo
                map.overlays.removeAll { it is Polyline }
                if (uiState.selectedLayer == MapLayer.CYCLING) {
                    uiState.cyclingPaths.forEach { path ->
                        Polyline(map).apply {
                            setPoints(path.points)
                            val (color, width) = when (path.type) {
                                PathType.CYCLEWAY -> Pair(COLOR_CYCLEWAY, 9f)
                                PathType.PATH     -> Pair(COLOR_PATH,     7f)
                                PathType.TRACK    -> Pair(COLOR_TRACK,    7f)
                            }
                            outlinePaint.color       = color
                            outlinePaint.strokeWidth = width
                            outlinePaint.alpha       = 220
                            outlinePaint.strokeCap   = android.graphics.Paint.Cap.ROUND
                            outlinePaint.strokeJoin  = android.graphics.Paint.Join.ROUND
                            map.overlays.add(this)
                        }
                    }
                }

                // marcador destino
                map.overlays.removeAll { it is DestinationMarker }
                uiState.activeDestination?.let { dest ->
                    val icon = createDestinationMarkerIcon(context)
                    val marker = DestinationMarker(map).apply {
                        position = GeoPoint(dest.lat, dest.lng)
                        title    = if (dest.name.isNotBlank()) dest.name else "Destino"
                        this.icon = icon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    map.overlays.add(marker)
                }

                // zonas seguras circulos e marcadores sempre visiveis
                map.overlays.removeAll { it is ZoneCircle || it is ZoneMarker }
                uiState.safeZones.forEach { zone ->
                    if (zone.lat == 0.0 && zone.lng == 0.0) return@forEach
                    val center = GeoPoint(zone.lat, zone.lng)

                    // circulo de raio cor da zona com transparencia
                    val zoneArgb = try { android.graphics.Color.parseColor(zone.color) }
                                   catch (_: Exception) { android.graphics.Color.parseColor("#FF6F00") }
                    val circle = ZoneCircle().apply {
                        points = circlePoints(center, zone.radiusM.toDouble())
                        fillPaint.color    = android.graphics.Color.argb(45,
                            android.graphics.Color.red(zoneArgb),
                            android.graphics.Color.green(zoneArgb),
                            android.graphics.Color.blue(zoneArgb))
                        outlinePaint.color = zoneArgb
                        outlinePaint.strokeWidth = 3f
                    }
                    map.overlays.add(circle)

                    // marcador central com icone de casa na cor da zona
                    val icon = markerIconCache.getOrPut(zone.color) { createHouseMarkerIcon(context, zone.color) }
                    val marker = ZoneMarker(map).apply {
                        position = center
                        title    = zone.name
                        this.icon = icon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerClickListener { _, _ ->
                            viewModel.selectZone(zone.id)
                            true
                        }
                    }
                    map.overlays.add(marker)
                }

                map.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // barra superior e banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LayerToggle(
                    selected = uiState.selectedLayer,
                    onSelect = viewModel::setLayer
                )
                Spacer(Modifier.width(8.dp))
                if (!uiState.isLoadingWeather && uiState.weatherInfo != null) {
                    WeatherCard(uiState.weatherInfo!!)
                } else if (uiState.isLoadingWeather) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(28.dp),
                        color       = VitalGreen,
                        strokeWidth = 2.dp
                    )
                }
            }
            // banner destino ativo
            if (uiState.activeDestination != null && !uiState.isAddingZone) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    color           = Color(0xF0204080),
                    shape           = RoundedCornerShape(14.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val dest = uiState.activeDestination
                        Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (dest != null && dest.name.isNotBlank())
                                       "Destino: ${dest.name}"
                                   else "Destino definido",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                            modifier   = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::clearDestination, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // banner adicionar zona logo abaixo da barra de topo
            if (uiState.isAddingZone) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    color           = Color(0xF0FF6F00),
                    shape           = RoundedCornerShape(14.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📍", fontSize = 18.sp)
                        Text(
                            "Toca no mapa para marcar a zona",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                            modifier   = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::exitAddZoneMode, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // painel inferior ciclovias so visivel no modo ciclismo
        AnimatedVisibility(
            visible  = uiState.selectedLayer == MapLayer.CYCLING && uiState.selectedZone == null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            enter    = slideInVertically(initialOffsetY = { it }),
            exit     = slideOutVertically(targetOffsetY = { it })
        ) {
            CyclingInfoPanel(
                uiState    = uiState,
                onRadius   = viewModel::setSearchRadius,
                onRefresh  = viewModel::refreshPaths
            )
        }

        // fabs acima do painel de ciclovias quando visivel
        val fabBottomPadding by animateDpAsState(
            targetValue = if (uiState.selectedLayer == MapLayer.CYCLING) 130.dp else 16.dp,
            label       = "fabBottom"
        )
        if (uiState.selectedZone == null) {
            FloatingActionButton(
                onClick        = { mapViewRef.value?.controller?.animateTo(userLocation, 16.0, 800L) },
                modifier       = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 12.dp, bottom = fabBottomPadding),
                containerColor = Color(0xFF1E1E1E),
                contentColor   = Color.White,
                shape          = RoundedCornerShape(14.dp),
                elevation      = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Minha localização")
            }
            FloatingActionButton(
                onClick        = {
                    if (uiState.isAddingZone) viewModel.exitAddZoneMode()
                    else viewModel.enterAddZoneMode()
                },
                modifier       = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, bottom = fabBottomPadding),
                containerColor = if (uiState.isAddingZone) Color(0xFFFF6F00) else Color(0xFF1E1E1E),
                contentColor   = Color.White,
                shape          = RoundedCornerShape(14.dp),
                elevation      = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.AddLocation, contentDescription = "Nova zona segura")
            }
        }

        // bottom sheet de destino slide-up
        AnimatedVisibility(
            visible  = uiState.showDestinationSheet,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter    = slideInVertically(initialOffsetY = { it }),
            exit     = slideOutVertically(targetOffsetY = { it })
        ) {
            DestinationSheet(
                onConfirm = viewModel::setAsDestination,
                onDismiss = viewModel::dismissDestinationSheet
            )
        }

        // painel de edicao de zona slide-up
        AnimatedVisibility(
            visible  = uiState.selectedZone != null && !uiState.showDestinationSheet,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter    = slideInVertically(initialOffsetY = { it }),
            exit     = slideOutVertically(targetOffsetY = { it })
        ) {
            uiState.selectedZone?.let { zone ->
                ZoneDetailSheet(
                    zone                = zone,
                    editingRadius       = uiState.editingRadius,
                    editingColor        = uiState.editingColor,
                    showDeleteConfirm   = uiState.showDeleteConfirm,
                    onRadiusChange      = viewModel::updateEditingRadius,
                    onColorChange       = viewModel::updateEditingColor,
                    onSave              = viewModel::saveZoneEdits,
                    onDeleteRequest     = viewModel::toggleDeleteConfirm,
                    onDeleteConfirm     = viewModel::deleteSelectedZone,
                    onDismiss           = viewModel::deselectZone,
                    onSetAsDestination  = { viewModel.setZoneAsDestination(zone) }
                )
            }
        }

        // dialogo de nome da zona
        if (uiState.showZoneNameDialog) {
            ZoneNameDialog(
                name         = uiState.pendingZoneName,
                radius       = uiState.pendingZoneRadius,
                isSaving     = uiState.isSavingZone,
                onNameChange = viewModel::updatePendingZoneName,
                onRadiusChange = viewModel::setPendingZoneRadius,
                onSave       = viewModel::savePendingZone,
                onDismiss    = viewModel::dismissZoneDialog
            )
        }
    }
}

// componentes

@Composable
private fun LayerToggle(selected: MapLayer, onSelect: (MapLayer) -> Unit) {
    Surface(
        color           = Color(0xEE111111),
        shape           = RoundedCornerShape(22.dp),
        shadowElevation = 6.dp
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            ToggleOption(
                label    = "🗺  Normal",
                active   = selected == MapLayer.STANDARD,
                onClick  = { onSelect(MapLayer.STANDARD) }
            )
            Spacer(Modifier.width(2.dp))
            ToggleOption(
                label    = "🚴  Ciclismo",
                active   = selected == MapLayer.CYCLING,
                onClick  = { onSelect(MapLayer.CYCLING) }
            )
        }
    }
}

@Composable
private fun ToggleOption(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick      = onClick,
        color        = if (active) VitalGreen else Color.Transparent,
        contentColor = if (active) Color.Black else Color.White,
        shape        = RoundedCornerShape(18.dp)
    ) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontSize   = 13.sp
        )
    }
}

@Composable
private fun WeatherCard(weather: WeatherInfo) {
    Surface(
        color           = Color(0xEE111111),
        shape           = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(weather.icon, fontSize = 24.sp)
            Column {
                Text(
                    weather.temperature,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 18.sp,
                    lineHeight = 20.sp
                )
                Text(weather.condition, color = Color.Gray, fontSize = 10.sp, lineHeight = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(weather.humidity, color = Color(0xFF90CAF9), fontSize = 10.sp)
                    Text(weather.wind,     color = Color(0xFFB0BEC5), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun CyclingInfoPanel(
    uiState: MapsUiState,
    onRadius: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color           = Color(0xF0111111),
        shape           = RoundedCornerShape(18.dp),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

            // linha de estado
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = Triple(uiState.isLoadingPaths, uiState.pathsLoaded, uiState.pathsError),
                    label       = "paths_status"
                ) { (loading, loaded, error) ->
                    when {
                        loading -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(14.dp),
                                color       = VitalGreen,
                                strokeWidth = 2.dp
                            )
                            Text("A procurar ciclovias…", color = Color.Gray, fontSize = 13.sp)
                        }
                        error != null && uiState.cyclingPaths.isEmpty() -> Text(
                            "⚠ $error",
                            color    = Color(0xFFFF6B6B),
                            fontSize = 13.sp
                        )
                        loaded -> PathCountRow(uiState)
                        else -> Text(
                            "🚴  Ciclovias",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp
                        )
                    }
                }

                // botao de refresh
                IconButton(
                    onClick  = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Recarregar",
                        tint     = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A))
            Spacer(Modifier.height(10.dp))

            // seletor de raio
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Raio", color = Color.Gray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3000 to "3 km", 5000 to "5 km", 10000 to "10 km", 20000 to "20 km").forEach { (m, label) ->
                        RadiusChip(
                            label    = label,
                            selected = uiState.searchRadiusM == m,
                            onClick  = { onRadius(m) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PathCountRow(uiState: MapsUiState) {
    val total = uiState.cyclingPaths.size
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "🚴  $total vias",
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 13.sp
        )
        if (uiState.cyclewayCount > 0) LegendDot(Color(0xFF00C853), "${uiState.cyclewayCount} ciclov.")
        if (uiState.pathCount     > 0) LegendDot(Color(0xFF29B6F6), "${uiState.pathCount} camin.")
        if (uiState.trackCount    > 0) LegendDot(Color(0xFFFFA726), "${uiState.trackCount} trilho")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
private fun RadiusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick         = onClick,
        color           = if (selected) VitalGreen else Color(0xFF1E1E1E),
        contentColor    = if (selected) Color.Black else Color.Gray,
        shape           = RoundedCornerShape(12.dp),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize   = 11.sp
        )
    }
}

// painel de edicao de zona segura

private val ZONE_COLORS = listOf(
    "#FF6F00", "#4CAF50", "#2196F3", "#F44336", "#9C27B0", "#00BCD4"
)

@Composable
private fun ZoneDetailSheet(
    zone: com.studio.vitalroute.data.model.FirestoreSafeZone,
    editingRadius: Int,
    editingColor: String,
    showDeleteConfirm: Boolean,
    onRadiusChange: (Int) -> Unit,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onSetAsDestination: () -> Unit = {}
) {
    Surface(
        modifier        = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color           = Color(0xFF1A1A1A),
        shape           = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {

            // barra de arrasto e fechar
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF444444))
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // nome da zona com icone colorido
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier         = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(editingColor))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏠", fontSize = 18.sp)
                }
                Column {
                    Text(zone.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    if (zone.address.isNotBlank())
                        Text(zone.address, color = Color.Gray, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A))
            Spacer(Modifier.height(16.dp))

            // cor do icone
            Text("Cor do ícone", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ZONE_COLORS.forEach { hex ->
                    val selected = editingColor == hex
                    val sizeDp   = if (selected) 36.dp else 30.dp
                    var mod = Modifier
                        .size(sizeDp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .clickable { onColorChange(hex) }
                    if (selected) mod = mod.border(2.5.dp, Color.White, CircleShape)
                    Box(modifier = mod)
                }
            }

            Spacer(Modifier.height(20.dp))

            // raio de detecao
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Raio de deteção", color = Color.Gray, fontSize = 12.sp)
                Text("$editingRadius m", color = VitalOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value         = editingRadius.toFloat(),
                onValueChange = { onRadiusChange(it.toInt()) },
                valueRange    = 25f..500f,
                colors        = SliderDefaults.colors(
                    thumbColor         = VitalOrange,
                    activeTrackColor   = VitalOrange,
                    inactiveTrackColor = Color(0xFF333333)
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("25 m",  color = Color.DarkGray, fontSize = 10.sp)
                Text("500 m", color = Color.DarkGray, fontSize = 10.sp)
            }

            Spacer(Modifier.height(8.dp))

            // botao definir como destino
            TextButton(
                onClick  = onSetAsDestination,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocationOn, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Definir como destino", color = Color(0xFF64B5F6), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(8.dp))

            // confirmacao de eliminacao ou botoes normais
            if (showDeleteConfirm) {
                Surface(color = Color(0xFF2A0000), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Apagar esta zona?", color = Color(0xFFFF6B6B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onDeleteRequest) { Text("Cancelar", color = Color.Gray, fontSize = 12.sp) }
                            Button(
                                onClick = onDeleteConfirm,
                                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
                            ) { Text("Apagar", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            } else {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDeleteRequest,
                        modifier = Modifier.weight(1f),
                        border  = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF992222)),
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B))
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Apagar", fontSize = 13.sp)
                    }
                    Button(
                        onClick  = onSave,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = VitalOrange, contentColor = Color.Black)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// dialogo de nome da zona segura

@Composable
private fun ZoneNameDialog(
    name: String,
    radius: Int,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onRadiusChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color  = Color(0xFF1A1A1A),
            shape  = RoundedCornerShape(20.dp),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("📍  Nova Zona Segura", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value         = name,
                    onValueChange = onNameChange,
                    label         = { Text("Nome (ex: Casa, Trabalho)", color = Color.Gray, fontSize = 12.sp) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = VitalOrange,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = VitalOrange
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() })
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Raio de chegada", color = Color.Gray, fontSize = 12.sp)
                    Text("$radius m", color = VitalOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                Slider(
                    value         = radius.toFloat(),
                    onValueChange = { onRadiusChange(it.toInt()) },
                    valueRange    = 25f..500f,
                    colors        = SliderDefaults.colors(
                        thumbColor       = VitalOrange,
                        activeTrackColor = VitalOrange,
                        inactiveTrackColor = Color(0xFF333333)
                    )
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("25 m", color = Color.DarkGray, fontSize = 10.sp)
                    Text("500 m", color = Color.DarkGray, fontSize = 10.sp)
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick  = onSave,
                        enabled  = name.isNotBlank() && !isSaving,
                        colors   = ButtonDefaults.buttonColors(containerColor = VitalOrange, contentColor = Color.Black)
                    ) {
                        if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        else Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// classes de overlay para zonas e destino para identificar e remover seletivamente

private class ZoneCircle : Polygon()
private class ZoneMarker(map: MapView) : Marker(map)
private class DestinationMarker(map: MapView) : Marker(map)

// bottom sheet de confirmacao de destino

@Composable
private fun DestinationSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier        = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color           = Color(0xFF1A1A1A),
        shape           = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
                    Text("Ponto selecionado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Cancelar", fontSize = 13.sp)
                }
                Button(
                    onClick  = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF204080), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Escolher como destino", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun createDestinationMarkerIcon(context: Context): BitmapDrawable {
    val size = 80
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1565C0")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

    val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = size * 0.08f
        strokeCap = android.graphics.Paint.Cap.ROUND
    }
    // cruz de alvo crosshair
    canvas.drawRect(size * 0.46f, size * 0.18f, size * 0.54f, size * 0.82f, fgPaint)
    canvas.drawRect(size * 0.18f, size * 0.46f, size * 0.82f, size * 0.54f, fgPaint)
    // circulo central
    val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1565C0")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size * 0.12f, holePaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createHouseMarkerIcon(context: Context, colorHex: String = "#FF6F00"): BitmapDrawable {
    val size = 80
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val parsedColor = try { android.graphics.Color.parseColor(colorHex) }
                      catch (_: Exception) { android.graphics.Color.parseColor("#FF6F00") }

    // fundo circulo na cor da zona
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = parsedColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

    val housePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }

    // telhado triangulo
    val roof = Path().apply {
        moveTo(size * 0.50f, size * 0.14f)
        lineTo(size * 0.12f, size * 0.50f)
        lineTo(size * 0.88f, size * 0.50f)
        close()
    }
    canvas.drawPath(roof, housePaint)

    // corpo da casa
    canvas.drawRect(size * 0.22f, size * 0.47f, size * 0.78f, size * 0.82f, housePaint)

    // porta recorte com a cor da zona
    val doorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = parsedColor
        style = Paint.Style.FILL
    }
    canvas.drawRect(size * 0.38f, size * 0.60f, size * 0.62f, size * 0.82f, doorPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun circlePoints(center: GeoPoint, radiusM: Double): List<GeoPoint> {
    val latRad = Math.toRadians(center.latitude)
    return (0..36).map { i ->
        val angle = Math.toRadians(i * 10.0)
        val dLat  = (radiusM / 111_000.0) * cos(angle)
        val dLon  = (radiusM / (111_000.0 * cos(latRad))) * sin(angle)
        GeoPoint(center.latitude + dLat, center.longitude + dLon)
    }
}

// tile source cartodb dark matter mapa escuro minimalista

private fun cartoDarkTileSource(): OnlineTileSourceBase =
    object : OnlineTileSourceBase(
        "CartoDark", 1, 19, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
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
} catch (_: Exception) { null }
