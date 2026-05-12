package com.studio.vitalroute.ui.bluetooth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.studio.vitalroute.ui.theme.*

@Composable
fun BluetoothScreen(
    navController: NavHostController,
    viewModel: BluetoothViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.init(context) }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Column {
                    Text(
                        "SENSORES BLUETOOTH",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Deteção de quedas & SOS automático",
                        color = Color.Gray, fontSize = 12.sp)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                // dispositivo conectado
                if (uiState.connectedDevice != null) {
                    ConnectedDeviceCard(
                        device      = uiState.connectedDevice!!,
                        statusLabel = uiState.gattStatusLabel,
                        onDisconnect = { viewModel.disconnect() }
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // feedback de último evento
                uiState.lastEventLabel?.let { label ->
                    val isAlert = label.contains("Queda") || label.contains("SOS enviado")
                    Card(
                        colors = CardDefaults.cardColors(
                            if (isAlert) VitalRed.copy(alpha = 0.15f)
                            else VitalGreen.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isAlert) VitalRed else VitalGreen,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isAlert) Icons.Default.Warning else Icons.Default.CheckCircle,
                                null,
                                tint = if (isAlert) VitalRed else VitalGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(label, color = Color.White, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // aviso modo simulado
                if (uiState.isSimulatedMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1200)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null,
                                tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Bluetooth não disponível neste dispositivo. A mostrar dispositivos de demonstração.",
                                color = Color(0xFFFFB300), fontSize = 12.sp, lineHeight = 17.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // sensor do telemóvel
                BtSectionHeader("SENSOR INTEGRADO DO TELEMÓVEL")
                Spacer(Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        if (uiState.phoneSensorActive) VitalGreen.copy(alpha = 0.1f) else CardGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (uiState.phoneSensorActive)
                                Modifier.border(1.dp, VitalGreen, RoundedCornerShape(14.dp))
                            else Modifier
                        ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (uiState.phoneSensorActive)
                                        VitalGreen.copy(alpha = 0.2f)
                                    else Color(0xFF2A2A2A),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PhoneAndroid, null,
                                tint = if (uiState.phoneSensorActive) VitalGreen else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Acelerómetro do Telemóvel",
                                color = Color.White, fontWeight = FontWeight.Medium
                            )
                            Text(
                                when {
                                    !uiState.phoneSensorAvailable -> "Sensor não disponível neste dispositivo"
                                    uiState.phoneSensorActive     -> "A monitorizar quedas"
                                    else                          -> "Inativo — ativa para detetar quedas sem pulseira"
                                },
                                color = if (uiState.phoneSensorActive) VitalGreen else Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = uiState.phoneSensorActive,
                            onCheckedChange = { viewModel.togglePhoneSensor(it) },
                            enabled = uiState.phoneSensorAvailable,
                            colors = SwitchDefaults.colors(checkedTrackColor = VitalGreen)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // scan
                BtSectionHeader("DISPOSITIVOS DISPONÍVEIS")
                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (uiState.isScanning) viewModel.stopScan()
                        else viewModel.startScan(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isScanning) Color.DarkGray else VitalGreen
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("A PROCURAR DISPOSITIVOS...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Bluetooth, null, tint = Color.White,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PROCURAR DISPOSITIVOS", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (uiState.discoveredDevices.isEmpty() && !uiState.isScanning) {
                    Text(
                        "Nenhum dispositivo encontrado.\nPrime o botão para iniciar a procura.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }

                uiState.discoveredDevices.forEach { device ->
                    DeviceListItem(
                        device           = device,
                        gattStatus       = uiState.gattStatus,
                        onConnect        = { viewModel.connect(device) }
                    )
                }

                // progresso da ligação gatt
                val isTransitioning = uiState.gattStatus !in listOf(
                    GattConnectionStatus.DISCONNECTED,
                    GattConnectionStatus.CONNECTED,
                    GattConnectionStatus.ERROR
                )
                AnimatedVisibility(
                    visible = isTransitioning || uiState.gattStatus == GattConnectionStatus.ERROR,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        GattStatusBanner(
                            status = uiState.gattStatus,
                            label  = uiState.gattStatusLabel
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(28.dp))

                // deteção de quedas
                BtSectionHeader("DETEÇÃO DE QUEDAS & SOS")
                Spacer(Modifier.height(12.dp))

                // Toggle principal
                Card(
                    colors = CardDefaults.cardColors(CardGray),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, null, tint = VitalOrange,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Deteção Automática de Quedas",
                                color = Color.White, fontWeight = FontWeight.Medium)
                            Text(
                                if (uiState.connectedDevice != null)
                                    "Ativo via ${uiState.connectedDevice!!.name}"
                                else "Liga um dispositivo para ativar",
                                color = Color.Gray, fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = uiState.fallDetectionEnabled,
                            onCheckedChange = { viewModel.toggleFallDetection(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = VitalOrange)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Sensibilidade
                AnimatedVisibility(visible = uiState.fallDetectionEnabled) {
                    Column {
                        Card(
                            colors = CardDefaults.cardColors(CardGray),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, null, tint = Color.Gray,
                                        modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Sensibilidade de Deteção",
                                        color = Color.White, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        sensitivityLabel(uiState.fallSensitivity),
                                        color = sensitivityColor(uiState.fallSensitivity),
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Slider(
                                    value = uiState.fallSensitivity,
                                    onValueChange = { viewModel.setFallSensitivity(it) },
                                    colors = SliderDefaults.colors(
                                        thumbColor = sensitivityColor(uiState.fallSensitivity),
                                        activeTrackColor = sensitivityColor(uiState.fallSensitivity)
                                    )
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Só quedas graves", color = Color.DarkGray, fontSize = 10.sp)
                                    Text("Qualquer impacto", color = Color.DarkGray, fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Delay SOS
                        Card(
                            colors = CardDefaults.cardColors(CardGray),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, null, tint = Color.Gray,
                                        modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Atraso antes de enviar SOS",
                                        color = Color.White, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.weight(1f))
                                    Text("${uiState.sosDelaySecs}s",
                                        color = VitalOrange, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(10, 15, 30, 60).forEach { secs ->
                                        val selected = uiState.sosDelaySecs == secs
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (selected) VitalOrange.copy(alpha = 0.2f)
                                                    else Color.DarkGray.copy(alpha = 0.3f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (selected) VitalOrange else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { viewModel.setSosDelay(secs) }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${secs}s",
                                                color = if (selected) VitalOrange else Color.Gray,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Tens ${uiState.sosDelaySecs} segundos para cancelar o SOS depois de uma queda ser detetada.",
                                    color = Color.Gray, fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Como funciona
                        Card(
                            colors = CardDefaults.cardColors(Color(0xFF1A1A2E)),
                            modifier = Modifier.fillMaxWidth().border(
                                1.dp, Color(0xFF2A2A4A), RoundedCornerShape(12.dp)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null,
                                        tint = Color(0xFF7B8FE8), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Como funciona", color = Color(0xFF7B8FE8),
                                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                InfoLine("O dispositivo monitoriza o acelerómetro em tempo real")
                                InfoLine("Uma queda detetada ativa um alerta haptico + countdown")
                                InfoLine("Se não cancelares, é enviada mensagem aos teus contactos SOS")
                                InfoLine("A mensagem inclui as tuas coordenadas GPS e hora")
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Botão testar
                        OutlinedButton(
                            onClick = { viewModel.simulateFall() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VitalRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VitalRed),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.BugReport, null,
                                tint = VitalRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("TESTAR DETEÇÃO DE QUEDA", color = VitalRed,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }

        // overlay sos countdown
        AnimatedVisibility(
            visible = uiState.isSosCountdown,
            enter = fadeIn() + scaleIn(),
            exit  = fadeOut() + scaleOut()
        ) {
            SosCountdownOverlay(
                remaining = uiState.sosCountdownRemaining,
                total     = uiState.sosDelaySecs,
                onCancel  = { viewModel.cancelSos() }
            )
        }

        // dialog sos enviado
        if (uiState.sosSent) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSosSent() },
                containerColor   = CardGray,
                icon = {
                    Icon(Icons.Default.SendTimeExtension, null,
                        tint = VitalRed, modifier = Modifier.size(40.dp))
                },
                title = {
                    Text("SOS Enviado!", color = VitalRed,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                },
                text = {
                    Text(
                        "Mensagem de emergência enviada aos teus contactos SOS com a tua localização GPS.",
                        color = Color.Gray, textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissSosSent() },
                        colors  = ButtonDefaults.buttonColors(containerColor = VitalGreen)
                    ) { Text("Estou Bem", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            )
        }
    }
}

// componentes privados

@Composable
private fun ConnectedDeviceCard(
    device: BleDevice,
    statusLabel: String,
    onDisconnect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(VitalGreen.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().border(1.dp, VitalGreen, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(VitalGreen.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(deviceIcon(device.type), null, tint = VitalGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, color = Color.White, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(VitalGreen, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("Conectado  •  ${device.rssi} dBm",
                        color = VitalGreen, fontSize = 12.sp)
                }
                if (statusLabel.isNotEmpty()) {
                    Text(statusLabel, color = Color.Gray, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
            TextButton(onClick = onDisconnect) {
                Text("DESLIGAR", color = VitalRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: BleDevice,
    gattStatus: GattConnectionStatus,
    onConnect: () -> Unit
) {
    val isConnecting = gattStatus in listOf(
        GattConnectionStatus.CONNECTING,
        GattConnectionStatus.DISCOVERING_SERVICES,
        GattConnectionStatus.SUBSCRIBING
    )
    val isClickable = !device.isConnected && !isConnecting

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable(enabled = isClickable) { onConnect() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    when {
                        device.isConnected -> VitalGreen.copy(alpha = 0.15f)
                        isConnecting       -> Color(0xFF7B8FE8).copy(alpha = 0.1f)
                        else               -> Color.DarkGray.copy(alpha = 0.4f)
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(deviceIcon(device.type), null,
                tint = when {
                    device.isConnected -> VitalGreen
                    isConnecting       -> Color(0xFF7B8FE8)
                    else               -> Color.Gray
                },
                modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(device.name, color = Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.Medium)
            Text(
                deviceTypeLabel(device.type) + "  •  ${device.rssi} dBm",
                color = Color.Gray, fontSize = 12.sp
            )
        }
        when {
            device.isConnected -> Text(
                "Conectado", color = VitalGreen,
                fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
            isConnecting -> CircularProgressIndicator(
                color     = Color(0xFF7B8FE8),
                modifier  = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            else -> Text(
                "LIGAR", color = Color(0xFF7B8FE8),
                fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
    }
    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.4f), thickness = 0.5.dp)
}

@Composable
private fun SosCountdownOverlay(remaining: Int, total: Int, onCancel: () -> Unit) {
    val progress = remaining.toFloat() / total.toFloat()
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning, null,
                tint  = VitalRed,
                modifier = Modifier.size((72 * pulse).dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("QUEDA DETETADA",
                color = VitalRed, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text("SOS enviado em...",
                color = Color.Gray, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(120.dp),
                    color = VitalRed,
                    strokeWidth = 8.dp,
                    trackColor = Color.DarkGray
                )
                Text(
                    "$remaining",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onCancel,
                colors  = ButtonDefaults.buttonColors(containerColor = VitalGreen),
                modifier = Modifier.fillMaxWidth(0.6f).height(52.dp),
                shape   = RoundedCornerShape(26.dp)
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("ESTOU BEM", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun GattStatusBanner(status: GattConnectionStatus, label: String) {
    val isError = status == GattConnectionStatus.ERROR
    val color   = if (isError) VitalRed else Color(0xFF7B8FE8)

    Card(
        colors   = CardDefaults.cardColors(color.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isError) {
                Icon(Icons.Default.Error, null,
                    tint = VitalRed, modifier = Modifier.size(18.dp))
            } else {
                CircularProgressIndicator(
                    color = color, modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = when (status) {
                        GattConnectionStatus.CONNECTING          -> "A ligar..."
                        GattConnectionStatus.DISCOVERING_SERVICES -> "A descobrir serviços..."
                        GattConnectionStatus.SUBSCRIBING         -> "A ativar notificações..."
                        GattConnectionStatus.ERROR               -> "Erro de ligação"
                        else                                     -> ""
                    },
                    color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
                if (label.isNotEmpty()) {
                    Text(label, color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun BtSectionHeader(title: String) {
    Text(
        title,
        color = Color.Gray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun InfoLine(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•", color = Color(0xFF7B8FE8), modifier = Modifier.padding(end = 6.dp))
        Text(text, color = Color.LightGray, fontSize = 12.sp)
    }
}

private fun deviceIcon(type: DeviceType): ImageVector = when (type) {
    DeviceType.SMARTWATCH  -> Icons.Default.Watch
    DeviceType.FITNESS_BAND -> Icons.Default.FitnessCenter
    DeviceType.HEART_RATE  -> Icons.Default.Favorite
    DeviceType.GENERIC     -> Icons.Default.Devices
}

private fun deviceTypeLabel(type: DeviceType): String = when (type) {
    DeviceType.SMARTWATCH  -> "Smartwatch"
    DeviceType.FITNESS_BAND -> "Pulseira Fitness"
    DeviceType.HEART_RATE  -> "Monitor Cardíaco"
    DeviceType.GENERIC     -> "Dispositivo BLE"
}

private fun sensitivityLabel(v: Float): String = when {
    v < 0.35f -> "BAIXA"
    v < 0.65f -> "MÉDIA"
    else      -> "ALTA"
}

private fun sensitivityColor(v: Float): Color = when {
    v < 0.35f -> VitalGreen
    v < 0.65f -> VitalOrange
    else      -> VitalRed
}
