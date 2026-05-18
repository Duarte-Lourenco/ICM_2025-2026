package com.studio.vitalroute.ui.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.api.NominatimRepository
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.FirestoreContact
import com.studio.vitalroute.data.model.FirestoreSafeZone
import com.studio.vitalroute.data.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class TrustedContact(
    val id: String           = "",
    val name: String,
    val relation: String,
    val phone: String,
    val sosEnabled: Boolean,
    val zonesEnabled: Boolean
)

data class SafeZone(
    val id: String,
    val name: String,
    val address: String,
    val icon: ImageVector,
    val color: String  = "#FF6F00",
    val radiusM: Int   = 150
)

// estado do dialogo de adicionar zona segura
data class AddZoneDialogState(
    val visible: Boolean   = false,
    val name: String       = "",
    val address: String    = "",
    val isSaving: Boolean  = false,
    val error: String?     = null,
    val geocodedLat: Double = 0.0,
    val geocodedLng: Double = 0.0,
    val geocodeStatus: String = ""   // vazio a localizar localizado sem coordenadas
)

// estado do dialogo de adicionar contacto
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
    val safeZones: List<SafeZone>       = emptyList(),
    val isLoadingZones: Boolean         = true,
    val immobilityAlertEnabled: Boolean = true,
    val immobilityMinutes: Int          = 5,
    val arrivalAlertEnabled: Boolean    = true,
    val routeDeviationEnabled: Boolean  = false,
    // dialogos
    val addDialog: AddContactDialogState = AddContactDialogState(),
    val addZoneDialog: AddZoneDialogState = AddZoneDialogState()
)


class SecurityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val repository = FirestoreRepository()

    init {
        loadContacts()
        loadSafeZones()
        loadSettings()
    }

    // zonas seguras firestore

    private fun loadSafeZones() {
        viewModelScope.launch {
            repository.getSafeZones()
                .catch { _uiState.update { it.copy(isLoadingZones = false) } }
                .collect { list ->
                    rawZones = list
                    _uiState.update {
                        it.copy(
                            isLoadingZones = false,
                            safeZones = list.map { z -> z.toUi() }
                        )
                    }
                }
        }
    }

    fun saveZoneEdits(zoneId: String, color: String, radiusM: Int) {
        val raw = rawZones.find { it.id == zoneId } ?: return
        viewModelScope.launch {
            try { repository.saveSafeZone(raw.copy(color = color, radiusM = radiusM)) }
            catch (_: Exception) {}
        }
    }

    // lista raw guardada para poder editar sem perder lat lng
    private var rawZones: List<FirestoreSafeZone> = emptyList()

    private fun FirestoreSafeZone.toUi(): SafeZone {
        val icon = when {
            name.contains("casa",      ignoreCase = true) ||
            name.contains("home",      ignoreCase = true) -> Icons.Default.Home
            name.contains("trabalho",  ignoreCase = true) ||
            name.contains("work",      ignoreCase = true) -> Icons.Default.Work
            else                                          -> Icons.Default.LocationOn
        }
        return SafeZone(id = id, name = name, address = address, icon = icon, color = color, radiusM = radiusM)
    }

    // diálogo adicionar zona

    fun openAddZoneDialog() {
        _uiState.update { it.copy(addZoneDialog = AddZoneDialogState(visible = true)) }
    }

    fun closeAddZoneDialog() {
        _uiState.update { it.copy(addZoneDialog = AddZoneDialogState()) }
    }

    fun updateZoneName(v: String)    { _uiState.update { it.copy(addZoneDialog = it.addZoneDialog.copy(name = v)) } }
    fun updateZoneAddress(v: String) {
        _uiState.update { it.copy(addZoneDialog = it.addZoneDialog.copy(address = v, geocodeStatus = "")) }
    }

    // geocodifica o endereco atual e atualiza o estado do dialogo
    fun geocodeCurrentAddress() {
        val addr = _uiState.value.addZoneDialog.address.trim()
        if (addr.isBlank()) return
        _uiState.update { it.copy(addZoneDialog = it.addZoneDialog.copy(geocodeStatus = "A localizar...")) }
        viewModelScope.launch {
            val result = NominatimRepository.geocode(addr)
            _uiState.update {
                it.copy(addZoneDialog = it.addZoneDialog.copy(
                    geocodedLat   = result?.lat ?: 0.0,
                    geocodedLng   = result?.lng ?: 0.0,
                    geocodeStatus = if (result != null) "✓ Localizado" else "! Sem coordenadas"
                ))
            }
        }
    }

    fun saveZoneFromDialog() {
        val d = _uiState.value.addZoneDialog
        if (d.name.isBlank()) return
        _uiState.update { it.copy(addZoneDialog = it.addZoneDialog.copy(isSaving = true, error = null)) }

        viewModelScope.launch {
            // usa coordenadas ja geocodificadas so refaz se ainda sao zero
            var lat = d.geocodedLat
            var lng = d.geocodedLng
            if (lat == 0.0 && lng == 0.0 && d.address.isNotBlank()) {
                val result = NominatimRepository.geocode(d.address.trim())
                if (result != null) {
                    lat = result.lat
                    lng = result.lng
                }
            }

            try {
                repository.saveSafeZone(
                    FirestoreSafeZone(
                        name    = d.name.trim(),
                        address = d.address.trim(),
                        lat     = lat,
                        lng     = lng
                    )
                )
                closeAddZoneDialog()
            } catch (e: Exception) {
                // fallback local
                val localZone = SafeZone(
                    id      = "local_${System.currentTimeMillis()}",
                    name    = d.name.trim(),
                    address = d.address.trim(),
                    icon    = Icons.Default.LocationOn
                )
                _uiState.update { s ->
                    s.copy(
                        safeZones     = s.safeZones + localZone,
                        addZoneDialog = AddZoneDialogState()
                    )
                }
            }
        }
    }

    fun deleteZone(zoneId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSafeZone(zoneId)
            } catch (_: Exception) {
                _uiState.update { s -> s.copy(safeZones = s.safeZones.filter { it.id != zoneId }) }
            }
        }
    }

    // contactos firestore

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

    // dialogo abrir com dados do picker do sistema

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

    // dialogo abrir vazio introducao manual

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

    // guardar contacto do dialogo

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
                // firestore guardou o flow atualiza automaticamente a lista
                closeAddDialog()
            } catch (_: Exception) {
                // sem firebase modo offline guarda localmente na sessao
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

    // apagar contacto firestore

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            try {
                repository.deleteContact(contactId)
                // o Flow atualiza automaticamente
            } catch (_: Exception) {
                // sem firebase remove do estado local
                _uiState.update { s ->
                    s.copy(contacts = s.contacts.filter { it.id != contactId })
                }
            }
        }
    }

    // toggles sos e zonas

    fun toggleSos(contactId: String, enabled: Boolean) {
        val c = _uiState.value.contacts.find { it.id == contactId } ?: return
        // atualiza estado local imediatamente ux responsivo
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
        // atualiza estado local imediatamente ux responsivo
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

    // definicoes

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

    fun updateSosCountdownSecs(value: Int) {
        _uiState.update { it.copy(sosCountdownSecs = value) }
        saveSettings()
    }

    fun toggleImmobilityAlert(enabled: Boolean) {
        _uiState.update { it.copy(immobilityAlertEnabled = enabled) }
        saveSettings()
    }

    fun updateImmobilityMinutes(value: Int) {
        _uiState.update { it.copy(immobilityMinutes = value) }
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
