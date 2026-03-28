package com.studio.vitalroute.ui.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.vitalroute.ui.theme.*
import com.studio.vitalroute.ui.components.BigMetric
import com.studio.vitalroute.ui.components.SosSlider

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = viewModel()  // cria ou reutiliza o ViewModel
) {
    // observa o estado, quando este muda, o ecra muda tbm
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(20.dp)) {

        // dados do estado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A0505))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (uiState.isRecording) "EM GRAVAÇÃO" else "PARADO",
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
            Text("🟢 LIVE", color = VitalGreen, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // metricas
        Row(Modifier.fillMaxWidth()) {
            BigMetric("⏱️ TEMPO", uiState.elapsedTime, Modifier.weight(1f))
            BigMetric("📏 KM", uiState.distance, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(40.dp))

        // quando desliza, envia o evento para o ViewModel
        SosSlider(onSosTriggered = { viewModel.triggerSos() })
    }
}