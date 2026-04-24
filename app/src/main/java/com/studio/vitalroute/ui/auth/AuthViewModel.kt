package com.studio.vitalroute.ui.auth

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

// ─────────────────────────────────────────────────────────────
//  Estado da UI de autenticação
// ─────────────────────────────────────────────────────────────

data class AuthUiState(
    val isLoading: Boolean   = false,
    val isLoggedIn: Boolean  = false,
    val error: String?       = null,
    val isRegisterMode: Boolean = false
)

// ─────────────────────────────────────────────────────────────
//  AuthViewModel
// ─────────────────────────────────────────────────────────────

class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val repository = FirestoreRepository()

    private val _uiState = MutableStateFlow(AuthUiState(
        isLoggedIn = auth.currentUser != null
    ))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ── Login ─────────────────────────────────────────────────

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Preenche o email e a password") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = parseFirebaseError(e.message))
                }
            }
        }
    }

    // ── Registo ───────────────────────────────────────────────

    fun signUp(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Preenche todos os campos") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "A password deve ter pelo menos 6 caracteres") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid = result.user?.uid ?: error("UID nulo após registo")
                // Guarda o perfil no Firestore
                repository.saveUserProfile(UserProfile(uid = uid, name = name.trim(), email = email.trim()))
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = parseFirebaseError(e.message))
                }
            }
        }
    }

    // ── Modo convidado (offline — sem Firebase) ───────────────
    //
    //  Não faz nenhuma chamada de rede. Entra diretamente na app
    //  com um utilizador local temporário. Útil para testar sem
    //  internet ou antes de criar conta.

    fun signInAsGuest() {
        _uiState.update { it.copy(isLoggedIn = true, error = null) }
    }

    // ── Logout ────────────────────────────────────────────────

    fun signOut() {
        auth.signOut()
        _uiState.update { it.copy(isLoggedIn = false) }
    }

    // ── Toggle login/registo ──────────────────────────────────

    fun toggleMode() {
        _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ── Traduz erros Firebase para português ─────────────────

    private fun parseFirebaseError(message: String?): String = when {
        message == null                             -> "Erro desconhecido"
        message.contains("INVALID_LOGIN_CREDENTIALS") ||
        message.contains("wrong-password") ||
        message.contains("user-not-found")         -> "Email ou password incorretos"
        message.contains("email-already-in-use")   -> "Este email já está registado"
        message.contains("invalid-email")           -> "Email inválido"
        message.contains("network")                 -> "Sem ligação à Internet. Verifica a internet do emulador ou usa 'Continuar como Convidado'"
        message.contains("too-many-requests")       -> "Demasiadas tentativas. Tenta mais tarde"
        else                                        -> message // mostra o erro real para debug
    }
}
