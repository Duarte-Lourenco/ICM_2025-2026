package com.studio.vitalroute

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.studio.vitalroute.navigation.VitalRouteNavGraph
import com.studio.vitalroute.ui.auth.AuthViewModel
import com.studio.vitalroute.ui.auth.LoginScreen
import com.studio.vitalroute.ui.recording.RecordingService
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    // Pede permissão de notificações em Android 13+
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* resultado ignorado — app funciona sem notificações */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            RecordingService.CHANNEL_ID,
            "Gravação de Atividade",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mostra o tempo e distância enquanto uma atividade está a ser gravada"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
