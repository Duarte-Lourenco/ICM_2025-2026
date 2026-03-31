package com.studio.vitalroute.ui.recording

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.vitalroute.ui.components.SosSlider
import com.studio.vitalroute.ui.theme.*

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Cabeçalho ─────────────────────────────────────────
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
            // Indicador de estado com pulsação visual
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

        Spacer(Modifier.height(24.dp))

        // ── Tempo em destaque ─────────────────────────────────
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

        // ── Grelha de métricas 2×2 ────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Straighten,
                label = "DISTÂNCIA",
                value = uiState.distance,
                unit = "km",
                accentColor = Color.White
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                label = "VELOCIDADE",
                value = uiState.speed,
                unit = "km/h",
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
                label = "ELEVAÇÃO",
                value = uiState.elevation,
                unit = "m",
                accentColor = Color(0xFF64B5F6) // azul suave
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFireDepartment,
                label = "CALORIAS",
                value = uiState.calories,
                unit = "kcal",
                accentColor = Color(0xFFFF8A65) // laranja queimado
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Botão Start / Stop ────────────────────────────────
        Button(
            onClick = {
                if (uiState.isRecording) viewModel.stopRecording()
                else                     viewModel.startRecording()
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

        // ── Informação de segurança ───────────────────────────
        if (!uiState.isRecording) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1A0D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, null, tint = VitalGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Deteção de queda e SOS automático ativos durante a gravação",
                        color = Color(0xFF81C784),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── SOS Slider ────────────────────────────────────────
        SosSlider(onSosTriggered = { viewModel.triggerSos() })

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────
//  Componentes privados
// ─────────────────────────────────────────────────────────────

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
            Text(
                text = unit,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
