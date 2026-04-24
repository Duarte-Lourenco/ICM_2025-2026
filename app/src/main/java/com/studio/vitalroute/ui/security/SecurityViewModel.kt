package com.studio.vitalroute.ui.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.FirestoreContact
import com.studio.vitalroute.data.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
//  Modelos de UI
// ─────────────────────────────────────────────────────────────

data class TrustedContact(
    val id: String           = "",
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

// Estado do diálogo de adicionar contacto
data class AddContactDialogState(
    val visible: Boolean   = false,
    val name: String       = "",
    val phone: String      = "",
    val relation: String   = "Família",
    val sosEnabled: Boolean   = true,
    val zonesEnabled: Boolean = false,
    val fromPicker: Boolean = false,
    val error: String?     = null,
    val isSaving: Boolean  = false
)

data class SecurityUiState(
    val contacts: List<TrustedContact>  = emptyList(),
    val isLoadingContacts: Boolean      = true,
    val fallSensitivity: Float          = 0.5f,
    val sosCountdownSecs: Int           = 15,
    val safeZones: List<SafeZone>       = listOf(
        SafeZone("Casa",     "Aveiro, Portugal",  Icons.Default.Home),
        SafeZone("Trabalho", "Campus UA, Aveiro", Icons.Default.Work)
    ),
    val immobilityAlertEnabled: Boolean = true,
    val immobilityMinutes: Int          = 5,
    val arrivalAlertEnabled: Boolean    = true,
    val routeDeviationEnabled: Boolean  = false,
    // Diálogo de adicionar/editar contacto
    val addDialog: AddContactDialogState = AddContactDialogState()
)

// ─────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────

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
                .catch { _uiState.update { it.copy(isLoadingContacts = false) } }
                .collect { list ->
                    _uiState.update {
                        it.copy(
                            isLoadingContacts = false,
                            contacts = list.map { c ->
                                TrustedContact(
                                    id           = c.id,
                                    name         = c.name,
                                    relation     = c.relation,
                                    phone        = c.phone,
                                    sosEnabled   = c.sosEnabled,
                                    zonesEnabled = c.zonesEnabled
                                )
                            }
                        )
                    }
                }
        }
    }

    // ── Diálogo: abrir com dados do picker do sistema ─────────

    fun openDialogFromPicker(name: String, phone: String) {
        _uiState.update {
            it.copy(
                addDialog = AddContactDialogState(
                    visible     = true,
                    name        = name,
                    phone       = phone,
                    fromPicker  = true
                )
            )
        }
    }

    // ── Diálogo: abrir vazio (introdução manual) ──────────────

    fun openAddDialog() {
        _uiState.update {
            it.copy(addDialog = AddContactDialogState(visible = true))
        }
    }

    fun closeAddDialog() {
        _uiState.update { it.copy(addDialog = AddContactDialogState()) }
    }

    fun updateDialogName(v: String)       { _uiState.update { it.copy(addDialog = it.addDialog.copy(name = v)) } }
    fun updateDialogPhone(v: String)      { _uiState.update { it.copy(addDialog = it.addDialog.copy(phone = v)) } }
    fun updateDialogRelation(v: String)   { _uiState.update { it.copy(addDialog = it.addDialog.copy(relation = v)) } }
    fun updateDialogSos(v: Boolean)       { _uiState.update { it.copy(addDialog = it.addDialog.copy(sosEnabled = v)) } }
    fun updateDialogZones(v: Boolean)     { _uiState.update { it.copy(addDialog = it.addDialog.copy(zonesEnabled = v)) } }

    // ── Guardar contacto (do diálogo) ─────────────────────────

    fun saveContactFromDialog() {
        val d = _uiState.value.addDialog
        if (d.name.isBlank() || d.phone.isBlank()) return

        _uiState.update { it.copy(addDialog = it.addDialog.copy(isSaving = true, error = null)) }

        val newContact = FirestoreContact(
            name         = d.name.trim(),
            relation     = d.relation,
            phone        = d.phone.trim(),
            sosEnabled   = d.sosEnabled,
            zonesEnabled = d.zonesEnabled
        )

        viewModelScope.launch {
            try {
                val savedId = repository.saveContact(newContact)
                // Firestore guardou — o Flow atualiza automaticamente a lista
                closeAddDialog()
            } catch (_: Exception) {
                // Sem Firebase (modo offline/convidado) → guarda localmente na sessão
                val localContact = TrustedContact(
                    id           = "local_${System.currentTimeMillis()}",
                    name         = newContact.name,
                    relation     = newContact.relation,
                    phone        = newContact.phone,
                    sosEnabled   = newContact.sosEnabled,
                    zonesEnabled = newContact.zonesEnabled
                )
                _uiState.update { s ->
                    s.copy(
                        contacts  = s.contacts + localContact,
                        addDialog = AddContactDialogState() // fecha o diálogo
                    )
                }
            }
        }
    }

    // ── Apagar contacto ───────────────────────────────────────

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            try {
                repository.deleteContact(contactId)
                // o Flow atualiza automaticamente
            } catch (_: Exception) {
                // Sem Firebase → remove do estado local
                _uiState.update { s ->
                    s.copy(contacts = s.contacts.filter { it.id != contactId })
                }
            }
        }
    }

    // ── Toggles SOS / Zonas ───────────────────────────────────

    fun toggleSos(contactId: String, enabled: Boolean) {
        val c = _uiState.value.contacts.find { it.id == contactId } ?: return
        // Atualiza estado local imediatamente (UX responsivo)
        _uiState.update { s ->
            s.copy(contacts = s.contacts.map {
                if (it.id == contactId) it.copy(sosEnabled = enabled) else it
            })
        }
        viewModelScope.launch {
            try {
                repository.saveContact(
                    FirestoreContact(
                        id = c.id, name = c.name, relation = c.relation,
                        phone = c.phone, sosEnabled = enabled, zonesEnabled = c.zonesEnabled
                    )
                )
            } catch (_: Exception) { /* estado local já foi atualizado acima */ }
        }
    }

    fun toggleZones(contactId: String, enabled: Boolean) {
        val c = _uiState.value.contacts.find { it.id == contactId } ?: return
        // Atualiza estado local imediatamente (UX responsivo)
        _uiState.update { s ->
            s.copy(contacts = s.contacts.map {
                if (it.id == contactId) it.copy(zonesEnabled = enabled) else it
            })
        }
        viewModelScope.launch {
            try {
                repository.saveContact(
                    FirestoreContact(
                        id = c.id, name = c.name, relation = c.relation,
                        phone = c.phone, sosEnabled = c.sosEnabled, zonesEnabled = enabled
                    )
                )
            } catch (_: Exception) { /* estado local já foi atualizado acima */ }
        }
    }

    // ── Definições ────────────────────────────────────────────

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getSettingsFlow()
                .catch {}
                .collect { s ->
                    _uiState.update {
                        it.copy(
                            fallSensitivity        = s.fallSensitivity,
                            sosCountdownSecs       = s.sosCountdownSecs,
                            immobilityAlertEnabled = s.immobilityAlertEnabled,
                            immobilityMinutes      = s.immobilityMinutes,
                            arrivalAlertEnabled    = s.arrivalAlertEnabled,
                            routeDeviationEnabled  = s.routeDeviationEnabled
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
                    UserSettings(
                        fallSensitivity        = s.fallSensitivity,
                        sosCountdownSecs       = s.sosCountdownSecs,
                        immobilityAlertEnabled = s.immobilityAlertEnabled,
                        immobilityMinutes      = s.immobilityMinutes,
                        arrivalAlertEnabled    = s.arrivalAlertEnabled,
                        routeDeviationEnabled  = s.routeDeviationEnabled
                    )
                )
            } catch (_: Exception) {}
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
