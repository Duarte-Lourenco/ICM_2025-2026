package com.studio.vitalroute.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.navigation.NavHostController
import com.studio.vitalroute.ui.theme.*
import com.studio.vitalroute.ui.components.SectionHeader
import com.studio.vitalroute.ui.components.DiaryStat
import com.studio.vitalroute.ui.components.HistoryItem

@Composable
fun DiaryScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("O MEU DIÁRIO", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("(Histórico e estatísticas)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(32.dp))
        SectionHeader("RESUMO DO MÊS")
        Card(colors = CardDefaults.cardColors(CardGray), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DiaryStat("🚴‍♂️ 340 km", "Total")
                    DiaryStat("⛰️ 4.200m", "Elevação")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DiaryStat("⏱️ 14h 20m", "Ativo")
                    DiaryStat("🛡️ 0", "Incidentes")
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        SectionHeader("HISTÓRICO")
        HistoryItem("Terça, 10 Março", "Fim de Tarde", "45.2 km", "22 km/h")
        Spacer(Modifier.height(32.dp))
        SectionHeader("OPÇÕES")
        Button(
            onClick = { navController.navigate("settings") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(Color.DarkGray)
        ) {
            Icon(Icons.Default.Settings, null)
            Spacer(Modifier.width(8.dp))
            Text("Definições Gerais")
        }
        Spacer(Modifier.height(40.dp))
    }
}