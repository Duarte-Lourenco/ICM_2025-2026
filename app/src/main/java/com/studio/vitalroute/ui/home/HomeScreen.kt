package com.studio.vitalroute.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.studio.vitalroute.ui.theme.*
import com.studio.vitalroute.ui.components.SectionHeader
import com.studio.vitalroute.ui.components.SensorRow

@Composable
fun HomeScreen(navController: NavHostController, viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        // top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = uiState.greeting, color = Color.Gray, fontSize = 13.sp)
                Text(
                    text = uiState.userName.ifBlank { "Atleta" },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val gpsColor = if (uiState.gpsEnabled) VitalGreen else Color(0xFFFF5252)
                    Icon(Icons.Default.SignalCellularAlt, null, tint = gpsColor, modifier = Modifier.size(14.dp))
                    Text("GPS", color = gpsColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Default.BatteryFull, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(uiState.batteryLevel, color = Color.White, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // sistema operacional
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF071207))
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(VitalGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, null, tint = VitalGreen, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "SISTEMA OPERACIONAL",
                        color = VitalGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Pronto para Arrancar",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Todos os sensores ativos · 1 contacto SOS",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // esta semana
        SectionHeader("ESTA SEMANA")
        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController.navigate("perfil") },
            colors = CardDefaults.cardColors(containerColor = CardGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WeekStat("🚴", uiState.weeklyKm, uiState.distUnit)
                    VerticalDivider(modifier = Modifier.height(44.dp), color = Color(0xFF2A2A2A))
                    WeekStat("⏱️", uiState.weeklyTime, "ativo")
                    VerticalDivider(modifier = Modifier.height(44.dp), color = Color(0xFF2A2A2A))
                    WeekStat("🛡️", uiState.weeklyIncidents, "incid.")
                }
                Spacer(Modifier.height(16.dp))
                val progress = uiState.weeklyGoalProgress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Objetivo semanal", color = Color.Gray, fontSize = 11.sp)
                    Text("${uiState.weeklyKm} / ${uiState.weeklyGoalDisplay} ${uiState.distUnit}", color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = VitalOrange,
                    trackColor = Color(0xFF2A2A2A)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // última atividade
        SectionHeader("ÚLTIMA ATIVIDADE")
        if (uiState.lastActivityDate.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("perfil") },
                colors = CardDefaults.cardColors(containerColor = CardGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🚴", fontSize = 30.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Nenhuma atividade ainda", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Inicia uma gravação para ver aqui o teu historial.",
                        color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (uiState.lastActivityId.isNotEmpty())
                        navController.navigate("activity/${uiState.lastActivityId}")
                    else
                        navController.navigate("perfil")
                },
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
                                imageVector = when (uiState.lastActivityType) {
                                    "running" -> Icons.Default.DirectionsRun
                                    "walking" -> Icons.Default.DirectionsWalk
                                    else      -> Icons.Default.DirectionsBike
                                },
                                contentDescription = null,
                                tint = VitalOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when (uiState.lastActivityType) {
                                    "running" -> "Corrida"
                                    "walking" -> "Caminhada"
                                    else      -> "Ciclismo"
                                },
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                        }
                        Text(uiState.lastActivityDate, color = Color.Gray, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("📏", uiState.lastActivityDist,  "Distância")
                        MiniStat("⏱️", uiState.lastActivityTime,  "Duração")
                        MiniStat("⚡", uiState.lastActivitySpeed, "Veloc.")
                        MiniStat("⛰️", uiState.lastActivityElev,  "Elevação")
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // sensores
        SectionHeader("SENSORES")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SensorRow(Icons.Default.SignalCellularAlt, "GPS", uiState.gpsStatus, if (uiState.gpsEnabled) VitalGreen else Color(0xFFFF5252))
                HorizontalDivider(color = Color(0xFF222222))
                SensorRow(Icons.Default.BatteryFull, "Bateria", uiState.batteryLevel, VitalGreen)
                HorizontalDivider(color = Color(0xFF222222))
                SensorRow(Icons.Default.Sensors, "Acelerómetro / Giroscópio", "Ativo", VitalGreen)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun WeekStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun MiniStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}
