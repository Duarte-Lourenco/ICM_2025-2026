package com.studio.vitalroute.ui.security

import android.Manifest
import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.vitalroute.ui.components.SectionHeader
import com.studio.vitalroute.ui.theme.*

private val RELATIONS = listOf("Família", "Cônjuge", "Pai/Mãe", "Filho/a", "Amigo/a", "Colega", "Médico", "Outro")

@Composable
fun SecurityScreen(viewModel: SecurityViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // lançador de permissão read_contacts
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) permissionDenied = true
    }

    // lançador do picker de contactos do sistema
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        // Ler nome e número de telefone via ContentResolver
        val cr: ContentResolver = context.contentResolver
        var name  = ""
        var phone = ""

        // 1. Nome
        cr.query(uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null, null)
            ?.use { c -> if (c.moveToFirst()) name = c.getString(0) ?: "" }

        // 2. ID do contacto (para obter os números de telefone)
        var contactId: String? = null
        cr.query(uri, arrayOf(ContactsContract.Contacts._ID), null, null, null)
            ?.use { c -> if (c.moveToFirst()) contactId = c.getString(0) }

        // 3. Números de telefone
        contactId?.let { id ->
            cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(id),
                null
            )?.use { c -> if (c.moveToFirst()) phone = c.getString(0) ?: "" }
        }

        viewModel.openDialogFromPicker(name, phone)
    }

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

        // estado geral de proteção
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
                        "PROTEÇÃO ATIVA",
                        color = VitalGreen, fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp
                    )
                    Text("Monitorização Contínua", color = Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        "${uiState.contacts.size} contactos · Queda: Ativa · SOS: ${uiState.sosCountdownSecs}s",
                        color = Color.Gray, fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // rede de confiança
        SectionHeader("REDE DE CONFIANÇA")

        if (uiState.isLoadingContacts) {
            Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                CircularProgressIndicator(color = VitalOrange, modifier = Modifier.size(24.dp))
            }
        } else if (uiState.contacts.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(CardGray),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.PersonOff, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhum contacto de emergência", color = Color.Gray, fontSize = 13.sp)
                    Text("Adiciona alguém que deve ser alertado em caso de queda.",
                        color = Color.DarkGray, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        } else {
            uiState.contacts.forEach { contact ->
                ContactCard(
                    contact   = contact,
                    onDelete  = { viewModel.deleteContact(contact.id) },
                    onToggleSos   = { viewModel.toggleSos(contact.id, it) },
                    onToggleZones = { viewModel.toggleZones(contact.id, it) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // Botão adicionar — dois modos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Picker de contactos do telemóvel
            Button(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    contactPickerLauncher.launch(null)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = VitalOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Contacts, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Dos Contactos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            // Entrada manual
            OutlinedButton(
                onClick = { viewModel.openAddDialog() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VitalOrange),
                border = androidx.compose.foundation.BorderStroke(1.dp, VitalOrange.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Manual", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (permissionDenied) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Permissão de contactos negada. Podes adicionar contactos manualmente.",
                color = Color(0xFFFF9800), fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(28.dp))

        // deteção de queda
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
                        activeTrackColor  = VitalOrange,
                        thumbColor        = VitalOrange,
                        inactiveTrackColor = Color(0xFF2A2A2A)
                    )
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Baixa", color = Color.Gray, fontSize = 11.sp)
                    Text("Média", color = Color.Gray, fontSize = 11.sp)
                    Text("Alta",  color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "TEMPO DE CONTAGEM SOS",
                    color = Color.Gray, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 30, 60).forEach { secs ->
                        val selected = uiState.sosCountdownSecs == secs
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.updateSosCountdownSecs(secs) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) VitalOrange.copy(alpha = 0.15f) else Color(0xFF1E1E1E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) VitalOrange else Color(0xFF2A2A2A)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "${secs}s",
                                    color = if (selected) VitalOrange else Color.Gray,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Após deteção de queda, tens ${uiState.sosCountdownSecs}s para cancelar antes do SOS ser enviado.",
                    color = Color.DarkGray, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // zonas seguras
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("ZONAS SEGURAS")
            TextButton(onClick = { viewModel.openAddZoneDialog() }) {
                Icon(Icons.Default.Add, null, tint = VitalGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Adicionar", color = VitalGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (uiState.isLoadingZones) {
            Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                CircularProgressIndicator(color = VitalGreen, modifier = Modifier.size(20.dp))
            }
        } else if (uiState.safeZones.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(CardGray),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LocationOff, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Nenhuma zona segura", color = Color.Gray, fontSize = 13.sp)
                    Text("Adiciona casa, trabalho ou qualquer local de chegada.",
                        color = Color.DarkGray, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        } else {
            uiState.safeZones.forEach { zone ->
                SafeZoneCard(
                    zone      = zone,
                    onDelete  = { viewModel.deleteZone(zone.id) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // alertas automáticos
        SectionHeader("ALERTAS AUTOMÁTICOS")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                AlertToggleRow(
                    icon     = Icons.Default.Timer,
                    title    = "Imobilidade prolongada",
                    subtitle = "Alerta após ${uiState.immobilityMinutes} min sem movimento",
                    enabled  = uiState.immobilityAlertEnabled,
                    onToggle = { viewModel.toggleImmobilityAlert(it) }
                )
                HorizontalDivider(color = Color(0xFF2A2A2A))
                AlertToggleRow(
                    icon     = Icons.Default.Home,
                    title    = "Chegada a zona segura",
                    subtitle = "Notifica contactos ao chegar a casa",
                    enabled  = uiState.arrivalAlertEnabled,
                    onToggle = { viewModel.toggleArrivalAlert(it) }
                )
                HorizontalDivider(color = Color(0xFF2A2A2A))
                AlertToggleRow(
                    icon     = Icons.Default.Route,
                    title    = "Desvio de rota",
                    subtitle = "Alerta se sair do percurso planeado",
                    enabled  = uiState.routeDeviationEnabled,
                    onToggle = { viewModel.toggleRouteDeviation(it) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    // diálogo de adicionar/confirmar contacto
    if (uiState.addDialog.visible) {
        AddContactDialog(
            state     = uiState.addDialog,
            onName    = { viewModel.updateDialogName(it) },
            onPhone   = { viewModel.updateDialogPhone(it) },
            onRelation = { viewModel.updateDialogRelation(it) },
            onSos     = { viewModel.updateDialogSos(it) },
            onZones   = { viewModel.updateDialogZones(it) },
            onConfirm = { viewModel.saveContactFromDialog() },
            onDismiss = { viewModel.closeAddDialog() }
        )
    }

    // diálogo de adicionar zona segura
    if (uiState.addZoneDialog.visible) {
        AddZoneDialog(
            state          = uiState.addZoneDialog,
            onName         = { viewModel.updateZoneName(it) },
            onAddress      = { viewModel.updateZoneAddress(it) },
            onAddressDone  = { viewModel.geocodeCurrentAddress() },
            onConfirm      = { viewModel.saveZoneFromDialog() },
            onDismiss      = { viewModel.closeAddZoneDialog() }
        )
    }
}


@Composable
private fun AddContactDialog(
    state: AddContactDialogState,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onRelation: (String) -> Unit,
    onSos: (Boolean) -> Unit,
    onZones: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(CardGray),
            shape  = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    if (state.fromPicker) "Confirmar Contacto" else "Adicionar Contacto",
                    color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                )
                Spacer(Modifier.height(16.dp))

                // Nome
                OutlinedTextField(
                    value         = state.name,
                    onValueChange = onName,
                    label         = { Text("Nome") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = dialogFieldColors()
                )
                Spacer(Modifier.height(10.dp))

                // Telefone
                OutlinedTextField(
                    value         = state.phone,
                    onValueChange = onPhone,
                    label         = { Text("Nº de telemóvel") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = dialogFieldColors()
                )
                Spacer(Modifier.height(14.dp))

                // Relação
                Text("Relação", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                // Grid 4 colunas
                val rows = RELATIONS.chunked(4)
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { rel ->
                            val selected = state.relation == rel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) VitalOrange.copy(alpha = 0.2f)
                                        else Color(0xFF2A2A2A)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) VitalOrange else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onRelation(rel) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    rel,
                                    color = if (selected) VitalOrange else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        // Preencher células vazias
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(8.dp))

                // Toggles SOS / Zonas
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToggleChip(
                        label    = "Alertas SOS",
                        checked  = state.sosEnabled,
                        color    = VitalRed,
                        modifier = Modifier.weight(1f),
                        onChange = onSos
                    )
                    ToggleChip(
                        label    = "Zonas Seguras",
                        checked  = state.zonesEnabled,
                        color    = VitalGreen,
                        modifier = Modifier.weight(1f),
                        onChange = onZones
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                    ) { Text("Cancelar") }

                    Button(
                        onClick  = onConfirm,
                        enabled  = state.name.isNotBlank() && state.phone.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = VitalOrange)
                    ) { Text("Guardar", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}


@Composable
private fun ContactCard(
    contact: TrustedContact,
    onDelete: () -> Unit,
    onToggleSos: (Boolean) -> Unit,
    onToggleZones: (Boolean) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardGray),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF2A2A2A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        contact.name.first().uppercase(),
                        color = VitalOrange, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${contact.relation} · ${contact.phone}", color = Color.Gray, fontSize = 12.sp)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, null, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A))
            Spacer(Modifier.height(10.dp))

            // Toggles inline
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleChip(
                    label    = "Alertas SOS",
                    checked  = contact.sosEnabled,
                    color    = VitalRed,
                    modifier = Modifier.weight(1f),
                    onChange = onToggleSos
                )
                ToggleChip(
                    label    = "Zonas Seguras",
                    checked  = contact.zonesEnabled,
                    color    = VitalGreen,
                    modifier = Modifier.weight(1f),
                    onChange = onToggleZones
                )
            }
        }
    }

    // Confirmação de eliminação
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor   = CardGray,
            title  = { Text("Remover contacto?", color = Color.White) },
            text   = { Text("${contact.name} será removido da tua rede de emergência.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Remover", color = VitalRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun ToggleChip(
    label: String,
    checked: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (checked) color.copy(alpha = 0.15f) else Color(0xFF2A2A2A))
            .border(1.dp, if (checked) color else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                null, tint = if (checked) color else Color.DarkGray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(label, color = if (checked) color else Color.Gray,
                fontSize = 12.sp, fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun ContactBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
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
            color = color, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp
        )
    }
}

@Composable
private fun SafeZoneCard(zone: SafeZone, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardGray),
        shape    = RoundedCornerShape(12.dp)
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
            Column(Modifier.weight(1f)) {
                Text(zone.name, color = Color.White, fontWeight = FontWeight.Bold)
                if (zone.address.isNotBlank())
                    Text(zone.address, color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, null, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor   = CardGray,
            title  = { Text("Remover zona?", color = Color.White) },
            text   = { Text("\"${zone.name}\" será removida das tuas zonas seguras.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Remover", color = VitalRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun AddZoneDialog(
    state: AddZoneDialogState,
    onName: (String) -> Unit,
    onAddress: (String) -> Unit,
    onAddressDone: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(CardGray),
            shape  = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Adicionar Zona Segura", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.name,
                    onValueChange = onName,
                    label = { Text("Nome (ex: Casa, Ginásio)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.address,
                    onValueChange = onAddress,
                    label = { Text("Endereço para geofencing") },
                    placeholder = { Text("ex: Av. da Universidade, Aveiro", color = Color.DarkGray, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { onAddressDone() }
                    ),
                    trailingIcon = {
                        if (state.geocodeStatus == "A localizar...") {
                            CircularProgressIndicator(
                                color = Color(0xFF64B5F6),
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (state.address.isNotBlank() && state.geocodeStatus.isEmpty()) {
                            IconButton(onClick = onAddressDone, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Search, null, tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )

                // Geocoding status
                if (state.geocodeStatus.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    val statusColor = when {
                        state.geocodeStatus.startsWith("✓") -> VitalGreen
                        state.geocodeStatus.startsWith("!") -> VitalOrange
                        else -> Color(0xFF64B5F6)
                    }
                    Text(state.geocodeStatus, color = statusColor, fontSize = 11.sp)
                }

                state.error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = VitalRed, fontSize = 11.sp)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "O endereço será geocodificado para deteção de chegada por GPS.",
                    color = Color.DarkGray, fontSize = 10.sp
                )
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = onConfirm,
                        enabled = state.name.isNotBlank() && !state.isSaving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = VitalGreen)
                    ) {
                        if (state.isSaving) CircularProgressIndicator(
                            color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp
                        ) else Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertToggleRow(
    icon: ImageVector, title: String, subtitle: String,
    enabled: Boolean, onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(
            checked = enabled, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = VitalGreen)
        )
    }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = VitalOrange,
    unfocusedBorderColor = Color.DarkGray,
    focusedLabelColor    = VitalOrange,
    unfocusedLabelColor  = Color.Gray,
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    cursorColor          = VitalOrange
)

