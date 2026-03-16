package com.studio.vitalroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlin.math.roundToInt

// --- PALETA DE CORES ---
val DarkBackground = Color(0xFF000000)
val CardGray = Color(0xFF1C1C1E)
val VitalOrange = Color(0xFFE65100)
val VitalRed = Color(0xFFFF3D00)
val VitalGreen = Color(0xFF4CAF50)
val NeonOrange = Color(0xFFFF9100)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VitalRouteApp() }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Inicio : Screen("inicio", "Início", Icons.Default.Home)
    object Mapas : Screen("mapas", "Mapas", Icons.Default.Map)
    object Iniciar : Screen("iniciar", "Iniciar", Icons.Default.RadioButtonChecked)
    object Seguranca : Screen("seguranca", "Segurança", Icons.Default.Shield)
    object Perfil : Screen("perfil", "Perfil", Icons.Default.BarChart)
}

@Composable
fun VitalRouteApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { 
            if (currentRoute != "settings") {
                VitalBottomBar(navController)
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Inicio.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Inicio.route) { HomeScreen() }
            composable(Screen.Mapas.route) { MapsScreen() }
            composable(Screen.Iniciar.route) { RecordingScreen() }
            composable(Screen.Seguranca.route) { SecurityScreen() }
            composable(Screen.Perfil.route) { DiaryScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
        }
    }
}

@Composable
fun VitalBottomBar(navController: NavHostController) {
    val items = listOf(Screen.Inicio, Screen.Mapas, Screen.Iniciar, Screen.Seguranca, Screen.Perfil)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier.fillMaxWidth().height(85.dp),
        color = Color.Black,
        border = BorderStroke(0.5.dp, Color.DarkGray)
    ) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route
                if (screen is Screen.Iniciar) {
                    Box(modifier = Modifier.weight(1.2f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { navController.navigate(screen.route) }, modifier = Modifier.size(60.dp).background(Brush.verticalGradient(listOf(VitalRed, VitalOrange)), CircleShape)) {
                            Icon(screen.icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f).clickable { navController.navigate(screen.route) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(screen.icon, null, tint = if (isSelected) VitalGreen else Color.Gray, modifier = Modifier.size(24.dp))
                        Text(text = screen.label, color = if (isSelected) VitalGreen else Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// --- ECRÃ DE DEFINIÇÕES GERAIS ---

@Composable
fun SettingsScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Text("DEFINIÇÕES GERAIS", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SectionHeader("PERFIL E CONTA")
            SettingsItem(Icons.Default.AccountCircle, "Dados Pessoais", "Nome, Peso, Idade")
            SettingsItem(Icons.Default.Email, "Email", "duarte@vitalroute.pt")
            SettingsItem(Icons.Default.Lock, "Segurança", "Alterar palavra-passe")

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("APP E UNIDADES")
            var metricSystem by remember { mutableStateOf(true) }
            SettingsToggle(Icons.Default.Straighten, "Sistema Métrico (km/h)", metricSystem) { metricSystem = it }
            
            var autostop by remember { mutableStateOf(true) }
            SettingsToggle(Icons.Default.Timer, "Auto-Pausa", autostop) { autostop = it }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("MAPAS E DADOS")
            SettingsItem(Icons.Default.CloudDownload, "Gerir Mapas Offline", "2.4 GB utilizados")
            SettingsItem(Icons.Default.History, "Limpar Histórico de Rotas", "Liberta espaço no dispositivo")

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("SENSORES EXTERNOS")
            SettingsItem(Icons.Default.Bluetooth, "Sensores Bluetooth", "Cinta Cardíaca, Cadência")

            Spacer(modifier = Modifier.height(32.dp))
            Card(
                colors = CardDefaults.cardColors(VitalOrange.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth().border(1.dp, VitalOrange, RoundedCornerShape(12.dp))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, null, tint = VitalOrange)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("VITALROUTE PREMIUM", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Plano Mensal Ativo", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("GERIR", color = VitalOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            Text("Terminar Sessão", color = Color.Red, modifier = Modifier.fillMaxWidth().clickable { }, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Versão 1.0.4 - VitalRoute", color = Color.DarkGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, sub: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).clickable { }, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            if (sub != null) Text(sub, color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.NavigateNext, null, tint = Color.DarkGray)
    }
}

@Composable
fun SettingsToggle(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp)); Text(title, Modifier.weight(1f), color = Color.White)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = VitalGreen))
    }
}

