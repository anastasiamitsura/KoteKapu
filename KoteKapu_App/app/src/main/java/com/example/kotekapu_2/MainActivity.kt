package com.example.kotekapu_2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.kotekapu_2.ui.theme.KoteKapu_2Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoteKapu_2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KoteKapu_2App()
                }
            }
        }
    }
}

@Composable
fun KoteKapu_2App() {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val apiService = remember { ApiService() }

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
    var selectedEvent by remember { mutableStateOf<Post?>(null) } // ДОБАВЬТЕ ЭТУ СТРОКУ

    val coroutineScope = rememberCoroutineScope()

    // Проверяем авторизацию при запуске
    LaunchedEffect(authManager) {
        authManager.isLoggedIn.collect { isLoggedIn ->
            println("DEBUG: Auth state changed - isLoggedIn: $isLoggedIn")
            currentScreen = if (isLoggedIn) {
                AppScreen.Main
            } else {
                AppScreen.Login
            }
        }
    }

    // Навигация
    when (currentScreen) {
        AppScreen.Splash -> SplashScreen()
        AppScreen.Login -> LoginScreen(
            apiService = apiService,
            authManager = authManager,
            onLoginSuccess = {
                println("DEBUG: Login success, navigating to Main")
                currentScreen = AppScreen.Main
            },
            onNavigateToRegister = {
                println("DEBUG: Navigating to Register")
                currentScreen = AppScreen.Register
            }
        )
        AppScreen.Register -> RegisterScreen(
            apiService = apiService,
            authManager = authManager,
            onRegisterSuccess = {
                println("DEBUG: Register success, navigating to CompleteProfile")
                currentScreen = AppScreen.CompleteProfile
            },
            onNavigateToLogin = {
                println("DEBUG: Navigating to Login")
                currentScreen = AppScreen.Login
            }
        )
        AppScreen.CompleteProfile -> CompleteProfileScreen(
            apiService = apiService,
            authManager = authManager,
            onProfileComplete = {
                println("DEBUG: Profile complete, navigating to Preferences")
                currentScreen = AppScreen.CompletePreferences
            },
            onSkip = {
                println("DEBUG: Skipping profile, navigating to Preferences")
                currentScreen = AppScreen.CompletePreferences
            }
        )
        AppScreen.CompletePreferences -> CompletePreferencesScreen(
            apiService = apiService,
            authManager = authManager,
            onPreferencesComplete = {
                println("DEBUG: Preferences complete, navigating to Main")
                currentScreen = AppScreen.Main
            }
        )
        AppScreen.Main -> MainScreen(
            apiService = apiService,
            authManager = authManager,
            onLogout = {
                println("DEBUG: Logout requested")
                coroutineScope.launch {
                    authManager.logout()
                }
            },
            onEventClick = { event ->
                println("DEBUG: Event clicked: ${event.title}")
                selectedEvent = event
                currentScreen = AppScreen.EventDetail
            },
            onSearchClick = {
                println("DEBUG: Search clicked")
                // TODO: Реализовать поиск
                // currentScreen = AppScreen.Search
            },
            onProfileClick = {
                println("DEBUG: Profile clicked")
                // TODO: Реализовать профиль
                // currentScreen = AppScreen.Profile
            }
        )
        AppScreen.EventDetail -> selectedEvent?.let { event ->
            EventDetailScreen(
                event = event,
                apiService = apiService,
                authManager = authManager,
                onBack = {
                    println("DEBUG: Back from event detail")
                    currentScreen = AppScreen.Main
                },
                onRegister = { registeredEvent ->
                    println("DEBUG: Event registered: ${registeredEvent.title}")
                    selectedEvent = registeredEvent
                }
            )
        } ?: run {
            // Если события нет, возвращаемся назад
            println("DEBUG: No event selected, returning to Main")
            currentScreen = AppScreen.Main
        }
        AppScreen.Search -> {
            // TODO: Реализовать экран поиска
            Text("Экран поиска (в разработке)")
        }
        AppScreen.Profile -> {
            // TODO: Реализовать экран профиля
            Text("Экран профиля (в разработке)")
        }
        AppScreen.CreateOrganisation -> {
            // TODO: Реализовать создание организации
            Text("Создание организации (в разработке)")
        }
    }
}

sealed class AppScreen {
    object Splash : AppScreen()
    object Login : AppScreen()
    object Register : AppScreen()
    object CompleteProfile : AppScreen()
    object CompletePreferences : AppScreen()
    object Main : AppScreen()
    object EventDetail : AppScreen()
    object Search : AppScreen()
    object Profile : AppScreen()
    object CreateOrganisation : AppScreen()
}