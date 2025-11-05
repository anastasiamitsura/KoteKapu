// CreateEventScreen.kt
package com.example.kotekapu_2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.kotekapu_2.ApiService
import com.example.kotekapu_2.AuthManager
import com.example.kotekapu_2.Post
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    apiService: ApiService,
    authManager: AuthManager,
    organisationId: Int,
    onBack: () -> Unit,
    onEventCreated: (Post) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFormats by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Доступные опции
    val interestOptions = listOf("IT", "Искусство", "Наука", "Спорт", "Бизнес", "Языки", "Творчество")
    val formatOptions = listOf("онлайн", "офлайн", "гибрид")
    val eventTypeOptions = listOf("хакатон", "лекция", "мастер-класс", "встреча", "семинар", "воркшоп", "конференция")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создание мероприятия") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (title.isBlank() || description.isBlank() || dateTime.isBlank()) {
                        errorMessage = "Заполните обязательные поля: название, описание и дата"
                        return@ExtendedFloatingActionButton
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val token = authManager.getCurrentToken()
                            if (token != null) {
                                val result = apiService.createEvent(
                                    token = token,
                                    organisationId = organisationId,
                                    title = title,
                                    description = description,
                                    dateTime = dateTime,
                                    location = location.ifEmpty { null },
                                    eventType = eventType.ifEmpty { null },
                                    interestTags = selectedInterests.toList(),
                                    formatTags = selectedFormats.toList()
                                )

                                if (result.isSuccess) {
                                    val response = result.getOrNull()
                                    response?.event?.let { event ->
                                        onEventCreated(event)
                                    }
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Ошибка создания мероприятия"
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка сети: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                icon = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Создать")
                    }
                },
                text = { Text("Создать мероприятие") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = "Новое мероприятие",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название мероприятия *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                isError = title.isBlank()
            )

            // Поле описания
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(120.dp),
                isError = description.isBlank()
            )

            // Дата и время
            OutlinedTextField(
                value = dateTime,
                onValueChange = { dateTime = it },
                label = { Text("Дата и время *") },
                placeholder = { Text("2024-03-15T14:00:00") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                isError = dateTime.isBlank(),
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = "Дата")
                }
            )

            // Местоположение
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Местоположение") },
                placeholder = { Text("Москва, ул. Тверская, 15 или Online") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            // Тип события
            OutlinedTextField(
                value = eventType,
                onValueChange = { eventType = it },
                label = { Text("Тип события") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                trailingIcon = {
                    Icon(Icons.Default.Event, contentDescription = "Тип события")
                }
            )

            // Интересы
            Text(
                text = "Интересы",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                interestOptions.forEach { interest ->
                    FilterChip(
                        selected = selectedInterests.contains(interest),
                        onClick = {
                            selectedInterests = if (selectedInterests.contains(interest)) {
                                selectedInterests - interest
                            } else {
                                selectedInterests + interest
                            }
                        },
                        label = { Text(interest) }
                    )
                }
            }

            // Форматы
            Text(
                text = "Форматы",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                formatOptions.forEach { format ->
                    FilterChip(
                        selected = selectedFormats.contains(format),
                        onClick = {
                            selectedFormats = if (selectedFormats.contains(format)) {
                                selectedFormats - format
                            } else {
                                selectedFormats + format
                            }
                        },
                        label = { Text(format) }
                    )
                }
            }

            // Сообщение об ошибке
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}