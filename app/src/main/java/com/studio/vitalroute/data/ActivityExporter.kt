package com.studio.vitalroute.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.studio.vitalroute.data.model.Activity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────
//  ActivityExporter — gera ficheiros GPX ou CSV para partilha
//
//  Uso:
//    ActivityExporter.exportGpx(context, activity)
//    ActivityExporter.exportCsv(context, activities)
//
//  Os ficheiros são escritos em cacheDir e partilhados via
//  FileProvider + ShareCompat Intent.
// ─────────────────────────────────────────────────────────────

object ActivityExporter {

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val fileFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    // ── Exportação GPX de uma atividade ──────────────────────

    fun exportGpx(context: Context, activity: Activity): Intent {
        val typeLabel = when (activity.type) {
            "running" -> "Corrida"
            "walking" -> "Caminhada"
            else      -> "Ciclismo"
        }
        val filename = "vitalroute_${activity.type}_${fileFmt.format(Date(activity.startTime))}.gpx"
        val file     = File(context.cacheDir, filename)

        file.bufferedWriter().use { w ->
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""" + "\n")
            w.write("""<gpx version="1.1" creator="VitalRoute" xmlns="http://www.topografix.com/GPX/1/1">""" + "\n")
            w.write("""  <metadata>""" + "\n")
            w.write("""    <name>$typeLabel — ${isoFmt.format(Date(activity.startTime))}</name>""" + "\n")
            w.write("""    <time>${isoFmt.format(Date(activity.startTime))}</time>""" + "\n")
            w.write("""  </metadata>""" + "\n")
            w.write("""  <trk>""" + "\n")
            w.write("""    <name>$typeLabel</name>""" + "\n")
            w.write("""    <trkseg>""" + "\n")

            if (activity.routePoints.isNotEmpty()) {
                // Tem pontos GPS reais — interpola elevação se disponível
                val elevStep = if (activity.elevationPoints.isNotEmpty() && activity.routePoints.isNotEmpty()) {
                    activity.elevationPoints.size.toDouble() / activity.routePoints.size
                } else 0.0

                activity.routePoints.forEachIndexed { i, pt ->
                    val parts = pt.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].trim()
                        val lng = parts[1].trim()
                        val ele = if (elevStep > 0 && activity.elevationPoints.isNotEmpty()) {
                            val idx = (i * elevStep).toInt().coerceIn(0, activity.elevationPoints.lastIndex)
                            activity.elevationPoints[idx]
                        } else activity.elevationM

                        val tMs = activity.startTime + (i.toLong() *
                            (activity.durationSeconds * 1000L / activity.routePoints.size.coerceAtLeast(1)))
                        w.write("""      <trkpt lat="$lat" lon="$lng"><ele>$ele</ele><time>${isoFmt.format(Date(tMs))}</time></trkpt>""" + "\n")
                    }
                }
            } else {
                // Sem pontos GPS — gera um waypoint único com os metadados
                w.write("""      <trkpt lat="0.0" lon="0.0"><ele>${activity.elevationM}</ele>""")
                w.write("""<time>${isoFmt.format(Date(activity.startTime))}</time></trkpt>""" + "\n")
            }

            w.write("""    </trkseg>""" + "\n")
            w.write("""  </trk>""" + "\n")
            w.write("""</gpx>""" + "\n")
        }

        return buildShareIntent(context, file, "application/gpx+xml")
    }

    // ── Exportação CSV de todas as atividades ─────────────────

    fun exportCsv(context: Context, activities: List<Activity>): Intent {
        val filename = "vitalroute_atividades_${fileFmt.format(Date())}.csv"
        val file     = File(context.cacheDir, filename)

        file.bufferedWriter().use { w ->
            w.write("Data,Tipo,Distância (km),Duração (min),Velocidade Média (km/h),Elevação (m),Calorias\n")
            val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "PT"))
            activities.forEach { a ->
                val date  = dateFmt.format(Date(a.startTime))
                val type  = when (a.type) { "running" -> "Corrida"; "walking" -> "Caminhada"; else -> "Ciclismo" }
                val dist  = "%.2f".format(a.distanceKm)
                val dur   = (a.durationSeconds / 60).toString()
                val speed = "%.1f".format(a.avgSpeedKmh)
                w.write("$date,$type,$dist,$dur,$speed,${a.elevationM},${a.calories}\n")
            }
        }

        return buildShareIntent(context, file, "text/csv")
    }

    // ── Helper ────────────────────────────────────────────────

    private fun buildShareIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type          = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
