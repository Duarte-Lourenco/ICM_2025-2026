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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.studio.vitalroute.data.ActivityExporter
import com.studio.vitalroute.ui.components.SectionHeader
import com.studio.vitalroute.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DiaryScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale("pt", "PT"))
        .format(Date()).replaceFirstChar { it.uppercaseChar() }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        // cabeçalho
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DIÁRIO",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(monthLabel, color = Color.Gray, fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botão de exportação
                Box {
                    IconButton(onClick = { viewModel.showExportMenu() }) {
                        Icon(Icons.Default.Share, "Exportar", tint = Color.Gray,
                            modifier = Modifier.size(22.dp))
                    }
                    DropdownMenu(
                        expanded         = uiState.showExportMenu,
                        onDismissRequest = { viewModel.hideExportMenu() },
                        containerColor   = Color(0xFF1E1E1E)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Exportar CSV", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.TableRows, null, tint = VitalGreen) },
                            onClick = {
                                viewModel.hideExportMenu()
                                val raw = viewModel.getRawActivities()
                                if (raw.isNotEmpty()) {
                                    val intent = ActivityExporter.exportCsv(context, raw)
                                    context.startActivity(android.content.Intent.createChooser(intent, "Partilhar CSV"))
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Exportar GPX (última atividade)", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Map, null, tint = VitalOrange) },
                            onClick = {
                                viewModel.hideExportMenu()
                                val raw = viewModel.getRawActivities()
                                if (raw.isNotEmpty()) {
                                    val intent = ActivityExporter.exportGpx(context, raw.first())
                                    context.startActivity(android.content.Intent.createChooser(intent, "Partilhar GPX"))
                                }
                            }
                        )
                    }
                }
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.Settings, null, tint = Color.Gray,
                        modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VitalGreen)
            }
        } else {

            // resumo do mês
            SectionHeader("RESUMO DO MÊS")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                val s = uiState.monthlySummary
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MonthStat("🚴", "${s.totalKm} km", "Distância")
                        VerticalDivider(modifier = Modifier.height(52.dp), color = Color(0xFF2A2A2A))
                        MonthStat("⛰️", "${s.totalElevation} m", "Elevação")
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF2A2A2A))
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MonthStat("⏱️", "${s.totalMinutes} min", "Ativo")
                        VerticalDivider(modifier = Modifier.height(52.dp), color = Color(0xFF2A2A2A))
                        MonthStat("🛡️", s.incidents, "Incidentes")
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // recordes pessoais
            SectionHeader("RECORDES PESSOAIS")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                val pb = uiState.personalBests
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PersonalBestRow(Icons.Default.Straighten, "Distância Máxima",  pb.longestRide)
                    HorizontalDivider(color = Color(0xFF2A2A2A))
                    PersonalBestRow(Icons.Default.Speed,      "Velocidade Máxima", pb.topSpeed)
                    HorizontalDivider(color = Color(0xFF2A2A2A))
                    PersonalBestRow(Icons.Default.Timer,      "Maior Duração",     pb.longestDuration)
                    HorizontalDivider(color = Color(0xFF2A2A2A))
                    PersonalBestRow(Icons.Default.Terrain,    "Maior Elevação",    pb.mostElevation)
                }
            }

            Spacer(Modifier.height(28.dp))

            // histórico de atividades
            SectionHeader("HISTÓRICO")

            if (uiState.activities.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚴", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Nenhuma atividade ainda",
                            color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Começa a gravar para ver o teu histórico aqui.",
                            color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                val maxElev = uiState.activities.maxOfOrNull { it.elevationM }?.takeIf { it > 0 } ?: 1
                uiState.activities.forEach { activity ->
                    ActivityCard(activity, maxElevationM = maxElev)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}


@Composable
private fun MonthStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun PersonalBestRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(VitalOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = VitalOrange, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f))
        Text(value, color = VitalOrange, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
private fun ActivityCard(activity: ActivityUiItem, maxElevationM: Int = 1) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (activity.type) {
                            "running" -> Icons.Default.DirectionsRun
                            "walking" -> Icons.Default.DirectionsWalk
                            else      -> Icons.Default.DirectionsBike
                        },
                        contentDescription = null,
                        tint = VitalOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (activity.type) {
                            "running" -> "Corrida"
                            "walking" -> "Caminhada"
                            else      -> "Ciclismo"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(activity.timeLabel, color = Color.Gray, fontSize = 11.sp)
            }
            Text(activity.dateLabel, color = Color.Gray, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ActivityMetric("📏", activity.distance,  "Distância")
                ActivityMetric("⏱️", activity.duration,  "Duração")
                ActivityMetric("⚡", activity.speed,     "Veloc.")
                ActivityMetric("⛰️", activity.elevation, "Elevação")
            }

            // Gráfico de perfil de elevação (linha real) ou barra relativa de fallback
            if (activity.elevationPoints.size >= 3) {
                Spacer(Modifier.height(12.dp))
                ElevationProfileChart(points = activity.elevationPoints)
            } else if (activity.elevationM > 0) {
                Spacer(Modifier.height(10.dp))
                ElevationBar(elevationM = activity.elevationM, maxElevationM = maxElevationM)
            }

            // Mini-rota (só aparece se houver pelo menos 3 pontos GPS)
            if (activity.routePoints.size >= 3) {
                Spacer(Modifier.height(12.dp))
                MiniRouteMap(points = activity.routePoints)
            }
        }
    }
}

