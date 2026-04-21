package com.studio.vitalroute.data.model

// ─────────────────────────────────────────────────────────────
//  Modelos Firestore — precisam de construtor sem argumentos
//  (todos os campos têm valores default)
// ─────────────────────────────────────────────────────────────

/**
 * Atividade gravada (ciclismo ou corrida).
 * Guardada em: users/{uid}/activities/{activityId}
 */
data class Activity(
    val id: String           = "",
    val type: String         = "cycling",   // "cycling" | "running"
    val startTime: Long      = 0L,          // epoch ms
    val endTime: Long        = 0L,
    val distanceKm: Double   = 0.0,
    val durationSeconds: Long= 0L,
    val avgSpeedKmh: Double  = 0.0,
    val elevationM: Int      = 0,
    val calories: Int        = 0
)

/**
 * Contacto de confiança para SOS.
 * Guardado em: users/{uid}/contacts/{contactId}
 */
data class FirestoreContact(
    val id: String          = "",
    val name: String        = "",
    val relation: String    = "",
    val phone: String       = "",
    val sosEnabled: Boolean = true,
    val zonesEnabled: Boolean = false
)

/**
 * Definições de segurança do utilizador.
 * Guardado em: users/{uid}/settings/main
 */
data class UserSettings(
    val fallSensitivity: Float     = 0.5f,
    val sosCountdownSecs: Int      = 15,
    val immobilityAlertEnabled: Boolean = true,
    val immobilityMinutes: Int     = 5,
    val arrivalAlertEnabled: Boolean = true,
    val routeDeviationEnabled: Boolean = false
)

/**
 * Perfil do utilizador.
 * Guardado em: users/{uid}
 */
data class UserProfile(
    val uid: String   = "",
    val name: String  = "",
    val email: String = ""
)
