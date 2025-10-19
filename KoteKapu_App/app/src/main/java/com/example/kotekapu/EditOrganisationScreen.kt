import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrganizationScreen(
    organization: Organization,
    onBackClick: () -> Unit,
    onSaveClick: (Organization) -> Unit,
    onAvatarChange: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var editedOrganization by remember { mutableStateOf(organization) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование организации") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val validationErrors = validateOrganization(editedOrganization)
                            if (validationErrors.isEmpty()) {
                                onSaveClick(editedOrganization)
                            } else {
                                errors = validationErrors
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Сохранить")
                    }
                }
            )
        },
        bottomBar = {
            if (organization.status == OrganizationStatus.ACTIVE) {
                EditOrganizationBottomBar(
                    onDeleteClick = { showDeleteDialog = true }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Статус организации
            OrganizationStatusCard(
                organization = editedOrganization,
                modifier = Modifier.padding(16.dp)
            )

            // Аватар и основная информация
            OrganizationHeaderSection(
                organization = editedOrganization,
                errors = errors,
                onOrganizationChange = { newOrg ->
                    editedOrganization = newOrg
                    if (errors.isNotEmpty()) errors = validateOrganization(newOrg)
                },
                onAvatarChange = onAvatarChange,
                modifier = Modifier.padding(16.dp)
            )

            // Описание и теги
            DescriptionAndTagsSection(
                organization = editedOrganization,
                errors = errors,
                onOrganizationChange = { newOrg ->
                    editedOrganization = newOrg
                    if (errors.isNotEmpty()) errors = validateOrganization(newOrg)
                },
                modifier = Modifier.padding(16.dp)
            )

            // Контактная информация
            ContactInfoSection(
                organization = editedOrganization,
                onOrganizationChange = { newOrg ->
                    editedOrganization = newOrg
                },
                modifier = Modifier.padding(16.dp)
            )

            // Социальные сети
            SocialLinksSection(
                organization = editedOrganization,
                onOrganizationChange = { newOrg ->
                    editedOrganization = newOrg
                },
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Диалог удаления организации
    if (showDeleteDialog) {
        DeleteOrganizationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDeleteClick()
            },
            organizationName = organization.name
        )
    }
}

@Composable
fun OrganizationStatusCard(
    organization: Organization,
    modifier: Modifier = Modifier
) {
    val statusInfo = when (organization.status) {
        OrganizationStatus.ACTIVE -> Pair("Организация активна", MaterialTheme.colorScheme.primary)
        OrganizationStatus.PENDING -> Pair("На проверке модератором", MaterialTheme.colorScheme.secondary)
        OrganizationStatus.REJECTED -> Pair("Отклонена модератором", MaterialTheme.colorScheme.error)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = statusInfo.second.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (organization.status) {
                    OrganizationStatus.ACTIVE -> Icons.Default.CheckCircle
                    OrganizationStatus.PENDING -> Icons.Default.DateRange
                    OrganizationStatus.REJECTED -> Icons.Default.Warning
                },
                contentDescription = null,
                tint = statusInfo.second
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusInfo.first,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusInfo.second
                )
                if (organization.status == OrganizationStatus.REJECTED && organization.rejectionReason.isNotEmpty()) {
                    Text(
                        text = "Причина: ${organization.rejectionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OrganizationHeaderSection(
    organization: Organization,
    errors: Map<String, String>,
    onOrganizationChange: (Organization) -> Unit,
    onAvatarChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Аватар
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (organization.avatarUrl != null) {
                        // TODO: Загрузка изображения с Coil/Glide
                        Text(
                            text = "Лого",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = organization.name.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = onAvatarChange,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Изменить логотип")
                    }

                    Text(
                        text = "Рекомендуемый размер: 500x500 px",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Название организации
            OutlinedTextField(
                value = organization.name,
                onValueChange = { newValue ->
                    onOrganizationChange(organization.copy(name = newValue))
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
                leadingIcon = {
                    Icon(Icons.Default.Build, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Город
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = organization.city,
                    onValueChange = { newValue ->
                        onOrganizationChange(organization.copy(city = newValue))
                    },
                    label = { Text("Город *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = errors.containsKey("city"),
                    supportingText = {
                        if (errors.containsKey("city")) {
                            Text(text = errors["city"]!!)
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    }
                )

                IconButton(
                    onClick = { /* TODO: Открыть выбор города */ },
                    modifier = Modifier
                        .height(56.dp)
                        .width(56.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Выбрать город")
                }
            }
        }
    }
}

@Composable
fun DescriptionAndTagsSection(
    organization: Organization,
    errors: Map<String, String>,
    onOrganizationChange: (Organization) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Описание и направления",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Описание
        OutlinedTextField(
            value = organization.description,
            onValueChange = { newValue ->
                onOrganizationChange(organization.copy(description = newValue))
            },
            label = { Text("Описание организации *") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            isError = errors.containsKey("description"),
            supportingText = {
                if (errors.containsKey("description")) {
                    Text(text = errors["description"]!!)
                } else {
                    Text("${organization.description.length}/1000")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Теги/направления деятельности
        OrganizationTagsSection(
            selectedTags = organization.tags,
            onTagsChange = { newTags ->
                onOrganizationChange(organization.copy(tags = newTags))
            },
            isError = errors.containsKey("tags")
        )
    }
}

@Composable
fun OrganizationTagsSection(
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
fun ContactInfoSection(
    organization: Organization,
    onOrganizationChange: (Organization) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Контактная информация",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Email
        OutlinedTextField(
            value = organization.contactEmail,
            onValueChange = { newValue ->
                onOrganizationChange(organization.copy(contactEmail = newValue))
            },
            label = { Text("Контактный email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            supportingText = {
                Text("Для связи с организацией")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Телефон
        OutlinedTextField(
            value = organization.contactPhone,
            onValueChange = { newValue ->
                onOrganizationChange(organization.copy(contactPhone = newValue))
            },
            label = { Text("Контактный телефон") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null)
            },
            supportingText = {
                Text("Для связи с организацией")
            }
        )
    }
}

@Composable
fun SocialLinksSection(
    organization: Organization,
    onOrganizationChange: (Organization) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Социальные сети и сайт",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.AddCircle
                        else Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Веб-сайт
                OutlinedTextField(
                    value = organization.website,
                    onValueChange = { newValue ->
                        onOrganizationChange(organization.copy(website = newValue))
                    },
                    label = { Text("Веб-сайт") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Call, contentDescription = null)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // VK
                OutlinedTextField(
                    value = organization.vkLink,
                    onValueChange = { newValue ->
                        onOrganizationChange(organization.copy(vkLink = newValue))
                    },
                    label = { Text("VK") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Telegram
                OutlinedTextField(
                    value = organization.telegramLink,
                    onValueChange = { newValue ->
                        onOrganizationChange(organization.copy(telegramLink = newValue))
                    },
                    label = { Text("Telegram") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Send, contentDescription = null)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Instagram
                OutlinedTextField(
                    value = organization.instagramLink,
                    onValueChange = { newValue ->
                        onOrganizationChange(organization.copy(instagramLink = newValue))
                    },
                    label = { Text("Instagram") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
fun EditOrganizationBottomBar(
    onDeleteClick: () -> Unit
) {
    Surface(tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Опасная зона",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onDeleteClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Удалить организацию")
            }
        }
    }
}

@Composable
fun DeleteOrganizationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    organizationName: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удаление организации") },
        text = {
            Column {
                Text("Вы уверены, что хотите удалить организацию \"$organizationName\"?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Это действие нельзя отменить. Все мероприятия организации также будут удалены.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun validateOrganization(organization: Organization): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (organization.name.isBlank()) {
        errors["name"] = "Введите название организации"
    } else if (organization.name.length < 3) {
        errors["name"] = "Название должно содержать минимум 3 символа"
    }

    if (organization.description.isBlank()) {
        errors["description"] = "Введите описание организации"
    } else if (organization.description.length < 20) {
        errors["description"] = "Описание должно содержать минимум 20 символов"
    } else if (organization.description.length > 1000) {
        errors["description"] = "Описание не должно превышать 1000 символов"
    }

    if (organization.city.isBlank()) {
        errors["city"] = "Введите город"
    }

    if (organization.tags.isEmpty()) {
        errors["tags"] = "Выберите хотя бы одно направление"
    }

    if (organization.contactEmail.isNotBlank() && !isValidEmail(organization.contactEmail)) {
        errors["contactEmail"] = "Введите корректный email"
    }

    if (organization.contactPhone.isNotBlank() && !isValidPhone(organization.contactPhone)) {
        errors["contactPhone"] = "Введите корректный номер телефона"
    }

    return errors
}

private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
    return email.matches(emailRegex.toRegex())
}

private fun isValidPhone(phone: String): Boolean {
    val phoneRegex = "^[+]?[0-9]{10,15}\$"
    return phone.matches(phoneRegex.toRegex())
}

@Preview(showBackground = true)
@Composable
fun EditOrganizationScreenPreview() {
    MaterialTheme {
        EditOrganizationScreen(
            organization = sampleOrganization,
            onBackClick = {},
            onSaveClick = {},
            onAvatarChange = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OrganizationStatusCardPreview() {
    MaterialTheme {
        Column {
            OrganizationStatusCard(
                organization = sampleOrganization.copy(status = OrganizationStatus.ACTIVE)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OrganizationStatusCard(
                organization = sampleOrganization.copy(
                    status = OrganizationStatus.REJECTED,
                    rejectionReason = "Несоответствие правилам платформы"
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrganizationTagsSectionPreview() {
    MaterialTheme {
        OrganizationTagsSection(
            selectedTags = listOf("Технологии и Инновации", "Наука и Просвещение"),
            onTagsChange = {},
            isError = false
        )
    }
}