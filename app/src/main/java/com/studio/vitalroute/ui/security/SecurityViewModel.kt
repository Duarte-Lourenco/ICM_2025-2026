package com.studio.vitalroute.ui.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.FirestoreContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrustedContact(
    val id: String = "",
    val name: String,
    val relation: String,
    val phone: String,
    val sosEnabled: Boolean,
    val zonesEnabled: Boolean
)

data class SafeZone(
    val name: String,
    val address: String,
    val icon: ImageVector
)

data class SecurityUiState(
    val contacts: List<TrustedContact> = emptyList(),
    val isLoadingContacts: Boolean = true,
    val fallSensitivity: Float = 0.5f,
    val sosCountdownSecs: Int = 15,
    val safeZones: List<SafeZone> = listOf(
        SafeZone("Casa",     "Aveiro, Portugal",  Icons.Default.Home),
        SafeZone("Trabalho", "Campus UA, Aveiro", Icons.Default.Work)
    ),
    val immobilityAlertEnabled: Boolean = true,
    val immobilityMinutes: Int = 5,
    val arrivalAlertEnabled: Boolean = true,
    val routeDeviationEnabled: Boolean = false
)

class SecurityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val repository = FirestoreRepository()

    init {
        loadContacts()
        loadSettings()
    }

    // ── Contactos (Firestore) ─────────────────────────────────

    private fun loadContacts() {
        viewModelScope.launch {
            repository.getContacts()
                .catch { /* sem autenticação — ignora */ }
                .collect { firestoreContacts ->
                    _uiState.update {
                        it.copy(
                            isLoadingContacts = false,
                            contacts = firestoreContacts.map { c ->
                                TrustedContact(
                                    id          = c.id,
                                    name        = c.name,
                                    relation    = c.relation,
                                    phone       = c.phone,
                                    sosEnabled  = c.sosEnabled,
                                    zonesEnabled = c.zonesEnabled
                                )
                            }
                        )
                    }
                }
        }
    }

    fun addContact(name: String, relation: String, phone: String) {
        viewModelScope.launch {
            try {
                repository.saveContact(
                    FirestoreContact(name = name, relation = relation, phone = phone)
                )
                // o Flow do loadContacts() atualiza automaticamente a lista
            } catch (e: Exception) { /* tratar */ }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            try { repository.deleteContact(contactId) }
            catch (e: Exception) { /* tratar */ }
        }
    }

    fun toggleSos(contactId: String, enabled: Boolean) {
        val contact = _uiState.value.contacts.find { it.id == contactId } ?: return
        viewModelScope.launch {
            try {
                repository.saveContact(
                    FirestoreContact(
                        id = contact.id, name = contact.name,
                        relation = contact.relation, phone = contact.phone,
                        sosEnabled = enabled, zonesEnabled = contact.zonesEnabled
                    )
                )
            } catch (e: Exception) { /* tratar */ }
        }
    }

    // ── Definições (Firestore) ────────────────────────────────

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getSettingsFlow()
                .catch { /* sem autenticação */ }
                .collect { settings ->
                    _uiState.update {
                        it.copy(
                            fallSensitivity        = settings.fallSensitivity,
                            sosCountdownSecs       = settings.sosCountdownSecs,
                            immobilityAlertEnabled = settings.immobilityAlertEnabled,
                            immobilityMinutes      = settings.immobilityMinutes,
                            arrivalAlertEnabled    = settings.arrivalAlertEnabled,
                            routeDeviationEnabled  = settings.routeDeviationEnabled
                        )
                    }
                }
        }
    }

    private fun saveSettings() {
        val s = _uiState.value
        viewModelScope.launch {
            try {
                repository.saveSettings(
                    com.studio.vitalroute.data.model.UserSettings(
                        fallSensitivity        = s.fallSensitivity,
                        sosCountdownSecs       = s.sosCountdownSecs,
                        immobilityAlertEnabled = s.immobilityAlertEnabled,
                        immobilityMinutes      = s.immobilityMinutes,
                        arrivalAlertEnabled    = s.arrivalAlertEnabled,
                        routeDeviationEnabled  = s.routeDeviationEnabled
                    )
                )
            } catch (e: Exception) { /* tratar */ }
        }
    }

    fun updateFallSensitivity(value: Float) {
        _uiState.update { it.copy(fallSensitivity = value) }
        saveSettings()
    }

    fun toggleImmobilityAlert(enabled: Boolean) {
        _uiState.update { it.copy(immobilityAlertEnabled = enabled) }
        saveSettings()
    }

    fun toggleArrivalAlert(enabled: Boolean) {
        _uiState.update { it.copy(arrivalAlertEnabled = enabled) }
        saveSettings()
    }

    fun toggleRouteDeviation(enabled: Boolean) {
        _uiState.update { it.copy(routeDeviationEnabled = enabled) }
        saveSettings()
    }
}
