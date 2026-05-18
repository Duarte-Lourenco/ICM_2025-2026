package com.studio.vitalroute.ui.diary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.studio.vitalroute.data.ActivityExporter
import com.studio.vitalroute.data.model.Activity
import com.studio.vitalroute.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

@Composable
fun ActivityDetailScreen(
    activityId: String,
    navController: NavHostController,
    viewModel: ActivityDetailViewModel = viewModel(factory = ActivityDetailViewModel.factory(activityId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState.deleted) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            containerColor   = Color(0xFF1E1E1E),
            title  = { Text("Apagar atividade?", color = Color.White, fontWeight = FontWeight.Bold) },
            text   = { Text("Esta ação não pode ser desfeita.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteActivity() }) {
                    Text("Apagar", color = VitalRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Text(
                text = "Atividade",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.showDeleteDialog() }) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252))
            }
            Box {
                IconButton(onClick = { viewModel.showExportMenu() }) {
                    Icon(Icons.Default.Share, null, tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = uiState.showExportMenu,
                    onDismissRequest = { viewModel.hideExportMenu() },
                    containerColor = Color(0xFF1E1E1E)
                ) {
                    DropdownMenuItem(
                        text = { Text("Exportar GPX", color = Color.White, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Map, null, tint = VitalOrange) },
                        onClick = {
                            viewModel.hideExportMenu()
                            uiState.activity?.let {
                                val intent = ActivityExporter.exportGpx(context, it)
                                context.startActivity(android.content.Intent.createChooser(intent, "Partilhar GPX"))
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Exportar CSV", color = Color.White, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.TableRows, null, tint = VitalGreen) },
                        onClick = {
                            viewModel.hideExportMenu()
                            uiState.activity?.let {
                                val intent = ActivityExporter.exportCsv(context, listOf(it))
                                context.startActivity(android.content.Intent.createChooser(intent, "Partilhar CSV"))
                            }
                        }
                    )
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VitalGreen)
                }
            }
            uiState.activity == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Atividade não encontrada", color = Color.Gray)
                }
            }
            else -> {
                ActivityDetailContent(activity = uiState.activity!!, useMetric = uiState.useMetric)
            }
        }
    }
}

@Composable
private fun ActivityDetailContent(activity: Activity, useMetric: Boolean) {
    val dateFmt = SimpleDateFormat("d MMMM yyyy", Locale("pt", "PT"))
    val timeFmt = SimpleDateFormat("HH:mm", Locale("pt", "PT"))
    val dateStr = dateFmt.format(Date(activity.startTime))
    val timeStr = timeFmt.format(Date(activity.startTime))
    val durationMin = activity.durationSeconds / 60

    val typeLabel = when (activity.type) {
        "running" -> "Corrida"
        "walking" -> "Caminhada"
        else      -> "Ciclismo"
    }
    val typeIcon = when (activity.type) {
        "running" -> Icons.Default.DirectionsRun
        "walking" -> Icons.Default.DirectionsWalk
        else      -> Icons.Default.DirectionsBike
    }

    val distDisplay  = if (useMetric) activity.distanceKm  else activity.distanceKm  * 0.621371
    val distUnit     = if (useMetric) "km" else "mi"
    val speedUnit    = if (useMetric) "km/h" else "mph"
    val avgSpeed     = if (useMetric) activity.avgSpeedKmh else activity.avgSpeedKmh * 0.621371
    val maxSpeed     = if (useMetric) activity.maxSpeedKmh else activity.maxSpeedKmh * 0.621371

    val paceStr = if (activity.distanceKm > 0.0) {
        val secsPerUnit = activity.durationSeconds / distDisplay
        "%d:%02d /$distUnit".format(secsPerUnit.toInt() / 60, secsPerUnit.toInt() % 60)
    } else "—"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // hero header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1F0D))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(typeIcon, null, tint = VitalGreen, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = typeLabel.uppercase(),
                            color = VitalGreen,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "$dateStr · $timeStr",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // distancia destaque principal
                Text(
                    text = "%.2f".format(distDisplay),
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 56.sp
                )
                Text(distUnit, color = Color.Gray, fontSize = 14.sp)
            }
        }

        // grid de metricas principais
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailStat(
                        label = "Duração",
                        value = if (durationMin >= 60)
                            "%dh %02dmin".format(durationMin / 60, durationMin % 60)
                        else
                            "$durationMin min"
                    )
                    VerticalDivider(modifier = Modifier.height(44.dp), color = Color(0xFF2A2A2A))
                    DetailStat(label = "Ritmo", value = paceStr)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF2A2A2A))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailStat(
                        label = "Vel. Média",
                        value = "%.1f $speedUnit".format(avgSpeed)
                    )
                    VerticalDivider(modifier = Modifier.height(44.dp), color = Color(0xFF2A2A2A))
                    DetailStat(
                        label = "Vel. Máxima",
                        value = if (activity.maxSpeedKmh > 0.0) "%.1f $speedUnit".format(maxSpeed) else "—",
                        accent = NeonOrange
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF2A2A2A))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailStat(
                        label = "Subida",
                        value = "${activity.elevationM} m",
                        accent = VitalOrange
                    )
                    VerticalDivider(modifier = Modifier.height(44.dp), color = Color(0xFF2A2A2A))
                    DetailStat(
                        label = "Calorias",
                        value = "${activity.calories} kcal",
                        accent = VitalRed
                    )
                }
            }
        }

        // perfil de elevacao
        if (activity.elevationPoints.size >= 3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PERFIL DE ELEVAÇÃO",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    DetailElevationChart(points = activity.elevationPoints)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // rota
        if (activity.routePoints.size >= 3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ROTA",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    DetailRouteMap(points = activity.routePoints)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DetailStat(label: String, value: String, accent: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun DetailElevationChart(points: List<Int>) {
    val minAlt = points.min()
    val maxAlt = points.max()
    val range  = (maxAlt - minAlt).coerceAtLeast(10)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Min: ${minAlt} m", color = Color(0xFF888888), fontSize = 10.sp)
        Text("Max: ${maxAlt} m", color = VitalOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(6.dp))
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val w = size.width
        val h = size.height
        val step = if (points.size > 1) w / (points.size - 1) else w

        val linePath = Path()
        val fillPath = Path()

        points.forEachIndexed { i, alt ->
            val x = i * step
            val y = h - ((alt - minAlt).toFloat() / range) * h

            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * step
                val prevY = h - ((points[i - 1] - minAlt).toFloat() / range) * h
                val cx = (prevX + x) / 2f
                linePath.cubicTo(cx, prevY, cx, y, x, y)
                fillPath.cubicTo(cx, prevY, cx, y, x, y)
            }
        }
        fillPath.lineTo((points.size - 1) * step, h)
        fillPath.close()

        drawPath(fillPath, color = VitalOrange.copy(alpha = 0.18f))
        drawPath(linePath, color = VitalOrange, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val lastX = (points.size - 1) * step
        val lastY = h - ((points.last() - minAlt).toFloat() / range) * h
        drawCircle(VitalOrange, radius = 5f, center = Offset(lastX, lastY))
        drawCircle(Color(0xFF1A1A1A), radius = 2.5f, center = Offset(lastX, lastY))
    }
}

