package com.studio.vitalroute.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.studio.vitalroute.ui.components.ContactRow

@Composable
fun SecurityScreen(viewModel: SecurityViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("SEGURANÇA", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(32.dp))
        SectionHeader("REDE DE CONFIANÇA")

        uiState.contacts.forEach { contact ->
            ContactRow(contact.name, contact.sosEnabled, contact.zonesEnabled)
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("DETEÇÃO DE QUEDA")
        Slider(
            value = uiState.fallSensitivity,
            onValueChange = { viewModel.updateFallSensitivity(it) },
            colors = SliderDefaults.colors(activeTrackColor = VitalOrange)
        )
    }
}