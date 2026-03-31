package com.studio.vitalroute.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.studio.vitalroute.ui.components.SectionHeader
import com.studio.vitalroute.ui.theme.*

// ─────────────────────────────────────────────────────────────
//  Modelos de dados locais
// ─────────────────────────────────────────────────────────────

private data class ActivityEntry(
    val type: String,
    val date: String,
    val period: String,
    val dist: String,
    val duration: String,
    val speed: String,
    val elevation: String
)

private val sampleActivities = listOf(
    ActivityEntry("Ciclismo", "Terça, 10 Mar",    "Fim de Tarde", "45.2 km", "1h 22m", "22 km/h", "320 m"),
    ActivityEntry("Ciclismo", "Domingo, 8 Mar",   "Manhã",        "32.1 km", "58 min",  "19 km/h", "180 m"),
    ActivityEntry("Corrida",  "Sábado, 5 Mar",    "Tarde",        "12.8 km", "1h 05m", "12 km/h",  "85 m"),
    ActivityEntry("Ciclismo", "Quarta, 1 Mar",    "Noite",        "67.8 km", "2h 10m", "31 km/h", "560 m")
)

// ─────────────────────────────────────────────────────────────
//  Ecrã principal
// ─────────────────────────────────────────────────────────────

@Composable
fun DiaryScreen(navController: NavHostController) {
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
                Text("Março 2025", color = Color.Gray, fontSize = 13.sp)
            }
            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(Icons.Default.Settings, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Resumo do mês ─────────────────────────────────────
        SectionHeader("RESUMO DO MÊS")
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
                    MonthStat("🚴", "340 km",   "Distância")
                    VerticalDivider(modifier = Modifier.height(52.dp), color = Color(0xFF2A2A2A))
                    MonthStat("⛰️", "4.200 m",  "Elevação")
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2A2A2A))
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MonthStat("⏱️", "14h 20m",  "Ativo")
                    VerticalDivider(modifier = Modifier.height(52.dp), color = Color(0xFF2A2A2A))
                    MonthStat("🛡️", "0",        "Incidentes")
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Recordes pessoais ─────────────────────────────────
        SectionHeader("RECORDES PESSOAIS")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                PersonalBestRow(Icons.Default.Straighten, "Distância Máxima",  "87.3 km",   "5 Mar 2025")
                HorizontalDivider(color = Color(0xFF2A2A2A))
                PersonalBestRow(Icons.Default.Speed,      "Velocidade Máxima", "38.2 km/h", "10 Mar 2025")
                HorizontalDivider(color = Color(0xFF2A2A2A))
                PersonalBestRow(Icons.Default.Timer,      "Maior Duração",     "3h 45m",    "22 Fev 2025")
                HorizontalDivider(color = Color(0xFF2A2A2A))
                PersonalBestRow(Icons.Default.Terrain,    "Maior Elevação",    "1.240 m",   "1 Mar 2025")
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Histórico de atividades ───────────────────────────
        SectionHeader("HISTÓRICO")
        sampleActivities.forEach { activity ->
            ActivityCard(activity)
            Spacer(Modifier.height(8.dp))
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
private fun PersonalBestRow(icon: ImageVector, label: String, value: String, date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(date, color = Color.Gray, fontSize = 11.sp)
        }
        Text(value, color = VitalOrange, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
private fun ActivityCard(activity: ActivityEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabeçalho do card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (activity.type == "Corrida") Icons.Default.DirectionsRun
                                      else Icons.Default.DirectionsBike,
                        contentDescription = null,
                        tint = VitalOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(activity.type, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(activity.period, color = Color.Gray, fontSize = 11.sp)
            }
            Text(activity.date, color = Color.Gray, fontSize = 11.sp)

            Spacer(Modifier.height(12.dp))

            // Métricas em linha
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ActivityMetric("📏", activity.dist,     "Distância")
                ActivityMetric("⏱️", activity.duration, "Duração")
                ActivityMetric("⚡", activity.speed,    "Veloc.")
                ActivityMetric("⛰️", activity.elevation,"Elevação")
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
