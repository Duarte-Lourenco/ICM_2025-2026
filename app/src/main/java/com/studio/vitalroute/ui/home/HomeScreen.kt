package com.studio.vitalroute.ui.home

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.vitalroute.ui.theme.*
import com.studio.vitalroute.ui.components.SectionHeader
import com.studio.vitalroute.ui.components.SensorRow

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        // ── Top bar ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = uiState.greeting, color = Color.Gray, fontSize = 13.sp)
                Text(
                    text = "Duarte",
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
                    Icon(Icons.Default.SignalCellularAlt, null, tint = VitalGreen, modifier = Modifier.size(14.dp))
                    Text("GPS", color = VitalGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Default.BatteryFull, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(uiState.batteryLevel, color = Color.White, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Sistema operacional ───────────────────────────────
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

        // ── Esta semana ───────────────────────────────────────
        SectionHeader("ESTA SEMANA")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WeekStat("🚴", uiState.weeklyKm, "km")
                    VerticalDivider(modifier = Modifier.height(44.dp), color = Color(0xFF2A2A2A))
                    WeekStat("⏱️", uiState.weeklyTime, "ativo")
                    VerticalDivider(modifier = Modifier.height(44.dp), color = Color(0xFF2A2A2A))
                    WeekStat("🛡️", uiState.weeklyIncidents, "incid.")
                }
                Spacer(Modifier.height(16.dp))
                val progress = (uiState.weeklyKm.toFloatOrNull() ?: 0f) / uiState.weeklyGoalKm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Objetivo semanal", color = Color.Gray, fontSize = 11.sp)
                    Text("${uiState.weeklyKm} / ${uiState.weeklyGoalKm.toInt()} km", color = Color.Gray, fontSize = 11.sp)
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

        // ── Última atividade ──────────────────────────────────
        SectionHeader("ÚLTIMA ATIVIDADE")
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
                        Icon(Icons.Default.DirectionsBike, null, tint = VitalOrange, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ciclismo", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text("Terça, 10 Mar", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("📏", "45.2 km", "Distância")
                    MiniStat("⏱️", "1h 22m", "Duração")
                    MiniStat("⚡", "22 km/h", "Veloc.")
                    MiniStat("⛰️", "320 m", "Elevação")
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Sensores ──────────────────────────────────────────
        SectionHeader("SENSORES")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SensorRow(Icons.Default.SignalCellularAlt, "GPS", uiState.gpsStatus, VitalGreen)
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
