package com.example.kotekapu_2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// Создайте файл CompletePreferencesScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletePreferencesScreen(
    apiService: ApiService,
    authManager: AuthManager,
    onPreferencesComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var selectedInterests by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFormats by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedEventTypes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<PreferencesResponse?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Загружаем категории при старте
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val result = apiService.getPreferenceCategories()
            if (result.isSuccess) {
                categories = result.getOrNull()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            0 -> "Интересы"
                            1 -> "Форматы"
                            2 -> "Типы событий"
                            else -> "Опрос"
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            // Прогресс бар
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(bottom = 24.dp)
            )

            when (currentStep) {
                0 -> InterestsStep(
                    categories = categories?.interest_categories ?: emptyList(),
                    selectedInterests = selectedInterests,
                    onSelectionChange = { selectedInterests = it }
                )
                1 -> FormatsStep(
                    formats = categories?.format_types ?: emptyList(),
                    selectedFormats = selectedFormats,
                    onSelectionChange = { selectedFormats = it }
                )
                2 -> EventTypesStep(
                    eventTypes = categories?.event_types ?: emptyList(),
                    selectedEventTypes = selectedEventTypes,
                    onSelectionChange = { selectedEventTypes = it }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Кнопки навигации
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    Button(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Назад")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        if (currentStep < 2) {
                            currentStep++
                        } else {
                            // Завершаем опрос
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val token = authManager.getCurrentToken()
                                    val userId = authManager.getCurrentUserId()

                                    if (token != null && userId != null) {
                                        val result = apiService.completePreferences(
                                            token = token,
                                            userId = userId,
                                            interests = selectedInterests.toList(),
                                            formats = selectedFormats.toList(),
                                            eventTypes = selectedEventTypes.toList()
                                        )

                                        if (result.isSuccess) {
                                            onPreferencesComplete()
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
                        }
                    },
                    enabled = when (currentStep) {
                        0 -> selectedInterests.isNotEmpty()
                        1 -> selectedFormats.isNotEmpty()
                        2 -> selectedEventTypes.isNotEmpty()
                        else -> true
                    } && !isLoading
                ) {
                    if (isLoading && currentStep == 2) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Text(if (currentStep == 2) "Готово" else "Далее")
                    }
                }
            }
        }
    }
}

@Composable
fun InterestsStep(
    categories: List<String>,
    selectedInterests: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    Column {
        Text(
            text = "Выберите самые интересные вам темы",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            items(categories) { category ->
                InterestChip(
                    text = category,
                    selected = selectedInterests.contains(category),
                    onSelectedChange = { selected ->
                        val newSelection = if (selected) {
                            selectedInterests + category
                        } else {
                            selectedInterests - category
                        }
                        onSelectionChange(newSelection)
                    }
                )
            }
        }
    }
}

@Composable
fun FormatsStep(
    formats: List<String>,
    selectedFormats: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    Column {
        Text(
            text = "Выберите предпочтительные форматы мероприятий",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        formats.forEach { format ->
            FormatChip(
                text = format,
                selected = selectedFormats.contains(format),
                onSelectedChange = { selected ->
                    val newSelection = if (selected) {
                        selectedFormats + format
                    } else {
                        selectedFormats - format
                    }
                    onSelectionChange(newSelection)
                }
            )
        }
    }
}

@Composable
fun EventTypesStep(
    eventTypes: List<String>,
    selectedEventTypes: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    Column {
        Text(
            text = "Выберите предпочтительные типы событий",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(eventTypes) { eventType ->
                EventTypeChip(
                    text = eventType,
                    selected = selectedEventTypes.contains(eventType),
                    onSelectedChange = { selected ->
                        val newSelection = if (selected) {
                            selectedEventTypes + eventType
                        } else {
                            selectedEventTypes - eventType
                        }
                        onSelectionChange(newSelection)
                    }
                )
            }
        }
    }
}

@Composable
fun InterestChip(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Card(
        onClick = { onSelectedChange(!selected) },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FormatChip(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    AssistChip(
        onClick = { onSelectedChange(!selected) },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.padding(4.dp)
    )
}

@Composable
fun EventTypeChip(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = { onSelectedChange(!selected) },
        label = { Text(text) },
        modifier = Modifier.padding(4.dp)
    )
}