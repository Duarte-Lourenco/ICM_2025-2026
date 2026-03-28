package com.studio.vitalroute.ui.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.studio.vitalroute.ui.theme.*
import com.studio.vitalroute.ui.components.SectionHeader
import com.studio.vitalroute.ui.components.SafetyInfoItem

@Composable
fun MapsScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("MAPAS & ROTAS", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(280.dp).background(Color(0xFF111111)))
        Spacer(Modifier.height(20.dp))
        SectionHeader("SEGURANÇA NO LOCAL")
        SafetyInfoItem(Icons.Default.MedicalServices, "SOCORRO", "2 Próximos", VitalOrange)
    }
}