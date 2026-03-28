package com.studio.vitalroute.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.vitalroute.ui.theme.*
import com.studio.vitalroute.ui.components.SectionHeader
import com.studio.vitalroute.ui.components.SensorRow

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Icon(Icons.Default.CloudDone, null, tint = VitalGreen, modifier = Modifier.size(28.dp))
            Text("100% 🔋", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
        Icon(Icons.Default.Shield, null, tint = VitalGreen, modifier = Modifier.size(80.dp))
        Text("PRONTO PARA ARRANCAR", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(48.dp))
        SectionHeader("SENSORES")
        Card(colors = CardDefaults.cardColors(CardGray), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorRow(Icons.Default.SignalCellularAlt, "GPS", uiState.gpsStatus, VitalGreen)
                SensorRow(Icons.Default.BatteryFull, "Bateria", uiState.batteryLevel, VitalGreen)
            }
        }
    }
}