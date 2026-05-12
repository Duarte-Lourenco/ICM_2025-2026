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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
            SettingsItem(
                icon  = Icons.Default.AccountCircle,
                title = "Nome de Exibição",
                sub   = uiState.displayName.ifBlank { "— não definido —" },
                onClick = { viewModel.startEditName() }
            )
            SettingsItem(Icons.Default.Email, "Email", uiState.email.ifBlank { "—" })
            SettingsItem(Icons.Default.Lock, "Segurança", "Alterar palavra-passe")

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("APP E UNIDADES")
            SettingsToggle(Icons.Default.Straighten, "Sistema Métrico (km/h)", uiState.metricSystem) {
                viewModel.toggleMetricSystem(it)
            }
            SettingsToggle(Icons.Default.Timer, "Auto-Pausa", uiState.autoPause) {
                viewModel.toggleAutoPause(it)
            }
            // Objetivo semanal
            Card(
                colors = CardDefaults.cardColors(CardGray),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, null,
                                tint = VitalOrange, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Objetivo Semanal", color = Color.White,
                                fontWeight = FontWeight.Medium)
                        }
                        Text("${uiState.weeklyGoalKm.toInt()} km",
                            color = VitalOrange, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = uiState.weeklyGoalKm,
                        onValueChange = { viewModel.setWeeklyGoal(it) },
                        valueRange = 10f..300f,
                        steps = 57,  // 5 km steps
                        colors = SliderDefaults.colors(
                            thumbColor = VitalOrange,
                            activeTrackColor = VitalOrange
                        )
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("10 km", color = Color.DarkGray, fontSize = 10.sp)
                        Text("300 km", color = Color.DarkGray, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

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

    // diálogo edição de nome
    if (uiState.isEditingName) {
        Dialog(onDismissRequest = { viewModel.cancelEditName() }) {
            Card(
                colors = CardDefaults.cardColors(CardGray),
                shape  = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Editar Nome", color = Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value         = uiState.editNameValue,
                        onValueChange = { viewModel.updateEditName(it) },
                        label         = { Text("Nome") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.saveName() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = VitalGreen,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor    = VitalGreen,
                            unfocusedLabelColor  = Color.Gray,
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White,
                            cursorColor          = VitalGreen
                        )
                    )
                    uiState.saveNameError?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = VitalRed, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = { viewModel.cancelEditName() },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                        ) { Text("Cancelar") }
                        Button(
                            onClick  = { viewModel.saveName() },
                            enabled  = uiState.editNameValue.isNotBlank() && !uiState.isSavingName,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = VitalGreen)
                        ) {
                            if (uiState.isSavingName) {
                                CircularProgressIndicator(
                                    color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp
                                )
                            } else {
                                Text("Guardar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
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