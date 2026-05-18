package com.studio.vitalroute.data

import android.content.Context
import android.location.Location
import android.os.Build
import android.telephony.SmsManager
import com.studio.vitalroute.data.firebase.FirestoreRepository
import java.util.Locale

//
//

object SosManager {

    /**
     * envia sms a todos os contactos sos
     * @param context contexto da aplicacao ou do servico
     * @param location ultima localizacao gps conhecida pode ser null
     * @param localContacts lista de contactos locais modo offline se vazia tenta ir buscar ao firestore
     */
    suspend fun sendSos(
        context: Context,
        location: Location? = null,
        localContacts: List<SosContact> = emptyList()
    ) {
        // obtem lista de contactos tenta firestore fallback local
        val contacts: List<SosContact> = if (localContacts.isNotEmpty()) {
            localContacts
        } else {
            try {
                FirestoreRepository()
                    .getContactsOnce()
                    .map { SosContact(name = it.name, phone = it.phone, sosEnabled = it.sosEnabled) }
            } catch (_: Exception) {
                emptyList()
            }
        }

        val sosContacts = contacts.filter { it.sosEnabled && it.phone.isNotBlank() }
        if (sosContacts.isEmpty()) return

        val message = buildMessage(location)
        val smsManager = getSmsManager(context)

        sosContacts.forEach { contact ->
            try {
                if (message.length > 160) {
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(
                        contact.phone, null, parts, null, null
                    )
                } else {
                    smsManager.sendTextMessage(
                        contact.phone, null, message, null, null
                    )
                }
            } catch (_: Exception) {
                // nao conseguiu enviar para este contacto continua para os proximos
            }
        }
    }

    private fun buildMessage(location: Location?): String {
        val locPart = if (location != null) {
            val lat = "%.5f".format(Locale.US, location.latitude)
            val lon = "%.5f".format(Locale.US, location.longitude)
            "\nLocalização: https://maps.google.com/?q=$lat,$lon"
        } else {
            "\n(Localização não disponível)"
        }
        return "ALERTA VITALROUTE\n" +
               "O teu contacto pode precisar de ajuda imediata!" +
               locPart +
               "\n-- Mensagem automática da app VitalRoute --"
    }

    @Suppress("DEPRECATION")
    private fun getSmsManager(context: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }

    /**
     * envia sms de tracking de localizacao a todos os contactos sos
     * usado pela partilha de localizacao em tempo real
     */
    private val sharedRepository = FirestoreRepository()

    suspend fun sendLocationUpdate(
        context: Context,
        location: android.location.Location?,
        activityType: String,
        isFinal: Boolean = false
    ) {
        val contacts = try {
            sharedRepository
                .getContactsOnce()
                .filter { it.sosEnabled && it.phone.isNotBlank() }
        } catch (_: Exception) { emptyList() }

        if (contacts.isEmpty()) return

        val message = buildLocationMessage(location, activityType, isFinal)
        val smsManager = getSmsManager(context)

        contacts.forEach { contact ->
            try {
                if (message.length > 160) {
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(contact.phone, null, message, null, null)
                }
            } catch (_: Exception) {}
        }
    }

    private fun buildLocationMessage(
        location: android.location.Location?,
        activityType: String,
        isFinal: Boolean
    ): String {
        val type = when (activityType) {
            "running" -> "corrida"
            "walking" -> "caminhada"
            else      -> "ciclismo"
        }
        return if (isFinal) {
            "[VitalRoute] A tua atividade de $type terminou."
        } else {
            val locPart = if (location != null) {
                val lat = "%.5f".format(Locale.US, location.latitude)
                val lon = "%.5f".format(Locale.US, location.longitude)
                " Localizacao: https://maps.google.com/?q=$lat,$lon"
            } else {
                " (GPS a iniciar)"
            }
            "[VitalRoute] A fazer $type.$locPart"
        }
    }

    // acesso publico ao smsmanager para uso externo ex recordingservice para geofencing
    @Suppress("DEPRECATION")
    fun getSmsManagerPublic(context: Context): SmsManager = getSmsManager(context)
}

// modelo simples para nao depender do modulo de dados completo
data class SosContact(
    val name: String,
    val phone: String,
    val sosEnabled: Boolean
)
