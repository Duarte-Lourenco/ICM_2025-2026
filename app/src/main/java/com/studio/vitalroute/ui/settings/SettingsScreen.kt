package com.studio.vitalroute.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
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
import androidx.navigation.NavHostController
import com.studio.vitalroute.ui.theme.*
import com.studio.vitalroute.ui.components.SectionHeader

@Composable
fun SettingsScreen(
    navController: NavHostController,
    onSignOut: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
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
            SettingsItem(Icons.Default.Email, "Email", uiState.email)
            SettingsItem(Icons.Default.Lock, "Segurança", "Alterar palavra-passe")

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("APP E UNIDADES")
            SettingsToggle(Icons.Default.Straighten, "Sistema Métrico (km/h)", uiState.metricSystem) {
                viewModel.toggleMetricSystem(it)
            }
            SettingsToggle(Icons.Default.Timer, "Auto-Pausa", uiState.autoPause) {
                viewModel.toggleAutoPause(it)
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("MAPAS E DADOS")
            SettingsItem(Icons.Default.CloudDownload, "Gerir Mapas Offline", uiState.offlineStorage)
            SettingsItem(Icons.Default.History, "Limpar Histórico de Rotas", "Liberta espaço no dispositivo")

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("SENSORES EXTERNOS")
            SettingsItem(
                icon     = Icons.Default.Bluetooth,
                title    = "Sensores Bluetooth",
                sub      = "Deteção de quedas & SOS automático",
                onClick  = { navController.navigate("bluetooth") }
            )

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
            Text("Terminar Sessão", color = Color.Red, modifier = Modifier.fillMaxWidth().clickable { onSignOut() }, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Versão 1.0.4 - VitalRoute", color = Color.DarkGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, sub: String? = null, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, Modifier.weight(1f), color = Color.White)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = VitalGreen))
    }
}