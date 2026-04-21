package com.studio.vitalroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.studio.vitalroute.navigation.VitalRouteNavGraph
import com.studio.vitalroute.ui.auth.AuthViewModel
import com.studio.vitalroute.ui.auth.LoginScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()

            // Ouve o estado de autenticação do Firebase em tempo real.
            // Quando o utilizador faz login/logout, a UI atualiza automaticamente.
            DisposableEffect(Unit) {
                val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
                    // Este listener é gerido pelo AuthViewModel mas aqui usamos
                    // o estado do ViewModel que já está sincronizado
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose { Firebase.auth.removeAuthStateListener(listener) }
            }

            if (authState.isLoggedIn) {
                // Utilizador autenticado — mostra a app principal
                VitalRouteNavGraph(onSignOut = { authViewModel.signOut() })
            } else {
                // Utilizador não autenticado — mostra o ecrã de login
                LoginScreen(viewModel = authViewModel)
            }
        }
    }
}
