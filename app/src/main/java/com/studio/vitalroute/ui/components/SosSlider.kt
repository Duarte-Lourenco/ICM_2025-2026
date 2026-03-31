package com.studio.vitalroute.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.studio.vitalroute.ui.theme.VitalRed
import kotlin.math.roundToInt

@Composable
fun SosSlider(onSosTriggered: () -> Unit) {
    val density = LocalDensity.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val maxOffset = (widthPx - with(density) { (56.dp + 48.dp).toPx() }).coerceAtLeast(0f)

    Box(Modifier.fillMaxWidth().height(64.dp).onGloballyPositioned { widthPx = it.size.width }) {
        Box(
            Modifier.fillMaxSize()
                .background(VitalRed.copy(0.15f), CircleShape)
                .border(2.dp, VitalRed.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("DESLIZAR PARA SOS", color = Color.White.copy(0.6f), fontWeight = FontWeight.Black)
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .padding(4.dp)
                .size(56.dp)
                .background(VitalRed, CircleShape)
                .pointerInput(maxOffset) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX >= maxOffset * 0.8f) onSosTriggered()
                            offsetX = 0f
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            offsetX = (offsetX + drag.x).coerceIn(0f, maxOffset)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardDoubleArrowRight, null, tint = Color.White)
        }
    }
}