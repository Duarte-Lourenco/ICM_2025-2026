package com.studio.vitalroute.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studio.vitalroute.ui.theme.*
import com.studio.vitalroute.ui.home.HomeScreen
import com.studio.vitalroute.ui.maps.MapsScreen
import com.studio.vitalroute.ui.recording.RecordingScreen
import com.studio.vitalroute.ui.security.SecurityScreen
import com.studio.vitalroute.ui.diary.DiaryScreen
import com.studio.vitalroute.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Inicio : Screen("inicio", "Início", Icons.Default.Home)
    object Mapas : Screen("mapas", "Mapas", Icons.Default.Map)
    object Iniciar : Screen("iniciar", "Iniciar", Icons.Default.RadioButtonChecked)
    object Seguranca : Screen("seguranca", "Segurança", Icons.Default.Shield)
    object Perfil : Screen("perfil", "Perfil", Icons.Default.BarChart)
}

@Composable
fun VitalRouteNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != "settings") {
                VitalBottomBar(navController)
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Inicio.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Inicio.route) { HomeScreen() }
            composable(Screen.Mapas.route) { MapsScreen() }
            composable(Screen.Iniciar.route) { RecordingScreen() }
            composable(Screen.Seguranca.route) { SecurityScreen() }
            composable(Screen.Perfil.route) { DiaryScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
        }
    }
}

@Composable
fun VitalBottomBar(navController: NavHostController) {
    val items = listOf(Screen.Inicio, Screen.Mapas, Screen.Iniciar, Screen.Seguranca, Screen.Perfil)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier.fillMaxWidth().height(85.dp),
        color = Color.Black,
        border = BorderStroke(0.5.dp, Color.DarkGray)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route
                if (screen is Screen.Iniciar) {
                    Box(
                        modifier = Modifier.weight(1.2f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { navController.navigate(screen.route) },
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    Brush.verticalGradient(listOf(VitalRed, VitalOrange)),
                                    CircleShape
                                )
                        ) {
                            Icon(screen.icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { navController.navigate(screen.route) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(screen.icon, null, tint = if (isSelected) VitalGreen else Color.Gray, modifier = Modifier.size(24.dp))
                        Text(text = screen.label, color = if (isSelected) VitalGreen else Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}