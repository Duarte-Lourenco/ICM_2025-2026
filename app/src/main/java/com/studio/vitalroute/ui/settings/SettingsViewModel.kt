package com.studio.vitalroute.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
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
    val displayName: String       = "",
    val email: String             = "",
    val weeklyGoalKm: Float       = 50f,
    // Dados físicos
    val weightKg: Float           = 70f,
    val heightCm: Int             = 170,
    val gender: String            = "male",
    // Edição de perfil
    val isEditingName: Boolean    = false,
    val editNameValue: String     = "",
    val isEditingWeight: Boolean  = false,
    val editWeightValue: String   = "",
    val isEditingHeight: Boolean  = false,
    val editHeightValue: String   = "",
    val isSavingName: Boolean     = false,
    val saveNameError: String?    = null,
    // Mudança de password
    val isChangingPassword: Boolean      = false,
    val currentPasswordInput: String     = "",
    val newPasswordInput: String         = "",
    val confirmPasswordInput: String     = "",
    val passwordChangeLoading: Boolean   = false,
    val passwordChangeError: String?     = null,
    val passwordChangeSuccess: Boolean   = false,
    // Mudança de email
    val isChangingEmail: Boolean         = false,
    val newEmailInput: String            = "",
    val emailChangePasswordInput: String = "",
    val emailChangeLoading: Boolean      = false,
    val emailChangeError: String?        = null,
    val emailChangeSuccess: Boolean      = false
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
                .catch { }
                .collect { s ->
                    _uiState.update { it.copy(weeklyGoalKm = s.weeklyGoalKm, metricSystem = s.metricSystem) }
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

        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile()
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            displayName     = profile.name.ifBlank { it.displayName },
                            editNameValue   = profile.name.ifBlank { it.displayName },
                            weightKg        = profile.weightKg,
                            heightCm        = profile.heightCm,
                            gender          = profile.gender,
                            editWeightValue = profile.weightKg.toInt().toString(),
                            editHeightValue = profile.heightCm.toString()
                        )
                    }
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
                val user = Firebase.auth.currentUser
                val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                user?.updateProfile(request)?.await()
                saveProfile(_uiState.value.copy(displayName = name))
                _uiState.update { it.copy(displayName = name, isEditingName = false, isSavingName = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingName = false, saveNameError = "Erro ao guardar: ${e.message}") }
            }
        }
    }

    // peso
    fun startEditWeight()  { _uiState.update { it.copy(isEditingWeight = true, editWeightValue = it.weightKg.toInt().toString()) } }
    fun updateEditWeight(v: String) { _uiState.update { it.copy(editWeightValue = v) } }
    fun cancelEditWeight() { _uiState.update { it.copy(isEditingWeight = false) } }
    fun saveWeight() {
        val w = _uiState.value.editWeightValue.toFloatOrNull() ?: return
        if (w < 20f || w > 300f) return
        _uiState.update { it.copy(weightKg = w, isEditingWeight = false) }
        viewModelScope.launch { saveProfile(_uiState.value) }
    }

    // altura
    fun startEditHeight()  { _uiState.update { it.copy(isEditingHeight = true, editHeightValue = it.heightCm.toString()) } }
    fun updateEditHeight(v: String) { _uiState.update { it.copy(editHeightValue = v) } }
    fun cancelEditHeight() { _uiState.update { it.copy(isEditingHeight = false) } }
    fun saveHeight() {
        val h = _uiState.value.editHeightValue.toIntOrNull() ?: return
        if (h < 100 || h > 250) return
        _uiState.update { it.copy(heightCm = h, isEditingHeight = false) }
        viewModelScope.launch { saveProfile(_uiState.value) }
    }

    // género
    fun setGender(g: String) {
        _uiState.update { it.copy(gender = g) }
        viewModelScope.launch { saveProfile(_uiState.value) }
    }

    // --- Alterar password ---

    fun startChangePassword() {
        _uiState.update {
            it.copy(isChangingPassword = true, currentPasswordInput = "",
                newPasswordInput = "", confirmPasswordInput = "",
                passwordChangeError = null, passwordChangeSuccess = false)
        }
    }

    fun cancelChangePassword() {
        _uiState.update { it.copy(isChangingPassword = false, passwordChangeError = null, passwordChangeSuccess = false) }
    }

    fun updateCurrentPasswordInput(v: String)  { _uiState.update { it.copy(currentPasswordInput = v) } }
    fun updateNewPasswordInput(v: String)       { _uiState.update { it.copy(newPasswordInput = v) } }
    fun updateConfirmPasswordInput(v: String)   { _uiState.update { it.copy(confirmPasswordInput = v) } }

    fun savePassword() {
        val s = _uiState.value
        if (s.currentPasswordInput.isBlank()) {
            _uiState.update { it.copy(passwordChangeError = "Introduz a password atual") }
            return
        }
        validatePassword(s.newPasswordInput)?.let { err ->
            _uiState.update { it.copy(passwordChangeError = err) }
            return
        }
        if (s.newPasswordInput != s.confirmPasswordInput) {
            _uiState.update { it.copy(passwordChangeError = "As passwords não coincidem") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(passwordChangeLoading = true, passwordChangeError = null) }
            try {
                reAuthenticate(s.currentPasswordInput)
                Firebase.auth.currentUser?.updatePassword(s.newPasswordInput)?.await()
                _uiState.update { it.copy(passwordChangeLoading = false, passwordChangeSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(passwordChangeLoading = false, passwordChangeError = parseAuthError(e.message)) }
            }
        }
    }

    // --- Alterar email ---

    fun startChangeEmail() {
        _uiState.update {
            it.copy(isChangingEmail = true, newEmailInput = "", emailChangePasswordInput = "",
                emailChangeError = null, emailChangeSuccess = false)
        }
    }

    fun cancelChangeEmail() {
        _uiState.update { it.copy(isChangingEmail = false, emailChangeError = null, emailChangeSuccess = false) }
    }

    fun updateNewEmailInput(v: String)              { _uiState.update { it.copy(newEmailInput = v) } }
    fun updateEmailChangePasswordInput(v: String)   { _uiState.update { it.copy(emailChangePasswordInput = v) } }

    fun saveEmail() {
        val s = _uiState.value
        if (s.newEmailInput.isBlank()) {
            _uiState.update { it.copy(emailChangeError = "Introduz o novo email") }
            return
        }
        if (s.emailChangePasswordInput.isBlank()) {
            _uiState.update { it.copy(emailChangeError = "Introduz a tua password atual") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(emailChangeLoading = true, emailChangeError = null) }
            try {
                reAuthenticate(s.emailChangePasswordInput)
                Firebase.auth.currentUser?.verifyBeforeUpdateEmail(s.newEmailInput.trim())?.await()
                _uiState.update { it.copy(emailChangeLoading = false, emailChangeSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(emailChangeLoading = false, emailChangeError = parseAuthError(e.message)) }
            }
        }
    }

    // --- Helpers ---

    private suspend fun reAuthenticate(password: String) {
        val user  = Firebase.auth.currentUser ?: throw Exception("Utilizador não autenticado")
        val email = user.email ?: throw Exception("Email não disponível")
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).await()
    }

    private fun validatePassword(password: String): String? = when {
        password.length < 8                -> "A password deve ter pelo menos 8 caracteres"
        !password.any { it.isUpperCase() } -> "A password deve conter pelo menos uma letra maiúscula"
        !password.any { it.isLowerCase() } -> "A password deve conter pelo menos uma letra minúscula"
        !password.any { it.isDigit() }     -> "A password deve conter pelo menos um número"
        else                               -> null
    }

    private fun parseAuthError(message: String?): String = when {
        message == null -> "Erro desconhecido"
        message.contains("wrong-password") ||
        message.contains("INVALID_LOGIN_CREDENTIALS") -> "Password atual incorreta"
        message.contains("email-already-in-use")      -> "Este email já está em uso"
        message.contains("invalid-email")             -> "Email inválido"
        message.contains("network")                   -> "Sem ligação à Internet"
        message.contains("requires-recent-login")     -> "Por segurança, termina sessão e volta a entrar antes de alterar"
        else -> message
    }

    private suspend fun saveProfile(s: SettingsUiState) {
        val user = Firebase.auth.currentUser ?: return
        try {
            repository.saveUserProfile(
                UserProfile(uid = user.uid, name = s.displayName, email = user.email ?: "",
                    weightKg = s.weightKg, heightCm = s.heightCm, gender = s.gender)
            )
        } catch (_: Exception) {}
    }

    // preferências

    fun toggleMetricSystem(enabled: Boolean) {
        _uiState.update { it.copy(metricSystem = enabled) }
        viewModelScope.launch {
            try {
                val current = repository.getSettings()
                repository.saveSettings(current.copy(metricSystem = enabled))
            } catch (_: Exception) {}
        }
    }
}
