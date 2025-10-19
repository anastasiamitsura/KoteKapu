import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.forEachIndexed
import kotlin.collections.toMutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    organization: Organization,
    onBackClick: () -> Unit,
    onCreateClick: (EventData) -> Unit
) {
    var eventData by remember { mutableStateOf(EventData()) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var currentStep by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    val steps = listOf("Основное", "Детали", "Дополнительно")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создание мероприятия") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Text(
                        text = "${organization.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        bottomBar = {
            CreateEventBottomBar(
                currentStep = currentStep,
                totalSteps = steps.size,
                onPreviousClick = { if (currentStep > 0) currentStep-- },
                onNextClick = {
                    val stepErrors = validateStep(currentStep, eventData)
                    if (stepErrors.isEmpty()) {
                        if (currentStep < steps.size - 1) {
                            currentStep++
                        } else {
                            onCreateClick(eventData)
                        }
                    } else {
                        errors = stepErrors
                    }
                },
                isLastStep = currentStep == steps.size - 1
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Шаги
            StepIndicator(
                steps = steps,
                currentStep = currentStep,
                modifier = Modifier.padding(16.dp)
            )

            when (currentStep) {
                0 -> BasicInfoStep(
                    eventData = eventData,
                    errors = errors,
                    onDataChange = { newData ->
                        eventData = newData
                        if (errors.isNotEmpty()) errors = validateStep(0, newData)
                    }
                )
                1 -> DetailsStep(
                    eventData = eventData,
                    errors = errors,
                    onDataChange = { newData ->
                        eventData = newData
                        if (errors.isNotEmpty()) errors = validateStep(1, newData)
                    }
                )
                2 -> AdditionalInfoStep(
                    eventData = eventData,
                    errors = errors,
                    onDataChange = { newData ->
                        eventData = newData
                        if (errors.isNotEmpty()) errors = validateStep(2, newData)
                    }
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StepIndicator(
    steps: List<String>,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / steps.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Шаг ${currentStep + 1} из ${steps.size}: ${steps[currentStep]}",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun BasicInfoStep(
    eventData: EventData,
    errors: Map<String, String>,
    onDataChange: (EventData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Основная информация",
            style = MaterialTheme.typography.headlineSmall
        )

        // Название мероприятия
        OutlinedTextField(
            value = eventData.title,
            onValueChange = { newValue ->
                onDataChange(eventData.copy(title = newValue))
            },
            label = { Text("Название мероприятия *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errors.containsKey("title"),
            supportingText = {
                if (errors.containsKey("title")) {
                    Text(text = errors["title"]!!)
                }
            }
        )

        // Описание
        OutlinedTextField(
            value = eventData.description,
            onValueChange = { newValue ->
                onDataChange(eventData.copy(description = newValue))
            },
            label = { Text("Описание мероприятия *") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            isError = errors.containsKey("description"),
            supportingText = {
                if (errors.containsKey("description")) {
                    Text(text = errors["description"]!!)
                } else {
                    Text("${eventData.description.length}/1000")
                }
            }
        )

        // Формат мероприятия
        Text(
            text = "Формат мероприятия *",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventFormat.entries.forEach { format ->
                val isSelected = eventData.format == format
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onDataChange(eventData.copy(format = format))
                    },
                    label = { Text(format.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (errors.containsKey("format")) {
            Text(
                text = errors["format"]!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun DetailsStep(
    eventData: EventData,
    errors: Map<String, String>,
    onDataChange: (EventData) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Детали мероприятия",
            style = MaterialTheme.typography.headlineSmall
        )

        // Дата и время
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Дата
            OutlinedTextField(
                value = eventData.date,
                onValueChange = { },
                label = { Text("Дата *") },
                modifier = Modifier.weight(1f),
                readOnly = true,
                isError = errors.containsKey("date"),
                supportingText = {
                    if (errors.containsKey("date")) {
                        Text(text = errors["date"]!!)
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
                    }
                }
            )

            // Время
            OutlinedTextField(
                value = eventData.time,
                onValueChange = { },
                label = { Text("Время *") },
                modifier = Modifier.weight(1f),
                readOnly = true,
                isError = errors.containsKey("time"),
                supportingText = {
                    if (errors.containsKey("time")) {
                        Text(text = errors["time"]!!)
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Выбрать время")
                    }
                }
            )
        }

        // Локация (для офлайн мероприятий)
        if (eventData.format != EventFormat.ONLINE) {
            OutlinedTextField(
                value = eventData.location,
                onValueChange = { newValue ->
                    onDataChange(eventData.copy(location = newValue))
                },
                label = { Text("Место проведения *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errors.containsKey("location"),
                supportingText = {
                    if (errors.containsKey("location")) {
                        Text(text = errors["location"]!!)
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { /* TODO: Открыть карту */ }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Выбрать на карте")
                    }
                }
            )
        }

        // Ссылка (для онлайн мероприятий)
        if (eventData.format != EventFormat.OFFLINE) {
            OutlinedTextField(
                value = eventData.onlineLink,
                onValueChange = { newValue ->
                    onDataChange(eventData.copy(onlineLink = newValue))
                },
                label = { Text("Ссылка для подключения *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errors.containsKey("onlineLink"),
                supportingText = {
                    if (errors.containsKey("onlineLink")) {
                        Text(text = errors["onlineLink"]!!)
                    }
                }
            )
        }

        // Максимальное количество участников
        OutlinedTextField(
            value = if (eventData.maxParticipants > 0) eventData.maxParticipants.toString() else "",
            onValueChange = { newValue ->
                val participants = newValue.toIntOrNull() ?: 0
                onDataChange(eventData.copy(maxParticipants = participants))
            },
            label = { Text("Макс. участников") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = {
                Text("Оставьте пустым для неограниченного количества")
            }
        )

        // Цена
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = if (eventData.price > 0) eventData.price.toString() else "",
                onValueChange = { newValue ->
                    val price = newValue.toIntOrNull() ?: 0
                    onDataChange(eventData.copy(price = price))
                },
                label = { Text("Цена (руб)") },
                modifier = Modifier.weight(2f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Цена")
                }
            )

            // Бесплатно чекбокс
            Card(
                onClick = {
                    onDataChange(eventData.copy(price = 0))
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (eventData.price == 0)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (eventData.price == 0) Icons.Default.CheckCircle
                        else Icons.Default.CheckCircle,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Бесплатно")
                }
            }
        }
    }
}

@Composable
fun AdditionalInfoStep(
    eventData: EventData,
    errors: Map<String, String>,
    onDataChange: (EventData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Дополнительная информация",
            style = MaterialTheme.typography.headlineSmall
        )

        // Теги мероприятия
        EventTagsSection(
            selectedTags = eventData.tags,
            onTagsChange = { newTags ->
                onDataChange(eventData.copy(tags = newTags))
            },
            isError = errors.containsKey("tags")
        )

        // Расписание
        ScheduleSection(
            schedule = eventData.schedule,
            onScheduleChange = { newSchedule ->
                onDataChange(eventData.copy(schedule = newSchedule))
            }
        )

        // Дополнительная информация
        OutlinedTextField(
            value = eventData.additionalInfo,
            onValueChange = { newValue ->
                onDataChange(eventData.copy(additionalInfo = newValue))
            },
            label = { Text("Дополнительная информация") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            supportingText = {
                Text("Любая дополнительная информация для участников")
            }
        )

        // Настройки видимости
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Настройки мероприятия",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Открыть регистрацию сразу")
                    Switch(
                        checked = eventData.registrationOpen,
                        onCheckedChange = { newValue ->
                            onDataChange(eventData.copy(registrationOpen = newValue))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EventTagsSection(
    selectedTags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    isError: Boolean
) {
    val availableTags = listOf(
        "Хакатон", "Лекция", "Мастер-класс", "Воркшоп", "Митап",
        "Конференция", "Семинар", "Тренинг", "Выставка", "Концерт",
        "Соревнование", "Фестиваль", "Нетворкинг", "Дискуссия", "Экскурсия"
    )

    Column {
        Text(
            text = "Тип мероприятия *",
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableTags.forEach { tag ->
                val isSelected = selectedTags.contains(tag)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newTags = if (isSelected) {
                            selectedTags - tag
                        } else {
                            selectedTags + tag
                        }
                        onTagsChange(newTags)
                    },
                    label = { Text(tag) }
                )
            }
        }

        if (isError) {
            Text(
                text = "Выберите хотя бы один тип мероприятия",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ScheduleSection(
    schedule: List<ScheduleItems>,
    onScheduleChange: (List<ScheduleItems>) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Расписание мероприятия",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowBack
                        else Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                if (schedule.isEmpty()) {
                    Button(
                        onClick = {
                            val newItem = ScheduleItems("", "", "")
                            onScheduleChange(schedule + newItem)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить пункт расписания")
                    }
                } else {
                    schedule.forEachIndexed { index, item ->
                        ScheduleItemEditor(
                            item = item,
                            onItemChange = { newItem ->
                                val newSchedule = schedule.toMutableList()
                                newSchedule[index] = newItem
                                onScheduleChange(newSchedule)
                            },
                            onRemove = {
                                onScheduleChange(schedule - item)
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val newItem = ScheduleItems("", "", "")
                            onScheduleChange(schedule + newItem)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить еще")
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleItemEditor(
    item: ScheduleItems,
    onItemChange: (ScheduleItems) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Пункт расписания", style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = item.time,
                onValueChange = { newValue ->
                    onItemChange(item.copy(time = newValue))
                },
                label = { Text("Время") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Например: 10:00-11:00") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = item.title,
                onValueChange = { newValue ->
                    onItemChange(item.copy(title = newValue))
                },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = item.description,
                onValueChange = { newValue ->
                    onItemChange(item.copy(description = newValue))
                },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun CreateEventBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    isLastStep: Boolean
) {
    Surface(tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка назад
            if (currentStep > 0) {
                OutlinedButton(onClick = onPreviousClick) {
                    Text("Назад")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Прогресс
            Text(
                text = "${currentStep + 1}/$totalSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Кнопка вперед/создать
            Button(onClick = onNextClick) {
                Text(if (isLastStep) "Создать мероприятие" else "Далее")
            }
        }
    }
}

private fun validateStep(step: Int, data: EventData): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    when (step) {
        0 -> {
            if (data.title.isBlank()) {
                errors["title"] = "Введите название мероприятия"
            } else if (data.title.length < 5) {
                errors["title"] = "Название должно содержать минимум 5 символов"
            }

            if (data.description.isBlank()) {
                errors["description"] = "Введите описание мероприятия"
            } else if (data.description.length < 50) {
                errors["description"] = "Описание должно содержать минимум 50 символов"
            } else if (data.description.length > 1000) {
                errors["description"] = "Описание не должно превышать 1000 символов"
            }
        }
        1 -> {
            if (data.date.isBlank()) {
                errors["date"] = "Выберите дату мероприятия"
            }

            if (data.time.isBlank()) {
                errors["time"] = "Выберите время мероприятия"
            }

            if (data.format == EventFormat.OFFLINE && data.location.isBlank()) {
                errors["location"] = "Введите место проведения"
            }

            if (data.format == EventFormat.ONLINE && data.onlineLink.isBlank()) {
                errors["onlineLink"] = "Введите ссылку для подключения"
            }
        }
        2 -> {
            if (data.tags.isEmpty()) {
                errors["tags"] = "Выберите тип мероприятия"
            }
        }
    }

    return errors
}

@Preview(showBackground = true)
@Composable
fun CreateEventScreenPreview() {
    MaterialTheme {
        CreateEventScreen(
            organization = Organization("1", "IT Community"),
            onBackClick = {},
            onCreateClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BasicInfoStepPreview() {
    MaterialTheme {
        BasicInfoStep(
            eventData = EventData(),
            errors = emptyMap(),
            onDataChange = {}
        )
    }
}