@Composable
private fun DetailRouteMap(points: List<String>) {
    val coords = remember(points) {
        points.mapNotNull { p ->
            val parts = p.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lng = parts[1].toDoubleOrNull()
                if (lat != null && lng != null) GeoPoint(lat, lng) else null
            } else null
        }
    }
    if (coords.size < 2) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(8.dp).background(Color(0xFF2196F3), RoundedCornerShape(4.dp)))
            Text("Partida", color = Color(0xFF888888), fontSize = 10.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(8.dp).background(VitalOrange, RoundedCornerShape(4.dp)))
            Text("Chegada", color = Color(0xFF888888), fontSize = 10.sp)
        }
        Text("${coords.size} pontos GPS", color = VitalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(8.dp))

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().apply {
                userAgentValue    = ctx.packageName
                osmdroidBasePath  = File(ctx.cacheDir, "osmdroid")
                osmdroidTileCache = File(ctx.cacheDir, "osmdroid/tiles")
            }
            MapView(ctx).apply {
                setTileSource(activityDetailTileSource())
                setMultiTouchControls(true)
                controller.setZoom(14.0)
                controller.setCenter(coords.first())

                // desenha o trajeto
                val polyline = Polyline(this).apply {
                    setPoints(coords)
                    outlinePaint.color       = android.graphics.Color.parseColor("#4CAF50")
                    outlinePaint.strokeWidth = 12f
                    outlinePaint.alpha       = 220
                    outlinePaint.strokeCap   = android.graphics.Paint.Cap.ROUND
                    outlinePaint.strokeJoin  = android.graphics.Paint.Join.ROUND
                }
                overlays.add(polyline)

                // ajusta o zoom para mostrar todo o trajeto
                val minLat = coords.minOf { it.latitude }
                val maxLat = coords.maxOf { it.latitude }
                val minLng = coords.minOf { it.longitude }
                val maxLng = coords.maxOf { it.longitude }
                val bb = BoundingBox(maxLat, maxLng, minLat, minLng)
                post { zoomToBoundingBox(bb, false, 80) }
            }
        },
        update = { map -> map.invalidate() },
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}

private fun activityDetailTileSource(): OnlineTileSourceBase =
    object : OnlineTileSourceBase(
        "CartoVoyager", 1, 19, 256, ".png",
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
