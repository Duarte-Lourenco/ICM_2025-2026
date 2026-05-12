package com.studio.vitalroute.data

import android.content.Context
import android.location.Location
import android.os.Build
import android.telephony.SmsManager
import com.studio.vitalroute.data.firebase.FirestoreRepository

//
//

object SosManager {

    /**
     * Envia SMS a todos os contactos SOS.
     * @param context   Contexto da aplicação ou do serviço
     * @param location  Última localização GPS conhecida (pode ser null)
     * @param localContacts  Lista de contactos locais (modo offline).
     *                       Se vazia, tenta ir buscar ao Firestore.
     */
    suspend fun sendSos(
        context: Context,
        location: Location? = null,
        localContacts: List<SosContact> = emptyList()
    ) {
        // Obtém a lista de contactos: tenta Firestore, fallback para os locais
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
                // Não conseguiu enviar para este contacto — continua para os próximos
            }
        }
    }

    private fun buildMessage(location: Location?): String {
        val locPart = if (location != null) {
            val lat = "%.5f".format(location.latitude)
            val lon = "%.5f".format(location.longitude)
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

    /** Acesso público ao SmsManager para uso externo (ex: RecordingService para geofencing). */
    @Suppress("DEPRECATION")
    fun getSmsManagerPublic(context: Context): SmsManager = getSmsManager(context)
}

// Modelo simples para não depender do módulo de dados completo
data class SosContact(
    val name: String,
    val phone: String,
    val sosEnabled: Boolean
)
