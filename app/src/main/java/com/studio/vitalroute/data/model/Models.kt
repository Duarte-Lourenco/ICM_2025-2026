package com.studio.vitalroute.data.model


/**
 * atividade gravada ciclismo ou corrida
 * guardada em users uid activities activityid
 */
data class Activity(
    val id: String                    = "",
    val type: String                  = "cycling",   // cycling running walking
    val startTime: Long               = 0L,          // epoch ms
    val endTime: Long                 = 0L,
    val distanceKm: Double            = 0.0,
    val durationSeconds: Long         = 0L,
    val avgSpeedKmh: Double           = 0.0,
    val maxSpeedKmh: Double           = 0.0,
    val elevationM: Int               = 0,
    val calories: Int                 = 0,
    // amostras de altitude a cada 30s para curva de elevacao
    val elevationPoints: List<Int>    = emptyList(),
    // pontos gps a cada 60s para visualizacao da rota
    val routePoints: List<String>     = emptyList()  // formato lat lng
)

/**
 * contacto de confianca para sos
 * guardado em users uid contacts contactid
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
 * definicoes de seguranca do utilizador
 * guardado em users uid settings main
 */
data class UserSettings(
    val fallSensitivity: Float     = 0.5f,
    val sosCountdownSecs: Int      = 15,
    val immobilityAlertEnabled: Boolean = true,
    val immobilityMinutes: Int     = 5,
    val arrivalAlertEnabled: Boolean = true,
    val routeDeviationEnabled: Boolean = false,
    val weeklyGoalKm: Float        = 50f,
    val metricSystem: Boolean      = true
)

/**
 * perfil do utilizador
 * guardado em users uid
 */
data class UserProfile(
    val uid: String      = "",
    val name: String     = "",
    val email: String    = "",
    val weightKg: Float  = 70f,
    val heightCm: Int    = 170,
    val gender: String   = "male"   // male female
)

/**
 * zona segura configurada pelo utilizador
 * guardada em users uid safezones zoneid
 */
data class FirestoreSafeZone(
    val id: String      = "",
    val name: String    = "",
    val address: String = "",
    val lat: Double     = 0.0,
    val lng: Double     = 0.0,
    val radiusM: Int    = 150,
    val color: String   = "#FF6F00"
)
