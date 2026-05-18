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

    // pede permissao de notificacoes em android 13
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* resultado ignorado app funciona sem notificacoes */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()

            // ouve estado de autenticacao do firebase em tempo real
            // quando o utilizador faz login ou logout a ui atualiza automaticamente
            DisposableEffect(Unit) {
                val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
                    // este listener e gerido pelo authviewmodel mas aqui usamos
                    // o estado do viewmodel que ja esta sincronizado
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose { Firebase.auth.removeAuthStateListener(listener) }
            }

            if (authState.isLoggedIn) {

                // utilizador autenticado mostra a app principal
                VitalRouteNavGraph(onSignOut = { authViewModel.signOut() })
            } else {
                // utilizador nao autenticado mostra o ecra de login
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
