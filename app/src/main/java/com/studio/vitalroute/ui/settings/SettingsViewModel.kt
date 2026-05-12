package com.studio.vitalroute.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUiState(
    val metricSystem: Boolean     = true,
    val autoPause: Boolean        = true,
    val displayName: String       = "",
    val email: String             = "",
    val offlineStorage: String    = "A calcular…",
    val weeklyGoalKm: Float       = 50f,
    // Edição de perfil
    val isEditingName: Boolean    = false,
    val editNameValue: String     = "",
    val isSavingName: Boolean     = false,
    val saveNameError: String?    = null
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val repository = FirestoreRepository()

    init {
        loadProfile()
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            repository.getSettingsFlow()
                .catch { /* Erro ignorado ou logado */ }
                .collect { s -> 
                    _uiState.update { it.copy(weeklyGoalKm = s.weeklyGoalKm) } 
                }
        }
    }

    fun setWeeklyGoal(km: Float) {
        _uiState.update { it.copy(weeklyGoalKm = km) }
        viewModelScope.launch {
            try {
                val current = repository.getSettings()
                repository.saveSettings(current.copy(weeklyGoalKm = km))
            } catch (_: Exception) {}
        }
    }

    // perfil

    private fun loadProfile() {
        val user = Firebase.auth.currentUser
        val email = user?.email ?: ""
        val name  = user?.displayName
            ?: user?.email?.substringBefore("@")
            ?: "Atleta"

        _uiState.update { it.copy(email = email, displayName = name, editNameValue = name) }

        // Tenta carregar perfil do Firestore (pode ter nome mais completo)
        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile()
                if (profile != null && profile.name.isNotBlank()) {
                    _uiState.update { it.copy(displayName = profile.name, editNameValue = profile.name) }
                }
            } catch (_: Exception) {}
        }
    }

    fun startEditName() {
        _uiState.update { it.copy(isEditingName = true, editNameValue = it.displayName, saveNameError = null) }
    }

    fun updateEditName(value: String) {
        _uiState.update { it.copy(editNameValue = value) }
    }

    fun cancelEditName() {
        _uiState.update { it.copy(isEditingName = false, saveNameError = null) }
    }

    fun saveName() {
        val name = _uiState.value.editNameValue.trim()
        if (name.isBlank()) return
        _uiState.update { it.copy(isSavingName = true, saveNameError = null) }

        viewModelScope.launch {
            try {
                // Atualiza Firebase Auth displayName
                val user = Firebase.auth.currentUser
                val request = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user?.updateProfile(request)?.await()

                // Guarda também no Firestore
                val uid = user?.uid ?: ""
                val email = user?.email ?: ""
                repository.saveUserProfile(UserProfile(uid = uid, name = name, email = email))

                _uiState.update { it.copy(
                    displayName  = name,
                    isEditingName = false,
                    isSavingName  = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSavingName  = false,
                    saveNameError = "Erro ao guardar: ${e.message}"
                )}
            }
        }
    }

    // preferências

    fun toggleMetricSystem(enabled: Boolean) {
        _uiState.update { it.copy(metricSystem = enabled) }
    }

    fun toggleAutoPause(enabled: Boolean) {
        _uiState.update { it.copy(autoPause = enabled) }
    }
}