// --- ECRÃS EXISTENTES ---

@Composable
fun DiaryScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("O MEU DIÁRIO", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("(Histórico e estatísticas)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(32.dp)); SectionHeader("RESUMO DO MÊS")
        Card(colors = CardDefaults.cardColors(CardGray), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { DiaryStat("🚴‍♂️ 340 km", "Total"); DiaryStat("⛰️ 4.200m", "Elevação") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { DiaryStat("⏱️ 14h 20m", "Ativo"); DiaryStat("🛡️ 0", "Incidentes") }
            }
        }
        Spacer(Modifier.height(32.dp)); SectionHeader("HISTÓRICO")
        HistoryItem("Terça, 10 Março", "Fim de Tarde", "45.2 km", "22 km/h")
        Spacer(Modifier.height(32.dp)); SectionHeader("OPÇÕES")
        Button(onClick = { navController.navigate("settings") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Color.DarkGray)) {
            Icon(Icons.Default.Settings, null); Spacer(Modifier.width(8.dp)); Text("Definições Gerais")
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun HomeScreen() {
    Column(Modifier.fillMaxSize().background(DarkBackground).verticalScroll(rememberScrollState()).padding(20.dp), Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Icon(Icons.Default.CloudDone, null, tint = VitalGreen, modifier = Modifier.size(28.dp))
            Text("100% 🔋", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp)); Icon(Icons.Default.Shield, null, tint = VitalGreen, modifier = Modifier.size(80.dp))
        Text("PRONTO PARA ARRANCAR", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(48.dp)); SectionHeader("SENSORES")
        Card(colors = CardDefaults.cardColors(CardGray), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorRow(Icons.Default.SignalCellularAlt, "GPS", "Forte", VitalGreen)
                SensorRow(Icons.Default.BatteryFull, "Bateria", "82%", VitalGreen)
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun StatItem(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = VitalGreen, modifier = Modifier.size(18.dp))
        Text(value, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun LastWorkoutCard() {
    Card(colors = CardDefaults.cardColors(CardGray), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF252525), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("🗺️ Mapa do Trajeto", color = Color.Gray) }
            Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                StatItem(Icons.AutoMirrored.Filled.DirectionsBike, "45.2 km")
                StatItem(Icons.Default.Timer, "2h 15m")
            }
        }
    }
}

@Composable
fun SensorRow(icon: ImageVector, label: String, status: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray); Text(label, Modifier.weight(1f).padding(start = 12.dp), Color.White)
        Box(Modifier.size(8.dp).background(color, CircleShape)); Text(status, Modifier.padding(start = 8.dp), Color.White, fontSize = 13.sp)
    }
}

@Composable
fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = Color.Gray, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.DarkGray)
    }
}

@Composable
fun DiaryStat(value: String, label: String) {
    Column { Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(label, color = Color.Gray, fontSize = 12.sp) }
}

@Composable
fun HistoryItem(date: String, title: String, dist: String, speed: String) {
    Card(colors = CardDefaults.cardColors(CardGray), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(date, color = Color.Gray, fontSize = 12.sp); Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { DiaryStat(dist, "Distância"); DiaryStat(speed, "Velocidade") }
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, subLabel: String, modifier: Modifier) {
    OutlinedButton(onClick = {}, modifier = modifier.height(80.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.DarkGray)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = VitalGreen, modifier = Modifier.size(18.dp))
                Text(label, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
            }
            Text(subLabel, color = Color.Gray, fontSize = 9.sp)
        }
    }
}

@Composable
fun MapsScreen() {
    Column(Modifier.fillMaxSize().background(DarkBackground).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("MAPAS & ROTAS", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(20.dp)); Box(Modifier.fillMaxWidth().height(280.dp).background(Color(0xFF111111)))
        Spacer(Modifier.height(20.dp)); SectionHeader("SEGURANÇA NO LOCAL")
        SafetyInfoItem(Icons.Default.MedicalServices, "SOCORRO", "2 Próximos", VitalOrange)
    }
}

