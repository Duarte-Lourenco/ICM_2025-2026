package com.studio.vitalroute.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = Color.Gray, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.DarkGray)
    }
}