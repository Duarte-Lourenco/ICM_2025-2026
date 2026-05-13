package com.studio.vitalroute.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val isRegisterMode: Boolean = false,
    val isEmailVerificationPending: Boolean = false,
    val pendingEmail: String = "",
    val verificationEmailSent: Boolean = false,
    // Recuperação de password
    val isForgotPassword: Boolean = false,
    val forgotEmail: String = "",
    val forgotLoading: Boolean = false,
    val forgotError: String? = null,
    val forgotSuccess: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val repository = FirestoreRepository()

    private val _uiState = MutableStateFlow(
        auth.currentUser.let { user ->
            when {
                user == null -> AuthUiState()
                user.isEmailVerified -> AuthUiState(isLoggedIn = true)
                else -> AuthUiState(isEmailVerificationPending = true, pendingEmail = user.email ?: "")
            }
        }
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Preenche o email e a password") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                } else {
                    var emailSentOk = false
                    try {
                        user?.sendEmailVerification()?.await()
                        emailSentOk = true
                        Log.d("AuthViewModel", "Verification email sent to ${email.trim()}")
                    } catch (emailEx: Exception) {
                        Log.e("AuthViewModel", "sendEmailVerification failed: ${emailEx.message}")
                    }
                    _uiState.update {
                        it.copy(isLoading = false, isEmailVerificationPending = true,
                            pendingEmail = email.trim(), verificationEmailSent = emailSentOk,
                            error = if (!emailSentOk) "Falhou o envio do email de verificação. Usa 'Reenviar email'." else null)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = parseFirebaseError(e.message)) }
            }
        }
    }

    fun signUp(name: String, email: String, password: String, weightKg: Float, heightCm: Int, gender: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Preenche todos os campos") }
            return
        }
        validatePassword(password)?.let { err ->
            _uiState.update { it.copy(error = err) }
            return
        }
        if (weightKg !in 20f..250f) {
            _uiState.update { it.copy(error = "Peso inválido (20 – 250 kg)") }
            return
        }
        if (heightCm !in 100..230) {
            _uiState.update { it.copy(error = "Altura inválida (100 – 230 cm)") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val user = result.user ?: error("Utilizador nulo após registo")
                repository.saveUserProfile(
                    UserProfile(uid = user.uid, name = name.trim(), email = email.trim(),
                        weightKg = weightKg, heightCm = heightCm, gender = gender)
                )
                var emailSentOk = false
                try {
                    user.sendEmailVerification().await()
                    emailSentOk = true
                    Log.d("AuthViewModel", "Verification email sent to ${email.trim()}")
                } catch (emailEx: Exception) {
                    Log.e("AuthViewModel", "sendEmailVerification failed: ${emailEx.message}")
                }
                _uiState.update {
                    it.copy(isLoading = false, isEmailVerificationPending = true,
                        pendingEmail = email.trim(), verificationEmailSent = emailSentOk,
                        error = if (!emailSentOk) "Conta criada, mas falhou o envio do email de verificação. Usa 'Reenviar email'." else null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = parseFirebaseError(e.message)) }
            }
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.currentUser?.reload()?.await()
                if (auth.currentUser?.isEmailVerified == true) {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isEmailVerificationPending = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Email ainda não verificado. Verifica a tua caixa de entrada.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = parseFirebaseError(e.message)) }
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, verificationEmailSent = false) }
            try {
                val user = auth.currentUser
                Log.d("AuthViewModel", "Resend: currentUser=${user?.email}, uid=${user?.uid}")
                user?.sendEmailVerification()?.await()
                Log.d("AuthViewModel", "Resend verification email sent to ${user?.email}")
                _uiState.update { it.copy(isLoading = false, verificationEmailSent = true) }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Resend failed: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Erro ao reenviar: ${e.message}") }
            }
        }
    }

    // --- Recuperação de password ---

    fun startForgotPassword(emailPrefill: String = "") {
        _uiState.update { it.copy(isForgotPassword = true, forgotEmail = emailPrefill,
            forgotError = null, forgotSuccess = false) }
    }

    fun cancelForgotPassword() {
        _uiState.update { it.copy(isForgotPassword = false, forgotError = null, forgotSuccess = false) }
    }

    fun updateForgotEmail(v: String) {
        _uiState.update { it.copy(forgotEmail = v) }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.forgotEmail.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(forgotError = "Introduz o teu email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(forgotLoading = true, forgotError = null) }
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.update { it.copy(forgotLoading = false, forgotSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(forgotLoading = false, forgotError = parseFirebaseError(e.message)) }
            }
        }
    }

    fun signInAsGuest() {
        _uiState.update { it.copy(isLoggedIn = true, error = null) }
    }

    fun signOut() {
        auth.signOut()
        _uiState.update { it.copy(isLoggedIn = false, isEmailVerificationPending = false, pendingEmail = "") }
    }

    fun toggleMode() {
        _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun validatePassword(password: String): String? = when {
        password.length < 8               -> "A password deve ter pelo menos 8 caracteres"
        !password.any { it.isUpperCase() } -> "A password deve conter pelo menos uma letra maiúscula"
        !password.any { it.isLowerCase() } -> "A password deve conter pelo menos uma letra minúscula"
        !password.any { it.isDigit() }     -> "A password deve conter pelo menos um número"
        else                               -> null
    }

    private fun parseFirebaseError(message: String?): String = when {
        message == null -> "Erro desconhecido"
        message.contains("INVALID_LOGIN_CREDENTIALS") ||
        message.contains("wrong-password") ||
        message.contains("user-not-found")       -> "Email ou password incorretos"
        message.contains("email-already-in-use") -> "Este email já está registado"
        message.contains("invalid-email")        -> "Email inválido"
        message.contains("network")              -> "Sem ligação à Internet. Verifica a internet do emulador ou usa 'Continuar como Convidado'"
        message.contains("too-many-requests")    -> "Demasiadas tentativas. Tenta mais tarde"
        else -> message
    }
}