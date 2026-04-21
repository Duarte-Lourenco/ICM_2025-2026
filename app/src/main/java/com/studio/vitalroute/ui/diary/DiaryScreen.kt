package com.studio.vitalroute.ui.diary

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        // ── Cabeçalho ─────────────────────────────────────────
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
            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(Icons.Default.Settings, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VitalGreen)
            }
        } else {

            // ── Resumo do mês ─────────────────────────────────
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

            // ── Recordes pessoais ─────────────────────────────
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

            // ── Histórico de atividades ───────────────────────
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
                uiState.activities.forEach { activity ->
                    ActivityCard(activity)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────
//  Componentes privados
// ─────────────────────────────────────────────────────────────

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
private fun ActivityCard(activity: ActivityUiItem) {
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
                        imageVector = if (activity.type == "running") Icons.Default.DirectionsRun
                                      else Icons.Default.DirectionsBike,
                        contentDescription = null,
                        tint = VitalOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (activity.type == "running") "Corrida" else "Ciclismo",
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
