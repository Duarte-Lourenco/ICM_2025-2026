package com.studio.vitalroute.ui.recording

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.vitalroute.ui.components.SosSlider
import com.studio.vitalroute.ui.theme.*

@Composable
fun RecordingScreen(
    onNavigateToMap: () -> Unit = {},
    viewModel: RecordingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // permissao de sms necessaria para sos
    var showSmsRationale by remember { mutableStateOf(false) }
    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (!granted) showSmsRationale = true }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    // permissao de localizacao
    var showPermissionRationale by remember { mutableStateOf(false) }
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startRecording()
        else showPermissionRationale = true
    }

    // sos countdown overlay
    if (uiState.isSosCountdown) {
        SosCountdownOverlay(
            secondsRemaining = uiState.sosCountdownRemaining,
            totalSeconds     = uiState.sosCountdownTotal,
            label            = uiState.lastAlertLabel ?: "Alerta detetado!",
            onCancel         = { viewModel.cancelSos() }
        )
        return
    }

    // sos enviado dialogo de confirmacao
    if (uiState.sosSent) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSosSent() },
            containerColor   = CardGray,
            icon  = { Icon(Icons.Default.Send, null, tint = VitalRed) },
            title = { Text("SOS Enviado", color = Color.White, fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "Mensagem de emergência enviada aos teus contactos de segurança.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissSosSent() },
                    colors  = ButtonDefaults.buttonColors(containerColor = VitalGreen)
                ) { Text("OK") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // cabecalho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GRAVAÇÃO",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Surface(
                    color = if (uiState.isRecording) VitalRed.copy(alpha = 0.15f) else Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (uiState.isRecording) "●" else "◼",
                            color = if (uiState.isRecording) VitalRed else Color.Gray,
                            fontSize = 10.sp
                        )
                        Text(
                            text = if (uiState.isRecording) "EM DIRETO" else "PARADO",
                            color = if (uiState.isRecording) VitalRed else Color.Gray,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // seletor de tipo de atividade so visivel quando nao esta a gravar
            if (!uiState.isRecording) {
                Text(
                    text = "TIPO DE ATIVIDADE",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActivityType.entries.forEach { type ->
                        val selected = uiState.activityType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectActivityType(type) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) VitalGreen.copy(alpha = 0.15f) else Color(0xFF141414),
                            border = if (selected)
                                BorderStroke(1.5.dp, VitalGreen)
                            else
                                BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(type.emoji, fontSize = 22.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = type.label,
                                    color = if (selected) VitalGreen else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            } else {
                // mostra o tipo atual durante a gravacao
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(uiState.activityType.emoji, fontSize = 16.sp)
                    Text(
                        text = uiState.activityType.label.uppercase(),
                        color = VitalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // tempo em destaque
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isRecording) Color(0xFF1A0A00) else Color(0xFF111111)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⏱  TEMPO DE ATIVIDADE",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.elapsedTime,
                        color = if (uiState.isRecording) VitalOrange else Color.White,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // grelha de metricas 2x2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Straighten,
                    label = "DISTÂNCIA",
                    value = uiState.distance,
                    unit = uiState.distUnit,
                    accentColor = Color.White
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "VELOCIDADE",
                    value = uiState.speed,
                    unit = uiState.speedUnit,
                    accentColor = VitalGreen
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Terrain,
                    label = "SUBIDA",
                    value = uiState.elevation,
                    unit = "m",
                    accentColor = Color(0xFF64B5F6)
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    label = "CALORIAS",
                    value = uiState.calories,
                    unit = "kcal",
                    accentColor = Color(0xFFFF8A65)
                )
            }

            Spacer(Modifier.height(16.dp))

            // card de destino so quando nao esta a gravar
            if (!uiState.isRecording) {
                DestinationCard(
                    hasDestination   = uiState.hasDestination,
                    destinationName  = uiState.destinationName,
                    onSetDestination = onNavigateToMap,
                    onClear          = { viewModel.clearDestination() }
                )
                Spacer(Modifier.height(12.dp))
            }

            // mapa em tempo real visivel durante gravacao ou quando ha destino
            if (uiState.isRecording || uiState.hasDestination) {
                LiveTrackingMap(
                    currentLat          = uiState.currentLat,
                    currentLng          = uiState.currentLng,
                    routePoints         = uiState.routePoints,
                    destinationLat      = uiState.destinationLat,
                    destinationLng      = uiState.destinationLng,
                    plannedRoutePoints  = uiState.plannedRoutePoints,
                    isRecording         = uiState.isRecording
                )
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(12.dp))
            }

            // dialogo sms negado
            if (showSmsRationale) {
                AlertDialog(
                    onDismissRequest = { showSmsRationale = false },
                    containerColor   = CardGray,
                    icon  = { Icon(Icons.Default.Message, null, tint = VitalRed) },
                    title = { Text("SMS necessário para SOS", color = Color.White, fontWeight = FontWeight.Bold) },
                    text  = {
                        Text(
                            "O SOS envia uma SMS de emergência aos teus contactos quando detetar uma queda ou imobilidade. Sem esta permissão, o SOS não funciona.",
                            color = Color.Gray
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showSmsRationale = false },
                            colors  = ButtonDefaults.buttonColors(containerColor = VitalRed)
                        ) { Text("OK") }
                    }
                )
            }

            // dialogo de permissao negada
            if (showPermissionRationale) {
                AlertDialog(
                    onDismissRequest = { showPermissionRationale = false },
                    containerColor   = CardGray,
                    icon  = { Icon(Icons.Default.LocationOff, null, tint = VitalOrange) },
                    title = { Text("Localização necessária", color = Color.White, fontWeight = FontWeight.Bold) },
                    text  = { Text("A gravação precisa de acesso à localização para registar a rota e distância. Ativa nas definições do sistema.", color = Color.Gray) },
                    confirmButton = {
                        Button(
                            onClick = { showPermissionRationale = false },
                            colors  = ButtonDefaults.buttonColors(containerColor = VitalOrange)
                        ) { Text("OK") }
                    }
                )
            }

            // banner de chegada a zona segura
            uiState.arrivedAtZone?.let { zoneName ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1A0D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✅", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Chegaste a $zoneName",
                                color = VitalGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Contactos notificados por SMS",
                                color = Color(0xFF81C784),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissArrival() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // partilha de localizacao em tempo real
            if (!uiState.isRecording) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1520)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Partilhar localização",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Contactos veem posição em tempo real",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = uiState.locationSharingEnabled,
                            onCheckedChange = { viewModel.toggleLocationSharing(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor  = Color(0xFF64B5F6),
                                checkedTrackColor  = Color(0xFF1A3A5C)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else if (uiState.isLocationSharing) {
                // indicador de partilha ativa botao de copiar link
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1520)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, null, tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "A partilhar localização em tempo real",
                                color = Color(0xFF64B5F6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                viewModel.copyLocationLink(context)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null,
                                tint = Color(0xFF64B5F6), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copiar link de localização",
                                color = Color(0xFF64B5F6), fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // botao start stop
            Button(
                onClick = {
                    if (uiState.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isRecording) Color(0xFF8B0000) else VitalGreen
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (uiState.isRecording) "PARAR GRAVAÇÃO" else "INICIAR GRAVAÇÃO",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            // info de seguranca
            if (!uiState.isRecording) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1A0D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, null, tint = VitalGreen,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Deteção de queda e SOS automático ativos durante a gravação",
                                color = Color(0xFF81C784),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // sos slider manual
            SosSlider(onSosTriggered = { viewModel.triggerSos() })
            Spacer(Modifier.height(32.dp))
        }
    }
}


@Composable
fun SosCountdownOverlay(
    secondsRemaining: Int,
    totalSeconds: Int = 15,
    label: String,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE1A0000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = VitalRed,
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
            )

            Text(
                text = label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // contador circular
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { secondsRemaining / totalSeconds.toFloat() },
                    modifier = Modifier.size(120.dp),
                    color = VitalRed,
                    strokeWidth = 8.dp,
                    trackColor = Color(0xFF3A0000)
                )
                Text(
                    text = secondsRemaining.toString(),
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = "O SOS vai ser enviado automaticamente.\nSe estiveres bem, cancela abaixo.",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Button(
                onClick  = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VitalGreen)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "ESTOU BEM — CANCELAR",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}


@Composable
private fun LiveTrackingMap(
    currentLat: Double,
    currentLng: Double,
    routePoints: List<String>,
    destinationLat: Double = 0.0,
    destinationLng: Double = 0.0,
    plannedRoutePoints: List<String> = emptyList(),
    isRecording: Boolean = false
) {
    val hasDestination = destinationLat != 0.0 || destinationLng != 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().apply {
                        userAgentValue    = ctx.packageName
                        osmdroidBasePath  = File(ctx.cacheDir, "osmdroid")
                        osmdroidTileCache = File(ctx.cacheDir, "osmdroid/tiles")
                    }
                    MapView(ctx).apply {
                        setTileSource(liveMapTileSource())
                        setMultiTouchControls(true)
                        controller.setZoom(if (isRecording) 17.0 else 15.0)
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        if (isRecording) locationOverlay.enableFollowLocation()
                        overlays.add(locationOverlay)
                    }
                },
                update = { map ->
                    // rota planeada azul inserida no inicio para ficar por baixo
                    map.overlays.removeAll { it is LivePlannedPolyline }
                    if (plannedRoutePoints.size >= 2) {
                        val pts = plannedRoutePoints.mapNotNull { p ->
                            val s = p.split(",")
                            if (s.size == 2) GeoPoint(s[0].toDoubleOrNull() ?: return@mapNotNull null,
                                                       s[1].toDoubleOrNull() ?: return@mapNotNull null)
                            else null
                        }
                        if (pts.size >= 2) {
                            val poly = LivePlannedPolyline(map)
                            poly.setPoints(pts)
                            poly.outlinePaint.color       = android.graphics.Color.parseColor("#1565C0")
                            poly.outlinePaint.strokeWidth = 10f
                            poly.outlinePaint.alpha       = 160
                            poly.outlinePaint.strokeCap   = android.graphics.Paint.Cap.ROUND
                            poly.outlinePaint.strokeJoin  = android.graphics.Paint.Join.ROUND
                            map.overlays.add(1, poly)
                        }
                    }

                    // rota percorrida verde
                    map.overlays.removeAll { it is Polyline && it !is LivePlannedPolyline }
                    val coords = routePoints.mapNotNull { p ->
                        val s = p.split(",")
                        if (s.size == 2) GeoPoint(s[0].toDoubleOrNull() ?: return@mapNotNull null,
                                                   s[1].toDoubleOrNull() ?: return@mapNotNull null)
                        else null
                    }
                    if (coords.size >= 2) {
                        val poly = Polyline(map)
                        poly.setPoints(coords)
                        poly.outlinePaint.color       = android.graphics.Color.parseColor("#4CAF50")
                        poly.outlinePaint.strokeWidth = 12f
                        poly.outlinePaint.alpha       = 210
                        poly.outlinePaint.strokeCap   = android.graphics.Paint.Cap.ROUND
                        poly.outlinePaint.strokeJoin  = android.graphics.Paint.Join.ROUND
                        map.overlays.add(poly)
                    }

                    // marcador de destino
                    map.overlays.removeAll { it is LiveDestMarker }
                    if (hasDestination) {
                        val m = LiveDestMarker(map)
                        m.position = GeoPoint(destinationLat, destinationLng)
                        m.title    = "Destino"
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        map.overlays.add(m)
                    }

                    // centra no destino se ainda nao ha fix gps
                    if (hasDestination && !isRecording && currentLat == 0.0 && currentLng == 0.0) {
                        map.controller.setCenter(GeoPoint(destinationLat, destinationLng))
                    }

                    map.invalidate()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )

            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                color    = Color(0xBB111111),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text          = if (isRecording) "● EM DIRETO" else "📍 DESTINO",
                    color         = if (isRecording) VitalRed else Color(0xFF64B5F6),
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier      = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (plannedRoutePoints.size >= 2) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color    = Color(0xBB111111),
                    shape    = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text          = "— Rota planeada",
                        color         = Color(0xFF64B5F6),
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier      = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(
    hasDestination: Boolean,
    destinationName: String,
    onSetDestination: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Place, null, tint = VitalOrange, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (hasDestination) "Destino definido" else "Destino",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    if (hasDestination) destinationName.ifBlank { "Localização marcada" }
                    else "Define um ponto de chegada para ver a rota no mapa",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            if (hasDestination) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            } else {
                TextButton(onClick = onSetDestination) {
                    Text("Definir", color = VitalOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private class LivePlannedPolyline(map: MapView) : Polyline(map)
private class LiveDestMarker(map: MapView) : Marker(map)

private fun liveMapTileSource(): OnlineTileSourceBase =
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

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, null, tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Text(
                    text = label,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(text = unit, color = Color.Gray, fontSize = 12.sp)
        }
    }
}
