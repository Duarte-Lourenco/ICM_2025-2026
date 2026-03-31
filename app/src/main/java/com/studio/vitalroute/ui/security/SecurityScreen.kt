package com.studio.vitalroute.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.studio.vitalroute.ui.components.SectionHeader
import com.studio.vitalroute.ui.theme.*

@Composable
fun SecurityScreen(viewModel: SecurityViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "SEGURANÇA",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(20.dp))

        // ── Estado geral de proteção ──────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF071207))
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(VitalGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, null, tint = VitalGreen, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "PROTEÇÃO ATIVA",
                        color = VitalGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Monitorização Contínua",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${uiState.contacts.size} contactos · Queda: Ativa · SOS: ${uiState.sosCountdownSecs}s",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Rede de confiança ─────────────────────────────────
        SectionHeader("REDE DE CONFIANÇA")
        uiState.contacts.forEach { contact ->
            ContactCard(contact)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VitalOrange),
            border = androidx.compose.foundation.BorderStroke(1.dp, VitalOrange.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Adicionar Contacto de Confiança", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(28.dp))

        // ── Deteção de queda ──────────────────────────────────
        SectionHeader("DETEÇÃO DE QUEDA")
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
                        Icon(Icons.Default.Warning, null, tint = VitalOrange, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sensibilidade do Sensor", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    SensitivityBadge(uiState.fallSensitivity)
                }
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = uiState.fallSensitivity,
                    onValueChange = { viewModel.updateFallSensitivity(it) },
                    colors = SliderDefaults.colors(
                        activeTrackColor = VitalOrange,
                        thumbColor = VitalOrange,
                        inactiveTrackColor = Color(0xFF2A2A2A)
                    )
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Baixa", color = Color.Gray, fontSize = 11.sp)
                    Text("Média", color = Color.Gray, fontSize = 11.sp)
                    Text("Alta", color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Após deteção de queda, contagem de ${uiState.sosCountdownSecs}s antes de enviar SOS automático.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Zonas seguras ─────────────────────────────────────
        SectionHeader("ZONAS SEGURAS")
        uiState.safeZones.forEach { zone ->
            SafeZoneCard(zone)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(20.dp))

        // ── Alertas automáticos ───────────────────────────────
        SectionHeader("ALERTAS AUTOMÁTICOS")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                AlertToggleRow(
                    icon = Icons.Default.Timer,
                    title = "Imobilidade prolongada",
                    subtitle = "Alerta após ${uiState.immobilityMinutes} min sem movimento",
                    enabled = uiState.immobilityAlertEnabled,
                    onToggle = { viewModel.toggleImmobilityAlert(it) }
                )
                HorizontalDivider(color = Color(0xFF2A2A2A))
                AlertToggleRow(
                    icon = Icons.Default.Home,
                    title = "Chegada a zona segura",
                    subtitle = "Notifica contactos ao chegar a casa",
                    enabled = uiState.arrivalAlertEnabled,
                    onToggle = { viewModel.toggleArrivalAlert(it) }
                )
                HorizontalDivider(color = Color(0xFF2A2A2A))
                AlertToggleRow(
                    icon = Icons.Default.Route,
                    title = "Desvio de rota",
                    subtitle = "Alerta se sair do percurso planeado",
                    enabled = uiState.routeDeviationEnabled,
                    onToggle = { viewModel.toggleRouteDeviation(it) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────
//  Componentes privados
// ─────────────────────────────────────────────────────────────

@Composable
private fun ContactCard(contact: TrustedContact) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar com inicial do nome
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF2A2A2A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.first().toString(),
                    color = VitalOrange,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${contact.relation} · ${contact.phone}", color = Color.Gray, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (contact.sosEnabled)   ContactBadge("SOS",   VitalRed)
                if (contact.zonesEnabled) ContactBadge("ZONAS", VitalGreen)
            }
        }
    }
}

@Composable
private fun ContactBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SensitivityBadge(value: Float) {
    val (label, color) = when {
        value < 0.33f -> "BAIXA" to VitalGreen
        value < 0.66f -> "MÉDIA" to VitalOrange
        else          -> "ALTA"  to VitalRed
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SafeZoneCard(zone: SafeZone) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(VitalGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(zone.icon, null, tint = VitalGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(zone.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text(zone.address, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.CheckCircle, null, tint = VitalGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AlertToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = VitalGreen)
        )
    }
}
