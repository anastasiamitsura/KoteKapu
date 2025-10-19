import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ChipDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrganizationScreen(
    onBackClick: () -> Unit,
    onCreateClick: (OrganizationData) -> Unit
) {
    var organizationData by remember { mutableStateOf(OrganizationData()) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создание организации") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            CreateOrganizationBottomBar(
                isValid = organizationData.isValid(),
                onCreateClick = {
                    if (validateForm(organizationData).isEmpty()) {
                        onCreateClick(organizationData)
                    } else {
                        errors = validateForm(organizationData)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Заголовок
            Text(
                text = "Создайте свою организацию",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            Text(
                text = "Заполните информацию об организации. После создания она будет отправлена на модерацию.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Основная форма
            OrganizationForm(
                organizationData = organizationData,
                errors = errors,
                onDataChange = { newData ->
                    organizationData = newData
                    // Очищаем ошибки при изменении данных
                    if (errors.isNotEmpty()) {
                        errors = validateForm(newData)
                    }
                }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun OrganizationForm(
    organizationData: OrganizationData,
    errors: Map<String, String>,
    onDataChange: (OrganizationData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Название организации
        OutlinedTextField(
            value = organizationData.name,
            onValueChange = { newValue ->
                onDataChange(organizationData.copy(name = newValue))
            },
            label = { Text("Название организации *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errors.containsKey("name"),
            supportingText = {
                if (errors.containsKey("name")) {
                    Text(text = errors["name"]!!)
                }
            },
            trailingIcon = {
                if (organizationData.name.isNotBlank()) {
                    Icon(
                        imageVector = if (errors.containsKey("name")) Icons.Default.Warning
                        else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (errors.containsKey("name")) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        // Краткое описание
        OutlinedTextField(
            value = organizationData.description,
            onValueChange = { newValue ->
                onDataChange(organizationData.copy(description = newValue))
            },
            label = { Text("Краткое описание *") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            isError = errors.containsKey("description"),
            supportingText = {
                if (errors.containsKey("description")) {
                    Text(text = errors["description"]!!)
                } else {
                    Text("${organizationData.description.length}/500")
                }
            }
        )

        // Город
        OutlinedTextField(
            value = organizationData.city,
            onValueChange = { newValue ->
                onDataChange(organizationData.copy(city = newValue))
            },
            label = { Text("Город *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errors.containsKey("city"),
            supportingText = {
                if (errors.containsKey("city")) {
                    Text(text = errors["city"]!!)
                }
            },
            trailingIcon = {
                IconButton(onClick = { /* TODO: Открыть поиск городов */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Выбрать город")
                }
            }
        )

        // Теги/направления деятельности
        TagsSection(
            selectedTags = organizationData.tags,
            onTagsChange = { newTags ->
                onDataChange(organizationData.copy(tags = newTags))
            },
            isError = errors.containsKey("tags")
        )

        // Социальные сети и сайт
        SocialLinksSection(
            socialLinks = organizationData.socialLinks,
            onSocialLinksChange = { newLinks ->
                onDataChange(organizationData.copy(socialLinks = newLinks))
            }
        )

        // Загрузка аватарки
        AvatarUploadSection(
            onAvatarSelected = { /* TODO: Обработка выбора изображения */ }
        )

        // Информация о модерации
        ModerationInfoCard()
    }
}

@Composable
fun TagsSection(
    selectedTags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    isError: Boolean
) {
    val availableTags = listOf(
        "Технологии и Инновации",
        "Искусство и Культура",
        "Наука и Просвещение",
        "Карьера и Бизнес",
        "Здоровье и Спорт",
        "Волонтерство и Благотворительность",
        "Языки и Путешествия",
        "Гейминг и Киберспорт",
        "Медиа и Блогинг",
        "Общество и Урбанистика"
    )

    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Направления деятельности *",
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Выбранные теги
        if (selectedTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                selectedTags.forEach { tag ->
                    AssistChip(
                        onClick = {
                            onTagsChange(selectedTags - tag)
                        },
                        label = {
                            Text(tag, style = MaterialTheme.typography.labelSmall)
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Удалить",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // Кнопка добавления тегов
        OutlinedButton(
            onClick = { isExpanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить направления")
        }

        if (isError) {
            Text(
                text = "Выберите хотя бы одно направление",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Диалог выбора тегов
        if (isExpanded) {
            AlertDialog(
                onDismissRequest = { isExpanded = false },
                title = { Text("Выберите направления") },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(availableTags) { tag ->
                            val isSelected = selectedTags.contains(tag)
                            Card(
                                onClick = {
                                    val newTags = if (isSelected) {
                                        selectedTags - tag
                                    } else {
                                        selectedTags + tag
                                    }
                                    onTagsChange(newTags)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle
                                        else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isExpanded = false }) {
                        Text("Готово")
                    }
                }
            )
        }
    }
}

@Composable
fun SocialLinksSection(
    socialLinks: SocialLinks,
    onSocialLinksChange: (SocialLinks) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ссылки на соцсети и сайт",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowBack
                        else Icons.Default.Add,
                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть"
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = socialLinks.website,
                    onValueChange = { newValue ->
                        onSocialLinksChange(socialLinks.copy(website = newValue))
                    },
                    label = { Text("Веб-сайт") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = socialLinks.vk,
                    onValueChange = { newValue ->
                        onSocialLinksChange(socialLinks.copy(vk = newValue))
                    },
                    label = { Text("VK") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = socialLinks.telegram,
                    onValueChange = { newValue ->
                        onSocialLinksChange(socialLinks.copy(telegram = newValue))
                    },
                    label = { Text("Telegram") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Send, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
fun AvatarUploadSection(onAvatarSelected: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Аватарка организации",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .clickable(onClick = onAvatarSelected),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Добавить аватарку",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Нажмите чтобы добавить аватарку",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Рекомендуемый размер: 500x500 px",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ModerationInfoCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Информация",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "После создания организация будет отправлена на модерацию. " +
                        "Вы получите уведомление о результате проверки.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateOrganizationBottomBar(
    isValid: Boolean,
    onCreateClick: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Создать организацию",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "После проверки модератором",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onCreateClick,
                enabled = isValid,
                modifier = Modifier.width(140.dp)
            ) {
                Text("Создать")
            }
        }
    }
}

// Функции валидации
private fun validateForm(data: OrganizationData): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (data.name.isBlank()) {
        errors["name"] = "Введите название организации"
    } else if (data.name.length < 3) {
        errors["name"] = "Название должно содержать минимум 3 символа"
    }

    if (data.description.isBlank()) {
        errors["description"] = "Введите описание организации"
    } else if (data.description.length < 20) {
        errors["description"] = "Описание должно содержать минимум 20 символов"
    } else if (data.description.length > 500) {
        errors["description"] = "Описание не должно превышать 500 символов"
    }

    if (data.city.isBlank()) {
        errors["city"] = "Введите город"
    }

    if (data.tags.isEmpty()) {
        errors["tags"] = "Выберите хотя бы одно направление"
    }

    return errors
}

@Preview(showBackground = true)
@Composable
fun CreateOrganizationScreenPreview() {
    MaterialTheme {
        CreateOrganizationScreen(
            onBackClick = {},
            onCreateClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TagsSectionPreview() {
    MaterialTheme {
        TagsSection(
            selectedTags = listOf("Технологии и Инновации", "Наука и Просвещение"),
            onTagsChange = {},
            isError = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SocialLinksSectionPreview() {
    MaterialTheme {
        SocialLinksSection(
            socialLinks = SocialLinks(
                website = "https://example.com",
                vk = "vk.com/example"
            ),
            onSocialLinksChange = {}
        )
    }
}