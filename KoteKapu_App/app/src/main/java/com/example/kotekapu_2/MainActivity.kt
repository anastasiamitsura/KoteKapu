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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.kotekapu_2.screens.CompletePreferencesScreen
import com.example.kotekapu_2.screens.CompleteProfileScreen
import com.example.kotekapu_2.screens.EventDetailScreen
import com.example.kotekapu_2.screens.HelpScreen
import com.example.kotekapu_2.screens.LoginScreen
import com.example.kotekapu_2.screens.MainScreen
import com.example.kotekapu_2.screens.ProfileScreen
import com.example.kotekapu_2.screens.RegisterScreen
import com.example.kotekapu_2.screens.SearchScreen
import com.example.kotekapu_2.screens.SplashScreen
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

    val isLoggedIn by authManager.isLoggedIn.collectAsState()
    val authToken by authManager.accessToken.collectAsState()
    val userId by authManager.userId.collectAsState()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
    var selectedEvent by remember { mutableStateOf<Post?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var forceNextScreen by remember { mutableStateOf<AppScreen?>(null) } // ← ДОБАВИТЬ

    val coroutineScope = rememberCoroutineScope()


    fun determineNextScreen(user: User): AppScreen {
        println("DEBUG: User state - profile_completed: ${user.safeIsProfileCompleted()}, preferences_completed: ${user.safeIsPreferencesCompleted()}")
        return when {
            !user.safeIsProfileCompleted() -> AppScreen.CompleteProfile
            !user.safeIsPreferencesCompleted() -> AppScreen.CompletePreferences
            else -> AppScreen.Main
        }
    }

    fun loadUserProfile() {
        coroutineScope.launch {
            try {
                val token = authManager.getCurrentToken()
                val userId = authManager.getCurrentUserId()

                if (token != null && userId != null) {
                    println("DEBUG: Loading user profile...")
                    val result = apiService.getUserProfile(token, userId)

                    if (result.isSuccess) {
                        val userProfile = result.getOrNull()
                        userProfile?.user?.let { user ->
                            currentUser = user
                            currentScreen = forceNextScreen ?: determineNextScreen(user)
                            println("DEBUG: User loaded: ${user.first_name}, screen: $currentScreen")
                        } ?: run {
                            println("DEBUG: User profile is null")
                            currentScreen = if (isLoggedIn) AppScreen.CompleteProfile else AppScreen.Login
                        }
                    } else {
                        println("DEBUG: Failed to load user profile")
                        currentScreen = if (isLoggedIn) AppScreen.CompleteProfile else AppScreen.Login
                    }
                } else {
                    println("DEBUG: No token or user ID")
                    currentScreen = AppScreen.Login
                }
            } catch (e: Exception) {
                println("DEBUG: Error loading profile: ${e.message}")
                currentScreen = if (isLoggedIn) AppScreen.CompleteProfile else AppScreen.Login
            } finally {
                isLoading = false
                forceNextScreen = null
            }
        }
    }


    LaunchedEffect(Unit) {
        println("DEBUG: App started, checking auth state...")
        loadUserProfile()
    }


    LaunchedEffect(isLoggedIn) {
        if (!isLoading) {
            println("DEBUG: Auth state changed to: $isLoggedIn")
            if (isLoggedIn) {
                loadUserProfile()
            } else {
                currentScreen = AppScreen.Login
                currentUser = null
            }
        }
    }


    when (currentScreen) {
        AppScreen.Splash -> SplashScreen()

        AppScreen.Login -> LoginScreen(
            apiService = apiService,
            authManager = authManager,
            onLoginSuccess = {
                println("DEBUG: Login success, loading profile...")
                loadUserProfile()
            },
            onNavigateToRegister = {
                currentScreen = AppScreen.Register
            }
        )

        AppScreen.Register -> RegisterScreen(
            apiService = apiService,
            authManager = authManager,
            onRegisterSuccess = {
                println("DEBUG: Register success, going to profile setup")
                currentScreen = AppScreen.CompleteProfile
            },
            onNavigateToLogin = {
                currentScreen = AppScreen.Login
            }
        )

        AppScreen.CompleteProfile -> CompleteProfileScreen(
            apiService = apiService,
            authManager = authManager,
            onProfileComplete = {
                println("DEBUG: Profile complete")
                // ВМЕСТО loadUserProfile() - принудительно переходим к следующему шагу
                forceNextScreen = AppScreen.CompletePreferences
                currentScreen = AppScreen.CompletePreferences
            },
            onSkip = {
                println("DEBUG: Profile skipped")
                forceNextScreen = AppScreen.CompletePreferences
                currentScreen = AppScreen.CompletePreferences
            }
        )

        AppScreen.CompletePreferences -> CompletePreferencesScreen(
            apiService = apiService,
            authManager = authManager,
            onPreferencesComplete = {
                println("DEBUG: Preferences complete")
                forceNextScreen = AppScreen.Main
                currentScreen = AppScreen.Main
            }
        )


        AppScreen.Main -> MainScreen(
            apiService = apiService,
            authManager = authManager,
            onLogout = {
                coroutineScope.launch {
                    authManager.logout()
                }
            },
            onEventClick = { event ->
                selectedEvent = event
                currentScreen = AppScreen.EventDetail
            },
            onSearchClick = {
                currentScreen = AppScreen.Search
            },
            onProfileClick = {
                currentScreen = AppScreen.Profile
            },
            onHelpClick = {
                currentScreen = AppScreen.Help
            }
        )

        AppScreen.EventDetail -> selectedEvent?.let { event ->
            EventDetailScreen(
                event = event,
                apiService = apiService,
                authManager = authManager,
                onBack = {
                    currentScreen = AppScreen.Main
                },
                onRegister = { registeredEvent ->
                    selectedEvent = registeredEvent
                }
            )
        } ?: run {
            currentScreen = AppScreen.Main
        }

        AppScreen.Profile -> ProfileScreen(
            apiService = apiService,
            authManager = authManager,
            onBack = { currentScreen = AppScreen.Main },
            onEditProfile = {
                // TODO: Реализовать экран редактирования профиля
            },
            onViewEventDetails = { event ->
                selectedEvent = event
                currentScreen = AppScreen.EventDetail
            }
        )

        AppScreen.Search -> SearchScreen(
            apiService = apiService,
            authManager = authManager,
            onBack = { currentScreen = AppScreen.Main },
            onEventClick = { event ->
                selectedEvent = event
                currentScreen = AppScreen.EventDetail
            },
            onOrganizationClick = { organization ->
                // TODO: Реализовать экран организации
            }
        )

        AppScreen.CreateOrganisation -> {
            Text("Создание организации (в разработке)")
        }

        AppScreen.Help -> HelpScreen(
            onBack = { currentScreen = AppScreen.Main }
        )
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
    object Help : AppScreen()
}