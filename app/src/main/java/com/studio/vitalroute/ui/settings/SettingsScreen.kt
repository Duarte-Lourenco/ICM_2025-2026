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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
            SettingsItem(Icons.Default.Email, "Email", uiState.email.ifBlank { "—" },
                onClick = { viewModel.startChangeEmail() })

            // Peso
            SettingsItem(
                icon  = Icons.Default.MonitorWeight,
                title = "Peso",
                sub   = "${uiState.weightKg.toInt()} kg",
                onClick = { viewModel.startEditWeight() }
            )
            if (uiState.isEditingWeight) {
                ProfileEditField(
                    label        = "Peso (kg)",
                    value        = uiState.editWeightValue,
                    keyboardType = KeyboardType.Number,
                    onValueChange = { viewModel.updateEditWeight(it) },
                    onSave       = { viewModel.saveWeight() },
                    onCancel     = { viewModel.cancelEditWeight() }
                )
            }

            // Altura
            SettingsItem(
                icon  = Icons.Default.Height,
                title = "Altura",
                sub   = "${uiState.heightCm} cm",
                onClick = { viewModel.startEditHeight() }
            )
            if (uiState.isEditingHeight) {
                ProfileEditField(
                    label        = "Altura (cm)",
                    value        = uiState.editHeightValue,
                    keyboardType = KeyboardType.Number,
                    onValueChange = { viewModel.updateEditHeight(it) },
                    onSave       = { viewModel.saveHeight() },
                    onCancel     = { viewModel.cancelEditHeight() }
                )
            }

            // Género
            Card(
                colors   = CardDefaults.cardColors(CardGray),
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Género", color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("male" to "Masculino", "female" to "Feminino").forEach { (key, label) ->
                            FilterChip(
                                selected = uiState.gender == key,
                                onClick  = { viewModel.setGender(key) },
                                label    = { Text(label, fontSize = 13.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1565C0),
                                    selectedLabelColor     = Color.White,
                                    containerColor         = Color(0xFF1A2A3A),
                                    labelColor             = Color.Gray
                                )
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            SettingsItem(Icons.Default.Lock, "Segurança", "Alterar palavra-passe",
                onClick = { viewModel.startChangePassword() })

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

    // diálogo alterar password
    if (uiState.isChangingPassword) {
        var showCurrent  by remember { mutableStateOf(false) }
        var showNew      by remember { mutableStateOf(false) }
        var showConfirm  by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { viewModel.cancelChangePassword() }) {
            Card(colors = CardDefaults.cardColors(CardGray), shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Alterar Password", color = Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))

                    if (uiState.passwordChangeSuccess) {
                        Surface(color = Color(0xFF002A00), shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp, 10.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = VitalGreen, modifier = Modifier.size(16.dp))
                                Text("Password alterada com sucesso!", color = VitalGreen, fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.cancelChangePassword() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VitalGreen)) {
                            Text("Fechar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        PasswordField("Password atual", uiState.currentPasswordInput, showCurrent,
                            { viewModel.updateCurrentPasswordInput(it) }, { showCurrent = !showCurrent })
                        Spacer(Modifier.height(10.dp))
                        PasswordField("Nova password", uiState.newPasswordInput, showNew,
                            { viewModel.updateNewPasswordInput(it) }, { showNew = !showNew })
                        Spacer(Modifier.height(4.dp))
                        Text("Mínimo 8 caracteres, com maiúscula, minúscula e número",
                            color = Color(0xFF666666), fontSize = 11.sp)
                        Spacer(Modifier.height(10.dp))
                        PasswordField("Confirmar nova password", uiState.confirmPasswordInput, showConfirm,
                            { viewModel.updateConfirmPasswordInput(it) }, { showConfirm = !showConfirm })

                        uiState.passwordChangeError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = VitalRed, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { viewModel.cancelChangePassword() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                            ) { Text("Cancelar") }
                            Button(onClick = { viewModel.savePassword() },
                                enabled = !uiState.passwordChangeLoading,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = VitalGreen)) {
                                if (uiState.passwordChangeLoading)
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                else Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // diálogo alterar email
    if (uiState.isChangingEmail) {
        var showPassword by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { viewModel.cancelChangeEmail() }) {
            Card(colors = CardDefaults.cardColors(CardGray), shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Alterar Email", color = Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))

                    if (uiState.emailChangeSuccess) {
                        Surface(color = Color(0xFF002A00), shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp, 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = VitalGreen, modifier = Modifier.size(16.dp))
                                    Text("Link de confirmação enviado!", color = VitalGreen, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Clica no link enviado para ${uiState.newEmailInput.trim()} para confirmar a alteração.",
                                    color = Color(0xFF88CC88), fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.cancelChangeEmail() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VitalGreen)) {
                            Text("Fechar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedTextField(
                            value = uiState.newEmailInput,
                            onValueChange = { viewModel.updateNewEmailInput(it) },
                            label = { Text("Novo email") },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.Gray) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            colors = settingsFieldColors()
                        )
                        Spacer(Modifier.height(10.dp))
                        PasswordField("Password atual", uiState.emailChangePasswordInput, showPassword,
                            { viewModel.updateEmailChangePasswordInput(it) }, { showPassword = !showPassword })

                        uiState.emailChangeError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = VitalRed, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { viewModel.cancelChangeEmail() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                            ) { Text("Cancelar") }
                            Button(onClick = { viewModel.saveEmail() },
                                enabled = !uiState.emailChangeLoading,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = VitalGreen)) {
                                if (uiState.emailChangeLoading)
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                else Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
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
fun ProfileEditField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors   = CardDefaults.cardColors(Color(0xFF0D1F2D)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value         = value,
                onValueChange = onValueChange,
                label         = { Text(label, color = Color.Gray, fontSize = 12.sp) },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor    = Color.White,
                    unfocusedTextColor  = Color.White,
                    focusedBorderColor  = Color(0xFF1565C0),
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text("Cancelar", color = Color.Gray) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onSave,
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text("Guardar") }
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

@Composable
private fun PasswordField(
    label: String,
    value: String,
    visible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.Gray) },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null, tint = Color.Gray
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        colors = settingsFieldColors()
    )
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = VitalGreen,
    unfocusedBorderColor = Color.DarkGray,
    focusedLabelColor    = VitalGreen,
    unfocusedLabelColor  = Color.Gray,
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    cursorColor          = VitalGreen,
    focusedContainerColor   = Color(0xFF111111),
    unfocusedContainerColor = Color(0xFF111111)
)