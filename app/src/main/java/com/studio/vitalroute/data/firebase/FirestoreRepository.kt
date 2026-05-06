package com.studio.vitalroute.data.firebase

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.studio.vitalroute.data.model.Activity
import com.studio.vitalroute.data.model.FirestoreContact
import com.studio.vitalroute.data.model.FirestoreSafeZone
import com.studio.vitalroute.data.model.UserProfile
import com.studio.vitalroute.data.model.UserSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────
//  FirestoreRepository — ponto único de acesso à base de dados
//
//  Estrutura Firestore:
//  users/{uid}/
//    ├── (doc)  name, email
//    ├── activities/{activityId}
//    ├── contacts/{contactId}
//    └── settings/main
// ─────────────────────────────────────────────────────────────

class FirestoreRepository {

    private val db   = Firebase.firestore
    private val auth = Firebase.auth

    // ID do utilizador autenticado (lança excepção se não estiver autenticado)
    private val uid: String
        get() = auth.currentUser?.uid
            ?: error("Utilizador não autenticado")

    // Referências de coleção
    private fun activitiesRef()  = db.collection("users").document(uid).collection("activities")
    private fun contactsRef()    = db.collection("users").document(uid).collection("contacts")
    private fun settingsRef()    = db.collection("users").document(uid).collection("settings")
    private fun userRef()        = db.collection("users").document(uid)
    private fun safeZonesRef()   = db.collection("users").document(uid).collection("safeZones")

    // ── Perfil do utilizador ──────────────────────────────────

    suspend fun saveUserProfile(profile: UserProfile) {
        userRef().set(profile).await()
    }

    suspend fun getUserProfile(): UserProfile? =
        userRef().get().await().toObject<UserProfile>()

    // ── Atividades ────────────────────────────────────────────

    /**
     * Guarda uma atividade no Firestore e devolve o ID gerado.
     */
    suspend fun saveActivity(activity: Activity): String {
        val doc = activitiesRef().document()
        doc.set(activity.copy(id = doc.id)).await()
        return doc.id
    }

    /**
     * Flow em tempo real com todas as atividades, ordenadas por data.
     * Actualiza automaticamente quando há alterações no Firestore.
     */
    fun getActivities(): Flow<List<Activity>> = callbackFlow {
        val listener = activitiesRef()
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject<Activity>() }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteActivity(activityId: String) {
        activitiesRef().document(activityId).delete().await()
    }

    // ── Contactos ─────────────────────────────────────────────

    suspend fun saveContact(contact: FirestoreContact): String {
        val doc = if (contact.id.isEmpty()) contactsRef().document()
                  else contactsRef().document(contact.id)
        doc.set(contact.copy(id = doc.id)).await()
        return doc.id
    }

    fun getContacts(): Flow<List<FirestoreContact>> = callbackFlow {
        val listener = contactsRef()
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject<FirestoreContact>() }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteContact(contactId: String) {
        contactsRef().document(contactId).delete().await()
    }

    /**
     * Leitura única (one-shot) de todos os contactos.
     * Usado pelo SosManager para enviar SMS sem precisar de Flow.
     */
    suspend fun getContactsOnce(): List<FirestoreContact> {
        val snapshot = contactsRef().get().await()
        return snapshot.documents.mapNotNull { it.toObject<FirestoreContact>() }
    }

    // ── Zonas seguras ─────────────────────────────────────────

    fun getSafeZones(): Flow<List<FirestoreSafeZone>> = callbackFlow {
        val listener = safeZonesRef()
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject<FirestoreSafeZone>() }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveSafeZone(zone: FirestoreSafeZone): String {
        val doc = if (zone.id.isEmpty()) safeZonesRef().document()
                  else safeZonesRef().document(zone.id)
        doc.set(zone.copy(id = doc.id)).await()
        return doc.id
    }

    suspend fun deleteSafeZone(zoneId: String) {
        safeZonesRef().document(zoneId).delete().await()
    }

    /** Leitura única das zonas seguras. Usada pelo RecordingService ao iniciar. */
    suspend fun getSafeZonesOnce(): List<FirestoreSafeZone> {
        val snapshot = safeZonesRef().get().await()
        return snapshot.documents.mapNotNull { it.toObject<FirestoreSafeZone>() }
            .filter { it.lat != 0.0 || it.lng != 0.0 }  // só zonas com coordenadas
    }

    /** Leitura única dos contactos com zonesEnabled=true. */
    suspend fun getZoneContactsOnce(): List<FirestoreContact> =
        getContactsOnce().filter { it.zonesEnabled && it.phone.isNotBlank() }

    // ── Localização em tempo real ─────────────────────────────

    /**
     * Escreve a posição atual do utilizador no Firestore para partilha em tempo real.
     * Estrutura: users/{uid}/liveLocation (documento único, sobrescrito a cada atualização)
     */
    suspend fun updateLiveLocation(lat: Double, lng: Double, speedKmh: Double, distKm: Double) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("liveLocation").document("current")
            .set(mapOf(
                "lat"         to lat,
                "lng"         to lng,
                "speedKmh"    to speedKmh,
                "distKm"      to distKm,
                "updatedAt"   to System.currentTimeMillis(),
                "isSharing"   to true
            )).await()
    }

    /**
     * Marca a partilha de localização como inativa quando a gravação termina.
     */
    suspend fun stopLiveLocation() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("liveLocation").document("current")
            .update("isSharing", false).await()
    }

    // ── Definições ────────────────────────────────────────────

    suspend fun saveSettings(settings: UserSettings) {
        settingsRef().document("main").set(settings).await()
    }

    suspend fun getSettings(): UserSettings =
        settingsRef().document("main").get().await()
            .toObject<UserSettings>() ?: UserSettings()

    fun getSettingsFlow(): Flow<UserSettings> = callbackFlow {
        val listener = settingsRef().document("main")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObject<UserSettings>() ?: UserSettings())
            }
        awaitClose { listener.remove() }
    }
}
