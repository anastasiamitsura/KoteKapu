package com.example.kotekapu_2

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_settings")

private val _profileCompleted = MutableStateFlow(false)
val profileCompleted: StateFlow<Boolean> = _profileCompleted

private val _preferencesCompleted = MutableStateFlow(false)
val preferencesCompleted: StateFlow<Boolean> = _preferencesCompleted

class AuthManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // StateFlow для наблюдения за состоянием авторизации
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken

    private val _userId = MutableStateFlow<Int?>(null)
    val userId: StateFlow<Int?> = _userId

    init {
        // Загружаем данные при инициализации в coroutine
        loadAuthData()
    }

    private fun loadAuthData() {
        coroutineScope.launch {
            try {
                val preferences = context.dataStore.data.first()
                val token = preferences[TOKEN_KEY]
                val isLoggedInValue = preferences[IS_LOGGED_IN_KEY] ?: false
                val userIdStr = preferences[USER_ID_KEY]

                _authToken.value = token
                _isLoggedIn.value = isLoggedInValue
                _userId.value = userIdStr?.toIntOrNull()

                // TODO: Запросить статус профиля с сервера
                // Временно устанавливаем значения
                _profileCompleted.value = true
                _preferencesCompleted.value = true
            } catch (e: Exception) {
                println("DEBUG: Error loading auth data: ${e.message}")
            }
        }
    }

    // Сохранение данных авторизации
    suspend fun saveAuthData(token: String, userId: Int) {
        try {
            context.dataStore.edit { preferences ->
                preferences[TOKEN_KEY] = token
                preferences[IS_LOGGED_IN_KEY] = true
                preferences[USER_ID_KEY] = userId.toString()
            }
            // Обновляем StateFlow
            _authToken.value = token
            _isLoggedIn.value = true
            _userId.value = userId

            println("DEBUG: Auth data saved - token: $token, userId: $userId")
        } catch (e: Exception) {
            println("DEBUG: Error saving auth data: ${e.message}")
        }
    }

    // Выход
    suspend fun logout() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(TOKEN_KEY)
                preferences.remove(IS_LOGGED_IN_KEY)
                preferences.remove(USER_ID_KEY)
            }
            // Обновляем StateFlow
            _authToken.value = null
            _isLoggedIn.value = false
            _userId.value = null

            println("DEBUG: User logged out")
        } catch (e: Exception) {
            println("DEBUG: Error during logout: ${e.message}")
        }
    }

    // Быстрая проверка авторизации (без suspend)
    fun isUserLoggedIn(): Boolean {
        return _isLoggedIn.value
    }

    // Получение токена (без suspend)
    fun getCurrentToken(): String? {
        return _authToken.value
    }

    // Получение ID пользователя (без suspend)
    fun getCurrentUserId(): Int? {
        return _userId.value
    }
}