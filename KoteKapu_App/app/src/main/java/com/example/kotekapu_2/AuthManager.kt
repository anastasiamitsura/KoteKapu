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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_settings")

class AuthManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)


    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken

    private val _userId = MutableStateFlow<Int?>(null)
    val userId: StateFlow<Int?> = _userId

    init {

        loadAuthData()
    }

    private fun loadAuthData() {
        coroutineScope.launch {
            try {
                val preferences = context.dataStore.data.first()
                val accessToken = preferences[ACCESS_TOKEN_KEY]
                val isLoggedInValue = preferences[IS_LOGGED_IN_KEY] ?: false
                val userIdStr = preferences[USER_ID_KEY]

                println("DEBUG: Loaded from DataStore - accessToken: $accessToken, isLoggedIn: $isLoggedInValue, userId: $userIdStr")


                _accessToken.value = accessToken
                _isLoggedIn.value = isLoggedInValue
                _userId.value = userIdStr?.toIntOrNull()

                println("DEBUG: StateFlow updated - accessToken: ${_accessToken.value}, isLoggedIn: ${_isLoggedIn.value}, userId: ${_userId.value}")

            } catch (e: Exception) {
                println("DEBUG: Error loading auth data: ${e.message}")
            }
        }
    }


    suspend fun saveAuthData(accessToken: String, userId: Int) {
        try {
            context.dataStore.edit { preferences ->
                preferences[ACCESS_TOKEN_KEY] = accessToken
                preferences[IS_LOGGED_IN_KEY] = true
                preferences[USER_ID_KEY] = userId.toString()
            }

            _accessToken.value = accessToken
            _isLoggedIn.value = true
            _userId.value = userId

            println("DEBUG: Auth data saved - accessToken: $accessToken, userId: $userId")
            println("DEBUG: StateFlow after save - accessToken: ${_accessToken.value}, isLoggedIn: ${_isLoggedIn.value}, userId: ${_userId.value}")

        } catch (e: Exception) {
            println("DEBUG: Error saving auth data: ${e.message}")
        }
    }


    suspend fun logout() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(ACCESS_TOKEN_KEY)
                preferences.remove(IS_LOGGED_IN_KEY)
                preferences.remove(USER_ID_KEY)
            }

            _accessToken.value = null
            _isLoggedIn.value = false
            _userId.value = null

            println("DEBUG: User logged out")
        } catch (e: Exception) {
            println("DEBUG: Error during logout: ${e.message}")
        }
    }


    fun isUserLoggedIn(): Boolean {
        return _isLoggedIn.value
    }


    fun getCurrentToken(): String? {
        return _accessToken.value
    }


    fun getCurrentUserId(): Int? {
        return _userId.value
    }
    fun getCurrentUser(): User? {
        return if (isLoggedIn.value && userId.value != null) {
            User(
                id = userId.value!!,
                email = "",
                first_name = "",
                last_name = ""
            )
        } else {
            null
        }
    }
}