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

class FirestoreRepository {

    private val db   = Firebase.firestore
    private val auth = Firebase.auth

    private val uid: String
        get() = auth.currentUser?.uid
            ?: error("Utilizador não autenticado")

    private fun activitiesRef()  = db.collection("users").document(uid).collection("activities")
    private fun contactsRef()    = db.collection("users").document(uid).collection("contacts")
    private fun settingsRef()    = db.collection("users").document(uid).collection("settings")
    private fun userRef()        = db.collection("users").document(uid)
    private fun safeZonesRef()   = db.collection("users").document(uid).collection("safeZones")

    suspend fun saveUserProfile(profile: UserProfile) {
        userRef().set(profile).await()
    }

    suspend fun getUserProfile(): UserProfile? =
        userRef().get().await().toObject<UserProfile>()

    suspend fun saveActivity(activity: Activity): String {
        val doc = activitiesRef().document()
        doc.set(activity.copy(id = doc.id)).await()
        return doc.id
    }

    fun getActivities(): Flow<List<Activity>> = callbackFlow {
        val listener = activitiesRef()
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject<Activity>()?.let { a ->
                            if (a.id.isEmpty()) a.copy(id = doc.id) else a
                        }
                    }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getActivityById(activityId: String): Activity? {
        val doc = activitiesRef().document(activityId).get().await()
        return doc.toObject<Activity>()?.let { a ->
            if (a.id.isEmpty()) a.copy(id = doc.id) else a
        }
    }

    suspend fun deleteActivity(activityId: String) {
        activitiesRef().document(activityId).delete().await()
    }

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

    // one-shot para o SosManager nao precisar de flow
    suspend fun getContactsOnce(): List<FirestoreContact> {
        val snapshot = contactsRef().get().await()
        return snapshot.documents.mapNotNull { it.toObject<FirestoreContact>() }
    }

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

    suspend fun getSafeZonesOnce(): List<FirestoreSafeZone> {
        val snapshot = safeZonesRef().get().await()
        return snapshot.documents.mapNotNull { it.toObject<FirestoreSafeZone>() }
            .filter { it.lat != 0.0 || it.lng != 0.0 }  // só zonas com coordenadas
    }

    suspend fun getZoneContactsOnce(): List<FirestoreContact> =
        getContactsOnce().filter { it.zonesEnabled && it.phone.isNotBlank() }

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

    suspend fun stopLiveLocation() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("liveLocation").document("current")
            .update("isSharing", false).await()
    }

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
