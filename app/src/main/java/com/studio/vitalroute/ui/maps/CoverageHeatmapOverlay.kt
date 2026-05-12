package com.studio.vitalroute.ui.maps

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

//
//

class CoverageHeatmapOverlay(
    private var towers: List<GeoPoint> = emptyList()
) : Overlay() {

    // Raio de cobertura simulado por antena (em metros)
    // Urbano: ~400-600m | Rural: até 2-5km
    private val coverageRadiusMeters = 350.0

    // Paint para o fundo de "sem cobertura" — vermelho subtil mas perceptível
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(20, 200, 30, 30)
        style = Paint.Style.FILL
    }

    fun updateTowers(newTowers: List<GeoPoint>) {
        towers = newTowers
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val projection = mapView.projection
        val zoom       = mapView.zoomLevelDouble

        // 1. fundo vermelho ("sem cobertura")
        canvas.drawRect(
            0f, 0f,
            mapView.width.toFloat(), mapView.height.toFloat(),
            bgPaint
        )

        if (towers.isEmpty()) return

        // 2. gradiente de cobertura por antena
        towers.forEach { tower ->
            val screenPt = projection.toPixels(tower, null) ?: return@forEach

            val metersPerPixel = TileSystem.GroundResolution(tower.latitude, zoom)
            val radiusPx = (coverageRadiusMeters / metersPerPixel)
                .toFloat()
                .coerceIn(30f, 2000f)

            val cx = screenPt.x.toFloat()
            val cy = screenPt.y.toFloat()

            // Centro visível mas leve → fade rápido para transparente
            val shader = RadialGradient(
                cx, cy, radiusPx,
                intArrayOf(
                    Color.argb( 55,  20, 200,  70),   // verde — cobertura boa
                    Color.argb( 35,  80, 200,  40),   // verde médio
                    Color.argb( 18, 160, 210,  20),   // amarelo-verde — fraca
                    Color.argb(  0, 230, 160,   0),   // transparente — limite
                ),
                floatArrayOf(0f, 0.40f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
                style = Paint.Style.FILL
            }

            canvas.drawCircle(cx, cy, radiusPx, paint)
        }
    }
}