// gráfico de linha de elevação real

@Composable
private fun ElevationProfileChart(points: List<Int>) {
    val minAlt = points.min()
    val maxAlt = points.max()
    val range  = (maxAlt - minAlt).coerceAtLeast(10)  // evita divisão por zero
    val lineColor   = VitalOrange
    val fillColor   = VitalOrange.copy(alpha = 0.15f)
    val labelColor  = Color(0xFF888888)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Perfil de elevação", color = labelColor, fontSize = 9.sp)
            Text("${minAlt}–${maxAlt} m", color = lineColor, fontSize = 9.sp,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val w = size.width
            val h = size.height
            val step = if (points.size > 1) w / (points.size - 1) else w

            // Constrói o path da linha
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
                    // Curva suave (bezier cúbico) entre pontos
                    val prevX = (i - 1) * step
                    val prevY = h - ((points[i - 1] - minAlt).toFloat() / range) * h
                    val cx = (prevX + x) / 2f
                    linePath.cubicTo(cx, prevY, cx, y, x, y)
                    fillPath.cubicTo(cx, prevY, cx, y, x, y)
                }
            }
            fillPath.lineTo((points.size - 1) * step, h)
            fillPath.close()

            // Área preenchida
            drawPath(fillPath, color = fillColor)
            // Linha de contorno
            drawPath(
                linePath,
                color = lineColor,
                style = Stroke(
                    width     = 2.5f,
                    cap       = StrokeCap.Round,
                    join      = StrokeJoin.Round
                )
            )
            // Ponto final (destaque)
            val lastX = (points.size - 1) * step
            val lastY = h - ((points.last() - minAlt).toFloat() / range) * h
            drawCircle(lineColor, radius = 4f, center = Offset(lastX, lastY))
            drawCircle(Color(0xFF1A1A1A), radius = 2f, center = Offset(lastX, lastY))
        }
    }
}

// barra de elevação relativa (fallback quando < 3 amostras)

@Composable
private fun ElevationBar(elevationM: Int, maxElevationM: Int) {
    val fraction = (elevationM.toFloat() / maxElevationM.toFloat()).coerceIn(0.05f, 1f)
    val barColor = when {
        fraction > 0.66f -> VitalOrange
        fraction > 0.33f -> Color(0xFFE6A817)
        else             -> VitalGreen
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Elevação máxima", color = Color.DarkGray, fontSize = 9.sp)
            Text("${elevationM} m", color = barColor, fontSize = 9.sp,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFF2A2A2A), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(barColor, RoundedCornerShape(3.dp))
            )
        }
    }
}

// mini-mapa da rota percorrida

@Composable
private fun MiniRouteMap(points: List<String>) {
    // Parse lat/lng pairs
    val coords = points.mapNotNull { p ->
        val parts = p.split(",")
        if (parts.size == 2) {
            val lat = parts[0].toDoubleOrNull()
            val lng = parts[1].toDoubleOrNull()
            if (lat != null && lng != null) Pair(lat, lng) else null
        } else null
    }
    if (coords.size < 2) return

    val minLat = coords.minOf { it.first }
    val maxLat = coords.maxOf { it.first }
    val minLng = coords.minOf { it.second }
    val maxLng = coords.maxOf { it.second }
    val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
    val lngRange = (maxLng - minLng).coerceAtLeast(0.0001)

    val routeColor  = VitalGreen
    val startColor  = Color(0xFF2196F3)
    val endColor    = VitalOrange

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Rota percorrida", color = Color(0xFF888888), fontSize = 9.sp)
            Text("${coords.size} pontos", color = VitalGreen, fontSize = 9.sp,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
        ) {
            val w = size.width
            val h = size.height
            val padding = 10f

            fun project(lat: Double, lng: Double): Offset {
                val x = padding + ((lng - minLng) / lngRange * (w - 2 * padding)).toFloat()
                // Latitude invertida: maior lat = mais acima
                val y = padding + ((maxLat - lat) / latRange * (h - 2 * padding)).toFloat()
                return Offset(x, y)
            }

            // Desenha a rota
            for (i in 1 until coords.size) {
                val p1 = project(coords[i - 1].first, coords[i - 1].second)
                val p2 = project(coords[i].first, coords[i].second)
                drawLine(
                    color       = routeColor.copy(alpha = 0.8f),
                    start       = p1,
                    end         = p2,
                    strokeWidth = 3f,
                    cap         = StrokeCap.Round
                )
            }
            // Ponto de partida (azul)
            val start = project(coords.first().first, coords.first().second)
            drawCircle(startColor, radius = 5f, center = start)
            drawCircle(Color(0xFF1A1A1A), radius = 2.5f, center = start)
            // Ponto de chegada (laranja)
            val end = project(coords.last().first, coords.last().second)
            drawCircle(endColor, radius = 5f, center = end)
            drawCircle(Color(0xFF1A1A1A), radius = 2.5f, center = end)
        }
    }
}

@Composable
private fun ActivityMetric(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(label, color = Color.Gray, fontSize = 9.sp)
    }
}
