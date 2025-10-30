package com.example.kotekapu_2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// Создайте новый файл CompleteProfileScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    apiService: ApiService,
    authManager: AuthManager,
    onProfileComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var placement by remember { mutableStateOf("") }
    var studyPlace by remember { mutableStateOf("") }
    var gradeCourse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заполнение профиля") },
                navigationIcon = {
                    IconButton(onClick = onSkip) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Добро пожаловать! Давайте заполним ваш профиль",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Номер телефона *") },
                placeholder = { Text("+7 999 123-45-67") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = age,
                onValueChange = { if (it.all { char -> char.isDigit() }) age = it },
                label = { Text("Возраст") },
                placeholder = { Text("18") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = placement,
                onValueChange = { placement = it },
                label = { Text("Населенный пункт *") },
                placeholder = { Text("Москва") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = studyPlace,
                onValueChange = { studyPlace = it },
                label = { Text("Учебное заведение") },
                placeholder = { Text("Школа/ВУЗ") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = gradeCourse,
                onValueChange = { gradeCourse = it },
                label = { Text("Класс/Курс") },
                placeholder = { Text("11 класс / 3 курс") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                singleLine = true
            )

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    if (phone.isBlank() || placement.isBlank()) {
                        errorMessage = "Заполните обязательные поля (отмечены *)"
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val token = authManager.getCurrentToken()
                            val userId = authManager.getCurrentUserId()

                            if (token != null && userId != null) {
                                val result = apiService.completeProfile(
                                    token = token,
                                    userId = userId,
                                    phone = phone,
                                    age = age.toIntOrNull(),
                                    placement = placement,
                                    studyPlace = studyPlace,
                                    gradeCourse = gradeCourse
                                )

                                if (result.isSuccess) {
                                    onProfileComplete()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Ошибка сохранения"
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка сети: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White
                    )
                } else {
                    Text("Продолжить")
                }
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Пропустить")
            }
        }
    }
}