@Composable
fun RecordingScreen() {
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A0505)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("EM GRAVAÇÃO", color = Color.Red, fontWeight = FontWeight.Bold)
            Text("🟢 LIVE", color = VitalGreen, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(32.dp)); Row(Modifier.fillMaxWidth()) {
            BigMetric("⏱️ TEMPO", "01:24:15", Modifier.weight(1f))
            BigMetric("📏 KM", "32.5", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(40.dp)); SosSlider(onSosTriggered = {})
    }
}

@Composable
fun SecurityScreen() {
    Column(Modifier.fillMaxSize().background(DarkBackground).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("SEGURANÇA", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(32.dp)); SectionHeader("REDE DE CONFIANÇA")
        ContactRow("Ana (Esposa)", true, true)
        Spacer(modifier = Modifier.height(24.dp)); SectionHeader("DETEÇÃO DE QUEDA")
        Slider(value = 0.5f, onValueChange = {}, colors = SliderDefaults.colors(activeTrackColor = VitalOrange))
    }
}

@Composable
fun ContactRow(name: String, sos: Boolean, zones: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Person, null, tint = Color.Gray); Text(name, Modifier.weight(1f).padding(start = 12.dp), Color.White)
        if(sos) Badge("SOS"); if(zones) Badge("ZONAS")
    }
}

@Composable
fun Badge(label: String) {
    Surface(Modifier.padding(start = 4.dp), VitalGreen.copy(0.1f), border = BorderStroke(1.dp, VitalGreen), shape = RoundedCornerShape(4.dp)) {
        Text(label, Modifier.padding(4.dp, 2.dp), VitalGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SafetyInfoItem(icon: ImageVector, title: String, sub: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color.DarkGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color) }
        Column(Modifier.weight(1f).padding(start = 16.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(sub, color = Color.Gray, fontSize = 12.sp) }
        Icon(Icons.AutoMirrored.Filled.NavigateNext, null, tint = Color.Gray)
    }
}

@Composable
fun BigMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) { Text(label, color = Color.Gray, fontSize = 12.sp); Text(value, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black) }
}

@Composable
fun SosSlider(onSosTriggered: () -> Unit) {
    val density = LocalDensity.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val maxOffset = (widthPx - with(density) { (56.dp + 48.dp).toPx() }).coerceAtLeast(0f)
    Box(Modifier.fillMaxWidth().height(64.dp).onGloballyPositioned { widthPx = it.size.width }) {
        Box(Modifier.fillMaxSize().background(VitalRed.copy(0.15f), CircleShape).border(2.dp, VitalRed.copy(0.5f), CircleShape), contentAlignment = Alignment.Center) { Text("DESLIZAR PARA SOS", color = Color.White.copy(0.6f), fontWeight = FontWeight.Black) }
        Box(modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }.padding(4.dp).size(56.dp).background(VitalRed, CircleShape).pointerInput(maxOffset) { detectDragGestures(onDragEnd = { if (offsetX >= maxOffset * 0.8f) onSosTriggered(); offsetX = 0f }, onDrag = { change, drag -> change.consume(); offsetX = (offsetX + drag.x).coerceIn(0f, maxOffset) }) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.KeyboardDoubleArrowRight, null, tint = Color.White) }
    }
}

@Composable
fun StopButton(onStop: () -> Unit) {
    var isPressing by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (isPressing) 1f else 0f, tween(1500, easing = LinearEasing), finishedListener = { if(it == 1f) onStop() })
    Box(Modifier.width(160.dp).height(60.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black).border(2.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp)).pointerInput(Unit) { detectTapGestures(onPress = { isPressing = true; try { awaitRelease() } finally { isPressing = false } }) }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color.White.copy(0.2f)).align(Alignment.CenterStart))
        Text("PARAR", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

fun Modifier.clickable(onClick: () -> Unit) = this.then(Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) })
