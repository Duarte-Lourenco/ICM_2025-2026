package com.studio.vitalroute.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studio.vitalroute.ui.theme.*

@Composable
fun BigMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun SensorRow(icon: ImageVector, label: String, status: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray)
        Text(label, Modifier.weight(1f).padding(start = 12.dp), Color.White)
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(status, Modifier.padding(start = 8.dp), Color.White, fontSize = 13.sp)
    }
}

@Composable
fun DiaryStat(value: String, label: String) {
    Column {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun HistoryItem(date: String, title: String, dist: String, speed: String) {
    Card(colors = CardDefaults.cardColors(CardGray), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(date, color = Color.Gray, fontSize = 12.sp)
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                DiaryStat(dist, "Distância")
                DiaryStat(speed, "Velocidade")
            }
        }
    }
}

@Composable
fun SafetyInfoItem(icon: ImageVector, title: String, sub: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color.DarkGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color)
        }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(sub, color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.NavigateNext, null, tint = Color.Gray)
    }
}

@Composable
fun ContactRow(name: String, sos: Boolean, zones: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Person, null, tint = Color.Gray)
        Text(name, Modifier.weight(1f).padding(start = 12.dp), Color.White)
        if (sos) Badge("SOS")
        if (zones) Badge("ZONAS")
    }
}

@Composable
fun Badge(label: String) {
    Surface(
        modifier = Modifier.padding(start = 4.dp),
        color = VitalGreen.copy(0.1f),
        border = BorderStroke(1.dp, VitalGreen),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(label, Modifier.padding(4.dp, 2.dp), VitalGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}